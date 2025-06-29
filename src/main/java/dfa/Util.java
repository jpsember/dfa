package dfa;

import js.base.BasePrinter;
import js.data.ByteArray;
import js.data.IntArray;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;

import java.util.*;

import static js.base.Tools.*;

public final class Util {

  private static Edge newEdge(CodeSet codeSet, State destinationState) {
    return new Edge(codeSet, destinationState);
  }

  public static void addEdge(State sourceState, CodeSet codeSet, State destinationState) {
    sourceState.edges().add(new Edge(codeSet, destinationState));
  }

  /**
   * Build list of states reachable from a particular source state.
   * The first state in the list will be the source state
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
    // Create new start state first, so it has the lowest id
    State newStartState = new State();

    List<State> newStartStateList = arrayList();
    List<State> newFinalStateList = arrayList();

    var newStateMap = new StateRenamer();

    List<State> stateSet = reachableStates(startState);

    for (State oldState : stateSet) {
      var newState = newStateMap.addOldToNew(oldState, null);
      // The new state's final flag is only true iff the old state was the *start* state
      newState.setFinal(oldState == startState);
      if (newState.finalState())
        newFinalStateList.add(newState);
      if (oldState.finalState())
        newStartStateList.add(newState);
    }

    for (State oldState : stateSet) {
      State newState = newStateMap.newStateForOld(oldState);
      for (Edge oldEdge : oldState.edges()) {
        State oldDest = oldEdge.destinationState();
        State newDest = newStateMap.newStateForOld(oldDest);
        // We want a reversed edge
        addEdge(newDest, oldEdge.codeSet(), newState);
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
    var renamer = new StateRenamer();
    renamer.createRenamedVersions(startState, true);
    var newEndState = renamer.newStateForOld(endState);
    if (newEndState == null)
      throw badArg("endState is not reachable from startState");
    return nfa(renamer.newStateForOld(startState), newEndState);
  }

  private static final CodeSet EPSILON_RANGE = CodeSet.withRange(State.EPSILON, 1 + State.EPSILON);

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
    final String forbidden = "'\"\\[]{}()";
    // Unless it corresponds to a non-confusing printable ASCII value,
    // just print its decimal equivalent
    if (charCode == State.EPSILON)
      return "(e)";
    if (charCode > ' ' && charCode < 0x7f && forbidden.indexOf(charCode) < 0)
      return "'" + (char) charCode + "'";
    if (charCode == State.CODEMAX - 1)
      return "MAX";
    return Integer.toString(charCode);
  }

  public static void printStateMachine(State initialState, Object... title) {
    pr(stateMachineToString(initialState, title));
  }

  public static String stateMachineToString(State initialState, Object... title) {
    final var dashes = "--------------------------------------------------------------------------\n";
    StringBuilder sb = new StringBuilder();
    sb.append("\nState Machine");
    if (title.length != 0) {
      sb.append(": ");
      sb.append(BasePrinter.toString(title));
    }
    sb.append('\n');
    sb.append(dashes);
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
    sb.append(state.id());
    sb.append(state.finalState() ? '*' : ' ');
    if (includeEdges) {
      sb.append("=>\n");
      for (Edge e : state.edges()) {
        sb.append("       ");
        sb.append(e.destinationState().id());
        sb.append(' ');
        sb.append(dumpCodeSet(e.labels()));
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  /**
   * Return a copy of a state machine, one whose edges are labelled with a disjoint subset of codes
   */
  public static void partitionEdges(State startState) {
    RangePartition par = new RangePartition();
    var reachable = reachableStates(startState);
    par.addStateCodeSets(reachable);
    par.apply(reachable);
  }

