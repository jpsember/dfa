package parsing;

import js.testutil.MyTestCase;


//  @Test
//  public void testJSON() {
//    var s = new Scanner(dfa(), sample());
//    s.read(J_CBROP);
//    s.read(J_STRING);
//    s.read(J_COLON);
//  }


import js.parsing.DFA;
import org.junit.Test;

import java.io.File;

import static js.base.Tools.*;

public class ParsingTest extends MyTestCase {


  @Test
  public void buildDFAFromSource() {
    todo("Get rid of json.dfa; and move unit tests to testutils repo");
    var dir = new File("src/main/resources/js/parsing");
//    var c = DFACache.SHARED_INSTANCE;
//    var dfa = c.buildFromSource(dir, "sample");
//    pr(dfa.toJson());
    if (false)
      pr(dir);
  }


  private static String sample() {
    loadTools();
    return "{\"alpha\":[1,2,3] }";
  }

  private DFA dfa() {
    if (mDFA == null) {
      mDFA = DFA.parse("{\"graph\":[0,16,1,125,1,-114,1,1,123,1,-116,1,1,116,1,117,1,1,110,1,94," +
          "1,1,102,1,64,1,1,93,1,62,1,1,91,1,60,1,1,58,1,58,1,1,49,9,39,1,1,48,1,-24,0,1,47,1,-73,0," +
          "1,45,1,-85,0,1,44,1,-87,0,1,35,1,-94,0,1,34,1,97,0,3,9,2,12,2,32,1,86,0,1,1,3,9,2,12,2,32," +
          "1,86,0,0,3,3,32,2,35,57,93,35,97,0,1,92,1,120,0,1,34,1,118,0,11,0,0,3,3,32,2,35,57,93,35,97," +
          "0,1,92,1,120,0,1,34,1,-115,0,11,3,3,32,2,35,57,93,35,97,0,1,92,1,120,0,1,34,1,118,0,1,1,1," +
          "32,96,-94,0,9,0,0,2,1,49,9,39,1,1,48,1,-24,0,0,2,1,42,1,-61,0,1,47,1,-94,0,0,2,2,1,41,43," +
          "85,-61,0,1,42,1,-47,0,0,3,3,1,41,43,4,48,80,-61,0,1,42,1,-47,0,1,47,1,-26,0,1,0,12,2,2,69," +
          "1,101,1,11,1,1,46,1,-10,0,0,1,1,48,10,-3,0,12,2,2,69,1,101,1,11,1,1,48,10,-3,0,0,2,1,48,10," +
          "32,1,2,43,1,45,1,25,1,0,1,1,48,10,32,1,12,1,1,48,10,32,1,12,3,1,48,10,39,1,2,69,1,101,1,11," +
          "1,1,46,1,-10,0,10,0,2,0,3,0,0,1,1,97,1,71,1,0,1,1,108,1,78,1,0,1,1,115,1,85,1,0,1,1,101,1," +
          "92,1,5,0,0,1,1,117,1,101,1,0,1,1,108,1,108,1,0,1,1,108,1,115,1,6,0,0,1,1,114,1,124,1,0,1,1," +
          "117,1,-125,1,0,1,1,101,1,-118,1,4,0,7,0,8,0],\"token_names\":\"WS BROP BRCL TRUE FALSE " +
          "NULL CBROP CBRCL COMMA COLON STRING NUMBER\",\"version\":\"$2\"}");
    }
    return mDFA;
  }

  private DFA mDFA;

  private static final int J_WS = 0;
  private static final int J_BROP = 1;
  private static final int J_BRCL = 2;
  private static final int J_TRUE = 3;
  private static final int J_FALSE = 4;
  private static final int J_NULL = 5;
  private static final int J_CBROP = 6;
  private static final int J_CBRCL = 7;
  private static final int J_COMMA = 8;
  private static final int J_COLON = 9;
  private static final int J_STRING = 10;
  private static final int J_NUMBER = 11;

}