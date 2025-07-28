package parsing;


import static js.base.Tools.*;

import js.base.BasePrinter;
import js.data.DataUtil;
import js.parsing.ScanException;

public final class Lexeme {

  public static final int ID_UNKNOWN = -1;
  public static final int ID_END_OF_INPUT = -2;

  public static final Lexeme END_OF_INPUT = new Lexeme(ID_END_OF_INPUT);
  private Lexeme(int id) {
    mId = id;
  }

//   Lexeme(String source, int id, String tokenName, String text, int lineNumber, int column) {
//    mSource = source;
//    mId = id;
//    mText = text;
//    mRow = lineNumber;
//    mColumn = column;
//    mLexemeName = tokenName;
//  }


  static Lexeme construct(Lexer lexer, int infoPointer) {
      var info = lexer.tokenInfo();
      var x = new Lexeme( info[infoPointer + Lexer.F_TOKEN_ID]);
      x.mInfoPtr = infoPointer;
      x.mLexer = lexer;
      return x;
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

  public String text() {
    todo("Lexme text");
    return "???text???";
  }

  public String locInfo() {
    todo("Lexeme locInfo");
    return "???locinfo???";
  }
//  public String source() {
//    return mSource;
//  }

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
  private final int mId;
  Lexer mLexer;
  int mInfoPtr;

  public boolean isEndOfInput() {
    return mId == ID_END_OF_INPUT;
  }
//  private final String mText;
//  private final String mLexemeName;
//  private final int mRow;
//  private final int mColumn;

}
