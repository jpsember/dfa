package dfa;

import js.base.BasePrinter;
import js.file.Files;
import js.parsing.DFA;
import js.parsing.RegExp;
import js.parsing.Scanner;
import js.parsing.Token;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static dfa.ToknUtils.*;
import static dfa.ToknUtils.addEps;
import static dfa.ToknUtils.statePair;
import static dfa.Util.OURCODEMIN;
import static dfa.Util.codeMax;
import static js.base.Tools.*;
import static js.base.Tools.hashMap;

//   Expressions have one of these types:
//
//   ALTERNATE
//   CONCAT : a Join expression, formed by concatenating one or more together
//   QUANTIFIED : a Quantified expression; followed optionally by '*', '+', or '?'
//   QUANTIFIED : a Parenthesized expression, which is optionally surrounded with (), {}, []
//

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
public class TokenRegParse implements IParseRegExp {

  /**
   * Parse a regular expression
   *
   * @param script           script to parse
   * @param tokenDefMap      a map of previously parsed regular expressions (mapping names to
   *                         ids) to be consulted if a curly brace expression appears in the
   *                         script
   * @param sourceLineNumber for error reporting, the line number where the regular expression
   *                         came from
   */
  public OurState[] parse(String script, Map<String, RegParse> tokenDefMap, int sourceLineNumber) {
    mOrigScript = script;
    mTokenDefMap = tokenDefMap;
    mOrigLineNumber = sourceLineNumber;


    // Load the DFA

    mScanner = new Scanner(getDfa(), script, T_WHITESPACE);
    parseScript();
    return new OurState[]{startState(), endState()};
  }

  public OurState startState() {
    checkNotNull(mStartState);
    return mStartState;
  }

  public OurState endState() {
    checkNotNull(mEndState);
    return mEndState;
  }


  private void parseScript() {
    StatePair sp = parseALTERNATE();
    mStartState = sp.start;
    mEndState = sp.end;
  }


  private Token mReadToken;

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
      OurState u = new OurState();
      OurState v = new OurState();

      addEps(u, e1.start);
      addEps(u, e2.start);
      OurState w = e1.end;
      addEps(w, v);
      addEps(w, v);

