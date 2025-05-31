package dfa;

import js.data.DataUtil;
import js.json.JSList;
import js.json.JSMap;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static js.base.Tools.*;

/**
 * This is a copy of what was DFA in the java-core parsing package, and
 * is here to facilitate construction of the new ('compact') Dfa.
 */
public class OldDfa {

  public static final int UNKNOWN_TOKEN = -1;

  OldDfa() {
  }


  public OldDfa(String script) {
    constructFromJson(new JSMap(script));
  }

  public OldDfa(JSMap json) {
    constructFromJson(json);
  }

  private void constructFromJson(JSMap map) {
    OurState.bumpDebugIds();
    if (map.getDouble("version") < 3.0)
      throw badArg("unsupported version:", map.prettyPrint());

    int finalStateIndex = map.getInt("final");
    mTokenNames = map.getList("tokens").asStringArray();
    for (int i = 0; i < mTokenNames.length; i++)
      mTokenNameIdMap.put(mTokenNames[i], i);
    JSList stateInfo = map.getList("states");

    mStates = new OurState[stateInfo.size()];
    for (int i = 0; i < mStates.length; i++)
      mStates[i] = new OurState(i == finalStateIndex, null);

    List<OurEdge> compiledEdges = arrayList();
    int stateId;
    for (stateId = 0; stateId < mStates.length; stateId++) {
      OurState s = mStates[stateId];
      JSList edges = stateInfo.get(stateId);
      compiledEdges.clear();
      int cursorMax = edges.size();
      int cursor = 0;
      while (cursor < cursorMax) {
        JSList edgeInfo;
        Object element = edges.getUnsafe(cursor);
        if (element instanceof JSList) {
          edgeInfo = (JSList) element;
        } else {
          edgeInfo = list().addUnsafe(element);
        }
        int[] codeSetList = buildCodeSet(edgeInfo);
        cursor++;

        int destStateIndex = finalStateIndex;
        if (cursor < cursorMax) {
          destStateIndex = edges.getInt(cursor);
          cursor++;
        }
        compiledEdges.add(new OurEdge(codeSetList, getState(destStateIndex)));
      }
      s.setEdges(compiledEdges);
      mStates[stateId] = s;
    }
  }

  /**
   * Get name of token, given its id. Returns "<UNKNOWN>" if its id is
   * UNKNOWN_TOKEN, or "<EOF>" if the id is nil Otherwise, assumes tokenId is
   * 0...n-1
   */
  public String tokenName(int tokenId) {
    if (tokenId == UNKNOWN_TOKEN)
      return "<UNKNOWN>";
    return mTokenNames[tokenId];
  }

  public Integer optTokenId(String tokenName) {
    return mTokenNameIdMap.get(tokenName);
  }

  public Integer tokenId(String tokenName) {
    Integer i = optTokenId(tokenName);
    if (i == null)
      badArg("token name not found:", tokenName);
    return i;
  }

  /**
   * Given a space-delimeted string of token names, construct a set of their
   * indexes
   */
  @Deprecated
  public TreeSet<Integer> parseTokenNames(String spaceDelimitedTokenNames) {
    TreeSet<Integer> set = treeSet();
    for (String name : split(spaceDelimitedTokenNames, ' ')) {
      Integer id = tokenId(name);
      if (id == null)
        throw new IllegalArgumentException("no token found:" + name);
      set.add(id);
    }
    return set;
  }

  /**
   * Build a code range (an array of 2n integers) from a list of integers
   */
  private static int[] buildCodeSet(JSList list) {
    List<Integer> codeSetList = arrayList();
    int elementsLength = list.size();
    int cursor = 0;
    while (cursor < elementsLength) {
      int a, b;
      Object value = list.getUnsafe(cursor);
      cursor++;
      if (!(value instanceof Double)) {
        a = ((Number) value).intValue();
        b = list.getInt(cursor);
        cursor++;
        if (b == 0)
          b = OurState.CODEMAX;
      } else {
        a = ((Number) value).intValue();
        b = a + 1;
      }
      codeSetList.add(a);
      codeSetList.add(b);
    }
    return DataUtil.intArray(codeSetList);
  }

  public String[] tokenNames() {
    return mTokenNames;
  }

  public OurState getStartState() {
    return mStates[0];
  }

  private OurState getState(int id) {
    return mStates[id];
  }

  private String[] mTokenNames;
  private Map<String, Integer> mTokenNameIdMap = hashMap();
  private OurState[] mStates;

  public OurState[] debStates() {
    return mStates;
  }

  private static class SimpleScanner {
    CharSequence text;
    int cursor;

    SimpleScanner(CharSequence text) {
      this.text = text;
    }

    char peek() {
      while (true) {
        if (cursor == text.length()) return 0;
        var c = text.charAt(cursor);
        if (c == ' ') {
          cursor++;
        } else {
          return c;
        }
      }
    }

    boolean readIf(char ch) {
      if (peek() == ch) {
        read();
        return true;
      }
      return false;
    }

    int readInt() {
      int curs = this.cursor;
      while (true) {
        var c = peek();
        if (c == '-' || (c >= '0' && c <= '9')) {
          read();
          continue;
        }
        break;
      }
      var tx = text.subSequence(curs, this.cursor);
      skipComma();
      return Integer.parseInt(tx.toString());
    }

