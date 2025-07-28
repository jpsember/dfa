package dfa;

import js.data.ByteArray;
import js.data.DataUtil;
import js.json.JSList;
import js.parsing.Scanner;
import js.testutil.MyTestCase;

import static js.base.Tools.*;
import static org.junit.Assert.*;

import org.junit.Test;

import js.parsing.DFA;
import parsing.Lexer;

import java.nio.charset.Charset;

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
  public void unknownSimple() {
    mAllowUnknown = true;
    script("c");
  }

  @Test
  public void unknown() {
    mAllowUnknown = true;
    script("aabbac");
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
}
