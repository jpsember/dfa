package dfa;

import js.base.BasePrinter;
import js.parsing.RegExp;

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

//    Parses a single regular expression from a string.
//    Produces an NFA with distinguished start and end states
//    (none of these states are marked as final states)
//
//    Here is the grammar for regular expressions.  Spaces are ignored,
//    and can be liberally sprinkled within the regular expressions to
//    aid readability.  To represent a space, the \s escape sequence must be used.
//    See the file 'sampletokens.txt' for some examples.
//
//   Expressions have one of these types:
//
//   E : base class
//   J : a Join expression, formed by concatenating one or more together
//   Q : a Quantified expression; followed optionally by '*', '+', or '?'
//   P : a Parenthesized expression, which is optionally surrounded with (), {}, []
//
//   E -> J '|' E
//      | J
//
//   J -> Q J
//      | Q
//
//   Q -> P '*'
//      | P '+'
//      | P '?'
//      | P
//
//   P -> '(' E ')'
//      | '{' TOKENNAME '}'
//      | '$' TOKENNAME
//      | '^' P
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
public class BespokeRegParse implements IParseRegExp {

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
    mScript = removeSpacesAndTabs(script);
    mTokenDefMap = tokenDefMap;
    mOrigLineNumber = sourceLineNumber;
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

  /**
   * Filter out all spaces and tabs, respecting escape sequences
   */
  private static String removeSpacesAndTabs(String s) {
    StringBuilder result = new StringBuilder();
    boolean escaped = false;
    for (int pos = 0; pos < s.length(); pos++) {
      int ch = s.charAt(pos);
      switch (ch) {
        case ' ':
        case '\t':
          if (escaped)
            escaped = false;
          else
            ch = -1;
          break;
        case '\\':
          escaped = !escaped;
          break;
        default:
          escaped = false;
          break;
      }
      if (ch >= 0)
        result.append((char) ch);
    }
    return result.toString();
  }

  /**
   * Raise an IllegalArgumentException, with a helpful message indicating the
   * parser's current location within the text
   */
  private RuntimeException abort(Object... msgs) {
    int i = mCursor - 1 - mCharBuffer.length();
    StringBuilder s = new StringBuilder();
    if (i > 4)
      s.append("...");

    for (int j = Math.max(0, i - 3); j < i; j++)
      s.append(mScript.charAt(j));
    s.append(" !!! ");
    for (int j = i; j < Math.min(mScript.length(), i + 3); j++)
      s.append(mScript.charAt(j));

    throw badArg("Parse exception;", BasePrinter.toString(msgs), ":", s, mOrigLineNumber, mOrigScript);
  }

