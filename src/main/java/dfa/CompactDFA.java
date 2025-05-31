package dfa;

import js.data.ShortArray;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;
import js.parsing.Token;

import java.nio.charset.StandardCharsets;
import java.util.List;

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

  public static String VERSION = "$1";

  public CompactDFA(String version, String[] tokenNames, short[] graph) {
    checkArgument(version.equals(VERSION), "bad version:", version, "; expected", VERSION);
    mVersion = version;
    mTokenNames = tokenNames;
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
    return toJson().toString();
  }

  public short[] graph() {
    return mGraph;
  }

  public String tokenName(int id) {
    if (id == DFA.UNKNOWN_TOKEN)
      return "<UNKNOWN>";
    return mTokenNames[id];
  }

  public String[] tokenNames() {
    return mTokenNames;
  }

  private String mVersion;
  private String[] mTokenNames;
  private short[] mGraph;


  // ----------------------------------------------------------------------------------------------
  // Parsing from a string
  // ----------------------------------------------------------------------------------------------

  /**
   * Parse a DFA from a string.
   *
   * It looks within the string (which might be a JSMap) for these fields:
   *
   * 1)  ( '0'...'9' '-')+
   * => an integer within the graph
   * 2)  '$' (any character except '$')*
   * => version (e.g. $42)
   * 3)  ('A'..'Z')+
   * => token id
   */
  public static CompactDFA parse(String str) {

    // Set default values
    //
    String version = VERSION;
    List<String> tokenNames = arrayList();
    var nums = ShortArray.DEFAULT_INSTANCE.newBuilder();

    var strBytes = str.getBytes(StandardCharsets.UTF_8);
    var i = 0;
    while (i < strBytes.length) {
      var j = i;
      var b = strBytes[i];
      if (b == '$') {
        while (true) {
          i++;
          b = strBytes[i];
          if (b == '"') {
            version = new String(strBytes, j, i - j);
            break;
          }
        }
      } else if (isUpper(b)) {
        while (true) {
          i++;
          if (!isUpper(strBytes[i])) {
            tokenNames.add(new String(strBytes, j, i - j));
            break;
          }
        }
      } else if (isNumber(b)) {
        while (true) {
          i++;
          if (!isNumber(strBytes[i])) {
            nums.add(Short.parseShort(new String(strBytes, j, i - j)));
            break;
          }
        }
      } else {
        i++;
      }
    }
    return new CompactDFA(version, tokenNames.toArray(new String[0]), nums.array());
  }

  // Parser helper functions

  private static boolean isUpper(byte b) {
    return b >= 'A' && b <= 'Z';
  }

  private static boolean isNumber(byte b) {
    return (b >= '0' && b <= '9') || b == '-';
  }

  public int numTokens() {
    return mTokenNames.length;
  }
}
