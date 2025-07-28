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
import js.parsing.ScanException;
import js.parsing.Token;

public class Lexer extends BaseObject {

  private static final boolean DEBUG = true && alert("DEBUG in effect");

  private static void p(Object... messages) {
    if (DEBUG)
      pr(insertStringToFront("Scanner>>>", messages));
  }

  private static final boolean DEBUG2 = true && alert("DEBUG in effect");

  private static void p2(Object... messages) {
    if (DEBUG2)
      pr(insertStringToFront("Scanner>>>", messages));
  }

  public Lexer(DFA dfa) {
    mDfa = dfa;
  }

  public Lexer withText(CharSequence text) {
    var s = text.toString();
    withBytes(s.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  public Lexer withBytes(byte[] sourceBytes) {
    mBytes = normalizeNewlines(sourceBytes);
//    mBytes = sourceBytes;
    return this;
  }
//  public Lexer(DFA dfa, byte[] sourceBytes) {
//    mDfa = dfa;
//    mBytes = sourceBytes;
//  }

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

    final byte CR = 0x0d;

    var srcLen = sourceBytes.length;
//    var destCursor = 0;
    var srcCursor = 0;
    while (srcCursor < srcLen) {
      var srcByte = sourceBytes[srcCursor];
      if (srcByte == CR && srcCursor + 1 < srcLen && sourceBytes[srcCursor + 1] == LF) {
        // We're at a ...CR, LF... pair.
        // Don't write anything; increment the source cursor only (to skip the CR)
      } else {
        // Copy the source byte to the destination, and increment both cursors
        output.add(srcByte);
//        sourceBytes[destCursor] = srcByte;
//        destCursor++;
      }
      srcCursor++;
    }
    // pr("srcLen:", srcLen, "srcCursor:", srcCursor, "destCursor:", destCursor);


    // Return the modified bytes only if they were in fact modified
    // if (srcCursor == destCursor) return sourceBytes;
    output.add((byte) 0);
    return output.array();
//    return Arrays.copyOf(sourceBytes, destCursor);
  }


  static final int //
      F_TOKEN_OFFSET = 0,
      F_TOKEN_ID = 1,
      F_LINE_NUMBER = 2,
      F_TOTAL = 3;


  private int[] mTokenInfo;
  private int mTokenCount;
//  private byte[] mNormalizedInput;

//  public Lexer withSourceDescription(String description) {
//    mSourceDescription = description;
//  }
//
//  @Override
//  protected String supplyName() {
//    return mSourceDescription;
//  }
//
//  private String mSourceDescription;

  private static int indexToInfoPtr(int index) {
    return index * F_TOTAL;
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
    int i = distance + mHistoryCursor;
    Lexeme result;
    if (i < 0 || i >= mTokenCount) {
      result = Lexeme.END_OF_INPUT;
    } else {
      var j = indexToInfoPtr(i);
      result = constructLexeme(j);
    }
    return result;
    // discardPrevReadInfo();
//    // Repeat until we've filled the history buffer with enough (non-skipped) tokens,
//    // or we've reached the end of the input
//    while (mHistoryCursor + distance >= mHistory.size()) {
//      Token token = peekAux();
//      if (token == null)
//        break;
//
//      // Advance the column, row numbers
//      {
//        for (int i = mLastTokenOffset; i < mLastTokenOffset + mLastTokenByteCount; i++) {
//          // For windows, unix, and (modern) osx, checking for a LF is sufficient
//          mColumn++;
//          if (mBytes[i] == 0x0a) {
//            mLineNumber++;
//            mColumn = 0;
//          }
//        }
//      }
//      if (!token.id(mSkipId))
//        mHistory.add(token);
//    }
//
//    Token ret = null;
//    if (mHistoryCursor + distance < mHistory.size()) {
//      ret = mHistory.get(mHistoryCursor + distance);
//    }
//    int slot = mHistoryCursor + distance;
//    if (slot >= mTokenCount)
//    int slot = indexToInfoPtr(mHistoryCursor + distance) ;
//    if (slot > mTokenInfo.length)
//      slot = mTokenInfo.length - F_TOTAL;
////    checkState(slot >= 0 && slot < mTokenInfo.length);
//    return slot;
//    return slot;
//    return ret;
  }

  private Lexeme constructLexeme(int infoIndex) {
    return Lexeme.construct(this, infoIndex);
  }

  int[] tokenInfo() {
    return mTokenInfo;
  }

  /**
   * Look at next token, without reading it
   */
  public Lexeme peek() {
    return peek(0);
  }

//  private Token peekAux() {
//    p2("peekAux, nextTokenStart", mNextTokenStart, "peekByte:", peekByte(0));
//    if (peekByte(0) == 0)
//      return null;
//    int bestLength = 1;
//    int bestId = Token.ID_UNKNOWN;
//    int byteOffset = 0;
//
//    var graph = mDfa.graph();
//
//    // <graph> ::= <state>*
//    //
//    // <state> ::= <1 + token id, or 0> <edge count> <edge>*
//    //
//    // <edge>  ::= <char_range count> <char_range>* <dest_state_offset, low byte first>
//    //
//    // <char_range> ::= <start of range (1..127)> <size of range>
//    //
//    //
//
//
//    // The first state is always the start state
//    int statePtr = 0;
//
//    while (true) {
//      p(VERT_SP, "byte offset:", byteOffset);
//      int ch = peekByte(byteOffset);
//
//      // If the byte is -128...-1, set it to 127.
//      // The convention is that any range that includes 127 will also include these bytes.
//      if (ch < 0)
//        ch = 127;
//
//      p("nextByte:", ch, "state_ptr:", statePtr, "max:", graph.length);
//      int nextState = -1;
//
//      int newTokenId = -1;
//      var tokenCode = graph[statePtr++];
//      if (tokenCode != 0) {
//        newTokenId = (tokenCode & 0xff) - 1;
//        p("..........token:", newTokenId, "offset:", byteOffset, "best:", bestLength);
//        if (newTokenId >= bestId || byteOffset > bestLength) {
//          bestLength = byteOffset;
//          bestId = newTokenId;
//          p("...........setting bestId:", mDfa.tokenName(bestId));
//        }
//      }
//
//      int edgeCount = graph[statePtr++];
//      p("...edge count:", edgeCount);
//
//      // Iterate over the edges
//      for (var en = 0; en < edgeCount; en++) {
//
//
//        //
//        // <edge>  ::= <char_range count> <char_range>* <dest_state_offset, low byte first>
//        //
//        // <char_range> ::= <start of range (1..127)> <size of range>
//        //
//
//        p("...edge #:", en);
//        boolean followEdge = false;
//
//        // Iterate over the char_ranges
//        //
//        var rangeCount = graph[statePtr++];
//        p("......ranges:", rangeCount);
//        for (var rn = 0; rn < rangeCount; rn++) {
//          int first = graph[statePtr++];
//          int rangeSize = graph[statePtr++];
//          int posWithinRange = ch - first;
//
//          p("......range #", rn, " [", first, "...", first + rangeSize, "]");
//          if (posWithinRange >= 0 && posWithinRange < rangeSize) {
//            followEdge = true;
//            p("......contains char, following edge");
//          }
//        }
//        var edgeDest = (graph[statePtr++] & 0xff) | ((graph[statePtr++] & 0xff) << 8);
//        if (followEdge) {
//          p("...following edge to:", edgeDest);
//          nextState = edgeDest;
//        }
//      }
//      statePtr = nextState;
//      p("...advanced to next state:", statePtr);
//      if (statePtr < 0) {
//        break;
//      }
//      byteOffset++;
//    }
//
//    String tokenText =
//        new String(mBytes, mNextTokenStart, bestLength);
//    mLastTokenOffset = mNextTokenStart;
//
//    mLastTokenByteCount = bestLength;
//    mNextTokenStart += bestLength;
//
//    Token peekToken = new Token("source description no longer supp", bestId, mDfa.tokenName(bestId), tokenText,
//        1 + mLineNumber,
//        1 + mColumn);
//    p2("peek token:", INDENT, peekToken);
//    if (peekToken.isUnknown() && !mAcceptUnknownTokens) {
//      throw new ScanException(peekToken, "unknown token");
//    }
//    return peekToken;
//  }

  /**
   * Read the next token
   */
  public Lexeme read() {
    return read(-1);
  }

//  /**
//   * Read the next token.  Throws an exception if no token exists.
//   *
//   * @param tokenId if >= 0, and next token does not have this id, throws an exception
//   * @return the token
//   */
//  public Token read(int tokenId) {
//    Token token = peek();
//    if (verbose()) {
//      log("read", token, tokenId >= 0 ? "(expected: " + tokenId + ")" : "");
//    }
//
//    if (token == null)
//      throw new ScanException(null, "no more tokens");
//    if (!mAcceptUnknownTokens && token.isUnknown())
//      throw new ScanException(token, "unknown token");
//    if (tokenId >= 0) {
//      if (token.id() != tokenId)
//        throw new ScanException(token, "unexpected token");
//    }
//    mHistoryCursor++;
//    return token;
//  }


  /**
   * Read the next token.  Throws an exception if token is missing or id doesn't match the
   * expected type.  This is the same as read(int ... expectedIds), except that it returns
   * the Token as a scalar value, instead of being within a List
   *
   * @param expectedId id of expected token; or -1 to match any
   * @return the read token (as a scalar value, instead of being within a List)
   */
  public Lexeme read(int expectedId) {
    var x = peek();
    if (x.isEndOfInput())
      throw new LexerException(x, "end of input");
    if (expectedId != ID_ANY) {
      if (expectedId != x.id())
        throw new LexerException(x, "expected id:", expectedId);
    }
    mHistoryCursor++;
    return x;
  }

//
//  /**
//   * Read the next n tokens.  Throws an exception if tokens are missing or their ids do not match the expected ids.
//   *
//   * @param expectedIds ids of expected tokens; or -1 to match any
//   */
//  public List<Token> read(int... expectedIds) {
//    var result = peekIf(expectedIds);
//    if (!result)
//      throw new ScanException(peek(), "token(s) did not have expected ids");
//    return tokens();
//  }


  private void resetAction() {
    mActionLength = 0;
    mActionCursor = 0;
    mActionCursorStart = mHistoryCursor;
    mActionResult = false;
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
//    extractTokens();

    //  if (tokenIds.length == 0) throw badArg("no token ids");

    resetAction();

//    mPrevTokenCount = 0;
//    mPrevHistoryCursor = mHistoryCursor;
//    mPrevWasRead = false;

    boolean success = true;

    var distance = INIT_INDEX;
    for (var seekId : tokenIds) {
      distance++;
      var i = mHistoryCursor + distance;
      if (i >= mTokenCount) {
        success = false;
        break;
      }
      var ii = indexToInfoPtr(i);
      if (mTokenInfo[ii + F_TOKEN_ID] != seekId) {
        success = false;
        break;
      }
    }

    if (success) {
      mActionLength = tokenIds.length;
      mActionResult = success;
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
//      mPrevWasRead = true;
      mHistoryCursor += mActionLength;
      p2("...readIf, advance cursor by prev token count", mActionLength, "to", mHistoryCursor);
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
    var x = constructLexeme(indexToInfoPtr(mActionCursor + mActionCursorStart));
    mActionCursor++;
    return x;
  }

  public boolean hasNext() {
    return !peek().isEndOfInput();
  }

  private void extractTokens() {
    var inputBytes = mBytes;
    checkState(inputBytes != null, "no input bytes yet");

    var ti = IntArray.newBuilder();

    var lineNumber = 0;
    var tokenStartOffset = 0;
    while (inputBytes[tokenStartOffset] != 0) {

      int bestId = Lexeme.ID_UNKNOWN;

      var graph = mDfa.graph();
      var byteOffset = tokenStartOffset;
      int bestOffset = tokenStartOffset;

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

//          F_TOKEN_OFFSET = 0,
//          F_TOKEN_ID = 1,
//          F_LINE_NUMBER = 2,
      ti.add(tokenStartOffset);
      ti.add(bestId);
      ti.add(lineNumber);

//      var nextCursor = bestOffset;

      pr("src cursor:", tokenStartOffset, "best len:", bestOffset - tokenStartOffset, "inputBytes:", JSList.with(inputBytes));
      // increment line number for each linefeed encountered
      for (var j = tokenStartOffset; j < bestOffset; j++)
        if (inputBytes[j] == LF)
          lineNumber++;

      tokenStartOffset = bestOffset;
    }

    ti.add(tokenStartOffset);
    ti.add(ID_END);
    ti.add(lineNumber);

    mTokenInfo = ti.array();
    mTokenCount = (mTokenInfo.length / F_TOTAL) - 1;
  }


//  /**
//   * Return the tokens last read (or matched) via a call to peek(), read(), read(n), readIf(), peekIs().
//   * Throws exception if the last such method call returned false.
//   *
//   * The returned list is a view into the history, and should not be modified.
//   */
//  public List<Token> tokens() {
//    if (mActionLength == 0)
//      throw badState("no previous peekIs() or readIf() call");
//    return mHistory.subList(mPrevHistoryCursor, mPrevHistoryCursor + mActionLength);
//  }

  // Action info
  private boolean mActionResult;
  private int mActionLength;
  private int mActionCursor;
  private int mActionCursorStart;

  //  private boolean mPrevWasRead;
  private int mPrevHistoryCursor;
  private List<Token> mPeekBuffer;

//
//  @Deprecated
//  public void unread() {
//    unread(1);
//  }
//
//  @Deprecated
//  public void unread(int count) {
//    if (mHistoryCursor < count)
//      throw new ScanException(null, "Token unavailable");
//    mHistoryCursor -= count;
//  }
//
//  private byte peekByte(int index) {
//    var absIndex = index + mNextTokenStart;
//    if (absIndex < mBytes.length) {
//      return mBytes[absIndex];
//    }
//    return 0;
//  }

  private DFA mDfa;
  private byte[] mBytes;
  private int mSkipId;
  private boolean mAcceptUnknownTokens;

  private int mNextTokenStart;
  private int mLastTokenOffset;
  private int mLastTokenByteCount;
  private int mLineNumber;
  private int mColumn;
  //  private List<Token> mHistory = arrayList();
  private int mHistoryCursor;


  public static final int ID_END = -2;
  public static final int ID_ANY = -1;

}
