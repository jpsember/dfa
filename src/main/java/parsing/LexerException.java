package parsing;

import js.base.BasePrinter;
import static js.base.Tools.*;

public final class LexerException extends RuntimeException {

  public LexerException(Lexeme token, Object... messages) {
    super(constructMessage(token, messages));
    mToken = token;
  }

  public Lexeme token() {
    return mToken;
  }

  private static String constructMessage(Lexeme token, Object[] messages) {
    String text = BasePrinter.toString(messages);

    var sb = new StringBuilder();

    if (token != null) {
      sb.append("-------------------------------------------------------------------------------\n");
      sb.append(token.plotWithinContext());
      sb.append("\n");
      sb.append(text);
      addLF(sb);
      sb.append("-------------------------------------------------------------------------------\n");
    }
    return sb.toString();
  }

  private Lexeme mToken;
}