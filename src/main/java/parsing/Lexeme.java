package parsing;


import java.util.List;

import static js.base.Tools.*;

public final class Lexeme {

  public static final int ID_UNKNOWN = -1;
  public static final int ID_END_OF_INPUT = -2;
  static final int ID_SKIP_NONE = -3;

  public static final Lexeme END_OF_INPUT;

  private Lexeme(int id) {
    mId = id;
  }

  static Lexeme construct(Lexer lexer, int infoPointer) {
    var info = lexer.tokenInfo();
    var x = new Lexeme(info[infoPointer + Lexer.F_TOKEN_ID]);
    x.mInfoPtr = infoPointer;
    x.mLexer = lexer;
    return x;
  }

  public Lexer lexer() {
    return mLexer;
  }

  public boolean isUnknown() {
    return mId == ID_UNKNOWN;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Id:");
    sb.append(mId);
    todo("Lexeme toString");
//    if (mSource != null) {
//      sb.append(mSource);
//      sb.append(' ');
//    }
//    sb.append("row ");
//    int c = sb.length();
//    sb.append(mRow);
//    c += 4;
//    tab(sb, c);
//    sb.append("col ");
//    sb.append(mColumn);
//    c += 7;
//    tab(sb, c);
//    {
//      String q = Integer.toString(id());
//      tab(sb, sb.length() + 3 - q.length());
//      sb.append(q);
//    }
//    c += 3;
//    tab(sb, c);
//    sb.append(':');
//    sb.append(mLexemeName);
//    c += 8;
//    tab(sb, c);
//    sb.append(' ');
//    DataUtil.escapeChars(mText, sb, true);
    return sb.toString();
  }

//  public String locInfo() {
//    StringBuilder sb = new StringBuilder();
//    if (mSource != null) {
//      sb.append(mSource);
//    }
//    sb.append("(");
//    sb.append(mRow);
//    sb.append(", ");
//    sb.append(mColumn);
//    sb.append(")");
//    return sb.toString();
//  }

  public int id() {
    return mId;
  }

  public boolean id(int value) {
    return mId == value;
  }

  // Not sure this needs to be public; for testing?
  public int infoPtr() {
    return mInfoPtr;
  }

  public String locInfo() {
    todo("Lexeme locInfo");
    return "???locinfo???";
  }


  public int row() {
    todo("Lexme text");
    return -1;
  }

  public int column() {
    todo("Lexme col");
    return -1;
  }

  public String name() {
    todo("Lexme name");
    return "???";
  }

//  public ScanException failWith(Object... messages) {
//    throw auxFail(messages);
//  }

//  @Deprecated // This doesn't throw the exception, which is very confusing;
//  public ScanException fail(Object... messages) {
//    return auxFail(messages);
//  }
//
//  private ScanException auxFail(Object... messages) {
//    String reason;
//    if (messages == null)
//      reason = "Unspecified problem";
//    else {
//      BasePrinter p = new BasePrinter();
//      p.pr(messages);
//      reason = p.toString();
//    }
//    return new ScanException(this, reason);
//  }

//  private final String mSource;

  public boolean isEndOfInput() {
    return mId == ID_END_OF_INPUT;
  }


  public String text() {
    if (mText != null)
      return mText;
    mText = mLexer.getText(mInfoPtr);
    return mText;
  }

  private final int mId;
  Lexer mLexer;
  int mInfoPtr;
  private String mText;

  static {
    var x = new Lexeme(ID_END_OF_INPUT);
    x.mText = "<END>";
    END_OF_INPUT = x;
  }


