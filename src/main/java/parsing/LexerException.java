package parsing;

import js.base.BasePrinter;
import js.data.DataUtil;
import js.parsing.Token;

import js.base.BasePrinter;
import js.data.DataUtil;

public final class LexerException extends RuntimeException {

  public LexerException(Lexeme token, Object... messages) {
    super(constructMessage(token, messages ));
    mToken = token;
  }

  public Lexeme token() {
    return mToken;
  }

  private static String constructMessage(  Lexeme token,Object[] messages) {
    String text = BasePrinter.toString(messages);
    if (token == null)
      return text;
    var tt = DataUtil.escapeChars(token.text(), true);
    return  token.toString() + ": " + tt + "; " + text;
  }

  private Lexeme mToken;
}