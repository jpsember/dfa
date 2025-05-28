package dfa;

import static js.base.Tools.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import js.base.BasePrinter;

public final class ToknUtils {

  private static OurEdge newEdge(OurState sourceState, int[] codeSet, OurState destinationState) {
    return new OurEdge(sourceState, codeSet, destinationState);
  }

  public static void addEdge(OurState sourceState, int[] codeSet, OurState destinationState) {
    sourceState.edges().add(new OurEdge(sourceState, codeSet, destinationState));
  }

  public static void addEdge(OurState sourceState, CodeSet codeSet, OurState destinationState) {
    addEdge(sourceState, codeSet.elements(), destinationState);
  }

  /**
   * Build set of states reachable from this state
   */
  public static List<OurState> reachableStates(OurState sourceState) {
    Set<OurState> knownStatesSet = hashSet();
    List<OurState> stack = arrayList();
    List<OurState> output = arrayList();
    push(stack, sourceState);
    knownStatesSet.add(sourceState);

    while (nonEmpty(stack)) {
      OurState state = pop(stack);
      output.add(state);
      for (OurEdge edge : state.edges()) {
        OurState dest = edge.destinationState();
        if (knownStatesSet.add(dest))
          push(stack, dest);
      }
    }
    return output;
  }

  /**
   * Construct the reverse of an NFA
   * 
   * @param startState
   *          start state for NFA
   * @return start state of reversed NFA
   */
  public static OurState reverseNFA(OurState startState) {
    OurState.bumpDebugIds();

    // Create new start state first, so it has the lowest id
    OurState newStartState = new OurState();

    List<OurState> newStartStateList = arrayList();
    List<OurState> newFinalStateList = arrayList();

    StateRenamer newStateMap = new StateRenamer();

    List<OurState> stateSet = reachableStates(startState);

    for (OurState s : stateSet) {
      OurState newState = newStateMap.put(s, new OurState(s == startState));
      if (newState.finalState())
        newFinalStateList.add(newState);
      if (s.finalState())
        newStartStateList.add(newState);
    }

    for (OurState oldState : stateSet) {
      OurState newState = newStateMap.get(oldState);
      for (OurEdge oldEdge : oldState.edges()) {
        OurState oldDest = oldEdge.destinationState();
        OurState newDest = newStateMap.get(oldDest);
        // We want a reversed edge
        addEdge(newDest, oldEdge.codeSets(), newState);
      }
    }

    //  Make start node point to each of the reversed start nodes

    for (OurState s : newStartStateList)
      addEps(newStartState, s);
    return newStartState;
  }

  /**
   * Duplicate the NFA reachable from a state
   * 
   * @param origToDupStateMap
   *          where to construct map of original state ids to new states
   */
  public static StatePair duplicateNFA(OurState startState, OurState endState) {

    Map<OurState, OurState> origToDupStateMap = hashMap();

    List<OurState> oldStates = reachableStates(startState);
    checkState(oldStates.contains(endState), "end state not reachable");

    for (OurState s : oldStates) {
      OurState s2 = new OurState(s.finalState(), null);
      origToDupStateMap.put(s, s2);
    }

    for (OurState s : oldStates) {
      OurState s2 = origToDupStateMap.get(s);
      for (OurEdge edge : s.edges()) {
        OurState newTargetState = origToDupStateMap.get(edge.destinationState());
        addEdge(s2, edge.codeSets(), newTargetState);
      }
    }
    return statePair(origToDupStateMap.get(startState), origToDupStateMap.get(endState));
  }

  private static int[] EPSILON_RANGE = { OurState.EPSILON, 1 + OurState.EPSILON };

  /**
   * Add an epsilon transition to a state
   */
  public static void addEps(OurState source, OurState target) {
    addEdge(source, EPSILON_RANGE, target);
  }

  public static StatePair statePair(OurState start, OurState end) {
    checkNotNull(start);
    checkNotNull(end);
    StatePair sp = new StatePair();
    sp.start = start;
    sp.end = end;
    return sp;
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
    if (charCode == OurState.EPSILON)
      return "(e)";
    if (charCode > ' ' && charCode < 0x7f && forbidden.indexOf(charCode) < 0)
      return "'" + Character.toString((char) charCode) + "'";
    if (charCode == OurState.CODEMAX - 1)
      return "MAX";
    return Integer.toString(charCode);
  }

