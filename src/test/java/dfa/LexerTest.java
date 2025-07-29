package dfa;

import js.base.Pair;
import js.data.ByteArray;
import js.data.DataUtil;
import js.file.FileException;
import js.file.Files;
import js.json.JSList;
import js.parsing.Scanner;
import js.testutil.MyTestCase;

import static js.base.Tools.*;
import static org.junit.Assert.*;

import org.junit.Test;

import js.parsing.DFA;
import parsing.Lexeme;
import parsing.Lexer;

import java.nio.charset.Charset;
import java.util.List;

public class LexerTest extends MyTestCase {

  @Test
  public void newlinesNone() {
    newlineTest("123");
  }

  @Test
  public void newlinesLF() {
    newlineTest("123L4LL56L");
  }

  @Test
  public void newlinesCRLF() {
    newlineTest("123CL4CLCL56CL");
  }

  @Test
  public void newlinesCRLFwithMissing() {
    newlineTest("123CL4CLCL56CL78");
  }

  @Test
  public void tailCRLF() {
    newlineTest("CL");
  }


  @Test
  public void newlinesCRLFWithOrphanCR() {
    newlineTest("123CL4L56CCL");
  }

  private void newlineTest(String s) {
    var b = ByteArray.newBuilder();
    for (var i : s.getBytes(Charset.forName("UTF-8"))) {
      var x = i;
      if (i == 'L')
        x = 0x0a;
      else if (i == 'C')
        x = 0x0d;
      b.add(x);
    }
    var srcBytes = b.array();
    var cvtBytes = Lexer.normalizeNewlines(srcBytes);
    if (false) {
      pr("srcBytes:", INDENT, JSList.with(srcBytes));
      pr("cvtBytes:", INDENT, JSList.with(cvtBytes));
    }
    var result = DataUtil.hexDump(cvtBytes, 0, cvtBytes.length, true);
    assertMessage(result);
  }

  @Test
  public void peekEndOfInput() {
    var x = lexer();
    x.withText("");
    x.start();
    assertTrue(
        x.peek().isEndOfInput());
  }


  @Test
  public void noInput() {
    script("");
  }

  @Test
  public void smallInput() {
    script("aabba");
  }

  @Test
  public void medium() {
    tokens("{\"graph\":[0,3,1,98,1,28,0,1,97,1,19,0,1,32,1,17,0,1,0,2,1,1,98,1,26,0,4,0,3,1,1,97,1,35,0,5,1,1,98,1,35,0],\"token_names\":\"SP A B PAIR PREF\",\"version\":\"$2\"}");
    skip(0);
    script("a b ab bab baa bab baba babb");
  }

  @Test
  public void hasNext() {
    var s = lexer();
    s.withText("");
    s.start();
    assertFalse(s.hasNext());
  }


  @Test
  public void hasNext2() {
    var s = lexer();
    s.withText("a");
    s.start();
    assertTrue(s.hasNext());
  }

  @Test
  public void hasNext3() {
    var s = lexer();
    s.withText("a");
    s.start();
    assertTrue(s.hasNext());
    s.read();
    assertFalse(s.hasNext());
  }

  @Test
  public void peekIf() {
    var s = lexer();
    s.withText("abab");
    s.start();
    assertTrue(s.peekIf(0, 1));
    assertFalse(s.peekIf(0, 1, 1));
    assertTrue(s.peekIf());
    assertTrue(s.peekIf(0, 1, 0, 1));
  }

  @Test(expected = IllegalStateException.class)
  public void attemptLexIfPeekIfFailed() {
    var s = lexer();
    s.withText("abab");
    s.start();
    s.peekIf(1, 0);
    s.token();
  }

  @Test(expected = IllegalStateException.class)
  public void attemptLexIfReadIfFailed() {
    var s = lexer();
    s.withText("abab");
    s.start();
    s.readIf(1, 0);
    s.token();
  }

  @Test
  public void readIf2() {
    var s = lexer();
    s.withText("abba");
    s.start();

    assertTrue(s.readIf(0, 1));
    assertEquals(0, s.token().id());
    assertEquals(1, s.token().id());

    assertFalse(s.readIf(0, 1));

    assertTrue(s.readIf(1, 0));
    assertEquals(1, s.token().id());
    assertEquals(0, s.token().id());
  }

