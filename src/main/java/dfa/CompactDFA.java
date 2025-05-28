package dfa;

import js.json.JSList;
import js.json.JSMap;

import static js.base.Tools.*;

/**
 * A new, more compact representation of a DFA
 */

// {
//   "version" : 5.0,
//   "tokens" : "space-delimited set of token names",
//   "graph"  : [ array of integers, described below ]
// }
//
// <graph> ::= <int: # of states> <state>*
//
// <state> ::= <edge count> <edge>*
//
// <edge>  ::= <int: number of char_range items> <char_range>* <dest_state_id>
//
// <char_range> ::= <int: start of range> <int: end of range (exclusive)>
//                | <int: -(token index + 1)>
//
// <dest_state_id> ::= offset of state within graph
//
public class CompactDFA {

  public static double VERSION = 5.0;

  public CompactDFA(double version, String tokenNames, short[] graph) {
    checkArgument(version == VERSION, "bad version:", version, "; expected", VERSION);
    mVersion = version;
    var sep = split(tokenNames, ' ');
    mTokenNames = sep.toArray(new String[0]);
    mGraph = graph;
  }

  public JSMap toJson() {
    var m = map();
    m.put("version", mVersion);
    m.put("token_names", String.join(" ", mTokenNames));
    m.put("graph", JSList.with(mGraph));
    return m;
  }

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  private double mVersion = 5.0;
  private String[] mTokenNames;
  private short[] mGraph;
}
