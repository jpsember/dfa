package dfa;

import js.base.BaseObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static js.base.Tools.*;
import static dfa.Util.*;

public class BinaryOper extends BaseObject {

  public static NFA aMinusB(NFA a, NFA b) {
    var oper = new BinaryOper(a, b);
    throw notSupported("aMinusB", oper);
  }

  private static long encodeFactorIds(int a, int b) {
    return a | (((long) b) << 32);
  }

  private static int factorAId(long encoded) {
    return (int) encoded;
  }

  private static int factorBId(long encoded) {
    return (int) (encoded >> 32);
  }


  // a pair of ids is stored in a long
  private Map<Integer, Long> productToFactorIdsMap = hashMap();
  private Map<Long, State> factorsToProductStateMap = hashMap();
  private Map<Integer, State> idToStateMap = hashMap();

  // We also have a search frontier, which is a stack of unexamined product states.

  private List<State> frontier = arrayList();
  private Map<Long, State> generatedStates = hashMap();

  private void addProductStateToMap(State productState, State factorA, State factorB) {
    generatedStates.put(encodeFactorIds(factorA.id(), factorB.id()), productState);
  }

  private void extendFrontier(State state) {
    frontier.add(state);
  }

  private State popFrontier() {
    return pop(frontier);
  }

  //  private void clearDataStructures() {
//    productToFactorIdsMap.clear();
//    factorsToProductStateMap.clear();
//    frontier.clear();
//    generatedStates.clear();
//  }
  private State getFactorAState(State productState) {
    // TODO: we are calling this method twice, once for A and once for B.
    var encoded = helperGetFactorState(productState);
    return stateForId(factorAId(encoded));
  }

  private State getFactorBState(State productState) {
    var encoded = helperGetFactorState(productState);
    return stateForId(factorBId(encoded));
  }

  private long helperGetFactorState(State productState) {
    var encoded = productToFactorIdsMap.get(productState.id());
    checkNotNull(encoded, "can't find product state in factor ids map:", productState.id());
    return encoded;
  }

  private State stateForId(int id) {
    var state = idToStateMap.get(id);
    checkNotNull(state, "no state found for id:", id);
    return state;
  }