  @Test
  public void readIfAny() {
    var s = lexer();
    s.withText("abba");
    s.start();
    assertTrue(s.readIf(0, Lexeme.ID_UNKNOWN));
  }

  @Test
  public void readIfAnyAtEnd() {
    var s = lexer();
    s.withText("");
    s.start();
    assertFalse(s.readIf(Lexeme.ID_UNKNOWN));
  }

  @Test
  public void readIf3() {
    var s = lexer();
    s.withText("abba");
    s.start();

    assertTrue(s.readIf(0, 1));
    assertFalse(s.readIf(0, 1));
    assertTrue(s.readIf(1, 0));
    assertFalse(s.hasNext());
  }

  @Test
  public void simple() {
    script("aabba");
  }

  @Test
  public void simpleSkipB() {
    lexer().withSkipId(1);
    script("aabba");
  }

  @Test
  public void unknownSimple() {
    rv();
    mAllowUnknown = true;
    script("c");
  }

  @Test
  public void unknown() {
    mAllowUnknown = true;
    script("aabbac");
  }


  @Test
  public void code1() {
    rv();
    proc();
  }


  @Test
  public void context() {
    rv();
    tokens("{\"graph\":[0,5,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0,1,120,1,108,0,1,47,1,39,0,0,1,1,42,1,46,0,0,3,2,1,41,43,85,46,0,2,1,41,43,85,46,0,1,42,1,67,0,0,5,3,1,41,43,4,48,80,46,0,3,1,41,43,4,48,80,46,0,3,1,41,43,4,48,80,46,0,1,42,1,67,0,1,47,1,106,0,3,0,2,1,1,120,1,108,0,1,3,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0],\"token_names\":\"WS CODE COMMENT\",\"version\":\"$2\"}");
    var text =
        //"x /* alpha\nbravo\ncharlie\ndelta */  x x x /* echo\n   fox\n   golf\n    hotel   */";
        "x\n\t x\n\t\t  x\n\t\t\t  x\n\t\t\t\t   x";

    var s = lexer();
    s.withText(text);
    s.start();


    List<Lexeme> y = arrayList();
    while (s.hasNext()) {
      y.add(s.read());
    }


    var sb = new StringBuilder();
    for (var z : y) {
      var st = z.plotWithinContext();
      sb.append(st);
      sb.append("\n--------------------------------------\n");
    }

    String result = sb.toString();
    pr(result);
    assertMessage(result);
  }


  private String tokenDefs() {
    if (mTokenDefs == null)
      mTokenDefs = "{\"graph\":[0,2,1,98,1,14,0,1,97,1,12,0,1,0,2,0],\"token_names\":\"A B\",\"version\":\"$2\"}";
    return mTokenDefs;
  }

  private String mTokenDefs;

  private void tokens(String s) {
    mTokenDefs = s;
  }

  private DFA dfa() {
    if (mDfa == null) {
      mDfa = DFA.parse(tokenDefs());
    }
    return mDfa;
  }


  private DFA mDfa;

  private Lexer lexer() {
    if (mLexer == null) {
      var m = new Lexer(dfa());
      mLexer = m;
    }
    return mLexer;
  }

  private Lexer mLexer;

  private void script(String text) {
    lexer().withText(text);
    if (mAllowUnknown)
      lexer().withAcceptUnknownTokens();
    if (mSkip != null)
      lexer().withSkipId(mSkip);

    var s = lexer();
    s.start();
    var sb = new StringBuilder();
    while (s.hasNext()) {
      var t = s.read();
      sb.append(String.format("%9s ", mDfa.tokenName(t.id())) + " '" + t.text() + "'");
      addLF(sb);
    }

    generateMessage(sb);
    assertGenerated();
  }

  private void skip(int skipIndex) {
    mSkip = skipIndex;
  }

  private Integer mSkip;
  private boolean mAllowUnknown;


  private void proc() {
    proc(null);
  }

  private boolean mDisallowUnknown;

  private void disallowUnknown() {
    mDisallowUnknown = true;
  }


