package dfa;

import js.parsing.Lexeme;
import js.parsing.Lexer;
import js.parsing.Scanner;
import js.parsing.Token;

import java.util.Map;

import static dfa.Util.*;
import static js.base.Tools.*;

//   From lowest to highest precedence...
//
//   One or more expressions separated by '|':
//   ------------------------------------------------
//   ALTERNATE -> BINARY '|' ALTERNATE
//      | BINARY
//
//   Binary operations (conjunction, disjunction, difference)
//   -----------------------------------------------------
//   BINARY -> CONCAT '--' BINARY
//          |  CONCAT '&&' BINARY   <--- need to figure out precedence here...
//          |  CONCAT '||' BINARY
//          |  CONCAT
//
//   One or more expressions separated only by whitespace:
//   -----------------------------------------------------
//   CONCAT -> QUANTIFIED CONCAT
//      | QUANTIFIED
//
//   An expression that has an optional quantifier (*,+,?):
//   ------------------------------------------------------
//   QUANTIFIED -> PAREN '*'
//      | PAREN '+'
//      | PAREN '?'
//      | PAREN
//
//   An expression that is optionally enclosed in parentheses:
//   ------------------------------------------------------
//   PAREN -> '(' ALTERNATE ')'
//      | $TOKENNAME
//      | BRACKETEXPR
//      | CODE_SET
//
//   BRACKETEXPR -> '[' SET_OPTNEG ']'
//
//   SET_OPTNEG -> SET+
//      |  SET* '^' SET+
//
//   SET -> CODE_SET
//      | CODE_SET '-' CODE_SET
//
//   CODE_SET ->         (see rexp_parser.rxp for token definitions)
//    a |  b |  c  ...   any printable except {,},[, etc.
//      |  \xhh                  hex value from 00...ff
//      |  \f | \n | \r | \t     formfeed, linefeed, return, tab
//      |  \s                    a space (' ')
//      |  \d                    digit
//      |  \w                    word character
//      |  \*                    where * is some other non-alphabetic
//                                character that needs to be escaped
//
// The parser performs recursive descent parsing;
// each method returns an NFA represented by
// a pair of states: the start and end states.
//
public class TokenDefinitionParser {

  public State[] parse(Lexer scanner, Map<String, TokenDefinition> tokenDefMap) {
    mTokenDefMap = tokenDefMap;
    mScanner = scanner;
    parseScript();
    return new State[]{startState(), endState()};
  }

  public State startState() {
    checkNotNull(mStartState);
    return mStartState;
  }

  public State endState() {
    checkNotNull(mEndState);
    return mEndState;
  }

  private void parseScript() {
    var nfa = parseALTERNATE();
    mStartState = nfa.start;
    mEndState = nfa.end;
  }

  private boolean hasNext() {
    return mScanner.hasNext();
  }

  private boolean peekIs(int... ids) {
    var tk = mScanner.peek();
    if (tk != null) {
      var id = tk.id();
      for (int n : ids) {
        if (n == id) return true;
      }
    }
    return false;
  }

  private boolean readIf(int tokenId) {
    mReadToken = null;
    if (mScanner.readIf(tokenId))
      mReadToken = mScanner.token();
    return mReadToken != null;
  }

  private Lexeme peekToken() {
    return mScanner.peek();
  }

  private Lexeme read(int expectedTokenId) {
    return mScanner.read(expectedTokenId);
  }

  private NFA parseALTERNATE() {
    NFA e1 = parseBINARY();

    if (readIf(T_ALTERNATE)) {
      NFA e2 = parseALTERNATE();
      State u = new State();
      State v = new State();

      addEps(u, e1.start);
      addEps(u, e2.start);
      State w = e1.end;
      addEps(w, v);

      addEps(e1.end, v);
      addEps(e2.end, v);
      e1 = nfa(u, v);
    }
    return e1;
  }

  private NFA parseBINARY() {
    NFA e1 = parseCONCAT();
    if (readIf(T_MINUS)) {
      var e2 = parseBINARY();
      return BinaryOper.aMinusB(e1, e2);
    } else if (readIf(T_AND)) {
      var e2 = parseBINARY();
      return BinaryOper.aAndB(e1, e2);
    }
    return e1;
  }

