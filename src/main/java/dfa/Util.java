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

  public static String versionString(float v) {
    return String.format("%.1f", v);
  }

}
