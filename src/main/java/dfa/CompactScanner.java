/**
 * MIT License
 *
 * Copyright (c) 2021 Jeff Sember
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import js.base.BaseObject;

import js.parsing.*;

public class CompactScanner extends BaseObject {

  private static final int SKIP_ID_NONE = -2;
  private static final boolean DEBUG = false && alert("DEBUG in effect");

  private static void p(Object... messages) {
    if (DEBUG)
      pr(insertStringToFront("CompactScanner>>>", messages));
  }

  public CompactScanner(CompactDFA dfa, String string, int skipId) {
    mDfa = dfa;
    mSkipId = (skipId < 0 ? SKIP_ID_NONE : skipId);
    mBytes = string.getBytes(StandardCharsets.UTF_8);
  }

  public CompactScanner(CompactDFA dfa, String string) {
    this(dfa, string, 0);
  }

  public void setAcceptUnknownTokens() {
    mAcceptUnknownTokens = true;
  }

  public void setSourceDescription(String description) {
    mSourceDescription = description;
  }

  protected String supplyName() {
    return mSourceDescription;
  }

  private String mSourceDescription;

  /**
   * Determine a token ahead, without reading it
   *
   * @return token, or null if end of input
   */
  public Token peek(int distance) {
    // Repeat until we've filled the history buffer with enough (non-skipped) tokens,
    // or we've reached the end of the input
    while (mHistoryCursor + distance >= mHistory.size()) {
      Token token = peekAux();
      if (token == null)
        break;

      // Advance the column, row numbers
      {
        for (int i = mLastTokenOffset; i < mLastTokenOffset + mLastTokenByteCount; i++) {
          // For windows, unix, and (modern) osx, checking for a LF is sufficient
          mColumn++;
          if (mBytes[i] == 0x0a) {
            mLineNumber++;
            mColumn = 0;
          }
        }
      }
      if (!token.id(mSkipId))
        mHistory.add(token);
    }

    Token ret = null;
    if (mHistoryCursor + distance < mHistory.size()) {
      ret = mHistory.get(mHistoryCursor + distance);
    }
    return ret;
  }

  /**
   * Determine next token, without reading it
   *
   * @return token, or null if end of input
   */
  public Token peek() {
    return peek(0);
  }

  private Token peekAux() {
    p("peekAux, nextTokenStart", mNextTokenStart, "peekByte:", peekByte(0));
    if (peekByte(0) == 0)
      return null;
    int bestLength = 1;
    int bestId = Token.ID_UNKNOWN;
    int byteOffset = 0;

    var graph = mDfa.graph();

    // <graph> ::= <int: # of states> <state>*
    //
    // <state> ::= <edge count> <edge>*
    //
    // <edge>  ::= <int: number of char_range items> <char_range>* <dest_state_id>
    //
    // <char_range> ::= <int: start of range> <int: end of range (exclusive)>
    //                | <int: -(token index + 1)>
    //
    // <dest_state_id> ::= offset of state within graph

    // The first state is always the start state
    int statePtr = 1; // skip the # of states

    while (true) {
      p(VERT_SP, "byte offset:", byteOffset);
      int ch = peekByte(byteOffset);
      p("nextByte:", ch, "state_ptr:", statePtr, "max:", graph.length);
      int nextState = -1;

      int edgeCount = graph[statePtr++];
      p("...edge count:", edgeCount);

      // Iterate over the edges
      for (var en = 0; en < edgeCount; en++) {

        p("...edge #:", en);
        boolean followEdge = false;

        // Iterate over the char_ranges
        //
        var rangeCount = graph[statePtr++];
        p("......ranges:", rangeCount);
        for (var rn = 0; rn < rangeCount; rn++) {
          // Range is either positive integers a,b or negative -x
          int first = graph[statePtr++];
          if (first < 0) { // it's a final state
            int newTokenId = -first - 1;
            p("..........token:", newTokenId, "offset:", byteOffset, "best:", bestLength);
            if (newTokenId >= bestId || byteOffset > bestLength) {
              bestLength = byteOffset;
              bestId = newTokenId;
              p("...........setting bestId:", mDfa.tokenName(bestId));
            }

          } else {
            int second = graph[statePtr++];
            p("......range #", rn, " [", first, "...", second, "]");
            // If the character is non-ascii, allow it if the range includes 128
            if (ch >= first && (ch < second || (ch >= 128 && second == 128))) {
              followEdge = true;
              p("......contains char, following edge");
            }
          }
        }
        var edgeDest = graph[statePtr++];
        if (followEdge) {
          p("...following edge to:", edgeDest);
          nextState = edgeDest;
        }
      }
      statePtr = nextState;
      p("...advanced to next state:", statePtr);
      if (statePtr < 0) {
//        if (bestId < 0)
//          halt("we couldn't find a token; bestLength:", bestLength, "bestId:", bestId,"text:", new String(mBytes, mNextTokenStart, bestLength));
        break;
      }
      byteOffset++;
    }

    String tokenText =
        new String(mBytes, mNextTokenStart, bestLength);
    mLastTokenOffset = mNextTokenStart;

    mLastTokenByteCount = bestLength;
    mNextTokenStart += bestLength;

    Token peekToken = new Token(mSourceDescription, bestId, mDfa.tokenName(bestId), tokenText,
        1 + mLineNumber,
        1 + mColumn);
    p("peek token:", INDENT, peekToken);
    if (peekToken.isUnknown() && !mAcceptUnknownTokens) {
      throw new ScanException(peekToken, "unknown token");
    }
    return peekToken;
  }

  public Token read() {
    return read(-1);
  }

  public Token read(int tokenId) {
    Token token = peek();
    if (verbose()) {
      log("read", token, tokenId >= 0 ? "(expected: " + tokenId + ")" : "");
    }

    if (token == null)
      throw new ScanException(null, "no more tokens");
    if (!mAcceptUnknownTokens && token.isUnknown())
      throw new ScanException(token, "unknown token");
    if (tokenId >= 0) {
      if (token.id() != tokenId)
        throw new ScanException(token, "unexpected token");
    }
    mHistoryCursor++;
    return token;
  }

  public Token readIf(int tokenId) {
    Token token = peek();
    boolean readIt = (token != null && tokenId == token.id());
    if (readIt) {
      read();
      return token;
    }
    return null;
  }

  public boolean hasNext() {
    return peek() != null;
  }

  public void unread() {
    unread(1);
  }

  public void unread(int count) {
    if (mHistoryCursor < count)
      throw new ScanException(null, "Token unavailable");
    mHistoryCursor -= count;
  }

  private byte peekByte(int index) {
    var absIndex = index + mNextTokenStart;
    if (absIndex < mBytes.length) {
      return mBytes[absIndex];
    }
    return 0;
  }

  private CompactDFA mDfa;
  private byte[] mBytes;
  private int mNextTokenStart;
  private int mLastTokenOffset;
  private int mLastTokenByteCount;
  private int mSkipId;
  private int mLineNumber;
  private int mColumn;
  private boolean mAcceptUnknownTokens;
  private List<Token> mHistory = arrayList();
  private int mHistoryCursor;
}
