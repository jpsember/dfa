package dfa;

import js.data.ByteArray;
import js.data.DataUtil;
import js.parsing.Scanner;
import js.testutil.MyTestCase;

import static js.base.Tools.*;

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
    var b =  ByteArray.newBuilder();
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
  public void simple() {
    dfa("{\"graph\":[0,2,1,98,1,14,0,1,97,1,12,0,1,0,2,0],\"token_names\":\"A B\",\"version\":\"$2\"}");
    script("aabba");
  }

  @Test
  public void unknown() {
    mAllowUnknown = true;
    dfa("{\"graph\":[0,2,1,98,1,14,0,1,97,1,12,0,1,0,2,0],\"token_names\":\"A B\",\"version\":\"$2\"}");
    script("aabbac");
  }

  private void dfa(String s) {
    log("parsing dfa from:", INDENT, s);
    mDfa = DFA.parse(s);
  }

  private DFA dfa() {
    checkNotNull(mDfa);
    return mDfa;
  }

  private DFA mDfa;

  private void script(String text) {
    var s = new Scanner(dfa(), text, -1);
    if (mAllowUnknown)
      s.setAcceptUnknownTokens();

    var sb = new StringBuilder();
    while (s.hasNext()) {
      var t = s.read();
      sb.append(String.format("%9s ", mDfa.tokenName(t.id())) + " loc:" + t.locInfo() + "  '" + t.text() + "'");
      addLF(sb);
    }

    generateMessage(sb);
    assertGenerated();
  }

  private boolean mAllowUnknown;
}
