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

    List<Integer> frontier = arrayList();
    Map<Integer, State> stateMap = hashMap();
    frontier.add(productId(a2.startState, b2.startState));


    while (!frontier.isEmpty()) {
      var pid = pop(frontier);
      if (stateMap.containsKey(pid))
        continue;

      // Determine the two factor states

      var aid = pid % MAX_STATE_ID;
      var bid = pid / MAX_STATE_ID;

      var fa = a2.states.get(aid);
      var fb = b2.states.get(bid);

      // get the set of labels of the two states

      todo("it would be convenient if the edges stored their code sets as CodeSet objects instead of primitive arrays");
      Set<CodeSet> labelSet = hashSet();
      for (var e:fa.edges()) {
//        e.
//        e.
      }


    }
    int aSize = a2.states.size();
    for (int i = 0; i < aSize; i++) {

    }

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
