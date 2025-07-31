package dfa;

import js.data.ByteArray;
import js.data.DataUtil;
import js.file.Files;
import js.geometry.MyMath;
import js.testutil.MyTestCase;

import static js.base.Tools.*;
import static org.junit.Assert.*;

import org.junit.Test;

import js.parsing.DFA;
import parsing.Lexeme;
import parsing.Lexer;
import parsing.LexerException;

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
    var result = DataUtil.hexDump(cvtBytes, 0, cvtBytes.length, true);
    assertMessage(result);
  }

  @Test
  public void peekEndOfInput() {
    var x = lexer();
    x.withText("");
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
    assertFalse(s.hasNext());
  }


  @Test
  public void hasNext2() {
    var s = lexer();
    s.withText("a");
    assertTrue(s.hasNext());
  }

  @Test
  public void hasNext3() {
    var s = lexer();
    s.withText("a");
    assertTrue(s.hasNext());
    s.read();
    assertFalse(s.hasNext());
  }

  @Test
  public void peekIf() {
    var s = lexer();
    s.withText("abab");
    assertTrue(s.peekIf(0, 1));
    assertFalse(s.peekIf(0, 1, 1));
    assertTrue(s.peekIf());
    assertTrue(s.peekIf(0, 1, 0, 1));
  }

  @Test(expected = IllegalStateException.class)
  public void attemptLexIfPeekIfFailed() {
    var s = lexer();
    s.withText("abab");
    s.peekIf(1, 0);
    s.token();
  }

  @Test(expected = IllegalStateException.class)
  public void attemptLexIfReadIfFailed() {
    var s = lexer();
    s.withText("abab");
    s.readIf(1, 0);
    s.token();
  }

  @Test
  public void readIf2() {
    var s = lexer();
    s.withText("abba");

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
    assertTrue(s.readIf(0, Lexeme.ID_UNKNOWN));
  }

  @Test
  public void readIfAnyAtEnd() {
    var s = lexer();
    s.withText("");
    assertFalse(s.readIf(Lexeme.ID_UNKNOWN));
  }

  @Test
  public void readIf3() {
    var s = lexer();
    s.withText("abba");

    assertTrue(s.readIf(0, 1));
    assertFalse(s.readIf(0, 1));
    assertTrue(s.readIf(1, 0));
    assertFalse(s.hasNext());
  }


  @Test
  public void peekIfThenRead() {
    var s = lexer();
    s.withText("ababab");

    assertTrue(s.peekIf(0, 1, 0));
    assertTrue(s.token().id(0));
    assertTrue(s.token().id(1));
    assertTrue(s.token().id(0));

    assertTrue(s.readIf(0, 1, 0));

    assertTrue(s.peekIf(1, 0, 1));
    // Look at, without consuming them
    assertTrue(s.token(0).id(1));
    assertTrue(s.token(1).id(0));
    assertTrue(s.token(2).id(1));

    // Consume them
    assertTrue(s.token().id(1));
    assertTrue(s.token().id(0));
    assertTrue(s.token().id(1));

    assertTrue(s.readIf(1, 0, 1));
    // Look at, without consuming them
    assertTrue(s.token(0).id(1));
    assertTrue(s.token(1).id(0));
    assertTrue(s.token(2).id(1));

    assertFalse(s.hasNext());
  }

  @Test(expected = LexerException.class)
  public void unknownToken() {
    var s = lexer();
    s.withText("aab^ba");
    s.read();
    s.read();
    s.read();
    s.read();
  }

  @Test
  public void unknownTokenDisplay() {
    var s = lexer();
    var sb = new StringBuilder();
    s.withText("ab\nba\naab?ba");
    while (s.hasNext()) {
      try {
        var tk = s.read();
        sb.append(tk.text());
        sb.append("\n");
      } catch (LexerException e) {
        sb.append(e.getMessage());
        sb.append('\n');
      }
    }
    assertMessage(sb);
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
    mAllowUnknown = true;
    script("c");
  }

  @Test
  public void unknown() {
    mAllowUnknown = true;
    script("aabbac");
  }

  @Test
  public void code() {
    proc();
  }

  @Test
  public void codeb() {
    noSkip();
    proc();
  }

  @Test
  public void rowsAndColumns() {
    tokens(JSON_DFA);
    var text = Files.readString(this.getClass(), testName() + ".txt");
    noSkip();
    var s = lexer();
    s.withText(text);

    var sb = new StringBuilder();

    while (s.hasNext()) {
      var t = s.read();
      sb.append(String.format("%5d %5d : %s\n", t.row() + 1, t.column() + 1, t.text()));
    }
    String result = sb.toString();
    assertMessage(result);
  }


  @Test
  public void context() {
    tokens("{\"graph\":[0,5,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0,1,120,1,108,0,1,47,1,39,0,0,1,1,42,1,46,0,0,3,2,1,41,43,85,46,0,2,1,41,43,85,46,0,1,42,1,67,0,0,5,3,1,41,43,4,48,80,46,0,3,1,41,43,4,48,80,46,0,3,1,41,43,4,48,80,46,0,1,42,1,67,0,1,47,1,106,0,3,0,2,1,1,120,1,108,0,1,3,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0,3,9,2,12,2,32,1,115,0],\"token_names\":\"WS CODE COMMENT\",\"version\":\"$2\"}");
    var text =
        "x\n\t x\n\t\t  x\n\t\t\t  x\n\t\t\t\t   x";
    noSkip();
    var s = lexer();
    s.withText(text);

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
    assertMessage(result);
  }

  @Test
  public void binarySearchTest() {
    resetSeed(123456);
    for (int j = 0; j < 100; j++) {
      var size = random().nextInt(20) + 1;
      List<Integer> ls = arrayList();
      for (int i = 0; i < size; i++)
        ls.add(random().nextInt(size + 1));
      ls.add(0);
      var vals = MyMath.permute(ls, random());
      vals.sort(null);
      for (int k = 0; k < 10; k++) {
        var seek = random().nextInt(size + 1);
        var slot = slowSlot(vals, seek);
        var slot2 = binarySearch(vals, seek);
        checkState(slot == slot2);
      }
    }
  }

  private static int slowSlot(List<Integer> vals, int target) {
    int result = 0;
    int i = INIT_INDEX;
    for (var x : vals) {
      i++;
      if (x <= target)
        result = i;
    }
    return result;
  }

  private static int binarySearch(List<Integer> vals, int target) {
    checkArgument(!vals.isEmpty() && target >= vals.get(0));

    // We will refer to the window start and size as a record count (i.e. / F_TOTAL),
    // to simplify the halving calculations
    //
    int searchMin = 0;
    int windowSize = vals.size();

    while (windowSize > 1) {

      var mid = searchMin + windowSize / 2;

      var midLineNumber = vals.get(mid);


      if (midLineNumber > target) {
        windowSize = mid - searchMin;
      } else {
        windowSize -= (mid - searchMin);
        searchMin = mid;
      }
      checkState(windowSize >= 1);
    }
    return searchMin;
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
    var sb = new StringBuilder();
    while (s.hasNext()) {
      var t = s.read();
      sb.append(t); //String.format("%9s ", mDfa.tokenName(t.id())) + " '" + t.text() + "'");
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

  private static final String JSON_DFA = "{\"graph\":[0,16,1,125,1,-114,1,1,123,1,-116,1,1,116,1,117,1,1,110,1,94,1,1," +
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
      " CBRCL COMMA COLON STRING NUMBER\",\"version\":\"$2\"}";

  private void proc(String sampleText) {
    if (sampleText == null) {
      String resourceName = testName() + ".txt";
      sampleText = Files.readString(this.getClass(), resourceName);
    }
    var source = sampleText;

    DFACompiler c = new DFACompiler();
    c.setVerbose(verbose());
    var jsonDFA = DFA.parse(JSON_DFA);


    {
      StringBuilder sb = new StringBuilder();
      var s = new Lexer(jsonDFA);
      s.withText(source);
      if (!mDisallowUnknown)
        s.withAcceptUnknownTokens();
      s.withSkipId(mSkipId);
      s.setVerbose(verbose());

      List<Lexeme> tok = arrayList();
      while (s.hasNext()) {
        var t = s.read();
        tok.add(t);
      }

      for (var tk : tok) {
        var cts = tk.plotWithinContext();
        sb.append("-----------------------------------------------------------------------------------\n");
        sb.append(cts);
        sb.append('\n')        ;
        sb.append(tk);
        addLF(sb);
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

  private void noSkip() {
    mSkipId = Lexeme.ID_SKIP_NONE;
  }

  private String mTestName;
  private int mVersion = -1;
  private int mSkipId;
}
