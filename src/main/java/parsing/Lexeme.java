package parsing;

import static js.base.Tools.*;

public final class Lexeme {

  public static final int ID_UNKNOWN = -1;
  public static final int ID_END_OF_INPUT = -2;
  public static final int ID_SKIP_NONE = -3;

    Lexeme(int id) {
    mId = id;
  }

  static Lexeme construct(Lexer lexer, int infoPointer) {
    var info = lexer.tokenInfo();
    var x = new Lexeme(info[infoPointer + Lexer.F_TOKEN_ID]);
    x.mInfoAddress = infoPointer;
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
    sb.append("#");
    var c = sb.length();
    sb.append(mId);
    c += 4;
    tab(sb, c);

    sb.append(name());
    c += 10;
    tab(sb, c);

    sb.append(' ');
    sb.append(locInfo());
    c += 10;
    tab(sb, c);
    var text = text();
    text = text.replace("\n", "\\n");
    var m = 50;
    var m2 = (m - 5) / 2;
    if (text.length() > m) {
      text = text.substring(0, m2) + " ... " + text.substring(text.length() - m2);
    }
    sb.append('"');
    sb.append(text);
    sb.append('"');
    return sb.toString();
  }

  public int id() {
    return mId;
  }

  public boolean id(int value) {
    return mId == value;
  }

  public int infoAddress() {
    return mInfoAddress;
  }

  public String locInfo() {
    return "(" + (1 + row()) + ":" + (1 + column()) + ")";
  }

  public int row() {
    return mLexer.tokenStartLineNumber(mInfoAddress);
  }

  public int column() {
    return mLexer.calculateColumnNumber(mInfoAddress);
  }

  public String name() {
    var dfa = lexer().dfa();
    switch (id()) {
      case ID_UNKNOWN:
        return ("<UNKNOWN>");
      case ID_END_OF_INPUT:
        return ("<END>");
    }
    return dfa.tokenName(id());
  }

  public LexerException failWith(Object... messages) {
    throw new LexerException(this, messages);
  }

  public boolean isEndOfInput() {
    return mId == ID_END_OF_INPUT;
  }

  public String text() {
    switch (mId) {
      case ID_END_OF_INPUT:
        return "<END>";
      case ID_UNKNOWN:
        return "<UNKNOWN>";
      default:
        return mLexer.dfa().tokenName(mId);
    }
  }

  public String plotWithinContext() {
    var context = lexer().buildPlotContext(this, 2);
    return lexer().plotContext(context);
  }


  private final int mId;
  private Lexer mLexer;
  private int mInfoAddress;

}
