package dfa;

import js.parsing.Scanner;
import js.parsing.Token;

import java.util.Map;

import static dfa.ToknUtils.*;
import static js.base.Tools.*;
import static dfa.Util.*;

//   One or more expressions separated by '|':
//   ------------------------------------------------
//   ALTERNATE -> CONCAT '|' ALTERNATE
//      | CONCAT
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
//   CODE_SET ->
//         a |  b |  c  ...   any printable except {,},[, etc.
//      |  \xhh                  hex value from 00...ff
//      |  \0xhh                 hex value from 00...ff
//      |  \ u hhhh                hex value from 0000...ffff (e.g., unicode)
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

  public State[] parse(Scanner scanner, Map<String, TokenDefinition> tokenDefMap) {
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
    StatePair sp = parseALTERNATE();
    mStartState = sp.start;
    mEndState = sp.end;
  }

  private boolean hasNext() {
    return mScanner.hasNext();
  }

  private boolean peekIs(int tokenId) {
    return mScanner.peek().id(tokenId);
  }

  private boolean readIf(int tokenId) {
    mReadToken = mScanner.readIf(tokenId);
    return mReadToken != null;
  }

  private Token peekToken() {
    var t = mScanner.peek();
    return t;
  }

  private Token read(int expectedTokenId) {
    return mScanner.read(expectedTokenId);
  }

  private StatePair parseALTERNATE() {
    StatePair e1 = parseCONCAT();

    if (readIf(T_ALTERNATE)) {
      StatePair e2 = parseALTERNATE();
      State u = new State();
      State v = new State();

      addEps(u, e1.start);
      addEps(u, e2.start);
      State w = e1.end;
      addEps(w, v);

      addEps(e1.end, v);
      addEps(e2.end, v);
      e1 = statePair(u, v);
    }
    return e1;
  }

  private StatePair parseCONCAT() {
    StatePair e1 = parseQUANTIFIED();
    if (hasNext() && !peekIs(T_TOKENID) && !peekIs(T_ALTERNATE) && !peekIs(T_PARCL)) {
      StatePair e2 = parseCONCAT();
      ToknUtils.addEps(e1.end, e2.start);
      e1 = statePair(e1.start, e2.end);
    }
    return e1;
  }

  private StatePair parseQUANTIFIED() {
    StatePair e1 = parsePAREN();
    if (readIf(T_ZERO_OR_MORE)) {
      ToknUtils.addEps(e1.start, e1.end);
      ToknUtils.addEps(e1.end, e1.start);
    } else if (readIf(T_ONE_OR_MORE)) {
      ToknUtils.addEps(e1.end, e1.start);
    } else if (readIf(T_ZERO_OR_ONE)) {
      ToknUtils.addEps(e1.start, e1.end);
    }
    return create_new_final_state_if_nec(e1);
  }

  private StatePair parsePAREN() {
    StatePair e1;
    var t = peekToken();
    p5("parsePAREN, next token:", t);
    if (t.id(T_PAROP)) {
      read(T_PAROP);
      e1 = parseALTERNATE();
      read(T_PARCL);
    } else if (t.id(T_RXREF)) {
      e1 = parseRegExpReference();
    } else if (t.id(T_BROP)) {
      e1 = parseBracketExpr();
    } else {
      CodeSet code_set = parse_code_set(false);
      // Construct a pair of states with an edge between them
      // labelled with this code set
      State sA = new State();
      State sB = new State();
      ToknUtils.addEdge(sA, code_set.elements(), sB);
      e1 = statePair(sA, sB);
    }
    return e1;
  }

  private StatePair parseRegExpReference() {
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
  private RuntimeException abortAtToken(Token token, Object... msgs) {
//    int i = mCursor - 1 - mCharBuffer.length();
//    StringBuilder s = new StringBuilder();
//    if (i > 4)
//      s.append("...");
//
//    for (int j = Math.max(0, i - 3); j < i; j++)
//      s.append(mScript.charAt(j));
//    s.append(" !!! ");
//    for (int j = i; j < Math.min(mScript.length(), i + 3); j++)
//      s.append(mScript.charAt(j));

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

  private CodeSet parse_code_set(boolean within_bracket_expr) {
    int val;
    p5("parse_code_set, next token:", mScanner.peek());
    if (readIf(T_ASCII)) {
      var tx = mReadToken.text();
      return CodeSet.withValue(tx.charAt(0));
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
      return CodeSet.withValue((h1 << 4) + h2);
    }
    throw badState("shouldn't have got here; next token:", mScanner.peek());
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
        if (result == null) {
          // If the set is nothing at this point, set it to include all printable characters (space...7f)
          result = CodeSet.withRange(32, 128);
        }
        break;
      }
      CodeSet nextResult;
      {
        var errToken = peekToken();
        CodeSet code_set = parse_code_set(true);
        if (readIf(T_RANGE)) {
          int u = code_set.singleValue();
          int v = parse_code_set(true).singleValue();
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

  private StatePair parseBracketExpr() {
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
    ToknUtils.addEdge(sA, result.elements(), sB);
    return statePair(sA, sB);
  }

  /**
   * If existing final state has outgoing edges, then create a new final state,
   * and add an e-transition to it from the old final state, so the final state
   * has no edges back
   */
  private StatePair create_new_final_state_if_nec(StatePair start_end_states) {
    State end_state = start_end_states.end;
    if (!end_state.edges().isEmpty()) {
      State new_final_state = new State();
      ToknUtils.addEps(end_state, new_final_state);
      start_end_states.end = new_final_state;
    }
    return start_end_states;
  }

  private static CodeSet sDigitCodeSet;
  private static CodeSet sWordCharCodeSet;

  private State mStartState;
  private State mEndState;
  private Map<String, TokenDefinition> mTokenDefMap;
  private Scanner mScanner;
  private Token mReadToken;

  // Token Ids generated by 'dev dfa' tool (DO NOT EDIT BELOW)
  public static final int T_WHITESPACE = 0;
  public static final int T_PAROP = 1;
  public static final int T_PARCL = 2;
  public static final int T_RXREF = 3;
  public static final int T_BROP = 4;
  public static final int T_BRCL = 5;
  public static final int T_BREXCEPT = 6;
  public static final int T_TOKENID = 7;
  public static final int T_ZERO_OR_MORE = 8;
  public static final int T_ZERO_OR_ONE = 9;
  public static final int T_ONE_OR_MORE = 10;
  public static final int T_ALTERNATE = 11;
  public static final int T_RANGE = 12;
  public static final int T_HEXVALUE = 13;
  public static final int T_OTHER_ESCAPE_SEQ = 14;
  public static final int T_WORD_CHAR = 15;
  public static final int T_DIGIT_CHAR = 16;
  public static final int T_FORMFEED = 17;
  public static final int T_CARRIAGERET = 18;
  public static final int T_SPACE = 19;
  public static final int T_TAB = 20;
  public static final int T_NEWLINE = 21;
  public static final int T_ASCII = 22;
  // End of token Ids generated by 'dev dfa' tool (DO NOT EDIT ABOVE)

}