  private void proc(String sampleText) {

    if (sampleText == null) {
      String resourceName = testName() + ".txt";
      sampleText = Files.readString(this.getClass(), resourceName);
    }
    var source = sampleText;

    DFACompiler c = new DFACompiler();
    c.setVerbose(verbose());
    var jsonDFA = DFA.parse("{\"graph\":[0,16,1,125,1,-114,1,1,123,1,-116,1,1,116,1,117,1,1,110,1,94,1,1," +
        "102,1,64,1,1,93,1,62,1,1,91,1,60,1,1,58,1,58,1,1,49,9,39,1,1,48,1,-24,0,1,47,1,-73,0,1,45,1,-85,0,1,44," +
        "1,-87,0,1,35,1,-94,0,1,34,1,97,0,3,9,2,12,2,32,1,86,0,1,1,3,9,2,12,2,32,1,86,0,0,3,3,32,2,35,57,93,35,97," +
        "0,1,92,1,120,0,1,34,1,118,0,11,0,0,3,3,32,2,35,57,93,35,97,0,1,92,1,120,0,1,34,1,-115,0,11,3,3,32,2," +
        "35,57,93,35,97,0,1,92,1,120,0,1,34,1,118,0,1,1,1,32,96,-94,0,9,0,0,2,1,49,9,39,1,1,48,1,-24,0,0,2,1," +
        "42,1,-61,0,1,47,1,-94,0,0,2,2,1,41,43,85,-61,0,1,42,1,-47,0,0,3,3,1,41,43,4,48,80,-61,0,1,42,1,-47," +
        "0,1,47,1,-26,0,1,0,12,2,2,69,1,101,1,11,1,1,46,1,-10,0,0,1,1,48,10,-3,0,12,2,2,69,1,101,1,11,1,1,48," +
        "10,-3,0,0,2,1,48,10,32,1,2,43,1,45,1,25,1,0,1,1,48,10,32,1,12,1,1,48,10,32,1,12,3,1,48,10,39,1,2," +
        "69,1,101,1,11,1,1,46,1,-10,0,10,0,2,0,3,0,0,1,1,97,1,71,1,0,1,1,108,1,78,1,0,1,1,115,1,85,1,0,1,1," +
        "101,1,92,1,5,0,0,1,1,117,1,101,1,0,1,1,108,1,108,1,0,1,1,108,1,115,1,6,0,0,1,1,114,1,124,1,0,1,1," +
        "117,1,-125,1,0,1,1,101,1,-118,1,4,0,7,0,8,0],\"token_names\":\"WS BROP BRCL TRUE FALSE NULL CBROP" +
        " CBRCL COMMA COLON STRING NUMBER\",\"version\":\"$2\"}");


    {
      StringBuilder sb = new StringBuilder();
      var s = new Lexer(jsonDFA);
      pr("text:",source);
      s.withText(source);
      if (!mDisallowUnknown)
        s.withAcceptUnknownTokens();
      s.withSkipId(0);
      s.setVerbose(verbose());
      s.start();

      List<Lexeme> tok = arrayList();
      while (s.hasNext()) {
        var t = s.read();
        tok.add(t);
      }

      for (var tk : tok) {
        var cts = tk.plotWithinContext();
        sb.append("-----------------------------------------------------------------------------------\n");
        sb.append(cts);
      }
      sb.append("-----------------------------------------------------------------------------------\n");

      String result = sb.toString();
      log("Parsed tokens:", INDENT, result);
      files().writeString(generatedFile("tokens.txt"), result);
    }

    assertGenerated();
  }


  private String testName() {
    parseName();
    return mTestName;
  }

  private int testVersion() {
    parseName();
    return mVersion;
  }

  private void parseName() {
    // If unit test name is xxxx123, extract the suffix 123 as the version for the input
    if (mTestName == null) {
      String nm = name();
      String testName = nm;
      int j = nm.length();
      while (true) {
        char c = nm.charAt(j - 1);
        if (!(c >= '0' && c <= '9'))
          break;
        j--;
      }
      if (j < nm.length()) {
        mVersion = Integer.parseInt(nm.substring(j));
        testName = nm.substring(0, j);
      }
      mTestName = testName;
    }
  }

  private String mTestName;
  private int mVersion = -1;
}
