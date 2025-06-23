package dfa;

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
   * @param script           script to parse
   * @param tokenDefMap      a map of previously parsed regular expressions (mapping names to
   *                         ids) to be consulted if a curly brace expression appears in the
   *                         script
   * @param sourceLineNumber for error reporting, the line number where the regular expression
   *                         came from
   */
  public void parse(String script, Map<String, RegParse> tokenDefMap, int sourceLineNumber) {

    var p = new BespokeRegParse();
    var states = p.parse(script, tokenDefMap, sourceLineNumber);
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

