package dfa;

import static js.base.Tools.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import js.base.BasePrinter;

public final class ToknUtils {

  private static Edge newEdge(State sourceState, int[] codeSet, State destinationState) {
    return new Edge(sourceState, codeSet, destinationState);
  }

  public static void addEdge(State sourceState, int[] codeSet, State destinationState) {
    sourceState.edges().add(new Edge(sourceState, codeSet, destinationState));
  }

  public static void addEdge(State sourceState, CodeSet codeSet, State destinationState) {
    addEdge(sourceState, codeSet.elements(), destinationState);
  }

  /**
   * Build set of states reachable from this state
   */
  public static List<State> reachableStates(State sourceState) {
    Set<State> knownStatesSet = hashSet();
    List<State> stack = arrayList();
    List<State> output = arrayList();
    push(stack, sourceState);
    knownStatesSet.add(sourceState);

    while (nonEmpty(stack)) {
      State state = pop(stack);
      output.add(state);
      for (Edge edge : state.edges()) {
        State dest = edge.destinationState();
        if (knownStatesSet.add(dest))
          push(stack, dest);
      }
    }
    return output;
  }

  /**
   * Construct the reverse of an NFA
   */
  public static State reverseNFA(State startState) {
    State.bumpIds();

    // Create new start state first, so it has the lowest id
    State newStartState = new State();

    List<State> newStartStateList = arrayList();
    List<State> newFinalStateList = arrayList();

    StateRenamer newStateMap = new StateRenamer();

    List<State> stateSet = reachableStates(startState);

    for (State s : stateSet) {
      State newState = newStateMap.put(s, new State(s == startState));
      if (newState.finalState())
        newFinalStateList.add(newState);
      if (s.finalState())
        newStartStateList.add(newState);
    }

    for (State oldState : stateSet) {
      State newState = newStateMap.get(oldState);
      for (Edge oldEdge : oldState.edges()) {
        State oldDest = oldEdge.destinationState();
        State newDest = newStateMap.get(oldDest);
        // We want a reversed edge
        addEdge(newDest, oldEdge.codeSets(), newState);
      }
    }

    //  Make start node point to each of the reversed start nodes

    for (State s : newStartStateList)
      addEps(newStartState, s);
    return newStartState;
  }

  /**
   * Duplicate the NFA reachable from a state
   */
  public static NFA duplicateNFA(State startState, State endState) {

    Map<State, State> origToDupStateMap = hashMap();

    List<State> oldStates = reachableStates(startState);
    checkState(oldStates.contains(endState), "end state not reachable");

    for (State s : oldStates) {
      State s2 = new State(s.finalState(), null);
      origToDupStateMap.put(s, s2);
    }

    for (State s : oldStates) {
      State s2 = origToDupStateMap.get(s);
      for (Edge edge : s.edges()) {
        State newTargetState = origToDupStateMap.get(edge.destinationState());
        addEdge(s2, edge.codeSets(), newTargetState);
      }
    }
    return nfa(origToDupStateMap.get(startState), origToDupStateMap.get(endState));
  }

  private static int[] EPSILON_RANGE = {State.EPSILON, 1 + State.EPSILON};

  /**
   * Add an epsilon transition to a state
   */
  public static void addEps(State source, State target) {
    addEdge(source, EPSILON_RANGE, target);
  }

  public static NFA nfa(State start, State end) {
    return new NFA(start, end);
  }

