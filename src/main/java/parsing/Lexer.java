package parsing;

import static js.base.Tools.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import js.base.BaseObject;
import js.data.ByteArray;
import js.data.IntArray;
import js.json.JSList;
import js.parsing.DFA;

public class Lexer extends BaseObject {

  private static final boolean DEBUG = false && alert("DEBUG in effect");

  private static void p(Object... messages) {
    if (DEBUG)
      pr(insertStringToFront("Scanner>>>", messages));
  }

  private static final boolean DEBUG2 = false && alert("DEBUG in effect");

  private static void p2(Object... messages) {
    if (DEBUG2)
      pr(insertStringToFront("Scanner>>>", messages));
  }

  public Lexer(DFA dfa) {
    mDfa = dfa;
  }

  public Lexer withText(CharSequence text) {
    todo("do we want to trigger a reset if already started?");
    var s = text.toString();
    withBytes(s.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  public Lexer withBytes(byte[] sourceBytes) {
    mBytes = normalizeNewlines(sourceBytes);
    return this;
  }

  public Lexer withSkipId(int skipId) {
    mSkipId = skipId;
    return this;
  }

  public Lexer withAcceptUnknownTokens() {
    mAcceptUnknownTokens = true;
    return this;
  }

  private static final byte LF = 0x0a;

  public static byte[] normalizeNewlines(byte[] sourceBytes) {
    var output = ByteArray.newBuilder();
    todo("use constants for CR, TAB, etc");
    final byte CR = 0x0d;
    var srcLen = sourceBytes.length;
    var srcCursor = 0;
    while (srcCursor < srcLen) {
      var srcByte = sourceBytes[srcCursor];
      if (srcByte == CR && srcCursor + 1 < srcLen && sourceBytes[srcCursor + 1] == LF) {
        // We're at a ...CR, LF... pair.
        // Don't write anything; increment the source cursor only (to skip the CR)
      } else {
        // Copy the source byte to the destination, and increment both cursors
        output.add(srcByte);
      }
      srcCursor++;
    }
    // Add zero marking the end of input
    output.add((byte) 0);
    return output.array();
  }


  static final int //
      F_TOKEN_OFFSET = 0,
      F_TOKEN_ID = 1,
      F_LINE_NUMBER = 2,
      F_TOTAL = 3;

  public static int TOKEN_INFO_REC_LEN = F_TOTAL;

  private int filteredIndexToInfoPtr(int index) {
    return mFilteredOffsets[index];
  }

  /**
   * Look at an upcoming token, without reading it
   *
   * @param distance number of tokens to look ahead (0 is next token, 1 is the one after that, ...)
   * @return Lexeme
   */
  public Lexeme peek(int distance) {
    assertStarted();
    resetAction();
    int i = distance + mReadIndex;
    Lexeme result;
    if (i < 0 || i >= mTokenCount) {
      return Lexeme.END_OF_INPUT;
    } else {
      var j = filteredIndexToInfoPtr(i);
      return constructLexeme(j);
    }
  }

  private Lexeme constructLexeme(int infoIndex) {
    return Lexeme.construct(this, infoIndex);
  }

  public int[] tokenInfo() {
    return mTokenInfo;
  }

  /**
   * Look at next token, without reading it
   */
  public Lexeme peek() {
    return peek(0);
  }

  /**
   * Read the next token
   */
  public Lexeme read() {
    return read(Lexeme.ID_SKIP_NONE);
  }


  /**
   * Read the next token.  Throws an exception if token is missing or id doesn't match the
   * expected type.  This is the same as read(int ... expectedIds), except that it returns
   * the Token as a scalar value, instead of being within a List
   *
   * @param expectedId id of expected token, or Lexeme.ID_SKIP_NONE
   * @return the read token (as a scalar value, instead of being within a List)
   */
  public Lexeme read(int expectedId) {
    var x = peek();
    if (x.isEndOfInput())
      throw new LexerException(x, "end of input");
    if (expectedId != Lexeme.ID_SKIP_NONE) {
      if (expectedId != x.id())
        throw new LexerException(x, "expected id:", expectedId);
    }
    mReadIndex++;
    return x;
  }

  private void resetAction() {
    mActionLength = 0;
    mActionCursor = 0;
    mActionCursorStart = mReadIndex;
  }

  public Lexer start() {
    assertNotStarted();
    extractTokens();
    resetAction();
    return this;
  }

  private void assertNotStarted() {
    checkState(mTokenInfo == null, "already started");
  }

  private void assertStarted() {
    checkState(mTokenInfo != null, "not started");
  }

  /**
   * Determine if the next n tokens exist and match the specified ids
   */
  public boolean peekIf(int... tokenIds) {
    p2("peekIs, tokenIds:", tokenIds);
    assertStarted();

    resetAction();

    boolean success = true;

    var distance = INIT_INDEX;
    for (var seekId : tokenIds) {
      distance++;
      var i = mReadIndex + distance;
      if (i >= mTokenCount) {
        success = false;
        break;
      }
      var ii = filteredIndexToInfoPtr(i);
      if (!idMatch(tokenId(ii), seekId)) {
        success = false;
        break;
      }
    }

    if (success) {
      mActionLength = tokenIds.length;
      p2("...setting prev token count:", mActionLength);
    }
    return success;
  }

  /**
   * If the next n tokens exist and match the specified ids, read them, and return true
   */
  public boolean readIf(int... tokenIds) {
    var result = peekIf(tokenIds);
    if (result) {
      mReadIndex += mActionLength;
      p2("...readIf, advance cursor by prev token count", mActionLength, "to", mReadIndex);
    }
    return result;
  }

  /**
   * Return the next token read or matched via call to peekIf() or readIf()
   */
  public Lexeme token() {
    assertStarted();
    if (mActionLength == 0)
      throw badState("no previous action");
    if (mActionCursor == mActionLength)
      throw badState("no tokens remain in action");
    var x = constructLexeme(filteredIndexToInfoPtr(mActionCursor + mActionCursorStart));
    mActionCursor++;
    return x;
  }

  public boolean hasNext() {
    assertStarted();
    return mReadIndex < mTokenCount;
  }

  private void extractTokens() {
    var inputBytes = mBytes;
    checkState(inputBytes != null, "no input bytes yet");

    var ti = IntArray.newBuilder();

    var filteredPtrs = IntArray.newBuilder();

    var lineNumber = 0;
    var tokenStartOffset = 0;

    var infCount = 5000;

    while (inputBytes[tokenStartOffset] != 0) {
      checkState(infCount-- != 0);

      int bestId = Lexeme.ID_UNKNOWN;

      var graph = mDfa.graph();
      var byteOffset = tokenStartOffset;
      int bestOffset = tokenStartOffset + 1;

      // <graph> ::= <state>*
      //
      // <state> ::= <1 + token id, or 0> <edge count> <edge>*
      //
      // <edge>  ::= <char_range count> <char_range>* <dest_state_offset, low byte first>
      //
      // <char_range> ::= <start of range (1..127)> <size of range>
      //
      //
      // The first state is always the start state
      int statePtr = 0;

      p(VERT_SP, "extracting next token", DASHES, CR, "best offset:", bestOffset, "best id:", bestId);
      while (true) {
        if (DEBUG) {
          p(VERT_SP, "byte offset:", byteOffset);
          if (inputBytes.length < 20) {
            p("inp:", JSList.with(Arrays.copyOfRange(inputBytes, byteOffset, inputBytes.length)));
          }
        }
        byte ch = inputBytes[byteOffset];

        // If the byte is -128...-1, set it to 127.
        // The convention is that any range that includes 127 will also include these bytes.
        if (ch < 0)
          ch = 127;

        p("nextByte:", ch, "state_ptr:", statePtr);
        int nextState = -1;

        var tokenCode = graph[statePtr++];
        if (tokenCode != 0) {
          int newTokenId = (tokenCode & 0xff) - 1;
          p("..........token:", newTokenId, "offset:", byteOffset, "best:", bestOffset);
          if (newTokenId >= bestId || byteOffset > bestOffset) {
            bestOffset = byteOffset;
            bestId = newTokenId;
            p("...........setting bestId:", mDfa.tokenName(bestId));
          }
        }

        int edgeCount = graph[statePtr++];
        p("...edge count:", edgeCount);

        // Iterate over the edges
        for (var en = 0; en < edgeCount; en++) {

          //
          // <edge>  ::= <char_range count> <char_range>* <dest_state_offset, low byte first>
          //
          // <char_range> ::= <start of range (1..127)> <size of range>
          //
          p("...edge #:", en);
          boolean followEdge = false;

          // Iterate over the char_ranges
          //
          var rangeCount = graph[statePtr++];
          p("......ranges:", rangeCount);
          for (var rn = 0; rn < rangeCount; rn++) {
            int first = graph[statePtr++];
            int rangeSize = graph[statePtr++];
            int posWithinRange = ch - first;

            p("......range #", rn, " [", first, "...", first + rangeSize, "]");
            if (posWithinRange >= 0 && posWithinRange < rangeSize) {
              followEdge = true;
              p("......contains char, following edge");
            }
          }
          var edgeDest = (graph[statePtr++] & 0xff) | ((graph[statePtr++] & 0xff) << 8);
          if (followEdge) {
            p("...following edge to:", edgeDest);
            nextState = edgeDest;
          }
        }
        statePtr = nextState;
        p("...advanced to next state:", statePtr);
        if (statePtr < 0) {
          break;
        }
        byteOffset++;
      }

      p("bestId:", bestId, "skip:", mSkipId, "filteredPtrs size:", filteredPtrs.size());
      if (bestId != mSkipId) {
        p("........adding offset to token info array:", ti.size());
        filteredPtrs.add(ti.size());
      }
      ti.add(tokenStartOffset);
      ti.add(bestId);
      ti.add(lineNumber);

      // increment line number for each linefeed encountered
      for (var j = tokenStartOffset; j < bestOffset; j++)
        if (inputBytes[j] == LF)
          lineNumber++;

      tokenStartOffset = bestOffset;
    }

    // Add a final entry so we can calculate the length of the last token
    {
      ti.add(tokenStartOffset);
      ti.add(Lexeme.ID_END_OF_INPUT);
      ti.add(lineNumber);
    }
    mTokenInfo = ti.array();
    mFilteredOffsets = filteredPtrs.array();
    mTokenCount = mFilteredOffsets.length;
  }

  private static boolean idMatch(int tokenId, int matchExpr) {
    return matchExpr == Lexeme.ID_UNKNOWN || matchExpr == tokenId;
  }

  // Action info
  private int mActionLength;
  private int mActionCursor;
  private int mActionCursorStart;


  public int tokenTextStart(int infoPtr) {
    return mTokenInfo[infoPtr + F_TOKEN_OFFSET];
  }

  String getText(int infoPtr) {
    var startOffset = tokenTextStart(infoPtr);
    var len = tokenLength(infoPtr);
    return new String(mBytes, startOffset, len, StandardCharsets.UTF_8);
  }

  private DFA mDfa;
  private byte[] mBytes;
  /*private*/ public int mSkipId = Lexeme.ID_SKIP_NONE;
  private boolean mAcceptUnknownTokens;
  private int mReadIndex;
  private int[] mTokenInfo;
  private int[] mFilteredOffsets;
  private int mTokenCount;


  public int tokenStartLineNumber(int infoPtr) {
    return mTokenInfo[infoPtr + F_LINE_NUMBER];
  }

  public int tokenId(int infoPtr) {
    return mTokenInfo[infoPtr + F_TOKEN_ID];
  }

  public int tokenLength(int infoPtr) {
    var r0 = infoPtr;
    var r1 = infoPtr + F_TOTAL;
    checkArgument(r1 < mTokenInfo.length);
    var result = tokenTextStart(r1) - tokenTextStart(r0);
    checkArgument(result > 0, "token length <= 0!");
    return result;
  }


  String plotContext(LexemePlotContext context) {

    // Try to keep the start of the token within view.
    // Later we might get fancy and try to center the entire token
    // (if it doesn't contain linfeeds)...

    final int TEXT_COLUMNS = 40; //110;

    var textLeft = Math.max(0, context.tokenColumn - (int) (TEXT_COLUMNS * .4f));

    var lineNumberFormatString = "%" + context.maxLineNumberDigits + "d";

    var maxTextColumns = TEXT_COLUMNS - context.maxLineNumberDigits;

    int paddingSp = 2;

    var sb = new StringBuilder();
    int maxIndex = Math.max(context.rows.size(), context.tokenRow + 2);
    for (var index = 0; index < maxIndex; index++) {
      String r = null;
      if (index < context.rows.size())
        r = context.rows.get(index);

      if (index == 1 + context.tokenRow) {
        sb.append(spaces(context.maxLineNumberDigits + paddingSp));
        var arrLength = context.tokenColumn - textLeft;
        for (int j = 0; j < arrLength; j++)
          sb.append('-');
        sb.append('^');
        sb.append('\n');
      }
      if (r == null) continue;
      sb.append(String.format(lineNumberFormatString, index + context.firstRowLineNumber));
      sb.append(": ");

      var textRight = Math.min(r.length(), maxTextColumns + textLeft);
      if (textRight > textLeft) {
        sb.append(r.substring(textLeft, textRight));
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  private int determineMaxDigits() {
    int maxLineNumber;
    {
      var info = tokenInfo();
      var lastLinePtr = info.length - Lexer.TOKEN_INFO_REC_LEN;
      maxLineNumber = tokenStartLineNumber(lastLinePtr);
    }
    int reqDigits = (int) Math.floor(1 + Math.log10(1 + maxLineNumber)); // Add 1 since internal line numbers start at 0
    reqDigits = Math.max(reqDigits, 4);
    return reqDigits;
  }

  LexemePlotContext buildPlotContext(Lexeme lexeme, int width) {

    var ret = new LexemePlotContext();
    ret.maxLineNumberDigits = determineMaxDigits();
    ret.token = lexeme;
    ret.rows = arrayList();
    ret.tokenRow = -1;

    // Determine line number for the target lexeme
    var targetLineNumber = tokenStartLineNumber(lexeme.mInfoPtr);

    // Look for last token that appears on line n-c-1, then
    // march forward, plotting tokens intersecting lines n-c through n+c

    todo("use binary search here");

    pr("seeking for targetLine", targetLineNumber, "-width", width, "-1", targetLineNumber - width - 1);
    var seek = 0;
    var bestSeek = -1;
    while (true) {
      if (tokenId(seek) == Lexeme.ID_END_OF_INPUT) {
        break;
      }
      var ln = tokenStartLineNumber(seek);
      if (bestSeek < 0 || ln <= targetLineNumber - width - 1) {
        bestSeek = seek;
        pr("bestSeek:", bestSeek);
      } else break;
      seek += Lexer.TOKEN_INFO_REC_LEN;
    }
    checkState(bestSeek >= 0);

    var textBytes = mBytes;
    int currentCursorPos = 0;
    var currentTokenInfo = bestSeek;
    StringBuilder destSb = null;

    final int TAB_WIDTH = 4;

    final boolean SHOW_TABS = false;

    while (true) {
      // If no more tokens, stop
      if (tokenId(currentTokenInfo) == Lexeme.ID_END_OF_INPUT)
        break;

      if (currentTokenInfo == lexeme.infoPtr()) {
        ret.tokenColumn = currentCursorPos;
      }

      var currentLineNum = tokenStartLineNumber(currentTokenInfo);

      // If beyond context window, stop
      if (currentLineNum > targetLineNumber + width) {
        break;
      }

      // If there's no receiver for the text we're going to plot, determine if
      // we should create one
      if (destSb == null) {
        // if (currentLineNum >= targetLineNumber - width) {
        destSb = new StringBuilder();
        ret.firstRowLineNumber = currentLineNum + 1;
        //}
        if (currentLineNum == targetLineNumber)
          ret.tokenRow = ret.rows.size();
      }
      var charIndex = tokenTextStart(currentTokenInfo);
      var tokLength = tokenLength(currentTokenInfo);

      for (int j = 0; j < tokLength; j++) {
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
    if (destSb != null)
      ret.rows.add(destSb.toString());
    return ret;
  }

  private static class LexemePlotContext {
    Lexeme token;
    List<String> rows;
    int tokenRow;
    int tokenColumn;
    int maxLineNumberDigits;
    int firstRowLineNumber;

    @Override
    public String toString() {
      var m = map();
      m.put("", "LexemePlotContext");
      m.put("rows", JSList.with(rows));
      m.put("tokenRow", tokenRow);
      m.put("tokenColumn", tokenColumn);
      m.put("maxLineNumberDigits", maxLineNumberDigits);
      m.put("firstRowLineNumber", firstRowLineNumber);
      return m.prettyPrint();
    }
  }


}
