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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import js.base.BaseObject;
import js.parsing.ScanException;
import js.parsing.Token;

public final class Scanner extends BaseObject {
//
//  private static final int SKIP_ID_NONE = -10000;
//
//  public Scanner(OldDFA dfa, Reader reader, int skipId) {
//    mDfa = dfa;
//    mSkipId = (skipId < 0 ? SKIP_ID_NONE : skipId);
//    mReader = reader;
//  }
//
//  public Scanner(OldDFA dfa, Reader reader) {
//    this(dfa, reader, 0);
//  }
//
//  public Scanner(OldDFA dfa, String string, int skipId) {
//    this(dfa, new StringReader(string), skipId);
//  }
//
//  public Scanner(OldDFA dfa, String string) {
//    this(dfa, new StringReader(string), 0);
//  }
//
//  public void setAcceptUnknownTokens() {
//    mAcceptUnknownTokens = true;
//  }
//
//  public void setSourceDescription(String description) {
//    mSourceDescription = description;
//  }
//
//  protected String supplyName() {
//    return mSourceDescription;
//  }
//
//  private String mSourceDescription;
//
//  /**
//   * Determine a token ahead, without reading it
//   *
//   * @return token, or null if end of input
//   */
//  public OldToken peek(int distance) {
//    // Repeat until we've filled the history buffer with enough (non-skipped) tokens,
//    // or we've reached the end of the input
//    while (mHistoryCursor + distance >= mHistory.size()) {
//      OldToken token = peekAux();
//      if (token == null)
//        break;
//
//      // Advance the column, row numbers
//      String tokenText = token.text();
//      for (int i = 0; i < tokenText.length(); i++) {
//        char c = tokenText.charAt(i);
//        mColumn++;
//        if (c == '\n') {
//          mLineNumber++;
//          mColumn = 0;
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
//    return ret;
//  }
//
//  /**
//   * Determine next token, without reading it
//   *
//   * @return token, or null if end of input
//   */
//  public OldToken peek() {
//    return peek(0);
//  }
//
//  private static int stateNum(OldDFA dfa, OldState state) {
//    int i = INIT_INDEX;
//    for (var s : dfa.debStates()) {
//      i++;
//      if (s == state) return i;
//    }
//    throw badArg("can't find state:", state);
//  }
//
//  private OldToken peekAux() {
//    if (peekChar(0) < 0)
//      return null;
//    int bestLength = 1;
//    int bestId = OldDFA.UNKNOWN_TOKEN;
//    String bestTokenName = null;
//    int charOffset = 0;
//    var state = mDfa.getStartState();
//
//    while (true) {
//      int ch = peekChar(charOffset);
//      OldState nextState = null;
//      for (var edge : state.edges()) {
//        if (edge.destinationState().finalState()) {
//          int newTokenId = OldState.edgeLabelToTokenId(edge.codeSets()[0]);
//          if (newTokenId >= bestId || charOffset > bestLength) {
//            bestLength = charOffset;
//            bestId = newTokenId;
//            bestTokenName = mDfa.tokenName(newTokenId);
//          }
//        } else {
//          // If the character is non-ascii, allow it if the range includes 255
//          var effectiveCh = (ch >= 128) ? 255 : ch;
//          if (rangeContainsValue(edge.codeSets(), effectiveCh)) {
//            nextState = edge.destinationState();
//            break;
//          }
//        }
//      }
//      if (nextState == null)
//        break;
//      state = nextState;
//      charOffset++;
//    }
//    String tokenText = skipChars(bestLength);
//    var peekToken = new OldToken(mSourceDescription, bestId, bestTokenName, tokenText, 1 + mLineNumber,
//        1 + mColumn);
//    if (bestLength == 0)
//      throw new ScanException(peekToken, "scanned zero-length token");
//    return peekToken;
//  }
//
//  public OldToken read() {
//    return read(-1);
//  }
//
//  public OldToken read(int tokenId) {
//    var token = peek();
//    if (verbose()) {
//      log("read", token, tokenId >= 0 ? "(expected: " + tokenId + ")" : "");
//    }
//
//
//    if (token == null)
//      throw new ScanException(null, "no more tokens");
//    if (!mAcceptUnknownTokens && token.isUnknown())
//      throw   new ScanException(null,"unknown token");
//    if (tokenId >= 0) {
//      if (token.id() != tokenId)
//        throw new ScanException(null, "unexpected token");
//    }
//    mHistoryCursor++;
//    return token;
//  }
//
//  @Deprecated
//  public Token read(String tokenName) {
//    return read(mDfa.tokenId(tokenName));
//  }
//
//  @Deprecated
//  public Token readIf(String tokenName) {
//    return readIf(mDfa.tokenId(tokenName));
//  }
//
//  public Token readIf(int tokenId) {
//    Token token = peek();
//    boolean readIt = (token != null && tokenId == token.id());
//    if (readIt) {
//      read();
//      return token;
//    }
//    return null;
//  }
//
//  public boolean hasNext() {
//    return peek() != null;
//  }
//
//  public String nameOf(Token token) {
//    return mDfa.tokenName(token.id());
//  }
//
//  public void unread() {
//    unread(1);
//  }
//
//  public void unread(int count) {
//    if (mHistoryCursor < count)
//      throw new ScanException(null, "Token unavailable");
//    mHistoryCursor -= count;
//  }
//
//  @Deprecated
//  public int readInt(int tokenId) {
//    return (int) ensureIntegerValue(read(tokenId).text(), Integer.MIN_VALUE, Integer.MAX_VALUE);
//  }
//
//  @Deprecated
//  public static long ensureIntegerValue(String numberString, long min, long max) {
//    try {
//      long value = Long.parseLong(numberString);
//      if (value < min || value > max)
//        badArg("integral value out of range of", min, "...", max, ":", numberString);
//      return value;
//    } catch (Throwable t) {
//      throw badArg("expected an integer, not:", quote(numberString));
//    }
//  }
//
//  private int peekChar(int index) {
//    try {
//      if (index < mCharacterBuffer.length()) {
//        return mCharacterBuffer.charAt(index);
//      }
//      int nRead = mReader.read(mReaderBuffer);
//      if (nRead < 0)
//        return -1;
//      mCharacterBuffer.append(mReaderBuffer, 0, nRead);
//      return mCharacterBuffer.charAt(index);
//    } catch (IOException e) {
//      throw asRuntimeException(e);
//    }
//  }
//
//  private String skipChars(int count) {
//    String s = mCharacterBuffer.substring(0, count);
//    mCharacterBuffer.delete(0, count);
//    return s;
//  }
//
//  private static boolean rangeContainsValue(int[] range, int value) {
//    // If the value is > 255, accept it if the range contains 255
//    var effectiveValue = (value > 255) ? 255 : value;
//    int i = 0;
//    while (i < range.length) {
//      if (effectiveValue < range[i])
//        return false;
//      if (effectiveValue < range[i + 1])
//        return true;
//      i += 2;
//    }
//    return false;
//  }
//
//  private Reader mReader;
//  private OldDFA mDfa;
//  private int mSkipId;
//  private int mLineNumber;
//  private int mColumn;
//  private boolean mAcceptUnknownTokens;
//  private List<OldToken> mHistory = arrayList();
//  private int mHistoryCursor;
//  private StringBuilder mCharacterBuffer = new StringBuilder();
//  private char[] mReaderBuffer = new char[256];
}
