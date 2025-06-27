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
    throw notSupported("aMinusB");
  }

  private static final int MAX_STATE_ID = 30_000;

  private BinaryOper(NFA a, NFA b) {
    setVerbose();

    var a2 = toDFA("A", a, 100);
    var b2 = toDFA("B", b, 200);
    pr(a2);
    pr(b2);

    // construct the product NFA of these two.

    // We keep a map of visited states.

    List<State> frontier = arrayList();
    Map<Integer, State> stateMap = hashMap();

    todo("assign consecutive new ids to the product states,but not their indices");
    
    var prodStart = constructProductState(a2.startState, b2.startState);
    frontier.add(prodStart);
    var productEndState = new State(false);
    productEndState.setId(MAX_STATE_ID);


    Set<CodeSet> workCodeSets = hashSet();

    while (!frontier.isEmpty()) {
      var productState = pop(frontier);
      if (stateMap.containsKey(productState.id()))
        continue;
      stateMap.put(productState.id(), productState);

      // Determine the two factor states

      var aid = productState.id() % MAX_STATE_ID;
      var bid = productState.id() / MAX_STATE_ID;

      var fa = a2.states.get(aid);
      var fb = b2.states.get(bid);

      // Construct the set of labels that appear in either of the edges.
      // Construct an edge for each label in the set, sending to the sink state(s) where appropriate.

      workCodeSets.clear();
      for (var x : fa.edges()) workCodeSets.add(x.codeSet());
      for (var x : fb.edges()) workCodeSets.add(x.codeSet());
      for (var x : workCodeSets) {
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

        int destProductId = productId(aTarget, bTarget);
        var destProductState = stateMap.get(destProductId);
        if (destProductState == null) {
          destProductState = constructProductState(aTarget, bTarget);
          frontier.add(destProductState);
        }

        // Add edge to product graph

        productState.edges().add(
            new Edge(x, destProductState)
        );
      }
    }

    // for each product state that has been marked as a final state,
    // clear that flag, and add an epsilon edge to the end state
    for (var ps : stateMap.values()) {
      if (ps.finalState()) {
        ps.setFinal(false);
        ps.edges().add(new Edge(CodeSet.epsilon(), productEndState));
      }
    }
    mEndState = productEndState;
    mStartState = prodStart;


  }

  private State mStartState;
  private State mEndState;

  private State constructProductState(State a, State b) {
    todo("Have State constructor that takes an id");
    int destProductId = productId(a, b);

    // Set final state according to the binary operation
    todo("Assuming MINUS operation");
    boolean finalState = a.finalState() && !b.finalState();
    var s = new State(finalState, arrayList());
    s.setId(destProductId);
    return s;
  }

  private static int productId(State a, State b) {
    var aId = a.id();
    var bId = b.id();

    checkArgument(Math.max(aId, bId) < MAX_STATE_ID, "state ids are too high");
    return aId + bId * MAX_STATE_ID;
  }

  /**
   * Convert NFA to a DFA, and add a sink non-final state
   */
  private AugDFA toDFA(String label, NFA nfa, int newInitialStateId) {
    todo("we have to be careful with the state debug ids... do they need to be unique across state machines?");
    // We have to make the end state a final state
    nfa.end.setFinal(true);

    log("toDFA:", INDENT, dumpStateMachine(nfa.start, "NFA to convert to DFA"));

    var xDFA = NFAToDFA.convert(nfa.start);
    log(dumpStateMachine(xDFA, "x as DFA"));

    var sinkState = new State();
    var xAug = new AugDFA(label + " (aug)", xDFA, sinkState, newInitialStateId);
    return xAug;
  }


  private static class AugDFA {

    AugDFA(String label, State startState, State sinkState, int initialStateId) {
      this.label = label;
      this.startState = startState;
      this.sinkState = sinkState;

      // Construct a list of states, including the sink state, and renumber them all.

      states = reachableStates(startState);
      states.add(sinkState);
      int id = initialStateId;
      for (var s : states) {
        s.setId(id);
        id++;
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
