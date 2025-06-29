package dfa;

import js.base.BaseObject;
import js.base.Pair;

import static js.base.Tools.*;
import static dfa.Util.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Set;
import java.util.TreeSet;

/**
 * Converts NFAs (nondeterministic, finite state automata) to minimal DFAs.
 *
 * Performs the subset construction algorithm described in (among other places)
 * http://en.wikipedia.org/wiki/Powerset_construction
 *
 * Also implements an innovative algorithm to partition a set of edge labels
 * into a set that has the property that no two elements have overlapping
 * regions. This allows us to perform the subset construction (and closure
 * operations) efficiently while supporting large possible character sets (e.g.,
 * unicode, which ranges from 0..0x10ffff. See RangePartition.rb for more
 * details.
 */
final class NFAToDFA extends BaseObject {

  public static State convert(State nfaStartState) {
    var converter = new NFAToDFA();
    var dfaStartState = converter.convertNFAToDFA(nfaStartState);
    return dfaStartState;
  }

  /**
   * Convert an NFA to a DFA; return the new start state
   */
  private State convertNFAToDFA(State start) {
    partitionEdges(start);

    // Reverse this NFA, convert to DFA, then reverse it, and convert it again.  
    // Apparently this  produces a minimal DFA.
    //
    log("reversing #1");
    start = reverseNFA(start);
    if (verbose())
      log(stateMachineToString(start, "after reverse #1"));

    start = cvtNFAToDFA(start);

    if (verbose())
      log("reversing #2");
    start = reverseNFA(start);
    if (verbose())
      log(stateMachineToString(start, "after reverse #2"));
    start = cvtNFAToDFA(start);
    return normalizeStates(start);
  }

  private static CodeSet constructKeyForStateCollection(Collection<State> states) {
    CodeSet keySet = new CodeSet();
    for (State s : states)
      keySet.add(s.id());
    return keySet;
  }

  /**
   * Convert NFA to DFA
   */
  private State cvtNFAToDFA(State start) {
    log("---------- cvtNFAToDFA -------------");
    mNFAStateSetToDFAStateMap.clear();
    mDFAStateToNFAStatesMap.clear();

    log("creating start state");
    start = create_dfa_state_if_necessary(eps_closure(start)).first;

    List<State> unmarked = arrayList();
    unmarked.add(start);

    while (nonEmpty(unmarked)) {
      State dfaState = pop(unmarked);

      Collection<State> nfaStateSubset = mDFAStateToNFAStatesMap.get(dfaState);
      if (nfaStateSubset == null)
        badState("dfaState had no entry in sorted_nfa_state_id_lists:", dfaState);

      if (verbose())
        log("popped DFA state:", dfaState, "with NFA states:", State.toString(nfaStateSubset));

      // Map of CodeSet => set of NFA states
      // 
      // The sets should be TreeSets, for deterministic results
      //
      Map<CodeSet, TreeSet<State>> moveMap = treeMap();

      for (State nfaState : nfaStateSubset) {
        log("...processing NFA state:", nfaState);

        for (Edge nfaEdge : nfaState.edges()) {
          log("......edge:", nfaEdge);

          CodeSet codeSet = nfaEdge.codeSet();

          // This CodeSet is guaranteed to not overlap any other (distinct) CodeSet.
          // Add the destination state to a list keyed to this CodeSet.

          // If the code set contains epsilon, we can assume it contains only epsilon
          // (because of the edge partitioning we did earlier);
          // and we can ignore it (as any state reachable from here lies within the
          // NFA subset we are processing)
          if (codeSet.contains(State.EPSILON))
            continue;

          TreeSet<State> nfaStates = moveMap.get(codeSet);
          if (nfaStates == null) {
            nfaStates = treeSet();
            moveMap.put(codeSet, nfaStates);
          }
          nfaStates.add(nfaEdge.destinationState());
          if (verbose())
            log("adding state:", nfaEdge.destinationState().id(), "to the list corresponding to CodeSet",
                codeSet);
        }
      }

      // Process each CodeSet->[NFA State] mapping, and generate a DFA state for the [NFA State] subset
      // (if none exists)

      for (Entry<CodeSet, TreeSet<State>> moveMapEntry : moveMap.entrySet()) {
        CodeSet codeSet = moveMapEntry.getKey();
        Set<State> nfaStates = moveMapEntry.getValue();
        log("processing map entry", codeSet, "=>", nfaStates);
        eps_closure(nfaStates);
        log("NFA states e-closure:", nfaStates);

        var result =
            create_dfa_state_if_necessary(nfaStates);
        State dfaDestState = result.first;
        log("DFA state for this set of NFA states:", dfaDestState);
        if (result.second) {
          log("...this was a new DFA state; marking it for exploration");
          unmarked.add(dfaDestState);
        }
        if (verbose())
          log(VERT_SP, "...adding DFA edge", dfaState, codeSet, "==>", dfaDestState, VERT_SP);
        addEdge(dfaState, codeSet, dfaDestState);
      }
    }
    if (verbose())
      log(stateMachineToString(start, "after nfa -> dfa conversion"));
    return start;
  }

  /**
   * Determine if a DFA state exists for a set of NFA states, and add one if
   * not.
   *
   * Returns DFA state, and true if state did not already exist
   */
  private Pair<State, Boolean> create_dfa_state_if_necessary(Collection<State> stateSet) {
    boolean createdFlag = false;

    CodeSet keySet = constructKeyForStateCollection(stateSet);
    if (verbose())
      log("create_dfa_state_if_nec?", keySet);

    State newState = mNFAStateSetToDFAStateMap.get(keySet);
    if (verbose())
      log("...existing state:", newState);

    if (newState == null) {
      createdFlag = true;
      newState = new State();
      // Determine if any of the NFA states were final states
      for (State nfaState : stateSet)
        if (nfaState.finalState()) {
          newState.setFinal(true);
          break;
        }
      mNFAStateSetToDFAStateMap.put(keySet, newState);
      mDFAStateToNFAStatesMap.put(newState, stateSet);
      if (verbose())
        log("...stored new DFA state:", newState.toString(true));
    }
    return pair(newState, createdFlag);
  }


  /**
   * Calculate the epsilon closure of a set of NFA states
   */
  private Set<State> eps_closure(Set<State> stateSet) {
    List<State> stk = arrayList();
    stk.addAll(stateSet);
    while (nonEmpty(stk)) {
      State s = pop(stk);
      for (Edge edge : s.edges()) {
        if (edge.contains(State.EPSILON)) {
          if (stateSet.add(edge.destinationState()))
            push(stk, edge.destinationState());
        }
      }
    }
    return stateSet;
  }

  private Set<State> eps_closure(State state) {
    // The set should be a TreeSet, so the order is deterministic (based on state's debug ids)
    Set<State> set = treeSet();
    set.add(state);
    return eps_closure(set);
  }

  // A map of NFA id sets to NFA states.
  // Each NFA id set is represented by a CodeSet, since they support equals+hashcode methods
  //
  private final Map<CodeSet, State> mNFAStateSetToDFAStateMap = hashMap();

  // Map of { DFA State -> [NFA state] }
  //
  private final Map<State, Collection<State>> mDFAStateToNFAStatesMap = hashMap();

}