  private BinaryOper(NFA a, NFA b) {
    setVerbose();

    var a2 = toDFA("A", a);
    var b2 = toDFA("B", b);
    log(a2);
    log(b2);


    // Partition the edge labels into disjoint codesets,
    // and construct new versions of the reachable states

    todo("There is some duplicated code here, where RangePartition was also used");
    {
      RangePartition par = new RangePartition();
      StateRenamer ren = new StateRenamer();
      log("constructing new versions for (a) start state:", a2.startState.id());
      ren.constructNewVersions(a2.startState);
      log("constructing new versions for (b) start state:", b2.startState.id());
      ren.constructNewVersions(b2.startState);

      for (State s : ren.oldStates()) {
        for (Edge edge : s.edges())
          par.addSet(edge.codeSet());
      }

      for (State s : ren.oldStates()) {
        State sNew = ren.get(s);
        for (Edge edge : s.edges()) {
          List<CodeSet> newLbls = par.apply(edge.codeSet());
          for (CodeSet x : newLbls) {
            addEdge(sNew, x, ren.get(edge.destinationState()));
          }
        }
      }

      a2.startState = ren.get(a2.startState);
      b2.startState = ren.get(b2.startState);
    }

    // !!!! we need to reconstruct the AugDFA since we've constructed new versions by the partitioning above

    // Use the AugDFA state list (including the sink state) as the number of states, for a map index


    // construct the product NFA of these two.

    // fun (id of product state) -> [factor state a, factor state b]

    // a pair of ids is stored in a long
//    Map<Integer, Long> productToFactorIdsMap = hashMap();
//    Map<Long, State> factorsToProductStateMap = hashMap();

    // We also have a search frontier, which is a stack of unexamined product states.

//    List<State> frontier = arrayList();
//    Map<Long, State> generatedStates = hashMap();

    mStartState = constructProductState(a2.startState, b2.startState);
    addProductStateToMap(mStartState, a2.startState, b2.startState);

//    private void addProductStateToMap(State productState, State factorA, State factorB) {
//
//    }
    // Store it in the map, and add it to the frontier as the initial search state
    // generatedStates.put(productId(a2.startState, b2.startState), mStartState);
    extendFrontier(mStartState);
    mEndState = new State();

    Set<CodeSet> workCodeSets = hashSet();

    log("processing frontier");

    // Continue searching until the frontier is empty
    //
    while (!frontier.isEmpty()) {
      var productState = popFrontier();
      log("frontier state:", productState);

      // Determine the two factor states
      var fa = getFactorAState(productState);
      var fb = getFactorBState(productState);
//
//      var aid = productState.id() % MAX_STATE_ID;
//      var bid = productState.id() / MAX_STATE_ID;
//
//      var fa = a2.states.get(aid);
//      var fb = b2.states.get(bid);

      log("...factor states:", INDENT, fa, CR, fb);

      // Construct the set of labels that appear in either of the edges.
      // Construct an edge for each label in the set, sending to the sink state(s) where appropriate.

      workCodeSets.clear();
      for (var x : fa.edges()) workCodeSets.add(x.codeSet());
      for (var x : fb.edges()) workCodeSets.add(x.codeSet());

      log("...code sets:", INDENT, workCodeSets);
      for (var x : workCodeSets) {
        log(".......code set:", x);
        // determine target states for this label
        var aTarget = a2.sinkState;
        for (var y : fa.edges()) {
          if (x.equals(y.codeSet())) {
            aTarget = y.destinationState();
            break;
          }
        }
        var bTarget = b2.sinkState;
        for (var y : fb.edges()) {
          if (x.equals(y.codeSet())) {
            bTarget = y.destinationState();
            break;
          }
        }


        var destProductId = productId(aTarget, bTarget);
        log(".......dest product id:", destProductId);

        var destProductState = generatedStates.get(destProductId);
        if (destProductState == null) {
          destProductState = constructProductState(aTarget, bTarget);
          frontier.add(destProductState);
          generatedStates.put(destProductId, destProductState);
          log(".........new product state, adding to frontier");
        }

        // Add edge to product graph

        var edge = new Edge(x, destProductState);
        productState.edges().add(edge);
        log("...added edge to product state:", INDENT, productState, ":", edge);
      }
    }

    // for each product state that has been marked as a final state,
    // clear that flag, and add an epsilon edge to the end state
    for (var ps : generatedStates.values()) {
      if (ps.finalState()) {
        ps.setFinal(false);
        ps.edges().add(new Edge(CodeSet.epsilon(), mEndState));
      }
    }

    pr(VERT_SP, "state machine:", INDENT,
        dumpStateMachine(mStartState, "Product state machine"));
  }

  private State mStartState;
  private State mEndState;

  private State constructProductState(State a, State b) {
    // Set final state according to the binary operation
    todo("Assuming MINUS operation");
    boolean finalState = a.finalState() && !b.finalState();

    var s = new State(finalState);
    log("...constructed product state:", s.id(), "final:", s.finalState());
    return s;
  }

  private static long productId(State a, State b) {
    var aId = a.id();
    var bId = b.id();
    return aId + (((long) bId) << 32L);
  }

  /**
   * Convert NFA to a DFA, and add a sink non-final state
   */
  private AugDFA toDFA(String label, NFA nfa) {
    // We have to make the end state a final state
    nfa.end.setFinal(true);

    log("toDFA:", INDENT, dumpStateMachine(nfa.start, "NFA to convert to DFA"));

    var xDFA = NFAToDFA.convert(nfa.start);
    log(dumpStateMachine(xDFA, "x as DFA"));

    var sinkState = new State();
    var xAug = new AugDFA(label + " (aug)", xDFA, sinkState);
    return xAug;
  }


  private static class AugDFA {

    AugDFA(String label, State startState, State sinkState) {
      this.label = label;
      this.startState = startState;
      this.sinkState = sinkState;

      // Construct a list of states, including the sink state, and renumber them all.

      states = reachableStates(startState);
      states.add(sinkState);

      var index = INIT_INDEX;
      for (var s : states) {
        index++;
        s.setId(index);
      }
    }

    State startState;
    State sinkState;
    String label;
    List<State> states;

    @Override
    public String toString() {
      return dumpStateMachine(startState, label + " (sink: " + sinkState.id() + ")");
    }
  }
}
