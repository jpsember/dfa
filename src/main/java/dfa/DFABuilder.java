package dfa;

import js.data.ByteArray;
import js.data.IntArray;
import js.data.ShortArray;
import js.json.JSList;
import js.parsing.DFA;

import java.util.ArrayList;
import java.util.List;

import static js.base.Tools.*;

/**
 * Converts a state machine to a DFA object, suitable for use by client programs
 */
class DFABuilder {

  public DFABuilder setStates(List<State> states) {
    mStates = new ArrayList<>(states);
    return this;
  }

  public DFABuilder setTokenNames(List<String> tokenNames) {
    mTokenNames = new ArrayList<>(tokenNames);
    return this;
  }

  private static final
  boolean DEBUG = false && alert("debug is on");


  public DFA build() {
    if (mBuilt != null) return mBuilt;
    mFirstDebugStateId = states().get(0).id();

    for (var state : states())
      addState(state);

    convertStateIdsToAddresses();
    var graph = encodeGraph();

    // // <edge>  ::= <number of char_range items> <char_range>* <dest_state_id, low byte first>

    if (DEBUG && false) {
      // Generate a JSON representation of this as the new DFA would (if it were implemented)
      var m = map();
      m.put("version", "$2");
      m.put("token_names", String.join(" ", mTokenNames));
      m.put("graph", JSList.with(graph));
      pr("xxx.dfa:", INDENT, m.toString());
    }

    if (false) {
      // Our new version uses an array of (unsigned) bytes, instead of shorts;
      // convert them to a short array for now...

      var sh = ShortArray.newBuilder();
      for (var b : graph) {
        sh.add((short) (b & 0xff));
      }
    }
    mBuilt = new DFA(DFA.VERSION, mTokenNames.toArray(new String[0]), graph);
    return mBuilt;
  }

  private static final int ENCODED_STATE_ID_OFFSET = 1_000_000;

  private void addState(State s) {
    if (DEBUG)
      pr(VERT_SP, "adding state, offset:", mGraph.size(), INDENT, s);

    // <state> ::= <1 + token_id> <edge_count> <edge>*
    var g = mGraph;
    mStateAddresses.add(g.size());

    // If the state has no edges, it will never be reached; so store nothing
    if (s.edges().isEmpty()) {
      if (DEBUG)
        pr("...no edges, skipping");
      return;
    }

    // Store 1 + token id, or 0 if none

    // Also, construct list of edges that aren't associated with the token
    List<Edge> filteredEdges = arrayList();
    {
      int prevTokenId = -1;
      int compiledTokenId = 0;
      for (var edge : s.edges()) {
        var codeSets = edge.labels();
        for (int i = 0; i < codeSets.length; i += 2) {
          var a = codeSets[i];
          var b = codeSets[i + 1];
          checkArgument(a < b && (a > 0) == (b > 0), "illegal char range:", a, b);
          if (a < 0) {
            // Convert the codeset token id to an index
            int tokenIndex = -b - 1;
            checkArgument(tokenIndex >= 0 && tokenIndex < numTokens(), "bad token index:", tokenIndex, "decoded from range:", a, b);

            // Apparently, there can be more than one token edges for a state... choose the one the produces the highest token id
            if (tokenIndex > prevTokenId) {
              prevTokenId = tokenIndex;
              checkArgument(tokenIndex >= 0 && tokenIndex < numTokens(), "bad token index:", tokenIndex, "decoded from range:", a, b);
              compiledTokenId = tokenIndex + 1;
              checkArgument(compiledTokenId > 0 && compiledTokenId < 256, "token index", tokenIndex, "yields out-of-range token id", compiledTokenId);
            }
          } else {
            filteredEdges.add(edge);
          }
        }
      }
      g.add(compiledTokenId);
    }

    // store edge count
    g.add(filteredEdges.size());

    // Add edges (omitting any associated with token ids)
    for (var edge : filteredEdges) {
      var codeSets = edge.labels();

      // <edge>  ::= <number of char_range items> <char_range>* <dest_state_id, low byte first>

      g.add(codeSets.length / 2);
      for (int i = 0; i < codeSets.length; i += 2) {
        var a = codeSets[i];
        var b = codeSets[i + 1];
        checkArgument(a < b && a > 0 && a <= 127 && b <= 128, "illegal char range:", a, b);
        g.add(a);
        g.add(b - a);
      }

      var destStateNumber = stateIndex(edge.destinationState());
      checkArgument(destStateNumber < 0x1_0000, "illegal destination state number");

      // During constructing, state offsets are represented by adding a large offset to both the low and high bytes
      g.add((destStateNumber & 0xff) + ENCODED_STATE_ID_OFFSET);
      g.add((destStateNumber >> 8) + ENCODED_STATE_ID_OFFSET);
    }
  }

  private int stateIndex(int debugStateId) {
    var result = debugStateId - mFirstDebugStateId;
    checkArgument(result >= 0 && result < states().size(), "can't find state index for:", debugStateId);
    return result;
  }

  private int stateIndex(State s) {
    return stateIndex(s.id());
  }

  private List<State> states() {
    return mStates;
  }

  private void convertStateIdsToAddresses() {
    var g = mGraph.array();
    int i = 0;
    while (i < g.length) {
      var a = g[i];
      var b = 0;
      if (i + 1 < g.length)
        b = g[i + 1];

      if (a >= ENCODED_STATE_ID_OFFSET) {
        var idLow = a - ENCODED_STATE_ID_OFFSET;
        var idHigh = b - ENCODED_STATE_ID_OFFSET;
        checkFitsInByte(idLow, "idLow");
        var decodedStateIndex = checkFitsInByte(idLow, "idLow") + (checkFitsInByte(idHigh, "idHigh"));
        checkArgument(decodedStateIndex >= 0 && decodedStateIndex < mStateAddresses.size(), "state address list has no value for:", decodedStateIndex);
        var stateAddr = mStateAddresses.get(decodedStateIndex);
        checkArgument(stateAddr >= 0 && stateAddr < 0x1_0000, "state address out of range:", stateAddr);
        g[i] = checkFitsInByte(stateAddr & 0xff, "low byte of state addr");
        g[i + 1] = checkFitsInByte(stateAddr >> 8, "high byte of state addr");
        i += 2;
      } else {
        i++;
      }
    }
  }

  private static int checkFitsInByte(int value, String message) {
    if (value < 0 || value >= 256)
      throw badArg("value doesn't fit in byte (" + message + "):", value);
    return value;
  }

  private byte[] encodeGraph() {
    var s = ByteArray.newBuilder();
    var src = mGraph.array();
    for (var i : src) {
      checkArgument(i >= 0 && i < 0x100, "encoded byte is out of range:", i);
      s.add((byte) i);
    }
    return s.array();
  }

  private int numTokens() {
    return mTokenNames.size();
  }

  private List<State> mStates;
  private IntArray.Builder mGraph = IntArray.newBuilder();
  private List<Integer> mStateAddresses = arrayList();
  private DFA mBuilt;
  private Integer mFirstDebugStateId;
  private List<String> mTokenNames;
}
