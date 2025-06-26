package dfa;

import dfa.gen.DfaConfig;
import js.base.BasePrinter;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static js.base.Tools.*;

public final class Util {

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
   * Return a copy of a state machine, one whose edges are labelled with a disjoint subset of codes
   */
  public static State partitionEdges(State startState) {
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
            new_edges.add(newEdge(state, prev_label.elements(), prev_dest));
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

  public static State validateDFA(State startState) {
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
    return startState;
  }

  public static final String EXT_RXP = "rxp" //
      , EXT_DFA = "dfa" //
      ;

  public static final int FTYPE_JAVA = 0 //
      , FTYPE_RUST = 1 //
      ;

  public static final double DFA_VERSION_5 = 5.1;

  public static final int MAX_TOKEN_DEF = 1_000;

  public static String versionString(float v) {
    return String.format("%.1f", v);
  }

  public static DfaConfig dfaConfig() {
    if (sDfaConfig == null)
      setConfig(DfaConfig.DEFAULT_INSTANCE);
    return sDfaConfig;
  }

  public static void setConfig(DfaConfig c) {
    sDfaConfig = c.build();
  }

  private static DfaConfig sDfaConfig;

  public static DFA getDfa() {
    if (sDFA == null) {
      sDFA = DFA.parse(Files.readString(TokenDefinitionParser.class, "rexp_parser.dfa"));
      return sDFA;
    }
    return sDFA;
  }

  private static DFA sDFA;

  /**
   * Get a description of a DFA; for development purposes only
   *
   * What we really could use though is a decompiler that takes a DFA (in json form)
   * and generates the States and whatnot, for this describe method to work with
   */
  public static JSMap describe(List<State> states, List<String> tokenNames) {
    var m = map();
    for (var s : states) {
      var stateKey = "" + s.debugId();
      var altKey = stateKey + "*";

      if (m.containsKey(stateKey) || m.containsKey(altKey)) {
        m.putNumbered(stateKey, "*** duplicate state id ***");
        continue;
      }
      var edgeMap = map();
      m.put(s.finalState() ? altKey : stateKey, edgeMap);
      for (var edge : s.edges()) {
        var ds = edge.destinationState();
        String edgeKey =
            ds.finalState() ? "*  " : String.format("%3d", edge.destinationState().debugId());
        if (edgeMap.containsKey(edgeKey))
          edgeMap.put("**ERR** " + edge.destinationState().debugId(), "duplicate destination state");
        else {
          edgeMap.putUnsafe(edgeKey, edgeDescription(edge, tokenNames));
        }
      }
    }
    return m;
  }

  private static Object edgeProblem(Edge edge, String message) {
    return "*** problem with edge: " + message + " ***; " + JSList.with(edge.codeSets());
  }

  /**
   * Get a description of an edge's code sets, for display as a map key
   */
  private static Object edgeDescription(Edge edge, List<String> tokenNames) {
    var cs = edge.codeSets();
    if (cs.length % 2 != 0) {
      return edgeProblem(edge, "odd number of elements");
    }
    StringBuilder sb = new StringBuilder();
    for (var i = 0; i < cs.length; i += 2) {
      var a = cs[i];
      var b = cs[i + 1];
      if ((a < 0 != b < 0) || (a >= b)) {
        return edgeProblem(edge, "illegal code set");
      }
      if (a == 0)
        return edgeProblem(edge, "illegal code set");
      if (a < 0) {
        if (b != a + 1 || cs.length != 2) {
          return edgeProblem(edge, "unexpected token id expr");
        }
        var tokenId = -b - 1;
        if (tokenId < 0 || tokenId >= tokenNames.size()) {
          return "*** no such token id: " + tokenId;
        }
        return "<" + tokenNames.get(tokenId) + ">";
      }

      if (b == a + 1) {
        append(sb, charExpr(a));
        continue;
      }

      var maxRun = (b == 256) ? 2 : 5;
      int skipStart = 1000;
      int skipEnd = 1000;
      if (b - a > 2 * maxRun + 4) {
        skipStart = a + maxRun;
        skipEnd = b - 1 - maxRun;
      }

      for (int j = a; j < b; j++) {
        if (j < skipStart || j > skipEnd) {
          append(sb, charExpr(j));
        } else if (j == skipStart) {
          append(sb, "...");
        }
      }
    }
    return sb.toString();
  }

  private static void append(StringBuilder sb, Object expr) {
    if (sb.length() > 0 && sb.charAt(sb.length() - 1) > ' ')
      sb.append(' ');
    sb.append(expr);
  }

  /**
   * Construct a string representation of a character code
   */
  private static String charExpr(int n) {
    switch (n) {
      case 0x0a:
        return "_LF";
      case 0x09:
        return "_HT";
      case 0x0d:
        return "_CR";
      case 0x20:
        return "_SP";
      default:
        if (n > 32 && n < 128) {
          return Character.toString((char) n);
        }
        return String.format("%02x", n);
    }
  }

}
