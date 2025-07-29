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

  public String plotWithinContext() {
    var context = lexer().buildPlotContext(this, 2);
    return lexer().plotContext(context);
  }

}
