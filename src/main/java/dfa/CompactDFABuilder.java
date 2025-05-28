package dfa;

import js.data.IntArray;
import js.data.ShortArray;
import js.parsing.DFA;
import js.parsing.Edge;
import js.parsing.State;

import java.util.ArrayList;
import java.util.List;

import static js.base.Tools.*;

class CompactDFABuilder {

  public CompactDFABuilder(DFA dfa) {
    mDfa = dfa;
  }

  public CompactDFA build() {
    if (mBuilt != null) return mBuilt;
    var dfa = mDfa;
    var g = mGraph;
    var sc = dfa.debStates().length;
    mFirstDebugStateId = dfa.debStates()[0].debugId();

    // <graph> ::= <int: # of states> <state>*
    g.add(sc);

    for (var state : dfa.debStates())
      addState(state);

    convertStateIdsToAddresses();
    var graph = encodeGraph();
    mBuilt = new CompactDFA(CompactDFA.VERSION, String.join(" ", dfa.tokenNames()), graph);
    return mBuilt;
  }

  private static final int ENCODED_STATE_ID_OFFSET = 1_000_000;

  private void addState(State s) {
    // <state> ::= <edge count> <edge>*
    var g = mGraph;
    mStateAddresses.add(g.size());
    g.add(s.edges().size());
    for (var edge : s.edges())
      addEdge(edge);
  }

  private void addEdge(Edge edge) {
    // <edge>  ::= <int: number of char_range items> <char_range>* <dest_state_id>
    var g = mGraph;

    g.add(edge.codeSets().length / 2);
    for (int i = 0; i < edge.codeSets().length; i += 2) {
      addCharRange(edge.codeSets(), i);
    }

    var destStateNumber = stateIndex(edge.destinationState());
    // During constructing, state offsets are represented by adding a large offset
    var tempStateNumber = destStateNumber + ENCODED_STATE_ID_OFFSET;
    g.add(tempStateNumber);
  }

  private int stateIndex(int debugStateId) {
    var result = debugStateId - mFirstDebugStateId;
    checkArgument(result >= 0 && result < mDfa.debStates().length, "can't find state index for:", debugStateId);
    return result;
  }

  private int stateIndex(State s) {
    return stateIndex(s.debugId());
  }

  private void addCharRange(int[] codeSets, int i) {
    var g = mGraph;
    var a = codeSets[i];
    var b = codeSets[i + 1];
    checkArgument(a < b && (a > 0) == (b > 0), "illegal char range:", a, b);
    if (a < 0) {
      // Convert the codeset token id to an index
      int tokenIndex = -b - 1;
      checkArgument(tokenIndex >= 0 && tokenIndex < numTokens(), "bad token index:", tokenIndex, "decoded from range:", a, b);
      // Convert the token index to a compact DFA encoded version (which at present is the same as the codeset version?)
      var compiledTokenId = -tokenIndex - 1;
      g.add(compiledTokenId);
    } else {
      g.add(a);
      g.add(b);
    }
  }

  private void convertStateIdsToAddresses() {
    var i = INIT_INDEX;
    var g = mGraph.array();
    for (var val : g) {
      i++;
      if (val >= ENCODED_STATE_ID_OFFSET) {
        var decodedStateIndex = val - ENCODED_STATE_ID_OFFSET;
        var stateIndex = decodedStateIndex;
        checkArgument(stateIndex >= 0 && stateIndex < mStateAddresses.size(), "state address list has no value for:", stateIndex);
        var stateAddr = mStateAddresses.get(stateIndex);
        g[i] = stateAddr;
      }
    }
  }

  private short[] encodeGraph() {
    var s = ShortArray.newBuilder();
    var src = mGraph.array();
    for (var i : src) {
      var sTry = (short) i;
      checkState(sTry == i, "failed to convert int to short:", i);
      s.add(sTry);
    }
    return s.array();
  }


  private int numTokens() {
    return mDfa.tokenNames().length;
  }

  private DFA mDfa;
  private IntArray.Builder mGraph = IntArray.newBuilder();
  //  private Map<Integer, Integer> mStateIdMap;
  private List<Integer> mStateAddresses = new ArrayList();
  private CompactDFA mBuilt;
  private Integer mFirstDebugStateId;

}
