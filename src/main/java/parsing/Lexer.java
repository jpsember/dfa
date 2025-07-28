package parsing;

import static js.base.Tools.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import js.base.BaseObject;
import js.data.ByteArray;
import js.data.IntArray;
import js.json.JSList;
import js.parsing.DFA;

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
    int i = distance + mReadIndex;
    Lexeme result;
    if (i < 0 || i >= mTokenCount) {
      return Lexeme.END_OF_INPUT;
    } else {
      var j = indexToInfoPtr(i);
      return constructLexeme(j);
    }
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

  /**
   * Read the next token
   */
  public Lexeme read() {
    return read(-1);
  }


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
    mReadIndex++;
    return x;
  }

  private void resetAction() {
    mActionLength = 0;
    mActionCursor = 0;
    mActionCursorStart = mReadIndex;
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
    var x = constructLexeme(indexToInfoPtr(mActionCursor + mActionCursorStart));
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

    var lineNumber = 0;
    var tokenStartOffset = 0;

    var infCount = 5000;

    while (inputBytes[tokenStartOffset] != 0) {
checkState(infCount-- != 0);

      int bestId = Lexeme.ID_UNKNOWN;

      var graph = mDfa.graph();
      var byteOffset = tokenStartOffset;
      int bestOffset = tokenStartOffset+1;

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


  // Action info
  private boolean mActionResult;
  private int mActionLength;
  private int mActionCursor;
  private int mActionCursorStart;


  String getText(int infoPtr) {
    var follow = infoPtr + F_TOTAL;
    var startOffset = mTokenInfo[infoPtr + F_TOKEN_OFFSET];
    var endOffset = mTokenInfo[follow + F_TOKEN_OFFSET];
    return new String(mBytes, startOffset, endOffset - startOffset, StandardCharsets.UTF_8);
  }

  private DFA mDfa;
  private byte[] mBytes;
  private int mSkipId;
  private boolean mAcceptUnknownTokens;


  private int mReadIndex;

  public static final int ID_END = -2;
  public static final int ID_ANY = -1;

}