      addEps(e1.end, v);
      addEps(e2.end, v);
      e1 = statePair(u, v);
    }
    return e1;
  }

  private StatePair parseCONCAT() {
    StatePair e1 = parseQUANTIFIED();
    if (hasNext() && !peekIs(T_ALTERNATE) && !peekIs(T_PARCL)) {
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
    if (t.id(T_PAROP)) {
      read(T_PAROP);
      e1 = parseALTERNATE();
      read(T_PARCL);
    } else if (t.id(T_RXREF)) {
      e1 = parseTokenDef();
    } else if (t.id(T_BROP)) {
      e1 = parseBracketExpr();
    } else {
      CodeSet code_set = parse_code_set(false);
      // Construct a pair of states with an edge between them
      // labelled with this code set
      OurState sA = new OurState();
      OurState sB = new OurState();
      ToknUtils.addEdge(sA, code_set.elements(), sB);
      e1 = statePair(sA, sB);
    }
//
//
//      char ch = peek(0);
//    switch (ch) {
//      case '(': {
//        read();
//        e1 = parseALTERNATE();
//        read(')');
//      }
//      break;
//      case '^':
//        read();
//        e1 = parsePAREN();
//        e1 = construct_complement(e1);
//        break;
//      case '{':
//      case '$':
//        e1 = parseTokenDef();
//        break;
//      case '[':
//        e1 = parseBracketExpr();
//        break;
//      default: {
//        CodeSet code_set = parse_code_set(false);
//        // Construct a pair of states with an edge between them
//        // labelled with this code set
//        OurState sA = new OurState();
//        OurState sB = new OurState();
//        ToknUtils.addEdge(sA, code_set.elements(), sB);
//        e1 = statePair(sA, sB);
//      }
//      break;
    return e1;
  }


//  private static final Pattern TOKENREF_EXPR = RegExp.pattern("[_A-Za-z][_A-Za-z0-9]*");

  private StatePair parseTokenDef() {
    var t = read(T_RXREF);
    var s = t.text();
    checkArgument(s.startsWith("$"), "expected '$' start;", s);

//    final String TOKEN_CHARS = "_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
//    char delim = read();
//    StringBuilder name = new StringBuilder();
//    if (delim == '$') {
//      while (true) {
//        char q = peek(0);
//        if (!charWithin(q, TOKEN_CHARS))
//          break;
//        read();
//        name.append(q);
//      }
//    } else {
//      while (true) {
//        char q = read();
//        if (q == '}')
//          break;
//        name.append(q);
//      }
//    }
//    String nameStr = name.toString();
//    if (!RegExp.patternMatchesString(TOKENREF_EXPR, nameStr))
//      throw abort("Problem with token name");
var nameStr = s.substring(1);
    RegParse regExp = mTokenDefMap.get(nameStr);
    if (regExp == null)
      throw abort("Undefined token:", nameStr);
    return duplicateNFA(regExp.startState(), regExp.endState());
  }

// ----------------------------------------------------------------------------------------------

  /**
   * Raise an IllegalArgumentException, with a helpful message indicating the
   * parser's current location within the text
   */
  private RuntimeException abort(Object... msgs) {
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

    throw badArg("Parse exception;", BasePrinter.toString(msgs), ":", s, mOrigLineNumber, mOrigScript);
  }

//  // Read next character as a hex digit
//  //
//  private int read_hex() {
//    char v = Character.toUpperCase(read());
//    if (v >= 48 && v < 58)
//      return v - 48;
//    if (v >= 65 && v < 71)
//      return v - 65 + 10;
//    throw abort("missing hex digit");
//  }

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

  private CodeSet parse_digit_code_set() {
    read();
    read();
    return digit_code_set().dup();
  }

  private CodeSet parse_word_code_set() {
    read();
    read();
    return wordchar_code_set().dup();
  }

  private CodeSet parse_code_set(boolean within_bracket_expr) {
    int val;
    char c;

    // If starts with \, special parsing required
    if (peek(0) != '\\') {
      c = read();
      val = c;
      if (within_bracket_expr && c == '^')
        throw abort("Illegal character within [ ] expression:", c);
    } else {
      char c2 = peek(1);
      if (c2 == 'd')
        return parse_digit_code_set();
      if (c2 == 'w')
        return parse_word_code_set();

      read();

      c = read();

      if (c == '0') {
        c = read();
        if (!charWithin(c, "xX"))
          throw abort("Unsupported escape sequence:", c);
        var h1 = read_hex();
        var h2 = read_hex();
        val = (h1 << 4) | h2;
      } else if (charWithin(c, "xX")) {
        val = (read_hex() << 4) | read_hex();
      } else if (charWithin(c, "uU")) {
        val = (read_hex() << 12) | (read_hex() << 8) | (read_hex() << 4) | read_hex();
      } else {
        switch (c) {
          case 'f':
            val = '\f';
            break;
          case 'r':
            val = '\r';
            break;
          case 'n':
            val = '\n';
            break;
          case 't':
            val = '\t';
            break;
          case 's':
            val = ' ';
            break;
          default:
            // If attempting to escape a letter (that doesn't appear in the list above) or a digit,
            // that's a problem, since it is most likely not what the user intended
            final String noEscapeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            if (charWithin(c, noEscapeChars))
              throw abort("Unsupported escape sequence:", quote(c));
            val = c;
            break;
        }
      }
    }
    return CodeSet.withValue(val);
  }

  private CodeSet parseSetSeq() {
    CodeSet result = null;
    while (true) {
      {
        var ch = peek(0);
        if (ch == '^' || ch == ']')
          break;
      }

      var nextResult = parseSET();
      if (result == null)
        result = nextResult;
      else {
        result.addSet(nextResult);
      }
    }
    return result;
  }

  private StatePair parseBracketExpr() {
    checkState(false,"not impl yet; bracket expr");
    read(T_BROP);

//    read('[');

    CodeSet rightSet = null;
    CodeSet leftSet = parseSetSeq();
    if (read_if('^')) {
      rightSet = parseSetSeq();
    }

    read(']');

    if (leftSet == null && rightSet == null) {
      throw abort("Empty character range");
    }
    if (leftSet == null)
      leftSet = CodeSet.withRange(1, 256);

    var result = leftSet;
    if (rightSet != null)
      result = result.difference(rightSet);

    if (result.isEmpty())
      throw abort("Empty character range");

    OurState sA = new OurState();
    OurState sB = new OurState();
    ToknUtils.addEdge(sA, result.elements(), sB);
    return statePair(sA, sB);
  }


  private static boolean charWithin(char c, String string) {
    return string.indexOf(c) >= 0;
  }


  /**
   * If existing final state has outgoing edges, then create a new final state,
   * and add an e-transition to it from the old final state, so the final state
   * has no edges back
   */
  private StatePair create_new_final_state_if_nec(StatePair start_end_states) {
    OurState end_state = start_end_states.end;
    if (!end_state.edges().isEmpty()) {
      OurState new_final_state = new OurState();
      ToknUtils.addEps(end_state, new_final_state);
      start_end_states.end = new_final_state;
    }
    return start_end_states;
  }

//  private boolean read_if(char expChar) {
//    boolean found = (peek(0) == expChar);
//    if (found)
//      read();
//    return found;
//  }

//  /**
//   * Construct an NFA that accepts the complement of an NFA
//   */
//  private StatePair construct_complement(StatePair statesp) {
//
//    OurState nfa_start = statesp.start;
//    OurState nfa_end = statesp.end;
//    checkArgument(!nfa_start.finalState() && !nfa_end.finalState());
//
//    //nfa_end = new OurState(false, nfa_end.edges());
//
//    NFAToDFA builder = new NFAToDFA();
//    OurState dfa_start_state = builder.convertNFAToDFA(nfa_start);
//
//    List<OurState> states = ToknUtils.reachableStates(dfa_start_state);
//
//    //
//    //        + Let S be the DFA's start state
//    //        + Create F, a new final state
//    //        + for each state X in the DFA (excluding F):
//    //          + if X is a final state, clear its final state flag;
//    //          + otherwise:
//    //            + construct C, a set of labels that is the complement of the union of any existing edge labels from X
//    //            + if C is nonempty, add transition on C from X to F
//    //            + if X is not the start state, add e-transition from X to F
//    //        + augment original NFA by copying each state X to a state X' (clearing final state flags)
//    //        + return [S', F']
//    //
//    //     We don't process any final states in the above loop, because we've sort
//    //     of "lost" once we reach a final state no matter what edges leave that
//    //     state. This is because we're looking for substrings of the input string
//    //     to find matches, instead of just answering a yes/no recognition question
//    //     for an (entire) input string.
//    //
//
//    OurState f = new OurState(false, null);
//
//    for (OurState x : states) {
//      if (x.finalState())
//        throw badState("unexpected final state");
//      CodeSet codeset = CodeSet.withRange(OURCODEMIN, codeMax());
//      for (OurEdge e : x.edges()) {
//        codeset = codeset.difference(CodeSet.with(e.codeSets()));
//      }
//      if (codeset.elements().length != 0) {
//        ToknUtils.addEdge(x, codeset.elements(), f);
//      }
//      ToknUtils.addEps(x, f);
//    }
//
//    states.add(f);
//
//    // Build a map of old to new states for the NFA
//    Map<OurState, OurState> new_state_map = hashMap();
//    for (OurState x : states) {
//      OurState x_new = new OurState();
//      new_state_map.put(x, x_new);
//    }
//
//    for (OurState x : states) {
//      OurState x_new = new_state_map.get(x);
//      for (OurEdge edge : x.edges()) {
//        x_new.edges().add(new OurEdge(edge.codeSets(), new_state_map.get(edge.destinationState())));
//      }
//    }
//    return statePair(new_state_map.get(dfa_start_state), new_state_map.get(f));
//  }

  private CodeSet parseSET() {
    CodeSet code_set = parse_code_set(true);
    if (read_if('-')) {
      int u = code_set.singleValue();
      int v = parse_code_set(true).singleValue();
      if (v < u)
        throw abort("Illegal range; u:", u, "v:", v);
      code_set = CodeSet.withRange(u, v + 1);
    }
    return code_set;
  }

//  private char peek(int position) {
//    while (mCharBuffer.length() <= position) {
//      char ch = 0;
//      if (mCursor < mScript.length()) {
//        ch = mScript.charAt(mCursor);
//        mCursor++;
//      }
//      mCharBuffer.append(ch);
//    }
//    return mCharBuffer.charAt(position);
//  }
//
//  private char read() {
//    return read((char) 0);
//  }
//
//  private char read(char expChar) {
//    char ch = peek(0);
//    mCharBuffer.deleteCharAt(0);
//    if (ch != 0 && (expChar == 0 || ch == expChar))
//      return ch;
//    throw abort("Unexpected end of input");
//  }

  private static CodeSet sDigitCodeSet;
  private static CodeSet sWordCharCodeSet;

  private OurState mStartState;
  private OurState mEndState;
  private String mOrigScript;
  private Map<String, RegParse> mTokenDefMap;
  private int mOrigLineNumber;
//  private StringBuilder mCharBuffer;
//  private int mCursor;


  private static DFA getDfa() {
    todo("have utility method for caching DFAs, parsing from resources");
    var dfa = getDfa();
    if (sDFA == null) {
      sDFA = DFA.parse(Files.readString(TokenRegParse.class, "rexp_parser.dfa"));
      return sDFA;
    }

    return sDFA;
  }

  private Scanner mScanner;

  private static DFA sDFA;

  // Token Ids generated by 'dev dfa' tool (DO NOT EDIT BELOW)
  public static final int T_WHITESPACE = 0;
  public static final int T_PAROP = 1;
  public static final int T_PARCL = 2;
  public static final int T_RXREF = 3;
  public static final int T_BROP = 4;
  public static final int T_BRCL = 5;
  public static final int T_TOKENID = 6;
  public static final int T_ZERO_OR_MORE = 7;
  public static final int T_ZERO_OR_ONE = 8;
  public static final int T_ONE_OR_MORE = 9;
  public static final int T_ALTERNATE = 10;
  public static final int T_RANGE = 11;
  public static final int T_ASCII = 12;
// End of token Ids generated by 'dev dfa' tool (DO NOT EDIT ABOVE)

}