  public static State normalizeStates(State startState) {
    var oldStartState = startState;
    var renamer = new StateRenamer();
    var oldStates = renamer.createRenamedVersions(oldStartState, true);
    for (var oldState : oldStates) {
      var newState = renamer.newStateForOld(oldState);
      normalizeState(newState);
    }
    return renamer.newStateForOld(oldStartState);
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
        .sort(Comparator.comparingInt(e -> e.destinationState().id()));
    List<Edge> new_edges = arrayList();
    CodeSet prev_label = null;
    State prev_dest = null;

    for (Edge edge : state.edges()) {
      int[] label = edge.codeSet().elements();
      State dest = edge.destinationState();

      // If this edge goes to the same state as the previous one (they are in sorted order already), merge with that one...
      if (prev_dest == dest)
        prev_label.addSet(label);
      else {
        if (prev_dest != null) {
          // Omit edges with no labels
          if (prev_label.elements().length != 0)
            new_edges.add(newEdge(prev_label, prev_dest));
        }
        // Must start a fresh copy!  Don't want to modify the original label.
        prev_label = CodeSet.with(label);
        prev_dest = edge.destinationState();
      }
    }

    if (prev_dest != null) {
      // Omit edges with no labels
      if (prev_label.elements().length != 0)
        new_edges.add(new Edge(prev_label, prev_dest));
    }

    if (alert("verifying")) {
      int x = new_edges.size();
      for (int a = 0; a < x - 1; a++) {
        var ea = new_edges.get(a);
        for (int b = a + 1; b < x; b++) {
          var eb = new_edges.get(b);
          if (ea.destinationState() == eb.destinationState()) {
            die("failed to merge states:", INDENT, ea, CR, eb);
          }
        }
      }
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
        if (edge.contains(State.EPSILON))
          push(stateStack, edge.destinationState());
      }
    }
    return false;
  }

  private static String toString(Edge edge) {
    StringBuilder sb = new StringBuilder();
    sb.append(dumpCodeSet(edge.labels()));
    sb.append(" => ");
    sb.append(edge.destinationState().id());
    return sb.toString();
  }

  static {
    BasePrinter.registerClassHandler(Edge.class, (x, p) -> p.append(toString((Edge) x)));
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

  public static DFA getDfa() {
    if (sDFA == null) {
      sDFA = DFA.parse(Files.readString(TokenDefinitionParser.class, "rexp_parser.dfa"));
      return sDFA;
    }
    return sDFA;
  }

  private static DFA sDFA;

  public static JSMap describe(DFA dfa) {
    var states = decompileDFA(dfa);
    return describe(states, Arrays.asList(dfa.tokenNames()));
  }

  /**
   * Get a description of a DFA; for development purposes only
   *
   * What we really could use though is a decompiler that takes a DFA (in json form)
   * and generates the States and whatnot, for this describe method to work with
   */
  public static JSMap describe(List<State> states, List<String> tokenNames) {
    int idAdjust = -states.get(0).id() + 100;
    var m = map();
    for (var s : states) {
      if (s.finalState()) continue;
      var stateKey = "" + (idAdjust + s.id());
      if (m.containsKey(stateKey)) {
        m.putNumbered(stateKey, "*** duplicate state id ***");
        continue;
      }
      var edgeMap = map();
      m.put(stateKey, edgeMap);
      int probCounter = 0;
      for (var edge : s.edges()) {
        var ds = edge.destinationState();
        if (ds.finalState()) continue;
        String edgeKey =
            ds.finalState() ? "*  " : String.format("%3d", idAdjust + edge.destinationState().id());
        if (edgeMap.containsKey(edgeKey)) {
          edgeMap.putUnsafe(edgeKey + "!" + (++probCounter), edgeDescription(edge, tokenNames));
          edgeMap.put("**ERR** " + (idAdjust + edge.destinationState().id()), "duplicate destination state");
        } else {
          edgeMap.putUnsafe(edgeKey, edgeDescription(edge, tokenNames));
        }
      }
    }
    return m;
  }

  private static Object edgeProblem(Edge edge, String message) {
    return "*** problem with edge: " + message + " ***; " + JSList.with(edge.labels());
  }

  /**
   * Get a description of an edge's code sets, for display as a map key
   */
  private static Object edgeDescription(Edge edge, List<String> tokenNames) {
    var cs = edge.labels();
    if (cs.length % 2 != 0) {
      return edgeProblem(edge, "odd number of elements");
    }
    StringBuilder sb = new StringBuilder();
    for (var i = 0; i < cs.length; i += 2) {
      var a = cs[i];
      var b = cs[i + 1];

      if ((a < 0 && b > 0)
          || a >= b
          || a == 0
      )
        return edgeProblem(edge, "illegal code set");
      if (a < 0) {
        if (cs.length != 2) {
          return edgeProblem(edge, "unexpected token id expr");
        }
        var tokenId = -a - 1;
        if (tokenId >= tokenNames.size()) {
          return "*** no such token id: " + tokenId;
        }
        return tokenNames.get(tokenId);
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
    todo("refactor to use \\ expressions where appropriate");
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

  private static class StateRenamer {

    public int getNewId(int oldId) {
      return mOldToNewIdsMap.get(oldId);
    }

    public State stateWithId(int id) {
      return mIdToStateMap.get(id);
    }

    public State newStateForOldId(int oldId) {
      return stateWithId(getNewId(oldId));
    }

    public State newStateForOld(State oldState) {
      return newStateForOldId(oldState.id());
    }

    public State addOldToNew(State oldState, State newStateOrNull) {
      var newState = newStateOrNull;
      if (newState == null)
        newState = new State(oldState.finalState());
      mOldToNewIdsMap.put(oldState.id(), newState.id());
      mIdToStateMap.put(oldState.id(), oldState);
      mIdToStateMap.put(newState.id(), newState);
      return newState;
    }

    /**
     * Create new states for all (old) states reachable from an (old) state.
     * The new states will have the same final flags, and (if includeEdges is true)
     * appropriate edges as well.
     *
     * Returns the list of old states reachable from the old start state (and the
     * old start state will be first in that list)
     */
    public List<State> createRenamedVersions(State oldStartState, boolean includeEdges) {
      var oldStates = reachableStates(oldStartState);
      for (State oldState : oldStates) {
        addOldToNew(oldState, null);
      }
      if (includeEdges) {
        for (State oldState : oldStates) {
          var newState = newStateForOld(oldState);
          for (Edge edge : oldState.edges()) {
            State newDestinationState = newStateForOld(edge.destinationState());
            addEdge(newState, edge.codeSet(), newDestinationState);
          }
        }
      }
      return oldStates;
    }

    Map<Integer, Integer> mOldToNewIdsMap = hashMap();
    Map<Integer, State> mIdToStateMap = hashMap();

  }

  private static State auxNewState(int offset, List<State> stateList, Map<Integer, Integer> offsetToIndexMap) {
    var state = new State();
    offsetToIndexMap.put(offset, stateList.size());
    stateList.add(state);
    return state;
  }

  public static List<State> decompileDFA(DFA dfa) {
    //
    // <graph> ::= <state>*
    //
    // <state> ::= <1 + token id, or 0> <edge count> <edge>*
    //
    // <edge>  ::= <char_range count> <char_range>* <dest_state_offset, low byte first>
    //
    // <char_range> ::= <start of range (1..127)> <size of range>
    //
    var g = dfa.graph();

    List<State> stateList = arrayList();
    Map<Integer, Integer> offsetToStateMap = hashMap();

    // scan the graph, determining state offsets, constructing (empty) states

    State finalState;
    {
      int offset = 0;
      while (offset < g.length) {
        auxNewState(offset, stateList, offsetToStateMap);
        var edgeCount = g[offset + 1];
        offset += 2;
        for (int j = 0; j < edgeCount; j++) {
          var charRanges = (int) g[offset];
          offset += 1 + (2 * charRanges) + 2;
        }
      }
      // Construct a single final state; it doesn't actually appear in the compiled DFA though
      finalState = auxNewState(offset, stateList, offsetToStateMap);
      finalState.setFinal(true);
    }

    // fill in states
    for (var entry : offsetToStateMap.entrySet()) {
      var offset = entry.getKey();
      var stateIndex = entry.getValue();
      var s = stateList.get(stateIndex);
      // If this is the final state, do nothing; there is no compiled data here
      if (offset == g.length)
        continue;
      var tokenId = ((int) g[offset]) - 1;
      int edgeCount = g[offset + 1];
      offset += 2;

      for (var n = 0; n < edgeCount; n++) {
        var charRanges = (int) g[offset];
        offset++;
        var ib = IntArray.newBuilder();
        for (var ri = 0; ri < charRanges; ri++) {
          int rangeStart = g[offset + 0];
          int rangeEnd = rangeStart + g[offset + 1];
          offset += 2;
          ib.add(rangeStart);
          ib.add(rangeEnd);
        }
        var destStateOffset = (((int) (g[offset + 0] & 0xff)) | (((int) (g[offset + 1]) & 0xff) << 8));
        offset += 2;
        var targetState = stateList.get(offsetToStateMap.get(destStateOffset));
        var edge = new Edge(CodeSet.with(ib.array()), targetState);
        s.edges().add(edge);
      }
      if (tokenId >= 0) {
        var edgeToFinal = new Edge(CodeSet.withValue(-tokenId - 1), finalState);
        s.edges().add(edgeToFinal);
      }
    }
    return stateList;
  }
}
