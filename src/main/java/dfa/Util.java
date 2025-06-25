package dfa;

import dfa.gen.DfaConfig;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;

import java.util.List;

import static js.base.Tools.*;

public final class Util {

  public static final String EXT_RXP = "rxp" //
      , EXT_DFA = "dfa" //
      ;

  public static final int FTYPE_JAVA = 0 //
      , FTYPE_RUST = 1 //
      ;

  public static final double DFA_VERSION_4 = 4.0;

  /**
   * One plus the maximum code represented
   *
   * Doesn't make a difference in the DFA size if the code max is reduced to 256
   * (i.e. for utf8 only)
   */
  public static int codeMax() {
    // The maximum code we can represent is 255, which is the largest code
    // that can fit in a byte.
    //
    // If a DFA is being used to scan utf8 or unicode, then a character range that
    // includes 255 will also accept any value > 255.
    //

    // TODO: actually, if a range includes 127, it will also include 127...255
    return 255;
  }

  public static final int MAX_TOKEN_DEF = 1_000;

  /**
   * Minimum code possible. Negative values indicate token ids, so we need this
   * to include the value -(# tokens)
   */
  public static final int OURCODEMIN = -MAX_TOKEN_DEF;

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



  // ----------------------------------------------------------------------------------------------

  /**
   * Get a description of a DFA; for development purposes only
   *
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
