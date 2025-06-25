package dfa;

import js.parsing.Scanner;
import js.testutil.MyTestCase;

import static js.base.Tools.*;

import org.junit.Test;

import js.parsing.DFA;

public class ScanTest extends MyTestCase {

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
