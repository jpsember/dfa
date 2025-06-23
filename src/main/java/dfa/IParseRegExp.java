package dfa;

import java.util.Map;

interface IParseRegExp {

  /**
   * Parse a regular expression
   *
   * @param script           script to parse
   * @param tokenDefMap      a map of previously parsed regular expressions (mapping names to
   *                         ids) to be consulted if a curly brace expression appears in the
   *                         script
   * @param sourceLineNumber for error reporting, the line number where the regular expression
   *                         came from
   * @return array of [startState, endState]
   */
  OurState[] parse(String script, Map<String, RegParse> tokenDefMap, int sourceLineNumber);
}