  private NFA parseCONCAT() {
    NFA e1 = parseQUANTIFIED();
    if (hasNext() && !peekIs(T_TOKENID, T_ALTERNATE, T_PARCL, T_MINUS, T_AND)) {
      NFA e2 = parseCONCAT();
      addEps(e1.end, e2.start);
      e1 = nfa(e1.start, e2.end);
    }
    return e1;
  }

  private NFA parseQUANTIFIED() {
    NFA e1 = parsePAREN();
    if (readIf(T_ZERO_OR_MORE)) {
      addEps(e1.start, e1.end);
      addEps(e1.end, e1.start);
    } else if (readIf(T_ONE_OR_MORE)) {
      addEps(e1.end, e1.start);
    } else if (readIf(T_ZERO_OR_ONE)) {
      addEps(e1.start, e1.end);
    }
    return withEndStateNoOutgoingEdges(e1);
  }

  private NFA parsePAREN() {
    NFA e1;
    var t = peekToken();
    if (t.id(T_PAROP)) {
      read(T_PAROP);
      e1 = parseALTERNATE();
      read(T_PARCL);
    } else if (t.id(T_RXREF)) {
      e1 = parseRegExpReference();
    } else if (t.id(T_BROP)) {
      e1 = parseBracketExpr();
    } else {
      CodeSet code_set = parseCodeSet();
      // Construct a pair of states with an edge between them
      // labelled with this code set
      State sA = new State();
      State sB = new State();
      addEdge(sA, code_set, sB);
      e1 = nfa(sA, sB);
    }
    return e1;
  }

  private NFA parseRegExpReference() {
    var t = read(T_RXREF);
    var s = t.text();
    var nameStr = s.substring(1);
    TokenDefinition regExp = mTokenDefMap.get(nameStr);
    if (regExp == null)
      throw abortAtToken(t, "undefined token");
    return duplicateNFA(regExp.startState(), regExp.endState());
  }

  /**
   * Raise an IllegalArgumentException, with a helpful message indicating the
   * parser's current location within the text
   */
  private RuntimeException abortAtToken(Lexeme token, Object... msgs) {
    if (token == null)
      throw badState(insertStringToFront("Unexpected end of file:", msgs));
    todo("embed original line number somehow?");
    throw token.failWith(msgs);
//    throw badArg("Parse exception;", BasePrinter.toString(msgs), ":", s, mOrigLineNumber, mOrigScript);
  }

  private static CodeSet digit_code_set() {
    if (sDigitCodeSet == null) {
      CodeSet cset = new CodeSet();
      cset.add('0', 1 + '9');
      sDigitCodeSet = cset;
    }
    return sDigitCodeSet;
  }

  private static CodeSet wordchar_code_set() {
    if (sWordCharCodeSet == null) {
      CodeSet cset = new CodeSet();
      cset.add('a', 1 + 'z');
      cset.add('A', 1 + 'Z');
      cset.add('_');
      sWordCharCodeSet = cset;
    }
    return sWordCharCodeSet;
  }

  private CodeSet parseCodeSet() {
    int val;
    if (readIf(T_ASCII)) {
      var tx = mReadToken.text();
      return CodeSet.withValue(tx.charAt(0));
    } else if (readIf(T_ANY_CHARACTER)) {
      return CodeSet.withRange(1, 1 + 127);
    } else if (readIf(T_WORD_CHAR)) {
      return wordchar_code_set().dup();
    } else if (readIf(T_DIGIT_CHAR)) {
      return digit_code_set().dup();
    } else if (readIf(T_FORMFEED)) {
      return CodeSet.withValue(0x0c);
    } else if (readIf(T_CARRIAGERET)) {
      return CodeSet.withValue(0x0d);
    } else if (readIf(T_SPACE)) {
      return CodeSet.withValue(0x20);
    } else if (readIf(T_TAB)) {
      return CodeSet.withValue(0x09);
    } else if (readIf(T_NEWLINE)) {
      return CodeSet.withValue(0x0a);
    } else if (readIf(T_OTHER_ESCAPE_SEQ)) {
      var tx = mReadToken.text();
      val = ((int) tx.charAt(1)) & 0xff;
      return CodeSet.withValue(val);
    } else if (readIf(T_HEXVALUE)) {
      var tx = mReadToken.text();
      var h1 = read_hex(tx.charAt(2));
      var h2 = read_hex(tx.charAt(3));
      var value = (h1 << 4) + h2;
      if (value < 1 || value >= MAX_CHAR_CODE) {
        abortAtToken(mReadToken, "Out of range hex value");
      }
      return CodeSet.withValue(value);
    } else if (readIf(T_ILLEGAL)) {
      abortAtToken(mReadToken, "Illegal sequence; did you mean '\\x..'?'");
    }
    throw abortAtToken(peekToken(), "unexpected token");
  }

