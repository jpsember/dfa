package dfa;

import js.parsing.Scanner;

import static js.base.Tools.*;

import java.util.Map;

public final class TokenDefinition {

  public TokenDefinition(int id, String name) {
    mId = id;
    mName = name;
  }

  public String name() {
    return mName;
  }

  public int id() {
    return mId;
  }

  /**
   * Parse a regular expression
   *
   * @param scanner     scanner
   * @param tokenDefMap a map of previously parsed regular expressions (mapping names to
   *                    ids) to be consulted when regular expression references are found
   */
  public void parse(Scanner scanner, Map<String, TokenDefinition> tokenDefMap) {
    var p = new TokenDefinitionParser();
    var states = p.parse(scanner, tokenDefMap);
    checkArgument(states[0] != null && states[1] != null);
    mStartState = states[0];
    mEndState = states[1];
  }

  public State startState() {
    checkNotNull(mStartState);
    return mStartState;
  }

  public State endState() {
    checkNotNull(mEndState);
    return mEndState;
  }

  private State mStartState;
  private State mEndState;

  private final int mId;
  private final String mName;

}

