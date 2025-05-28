package dfa;

import js.data.IntArray;
import js.data.ShortArray;
import js.parsing.DFA;
import js.parsing.Edge;
import js.parsing.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    {
//      mStateIdMap = hashMap();
      mFirstDebugStateId = dfa.debStates()[0].debugId();
//       for (var s : dfa.debStates()) {
//        var id = s.debugId();
//        if (mFirstDebugStateId == null) {
//          pr("mFirstDebugStateId:",id);
//          mFirstDebugStateId = id;
//        }
//        pr("storing state id mapping id:",id,"==>",id- mFirstDebugStateId);
//        var prevValue = mStateIdMap.put(id, id - mFirstDebugStateId);
//        checkState(prevValue == null, "duplicate State.debugId():", id);
//      }
    }

    // <graph> ::= <int: # of states> <state>*
    g.add(sc);

    for (var state : dfa.debStates()) {
      addState(state);
    }

    convertStateIdsToAddresses();
    var graph = encodeGraph();
    mBuilt = new CompactDFA(CompactDFA.VERSION, String.join(" ", dfa.tokenNames()), graph);
    return mBuilt;
  }


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

    // During constructing, state offsets are represented by negative indexes
    g.add(-(destStateNumber + 1));
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
      int tokenId = -b - 1;
      checkArgument(tokenId >= 0 && tokenId < numTokens(), "bad token id:", tokenId, "for range:", a, b);
      g.add(CompactDFA.TOKEN_OFFSET + tokenId);
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
      if (val < 0) {
        var decodedStateIndex = (-val) - 1;
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
    checkArgument(src.length < CompactDFA.TOKEN_OFFSET, "graph has grown too large:", src.length);
    for (var i : src) {
      var sTry = (short) i;
      checkState(sTry == i, "failed to convert int to short:", i);
      s.add(sTry);
    }
    return s.array();
  }


  //
  // <graph> ::= <int: # of states> <state>*
  //
  // <state> ::= <edge count> <edge>*
  //
  // <edge>  ::= <int: number of char_range items> <char_range>* <dest_state_id>
  //
  // <char_range> ::= <int: start of range> <int: end of range (exclusive)>
  //                | <int: -(token index + 1)>
  //
  // <dest_state_id> ::= offset of state within graph
  //

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