  private LexemePlotContext buildPlotContext(int width) {

    todo("last token doesn't print properly");

    final boolean DZ = false && alert("logging in effect");

    var ret = new LexemePlotContext();

    int maxLineNumber;
    {
      var info = lexer().tokenInfo();
      todo("Check that empty text still produces end of input");
      var lastLinePtr = info.length - Lexer.TOKEN_INFO_REC_LEN;
      maxLineNumber = lexer().tokenStartLineNumber(lastLinePtr);
    }
    int reqDigits = (int) Math.floor(1 + Math.log10(1 + maxLineNumber)); // Add 1 since internal line numbers start at 0
    reqDigits = Math.max(reqDigits, 4);
    ret.maxLineNumberDigits = reqDigits;

    ret.token = this;
    ret.rows = arrayList();
    ret.tokenRow = -1;

    var targetInfoPtr = infoPtr();

    if (DZ) pr("tokenContext, lexeme:", targetInfoPtr);
    if (DZ) pr("max info ptr:", lexer().tokenInfo().length);

    // Determine line number for the target lexeme
    var targetLineNumber = lexer().tokenStartLineNumber(infoPtr());

    // Look for last token that appears on line n-c-1, then
    // march forward, plotting tokens intersecting lines n-c through n+c

    var seek = 0;
    var bestSeek = -1;
    while (true) {
      if (lexer().tokenId(seek) == Lexeme.ID_END_OF_INPUT) {
        break;
      }
      var ln = lexer().tokenStartLineNumber(seek);
      if (bestSeek < 0 || ln <= targetLineNumber - width - 1) {
        bestSeek = seek;
      } else break;
      seek += Lexer.TOKEN_INFO_REC_LEN;
    }
    checkState(bestSeek >= 0);

    var textBytes = lexer().tempBytes();
    int currentCursorPos = 0;
    var currentTokenInfo = bestSeek;
    StringBuilder destSb = null;

//    int centerRowNumber = -1;

    final int TAB_WIDTH = 4;

    final boolean SHOW_TABS = false;

    while (true) {


      if (DZ) pr(VERT_SP, "plot, next token; info:", currentTokenInfo, "max:", lexer().tokenInfo().length);

      // If no more tokens, stop
      if (lexer().tokenId(currentTokenInfo) == Lexeme.ID_END_OF_INPUT)
        break;

      if (currentTokenInfo == this.infoPtr()) {
        ret.tokenColumn = currentCursorPos;
      }

      var currentLineNum = lexer().tokenStartLineNumber(currentTokenInfo);
      if (DZ) pr("...token starts at line:", currentLineNum);

      // If beyond context window, stop
      if (currentLineNum > targetLineNumber + width) {
        if (DZ) pr("...beyond window, stopping");
        break;
      }

      // If there's no receiver for the text we're going to plot, determine if
      // we should create one
      if (destSb == null) {
        if (currentLineNum >= targetLineNumber - width) {
          destSb = new StringBuilder();
          ret.firstRowLineNumber = currentLineNum + 1;
        }
        if (currentLineNum == targetLineNumber)
          ret.tokenRow = ret.rows.size();
        if (DZ) pr("built receiver:", destSb, "centerRowNumber:", ret.tokenRow);
      }
      var charIndex = lexer().tokenTextStart(currentTokenInfo);
      var tokLength = lexer().tokenLength(currentTokenInfo);

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
    if (destSb != null)
      ret.rows.add(destSb.toString());
    return ret;
  }

  public String plotWithinContext() {
    var context = buildPlotContext(2);
    return plotContext(context);
  }

  private String plotContext(LexemePlotContext context) {
    var lineNumberFormatString = "%" + context.maxLineNumberDigits + "d";
    int paddingSp = 2;

    var sb = new StringBuilder();
    int index = INIT_INDEX;
    for (var r : context.rows) {
      index++;
      if (index == 1 + context.tokenRow) {
        sb.append(spaces(context.maxLineNumberDigits + paddingSp));
        for (int j = 0; j < context.tokenColumn; j++)
          sb.append('-');
        sb.append('^');
        sb.append('\n');
      }
      sb.append(String.format(lineNumberFormatString, index + context.firstRowLineNumber));
      sb.append(": ");
      sb.append(r);
      sb.append('\n');
    }
    return sb.toString();
  }

  private static class LexemePlotContext {
    Lexeme token;
    List<String> rows;
    int tokenRow;
    int tokenColumn;
    int maxLineNumberDigits;
    int firstRowLineNumber;
  }
}
