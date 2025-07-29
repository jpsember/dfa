package dfa;

import js.base.Pair;
import js.data.ByteArray;
import js.data.DataUtil;
import js.json.JSList;
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
//    // Read to one of the x's in between the two comments
//
//    Lexeme tk = null;
//    for (int i = 0; i <= 4; i++) {
//      tk = s.read();
//      pr("read token:", tk, tk.text());
//    }


    var sb = new StringBuilder();
    for (var z : y) {
      var context = tokenContext(z, 3);
      var st = plotContext(context);
      sb.append(st);
    }

    String result = sb.toString();
//    {
//      var sb = new StringBuilder();
//      sb.append("---- context ----\n");
//      sb.append("|               |\n");
//      int index = INIT_INDEX;
//      for (var r : context.rows) {
//        index++;
//        if (index == 1 + context.tokenRow) {
//          for (int j = 0; j < context.tokenColumn; j++)
//            sb.append('-');
//          sb.append('^');
//          sb.append('\n');
//        }
//        sb.append(r);
//        sb.append('\n');
//      }
//      sb.append("|               |\n");
//      sb.append("-----------------\n");
//      result = sb.toString();
//    }

    pr(result);
    assertMessage(result);
  }


  private static String plotContext(TokenContext context) {
    String result;
    {
      var sb = new StringBuilder();
      sb.append("---- context ----\n");
      sb.append("|               |\n");
      int index = INIT_INDEX;
      for (var r : context.rows) {
        index++;
        if (index == 1 + context.tokenRow) {
          for (int j = 0; j < context.tokenColumn; j++)
            sb.append('-');
          sb.append('^');
          sb.append('\n');
        }
        sb.append(r);
        sb.append('\n');
      }
      sb.append("|               |\n");
      sb.append("-----------------\n");
      result = sb.toString();
    }
    return result;
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


  public static class TokenContext {
    Lexeme token;
    List<String> rows;
    int tokenRow;
    int tokenColumn;
  }

  public static TokenContext tokenContext(Lexeme x, int width) {
final boolean DZ = false && alert("logging in effect");

    var ret = new TokenContext();
    ret.token = x;
    ret.rows = arrayList();
    ret.tokenRow = -1;

    if (false) {
      pr("determine length of each token");
      var lexer = x.lexer();
      var maxPtr = lexer.tokenInfo().length;
      for (int i = 0; i < maxPtr; i += Lexer.TOKEN_INFO_REC_LEN) {
        pr("i:", i);
        pr("id:", lexer.tokenId(i));
        if (lexer.tokenId(i) == Lexeme.ID_END_OF_INPUT) break;
        pr("ln:", lexer.tokenLength(i));
      }
      pr("...done");
    }

//    List<String> outStrings = arrayList();

    var lexer = x.lexer();
    var targetInfoPtr = x.infoPtr();


    if (DZ)  pr("tokenContext, lexeme:", targetInfoPtr);
    if (DZ)  pr("max info ptr:", lexer.tokenInfo().length);

    // Determine line number for the target lexeme
    var targetLineNumber = lexer.tokenStartLineNumber(x.infoPtr());

    // Look for last token that appears on line n-c-1, then
    // march forward, plotting tokens intersecting lines n-c through n+c

    var seek = 0;
    var bestSeek = -1;
    while (true) {
      if (lexer.tokenId(seek) == Lexeme.ID_END_OF_INPUT) {
        break;
      }
      var ln = lexer.tokenStartLineNumber(seek);
      if (bestSeek < 0 || ln <= targetLineNumber - width - 1) {
        bestSeek = seek;
      } else break;
      seek += Lexer.TOKEN_INFO_REC_LEN;
    }
    checkState(bestSeek >= 0);

    var textBytes = lexer.tempBytes();
    int currentCursorPos = 0;
    var currentTokenInfo = bestSeek;
    StringBuilder destSb = null;

//    int centerRowNumber = -1;

    final int TAB_WIDTH = 4;

    final boolean SHOW_TABS = false;

    while (true) {


      if (DZ)  pr(VERT_SP, "plot, next token; info:", currentTokenInfo, "max:", lexer.tokenInfo().length);

      // If no more tokens, stop
      if (lexer.tokenId(currentTokenInfo) == Lexeme.ID_END_OF_INPUT)
        break;

      if (currentTokenInfo == x.infoPtr()) {
        ret.tokenColumn = currentCursorPos;
      }

      var currentLineNum = lexer.tokenStartLineNumber(currentTokenInfo);
      if (DZ) pr("...token starts at line:", currentLineNum);

      // If beyond context window, stop
      if (currentLineNum > targetLineNumber + width) {
        if (DZ)   pr("...beyond window, stopping");
        break;
      }

      // If there's no receiver for the text we're going to plot, determine if
      // we should create one
      if (destSb == null) {
        if (currentLineNum >= targetLineNumber - width)
          destSb = new StringBuilder();
        if (currentLineNum == targetLineNumber)
          ret.tokenRow = ret.rows.size();
        if (DZ)  pr("built receiver:", destSb, "centerRowNumber:", ret.tokenRow);
      }
      var charIndex = lexer.tokenTextStart(currentTokenInfo);
      var tokLength = lexer.tokenLength(currentTokenInfo);

      for (int j = 0; j < tokLength; j++) {
       if (DZ) pr("...plot token char loop, index:", j, "destSb:", destSb);
        var ch = textBytes[charIndex + j];
        if (ch == '\n') {
          if (destSb != null) {
            ret.rows.add(destSb.toString());
          }
          currentCursorPos = 0;
          currentLineNum++;
          destSb = null;
          if (currentLineNum >= targetLineNumber - width && currentLineNum <= targetLineNumber + width) {
            destSb = new StringBuilder();
            if (currentLineNum == targetLineNumber)
              ret.tokenRow = ret.rows.size();
          }
        } else if (ch == '\t') {
          int tabMod = currentCursorPos % TAB_WIDTH;
          if (tabMod == 0) tabMod = TAB_WIDTH;
          if (destSb != null) {
            for (int k = 0; k < tabMod; k++) {
              final char TAB_CHAR = SHOW_TABS ? '#' : ' ';
              destSb.append(TAB_CHAR);
            }
          }
          currentCursorPos += tabMod;
        } else {
          if (destSb != null) {
            destSb.append((char) ch);
          }
          currentCursorPos++;
        }
      }

      // We've plotted a single token

      currentTokenInfo += Lexer.TOKEN_INFO_REC_LEN;
    }
    // Flush remaining strinbuilder?
    if (destSb != null)
      ret.rows.add(destSb.toString());

//    List<String> rows = arrayList();
//    rows.add("aaaa");
//    rows.add("bbb");
//    rows.add("ccc");
    return ret;
//    return pair(centerRowNumber, outStrings); //, rows);
  }
}
