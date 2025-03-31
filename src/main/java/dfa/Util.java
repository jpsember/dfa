package dfa;

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
   */
  public static final int OURCODEMAX = 0x110000;

  public static final int MAX_TOKEN_DEF = 1_000;
  
  /**
   * Minimum code possible.  Negative values indicate token ids, so we need this to 
   * include the value -(# tokens)
   */
  public static final int OURCODEMIN = -MAX_TOKEN_DEF;

  public static String versionString(float v) {
    return String.format("%.1f", v);
  }

}