  private static int read_hex(char ch) {
    if (ch >= 'a') {
      ch = (char) (ch + ('A' - 'a'));
    }
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    checkArgument(ch >= 'A' && ch <= 'F');
    return (ch - 'A') + 10;
  }

  private CodeSet parseBracketSeq() {
    CodeSet result = null;
    while (true) {
      if (peekIs(T_BRCL) || peekIs(T_BREXCEPT)) {
        // If the set is nothing at this point, set it to include any character
        if (result == null) {
          result = CodeSet.ALL;
        }
        break;
      }
      CodeSet nextResult;
      {
        var errToken = peekToken();
        CodeSet code_set = parseCodeSet();
        if (readIf(T_RANGE)) {
          int u = code_set.singleValue();
          int v = parseCodeSet().singleValue();
          if (v < u)
            throw abortAtToken(errToken, "Illegal range; u:", u, "v:", v);
          code_set = CodeSet.withRange(u, v + 1);
        }
        nextResult = code_set;
      }
      if (result == null)
        result = nextResult;
      else {
        result.addSet(nextResult);
      }
    }
    return result;
  }

  private NFA parseBracketExpr() {
    var start = read(T_BROP);

    CodeSet rightSet = null;
    CodeSet leftSet = parseBracketSeq();
    if (readIf(T_BREXCEPT)) {
      rightSet = parseBracketSeq();
    }

    read(T_BRCL);

    if (leftSet == null && rightSet == null) {
      throw abortAtToken(start, "Empty character range");
    }
    if (leftSet == null)
      leftSet = CodeSet.withRange(1, 256);

    var result = leftSet;
    if (rightSet != null)
      result = result.difference(rightSet);

    if (result.isEmpty())
      throw abortAtToken(start, "Empty character range");

    State sA = new State();
    State sB = new State();
    addEdge(sA, result, sB);
    return nfa(sA, sB);
  }

  /**
   * If necessary, ensure end state has no outgoing edges, by adding e-transition
   * from existing end state to a new one
   */
  private NFA withEndStateNoOutgoingEdges(NFA nfa) {
    State end_state = nfa.end;
    if (!end_state.edges().isEmpty()) {
      State new_final_state = new State();
      addEps(end_state, new_final_state);
      return nfa(nfa.start, new_final_state);
    }
    return nfa;
  }

  private static NFA setMinus(NFA a, NFA b) {
    throw notSupported("setMinus");
  }


  private static CodeSet sDigitCodeSet;
  private static CodeSet sWordCharCodeSet;

  private State mStartState;
  private State mEndState;
  private Map<String, TokenDefinition> mTokenDefMap;
  private Lexer mScanner;
  private Lexeme mReadToken;

  // Token Ids generated by 'dev dfa' tool (DO NOT EDIT BELOW)
  public static final int T_WHITESPACE = 0;
  public static final int T_PAROP = 1;
  public static final int T_PARCL = 2;
  public static final int T_RXREF = 3;
  public static final int T_BROP = 4;
  public static final int T_BRCL = 5;
  public static final int T_BREXCEPT = 6;
  public static final int T_ANY_CHARACTER = 7;
  public static final int T_MINUS = 8;
  public static final int T_OR = 9;
  public static final int T_TOKENID = 10;
  public static final int T_ZERO_OR_MORE = 11;
  public static final int T_ZERO_OR_ONE = 12;
  public static final int T_ONE_OR_MORE = 13;
  public static final int T_ALTERNATE = 14;
  public static final int T_RANGE = 15;
  public static final int T_HEXVALUE = 16;
  public static final int T_OTHER_ESCAPE_SEQ = 17;
  public static final int T_WORD_CHAR = 18;
  public static final int T_DIGIT_CHAR = 19;
  public static final int T_FORMFEED = 20;
  public static final int T_CARRIAGERET = 21;
  public static final int T_SPACE = 22;
  public static final int T_TAB = 23;
  public static final int T_NEWLINE = 24;
  public static final int T_ASCII = 25;
  public static final int T_AND = 26;
  public static final int T_ILLEGAL = 27;
  // End of token Ids generated by 'dev dfa' tool (DO NOT EDIT ABOVE)

}