    void readExp(char ch) {
      var ch2 = read();
      if (ch != ch2)
        badArg("unexpected character:", ch2, "; was expecting:", ch);
    }

    char read() {
      var x = peek();
      checkArgument(x != 0, "unexpected EOF");
      this.cursor++;
      return x;
    }

    String readStr() {
      readExp('"');
      var sb = new StringBuilder();
      while (true) {
        if (readIf('"')) {
          skipComma();
          break;
        }
        sb.append(read());
      }
      return sb.toString();
    }

    String readPhrase(String endTokens) {
      var s = new StringBuilder();
      int nestLevel = 0;
      while (true) {
        var ch = peek();
        if (ch == '[' || ch == '{') {
          nestLevel++;
        } else if (ch == ']' || ch == '}') {
          if (nestLevel == 0) break;
          nestLevel--;
        }
        if (nestLevel == 0 && endTokens.indexOf(ch) >= 0) {
          break;
        }
        s.append(read());
      }
      skipComma();
      return s.toString();
    }

    void skipComma() {
      while (readIf(',')) ;
    }

  }


  public static OldDfa parseDfaUsingBespokeParser(String source) {
    return parseDfaFromJson(source);
  }

  /**
   * A bespoke parser for json.dfa, which is simpler than general
   * json: only a single outer map {...}, no floating point numbers, and
   * no strings that need escaping.  This allows us to bootstrap to construct
   * a DFA to parse (more elaborate) json content
   */
  public static OldDfa parseDfaFromJson(String source) {
    OurState.bumpDebugIds();

    var dfa = new OldDfa();
    List<OurState> states = arrayList();

    // Parse the outer map {....} into a map of keys and values that are
    // json subexpressions that will need to be parsed separately.

    Map<String, String> mp = hashMap();

    {
      var scanner = new OldDfa.SimpleScanner(source);

      scanner.readExp('{');
      while (true) {
        if (scanner.readIf('}')) break;
        var key = scanner.readStr();
        scanner.readExp(':');
        var value = scanner.readPhrase(",}");
        mp.put(key, value);
      }
    }

    // Validate the version number, and final state index
    checkArgument(mp.get("version").equals("4.0"), "unexpected version",INDENT,mp);

    var finalStateIndex = Integer.parseInt(mp.get("final"));

    // Parse the token names
    {
      var scanner = new OldDfa.SimpleScanner(mp.get("tokens"));
      List<String> tokenNames = arrayList();
      while (scanner.peek() != 0) {
        var ch = scanner.read();
        if (ch == '"') {
          tokenNames.add(scanner.readPhrase("\""));
          scanner.readExp('"');
        }
      }
      dfa.mTokenNames = tokenNames.toArray(new String[0]);
    }

    // Parse the states.
    //
    // This would be quite a bit simpler if we knew the number of states in advance,
    // so we could pre-construct the required number of states in advance...
    {
      var scanner = new OldDfa.SimpleScanner(mp.get("states"));
      scanner.readExp('['); // open list of states
      int stateNumber = 0;

      while (true) {
        scanner.skipComma();
        if (scanner.readIf(']')) {
          scanner.skipComma();
          break;
        }
        scanner.readExp('['); // open a state

        var currentState = extendStates(states, stateNumber);

        // Loop over edges
        while (true) {
          scanner.skipComma();
          if (scanner.readIf(']')) break;
          // Loop over edge info
          scanner.readExp('[');
          List<Integer> codeSets = arrayList();
          while (!scanner.readIf(']')) {
            codeSets.add(scanner.readInt());
            scanner.skipComma();
          }
          scanner.skipComma();
          int targetState = finalStateIndex;
          // If there's another number, it's the target state index; otherwise, the target state is the end state
          if (scanner.peek() != ']') {
            targetState = scanner.readInt();
            scanner.skipComma();
          }

          var targetStatePtr = extendStates(states, targetState);

          int[] ia = new int[codeSets.size()];
          int i = INIT_INDEX;
          for (Integer x : codeSets) {
            i++;
            ia[i] = x;
          }
          var edge = new OurEdge(ia, targetStatePtr);
          currentState.edges().add(edge);
        }
        stateNumber++;
      }
      states.get(finalStateIndex).setFinal(true);
      dfa.mStates = states.toArray(new OurState[0]);
    }
    return dfa;
  }

  // Extend a list of states to accomodate a particular state number (if necessary);
  // return that state
  //
  private static OurState extendStates(List<OurState> states, int stateNumber) {
    while (stateNumber >= states.size())
      states.add(new OurState());
    return states.get(stateNumber);
  }

  // ----------------------------------------------------------------------------------------------

  /**
   * Get a description of the DFA; for development purposes only
   */
  public JSMap describe() {
    var m = map();
    for (var s : mStates) {
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
          edgeMap.putUnsafe(edgeKey, edgeDescription(edge));
        }
      }
    }
    return m;
  }

  private static Object edgeProblem(OurEdge edge, String message) {
    return "*** problem with edge: " + message + " ***; " + JSList.with(edge.codeSets());
  }

  /**
   * Get a description of an edge's code sets, for display as a map key
   */
  private Object edgeDescription(OurEdge edge) {
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
        if (tokenId < 0 || tokenId >= mTokenNames.length) {
          return "*** no such token id: " + tokenId;
        }
        return "<" + tokenName(tokenId) + ">";
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
