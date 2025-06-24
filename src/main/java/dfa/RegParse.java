package dfa;

import js.parsing.Scanner;

import static js.base.Tools.*;

import java.util.Map;

public final class RegParse {

  public RegParse(int id, String name) {
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
   *                    ids) to be consulted if a curly brace expression appears in the
   *                    script
   */
  public void parse(Scanner scanner, Map<String, RegParse> tokenDefMap) {
    var p = new TokenRegParse();
    var states = p.parse(scanner, tokenDefMap);
    mStartState = states[0];
    mEndState = states[1];
  }

  public OurState startState() {
    checkNotNull(mStartState);
    return mStartState;
  }

  public OurState endState() {
    checkNotNull(mEndState);
    return mEndState;
  }

  private OurState mStartState;
  private OurState mEndState;

  private final int mId;
  private final String mName;

}

