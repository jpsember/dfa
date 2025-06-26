/**
 * MIT License
 *
 * Copyright (c) 2022 Jeff Sember
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/
package dfa;

import static js.base.Tools.*;
import static org.junit.Assert.*;

import js.parsing.DFA;
import js.parsing.ScanException;
import js.parsing.Scanner;
import org.junit.Test;

import js.base.BasePrinter;
import js.file.FileException;
import js.file.Files;
import js.parsing.RegExp;
import js.testutil.MyTestCase;

import static dfa.Util.*;

public class CompileTest extends MyTestCase {

  @Override
  public void setup() {
    super.setup();
    // We need to reset the debug ids before each test, for deterministic behaviour
    State.resetIds();
    DFACompiler.sVerbosity = false;
  }

  @Test
  public void jsona1() {
    proc();
  }

  @Test
  public void datagen1() {
    proc();
  }

  @Test
  public void escape1() {
    proc();
  }

  @Test
  public void multiline1() {
    disallowUnknown();
    proc();
  }

  @Test
  public void simple() {
    proc("abbaaa");
  }

  @Test
  public void string() {
    proc(" \"hello\" ");
  }

  @Test
  public void complex() {
    proc("// comment\n1234\n  'hello'  ");
  }

  @Test(expected = ScanException.class)
  public void zerolen() {
    proc("hello");
  }

  @Test
  public void complex1() {
    proc();
  }

  private void verboseRex() {
    DFACompiler.sVerbosity = true;
  }

  @Test
  public void minus1() {
    if (mark("disabled")) return;
//    verboseRex();
    proc();
  }

  @Test
  public void subsumed() {
    try {
      proc("abab bca bbc bc");
      die("expected redundant tokens");
    } catch (IllegalArgumentException e) {
      checkState(e.getMessage().contains("Subsumed"));
    }
  }

  @Test
  public void predef1() {
    proc();
  }

  @Test
  public void identifier() {
    proc("  alpha123 123 123alpha123");
  }

  @Test
  public void alpha() {
    proc(" if if  ifif  ");
  }

  @Test
  public void printStateMachine() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, cs('X'), a);
    addEdge(s, cs('Y'), b);
    addEdge(a, cs('Z'), f);
    addEdge(b, cs('W'), f);

    dump(s);
    assertSb();
  }

  @Test
  public void reverseDFA() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, cs('X'), a);
    addEdge(s, cs('Y'), b);
    addEdge(a, cs('Z'), f);
    addEdge(b, cs('W'), f);

    dump(s, "input");
    State s2 = reverseNFA(s);
    dump(s2, "reversed");
    assertSb();
  }

  @Test
  public void partition() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, cs("abcdefgh"), a);
    addEdge(s, cs("cde"), b);
    addEdge(a, cs("uvwx"), f);
    addEdge(b, cs("wxyz"), f);

    dump(s, "input");
    State s2 = partitionEdges(s);
    dump(s2, "partitioned");
    assertSb();
  }

  @Test
  public void normalizeMergeLabels() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, cs("abcd"), a);
    addEdge(s, cs("efgh"), a);
    addEdge(s, cs("cde"), b);
    addEdge(a, cs("uvwx"), f);
    addEdge(b, cs("wxyz"), f);

    dump(s, "input");
    State s2 = normalizeStates(s);
    dump(s2, "normalized");
    assertSb();
  }

  @Test
  public void normalizeOmitEmptyLabels() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, cs(""), a);
    addEdge(s, cs("cde"), b);
    addEdge(a, cs("uvwx"), f);
    addEdge(b, cs("wxyz"), f);

    dump(s, "input");
    State s2 = normalizeStates(s);
    dump(s2, "normalized");
    assertSb();
  }

  @Test
  public void acceptsEmptyStringTrue() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, CodeSet.epsilon(), a);
    addEdge(s, cs("cde"), b);

    CodeSet ck = cs("uvwx");
    ck.add(State.EPSILON);

    addEdge(a, ck, f);
    addEdge(b, cs("wxyz"), f);

    dump(s, "input");

    assertTrue(acceptsEmptyString(s, f));
  }

  @Test
  public void acceptsEmptyStringFalse() {
    State s = new State(false);
    State a = new State();
    State b = new State();
    State f = new State(true);

    addEdge(s, CodeSet.epsilon(), a);
    addEdge(s, cs("cde"), b);

    CodeSet ck = cs("uvwx");
    addEdge(a, ck, f);
    addEdge(b, cs("wxyz"), f);

    dump(s, "input");

    assertFalse(acceptsEmptyString(s, f));
  }

  private void dump(State state, Object... messages) {
    String message;
    if (messages.length == 0)
      message = name();
    else
      message = BasePrinter.toString(messages);
    sb().append(dumpStateMachine(state, message));
  }

  private StringBuilder sb() {
    if (mStringBuilder == null)
      mStringBuilder = new StringBuilder();
    return mStringBuilder;
  }

  private StringBuilder mStringBuilder;

  private void assertSb() {
    assertMessage(sb().toString());
  }

  private static CodeSet cs(char value) {
    return CodeSet.withValue(value);
  }

  private static CodeSet cs(String expr) {
    checkArgument(RegExp.patternMatchesString("\\w*", expr));
    CodeSet cs = new CodeSet();
    for (int i = 0; i < expr.length(); i++) {
      char c = expr.charAt(i);
      cs.add(c);
    }
    return cs;
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
  private boolean mDisallowUnknown;

  private void proc() {
    proc(null);
  }

  private void disallowUnknown() {
    mDisallowUnknown = true;
  }

  private void proc(String sampleText) {

    String resourceName = testName() + ".rxp";
    mScript = Files.readString(this.getClass(), resourceName);

    DFACompiler c = new DFACompiler();
    c.setVerbose(verbose());
    mDFAJson = c.parse(mScript);

    files().writeString(generatedFile("dfa.json"), mDFAJson.toString());
    //files().writeString(generatedFile("dfa_description.json"), new DFA(mDFAJson).describe().prettyPrint());

    if (sampleText == null) {
      // If there's a sample text file, read it
      String filename = testName() + ".txt";
      if (testVersion() >= 0)
        filename = testName() + testVersion() + ".txt";
      try {
        sampleText = Files.readString(this.getClass(), filename);
      } catch (FileException e) {
      }
      if (sampleText == null && testVersion() >= 0)
        badArg("missing sample text file:", filename);
    }

    if (sampleText != null) {
      StringBuilder sb = new StringBuilder();

      // Don't skip any tokens
      var s = new Scanner(dfa(), sampleText, -1);
      if (!mDisallowUnknown)
        s.setAcceptUnknownTokens();
      s.setVerbose(verbose());
      while (s.hasNext()) {
        var t = s.read();
        if (verbose())
          pr(">>", t);
        sb.append(t);
        sb.append('\n');
      }
      String result = sb.toString();
      log("Parsed tokens:", INDENT, result);
      files().writeString(generatedFile("tokens.txt"), result);
    }

    assertGenerated();
  }

  private DFA dfa() {
    return mDFAJson;
  }
  /* * / * */

  /* /* /* /*   /   *  */
  private String mScript;
  private DFA mDFAJson;
}