  public static String dumpStateMachine(OurState initialState, Object... title) {
    final var dashes = "--------------------------------------------------------------------------\n";
    StringBuilder sb = new StringBuilder();
//    sb.append(dashes);
    sb.append("\nState Machine\n");
    sb.append(dashes);
    
    if (title.length != 0) {
      sb.append(" : ");
      sb.append(BasePrinter.toString(title));
    }
    sb.append('\n');

    List<OurState> reachableStates = reachableStates(initialState);

    // Sort them by their debug ids
    reachableStates.sort(null);

    // But make sure the start state is first
    sb.append(toString(initialState, true));
    for (OurState s : reachableStates) {
      if (s == initialState)
        continue;
      sb.append(toString(s, true));
    }
    sb.append(dashes);
    return sb.toString();
  }

  public static String toString(OurState state, boolean includeEdges) {
    StringBuilder sb = new StringBuilder();
    sb.append(state.debugId());
    sb.append(state.finalState() ? '*' : ' ');
    if (includeEdges) {
      sb.append("=>\n");
      for (OurEdge e : state.edges()) {
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
  public static OurState partitionEdges(OurState startState) {
    RangePartition par = new RangePartition();

    StateRenamer ren = new StateRenamer();
    ren.constructNewVersions(startState);

    for (OurState s : ren.oldStates()) {
      for (OurEdge edge : s.edges())
        par.addSet(CodeSet.with(edge.codeSets()));
    }

    for (OurState s : ren.oldStates()) {
      OurState sNew = ren.get(s);
      for (OurEdge edge : s.edges()) {
        List<CodeSet> newLbls = par.apply(CodeSet.with(edge.codeSets()));
        for (CodeSet x : newLbls) {
          addEdge(sNew, x, ren.get(edge.destinationState()));
        }
      }
    }
    return ren.get(startState);
  }

  public static OurState normalizeStates(OurState startState) {
    StateRenamer renamer = new StateRenamer();
    renamer.constructNewVersionsWithEdges(startState);
    for (OurState oldState : renamer.oldStates()) {
      OurState newState = renamer.get(oldState);
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
   * 
   */
  private static void normalizeState(OurState state) {
    // Sort edges by destination state ids
    state.edges()
        .sort((e1, e2) -> Integer.compare(e1.destinationState().debugId(), e2.destinationState().debugId()));

    List<OurEdge> new_edges = arrayList();
    CodeSet prev_label = null;
    OurState prev_dest = null;

    for (OurEdge edge : state.edges()) {
      int[] label = edge.codeSets();
      OurState dest = edge.destinationState();

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
        new_edges.add(new OurEdge(prev_label.elements(), prev_dest));
    }

    state.setEdges(new_edges);
  }

  /**
   * Determine if a state machine can get from one state to another while
   * consuming no input
   */
  public static boolean acceptsEmptyString(OurState stateA, OurState stateB) {
    Set<OurState> markedStates = hashSet();
    List<OurState> stateStack = arrayList();
    push(stateStack, stateA);
    while (nonEmpty(stateStack)) {
      OurState state = pop(stateStack);
      if (markedStates.contains(state))
        continue;
      markedStates.add(state);
      if (state == stateB)
        return true;

      for (OurEdge edge : state.edges()) {
        if (CodeSet.contains(edge.codeSets(), OurState.EPSILON))
          push(stateStack, edge.destinationState());
      }
    }
    return false;
  }

  private static String toString(OurEdge edge) {
    StringBuilder sb = new StringBuilder();
    sb.append(dumpCodeSet(edge.codeSets()));
    sb.append(" => ");
    sb.append(edge.destinationState().debugId());
    return sb.toString();
  }

  static {
    BasePrinter.registerClassHandler(OurEdge.class, (x, p) -> p.append(toString((OurEdge) x)));
  }

  public static void validateDFA(OurState startState) {
    for (OurState s : reachableStates(startState)) {
      CodeSet prevSet = new CodeSet();
      for (OurEdge e : s.edges()) {
        if (CodeSet.contains(e.codeSets(), OurState.EPSILON))
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