  // Read next character as a hex digit
  //
  private int read_hex() {
    char v = Character.toUpperCase(read());
    if (v >= 48 && v < 58)
      return v - 48;
    if (v >= 65 && v < 71)
      return v - 65 + 10;
    throw abort("missing hex digit");
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

  private void parseScript() {
    mCharBuffer = new StringBuilder();
    mCursor = 0;
    StatePair sp = parseE();
    mStartState = sp.start;
    mEndState = sp.end;
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
    read('[');

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

  private static final Pattern TOKENREF_EXPR = RegExp.pattern("[_A-Za-z][_A-Za-z0-9]*");

  private StatePair parseTokenDef() {
    final String TOKEN_CHARS = "_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    char delim = read();
    StringBuilder name = new StringBuilder();
    if (delim == '$') {
      while (true) {
        char q = peek(0);
        if (!charWithin(q, TOKEN_CHARS))
          break;
        read();
        name.append(q);
      }
    } else {
      while (true) {
        char q = read();
        if (q == '}')
          break;
        name.append(q);
      }
    }
    String nameStr = name.toString();
    if (!RegExp.patternMatchesString(TOKENREF_EXPR, nameStr))
      throw abort("Problem with token name");

    RegParse regExp = mTokenDefMap.get(nameStr);
    if (regExp == null)
      throw abort("Undefined token:", nameStr);
    return duplicateNFA(regExp.startState(), regExp.endState());
  }

  private StatePair parseP() {
    char ch = peek(0);
    StatePair e1;
    switch (ch) {
      case '(': {
        read();
        e1 = parseE();
        read(')');
      }
      break;
      case '^':
        read();
        e1 = parseP();
        e1 = construct_complement(e1);
        break;
      case '{':
      case '$':
        e1 = parseTokenDef();
        break;
      case '[':
        e1 = parseBracketExpr();
        break;
      default: {
        CodeSet code_set = parse_code_set(false);
        // Construct a pair of states with an edge between them
        // labelled with this code set
        OurState sA = new OurState();
        OurState sB = new OurState();
        ToknUtils.addEdge(sA, code_set.elements(), sB);
        e1 = statePair(sA, sB);
      }
      break;
    }
    return e1;
  }

  private StatePair parseE() {
    StatePair e1 = parseJ();
    if (read_if('|')) {
      StatePair e2 = parseE();
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

  private static boolean charWithin(char c, String string) {
    return string.indexOf(c) >= 0;
  }

  private StatePair parseJ() {
    StatePair e1 = parseQ();
    char p = peek(0);
    if (p != 0 && !charWithin(p, "|)")) {
      StatePair e2 = parseJ();
      ToknUtils.addEps(e1.end, e2.start);
      e1 = statePair(e1.start, e2.end);
    }
    return e1;
  }

  private StatePair parseQ() {
    StatePair e1 = parseP();
    char p = peek(0);
    if (p == '*') {
      read();
      ToknUtils.addEps(e1.start, e1.end);
      ToknUtils.addEps(e1.end, e1.start);
    } else if (p == '+') {
      read();
      ToknUtils.addEps(e1.end, e1.start);
    } else if (p == '?') {
      read();
      ToknUtils.addEps(e1.start, e1.end);
    }
    return create_new_final_state_if_nec(e1);
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

  private boolean read_if(char expChar) {
    boolean found = (peek(0) == expChar);
    if (found)
      read();
    return found;
  }

  /**
   * Construct an NFA that accepts the complement of an NFA
   */
  private StatePair construct_complement(StatePair statesp) {

    OurState nfa_start = statesp.start;
    OurState nfa_end = statesp.end;
    checkArgument(!nfa_start.finalState() && !nfa_end.finalState());

    //nfa_end = new OurState(false, nfa_end.edges());

    NFAToDFA builder = new NFAToDFA();
    OurState dfa_start_state = builder.convertNFAToDFA(nfa_start);

    List<OurState> states = ToknUtils.reachableStates(dfa_start_state);

    //
    //        + Let S be the DFA's start state
    //        + Create F, a new final state
    //        + for each state X in the DFA (excluding F):
    //          + if X is a final state, clear its final state flag;
    //          + otherwise:
    //            + construct C, a set of labels that is the complement of the union of any existing edge labels from X
    //            + if C is nonempty, add transition on C from X to F
    //            + if X is not the start state, add e-transition from X to F
    //        + augment original NFA by copying each state X to a state X' (clearing final state flags)
    //        + return [S', F']
    //
    //     We don't process any final states in the above loop, because we've sort
    //     of "lost" once we reach a final state no matter what edges leave that
    //     state. This is because we're looking for substrings of the input string
    //     to find matches, instead of just answering a yes/no recognition question
    //     for an (entire) input string.
    //

    OurState f = new OurState(false, null);

    for (OurState x : states) {
      if (x.finalState())
        throw badState("unexpected final state");
      CodeSet codeset = CodeSet.withRange(OURCODEMIN, codeMax());
      for (OurEdge e : x.edges()) {
        codeset = codeset.difference(CodeSet.with(e.codeSets()));
      }
      if (codeset.elements().length != 0) {
        ToknUtils.addEdge(x, codeset.elements(), f);
      }
      ToknUtils.addEps(x, f);
    }

    states.add(f);

    // Build a map of old to new states for the NFA
    Map<OurState, OurState> new_state_map = hashMap();
    for (OurState x : states) {
      OurState x_new = new OurState();
      new_state_map.put(x, x_new);
    }

    for (OurState x : states) {
      OurState x_new = new_state_map.get(x);
      for (OurEdge edge : x.edges()) {
        x_new.edges().add(new OurEdge(edge.codeSets(), new_state_map.get(edge.destinationState())));
      }
    }
    return statePair(new_state_map.get(dfa_start_state), new_state_map.get(f));
  }

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

  private char peek(int position) {
    while (mCharBuffer.length() <= position) {
      char ch = 0;
      if (mCursor < mScript.length()) {
        ch = mScript.charAt(mCursor);
        mCursor++;
      }
      mCharBuffer.append(ch);
    }
    return mCharBuffer.charAt(position);
  }

  private char read() {
    return read((char) 0);
  }

  private char read(char expChar) {
    char ch = peek(0);
    mCharBuffer.deleteCharAt(0);
    if (ch != 0 && (expChar == 0 || ch == expChar))
      return ch;
    throw abort("Unexpected end of input");
  }

  private static CodeSet sDigitCodeSet;
  private static CodeSet sWordCharCodeSet;

  private OurState mStartState;
  private OurState mEndState;
  private String mOrigScript;
  private String mScript;
  private Map<String, RegParse> mTokenDefMap;
  private int mOrigLineNumber;
  private StringBuilder mCharBuffer;
  private int mCursor;


}