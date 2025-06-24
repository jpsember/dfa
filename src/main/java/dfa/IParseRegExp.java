package dfa;

import java.util.Map;
import js.parsing.Scanner;


@Deprecated
interface IParseRegExp {

  /**
   * Parse a regular expression
   *
   * @param scanner          scanner for parsing
   * @param tokenDefMap      a map of previously parsed regular expressions (mapping names to
   *                         ids) to be consulted if a curly brace expression appears in the
   *                         script
   * @return array of [startState, endState]
   */
  OurState[] parse(Scanner scanner, Map<String, RegParse> tokenDefMap );
}
