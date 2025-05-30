package dfa;

import dfa.gen.DfaConfig;
import js.json.JSMap;
import js.parsing.DFA;

public final class Util {

  public static final String EXT_RXP = "rxp" //
      , EXT_DFA = "dfa" //
      ;

  public static final int FTYPE_JAVA = 0 //
      , FTYPE_RUST = 1 //
      ;

  public static final double DFA_VERSION_3 = 3.0;
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
    return preVersion4() ? 0x110000 : 255;
  }

  public static boolean preVersion4() {
    return dfaConfig().version() < DFA_VERSION_4;
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

  public static DFA convertOldDFAJSMapToCompactDFA(JSMap oldDFAJSMap) {
    var oldDfa = OldDfa.parseDfaUsingBespokeParser(oldDFAJSMap.toString());
    var b = new DFABuilder(oldDfa);
    return b.build();
  }
}