  public static String dumpCodeSet(int[] elements) {
    checkArgument((elements.length & 1) == 0);

    StringBuilder sb = new StringBuilder("{");
    int i = 0;
    while (i < elements.length) {
      if (i > 0)
        sb.append(' ');

      int lower = elements[i];
      int upper = elements[i + 1];
      sb.append(elementToString(lower));
      if (upper != 1 + lower) {
        sb.append("..");
        sb.append(elementToString(upper - 1));
      }
      i += 2;
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * Get a debug description of a value within a CodeSet
   */
  private static String elementToString(int charCode) {
    final String forbidden = "\'\"\\[]{}()";
    // Unless it corresponds to a non-confusing printable ASCII value,
    // just print its decimal equivalent
    if (charCode == State.EPSILON)
      return "(e)";
    if (charCode > ' ' && charCode < 0x7f && forbidden.indexOf(charCode) < 0)
      return "'" + Character.toString((char) charCode) + "'";
    if (charCode == State.CODEMAX - 1)
      return "MAX";
    return Integer.toString(charCode);
  }

  public static String dumpStateMachine(State initialState, Object... title) {
    final var dashes = "--------------------------------------------------------------------------\n";
    StringBuilder sb = new StringBuilder();
    sb.append("\nState Machine\n");
    sb.append(dashes);

    if (title.length != 0) {
      sb.append(" : ");
      sb.append(BasePrinter.toString(title));
    }
    sb.append('\n');

    List<State> reachableStates = reachableStates(initialState);

    // Sort them by their debug ids
    reachableStates.sort(null);

    // But make sure the start state is first
    sb.append(toString(initialState, true));
    for (State s : reachableStates) {
      if (s == initialState)
        continue;
      sb.append(toString(s, true));
    }
    sb.append(dashes);
    return sb.toString();
  }

  public static String toString(State state, boolean includeEdges) {
    StringBuilder sb = new StringBuilder();
    sb.append(state.debugId());
    sb.append(state.finalState() ? '*' : ' ');
    if (includeEdges) {
      sb.append("=>\n");
      for (Edge e : state.edges()) {
        sb.append("       ");
        sb.append(e.destinationState().debugId());
        sb.append(' ');
        sb.append(dumpCodeSet(e.codeSets()));
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  /**
   * Modify a state machine's edges so each is labelled with a disjoint subset
   * of characters.
   */
  public static State partitionEdges(State startState) {

    alert("Does this return a new state machine, or does it actually modify the provided one?");
    RangePartition par = new RangePartition();

    StateRenamer ren = new StateRenamer();
    ren.constructNewVersions(startState);

    for (State s : ren.oldStates()) {
      for (Edge edge : s.edges())
        par.addSet(CodeSet.with(edge.codeSets()));
    }

    for (State s : ren.oldStates()) {
      State sNew = ren.get(s);
      for (Edge edge : s.edges()) {
        List<CodeSet> newLbls = par.apply(CodeSet.with(edge.codeSets()));
        for (CodeSet x : newLbls) {
          addEdge(sNew, x, ren.get(edge.destinationState()));
        }
      }
    }
    return ren.get(startState);
  }

  public static State normalizeStates(State startState) {
    StateRenamer renamer = new StateRenamer();
    renamer.constructNewVersionsWithEdges(startState);
    for (State oldState : renamer.oldStates()) {
      State newState = renamer.get(oldState);
      normalizeState(newState);
    }
    return renamer.get(startState);
  }

  /**
   * Normalize a state
   *
   * [] merge edges that go to a common state
   *
   * [] sort edges by destination state debug ids
   *
   * [] delete edges that have empty labels
   */
  private static void normalizeState(State state) {
    // Sort edges by destination state ids
    state.edges()
        .sort((e1, e2) -> Integer.compare(e1.destinationState().debugId(), e2.destinationState().debugId()));

    List<Edge> new_edges = arrayList();
    CodeSet prev_label = null;
    State prev_dest = null;

    for (Edge edge : state.edges()) {
      int[] label = edge.codeSets();
      State dest = edge.destinationState();

      // If this edge goes to the same state as the previous one (they are in sorted order already), merge with that one...
      if (prev_dest == dest)
        prev_label.addSet(label);
      else {
        if (prev_dest != null) {
          // Omit edges with no labels
          if (prev_label.elements().length != 0)
            new_edges.add(ToknUtils.newEdge(state, prev_label.elements(), prev_dest));
        }
        // Must start a fresh copy!  Don't want to modify the original label.
        prev_label = CodeSet.with(label);
        prev_dest = edge.destinationState();
      }
    }

    if (prev_dest != null) {
      // Omit edges with no labels
      if (prev_label.elements().length != 0)
        new_edges.add(new Edge(prev_label.elements(), prev_dest));
    }

    state.setEdges(new_edges);
  }

  /**
   * Determine if a state machine can get from one state to another while
   * consuming no input
   */
  public static boolean acceptsEmptyString(State stateA, State stateB) {
    Set<State> markedStates = hashSet();
    List<State> stateStack = arrayList();
    push(stateStack, stateA);
    while (nonEmpty(stateStack)) {
      State state = pop(stateStack);
      if (markedStates.contains(state))
        continue;
      markedStates.add(state);
      if (state == stateB)
        return true;

      for (Edge edge : state.edges()) {
        if (CodeSet.contains(edge.codeSets(), State.EPSILON))
          push(stateStack, edge.destinationState());
      }
    }
    return false;
  }

  private static String toString(Edge edge) {
    StringBuilder sb = new StringBuilder();
    sb.append(dumpCodeSet(edge.codeSets()));
    sb.append(" => ");
    sb.append(edge.destinationState().debugId());
    return sb.toString();
  }

  static {
    BasePrinter.registerClassHandler(Edge.class, (x, p) -> p.append(toString((Edge) x)));
  }

  public static void validateDFA(State startState) {
    for (State s : reachableStates(startState)) {
      CodeSet prevSet = new CodeSet();
      for (Edge e : s.edges()) {
        if (CodeSet.contains(e.codeSets(), State.EPSILON))
          badArg("edge accepts epsilon:", INDENT, toString(s, true));

        // See if the code set intersects union of previous edges' code sets
        CodeSet ours = CodeSet.with(e.codeSets());
        CodeSet inter = ours.intersect(prevSet);
        if (!inter.isEmpty())
          badArg("multiple edges on inputs:", inter, INDENT, toString(s, true));
        prevSet.addSet(ours);
      }
    }
  }

}
