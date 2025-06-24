package dfa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;
import js.parsing.RegExp;
import js.parsing.Scanner;

import static js.base.Tools.*;
import static dfa.Util.*;

public final class DFACompiler extends BaseObject {

  private List<RegParse> mTokenRecords;
  // Maps token name to token entry
  private Map<String, RegParse> mTokenNameMap;
  private int next_token_id;


  private void parseExpressions(String script, boolean debug) {
    var scanner = new Scanner(getDfa(), script);
    while (scanner.hasNext()) {
      var exprId = scanner.read(TokenRegParse.T_TOKENID);
      var tokenName = chomp(exprId.text(), ":");

      // Give it the next available token id, if it's not an anonymous token; else -1

      int token_id = -1;
      if (tokenName.charAt(0) != '_') {
        if (next_token_id == MAX_TOKEN_DEF)
          throw badArg("Too many token definitions");
        token_id = next_token_id;
        next_token_id++;
      }
      var rex = new RegParse(token_id, tokenName);
      if (mTokenNameMap.containsKey(tokenName))
        throw exprId.failWith("Duplicate token name");

      rex.parse(scanner, mTokenNameMap);
      p5("storing token name in map:", tokenName);

      mTokenNameMap.put(tokenName, rex);

      if (rex.id() < 0)
        continue;

      if (ToknUtils.acceptsEmptyString(rex.startState(), rex.endState()))
        throw exprId.failWith("Accepts zero-length tokens");

      mTokenRecords.add(rex);
      if (verbose())
        log(ToknUtils.dumpStateMachine(rex.startState(), "regex for", tokenName));
    }

  }

  public DFA parse(String script) {
    int next_token_id = 0;
    mTokenRecords = arrayList();
    mTokenNameMap = hashMap();

//    ArrayList<Integer> originalLineNumbers = arrayList();
    //  ArrayList<String> sourceLines = parseLines(script, originalLineNumbers);

    // Parse the predefined expressions, and insert those lines before the current ones
    {
      var predefExpr = parsePredefinedExpressions();
      if (false && alert("GETTING RID OF ALL PREDEFINEDS")) {
        predefExpr = "";
      }

      // Pretty confident these are working, so disable logging
      parseExpressions(predefExpr, false);

//      sourceLines.addAll(0, predefinedLines);
//      ArrayList<Integer> newLineNumbers = arrayList();
//      for (int i = 0; i < predefinedLines.size(); i++)
//        newLineNumbers.add(-1);
//      newLineNumbers.addAll(originalLineNumbers);
//      originalLineNumbers = newLineNumbers;
    }

//    // Now that we've stitched together lines where there were trailing \ characters,
//    // process each line as a complete token definition
//
//    int line_index = INIT_INDEX;
//    for (String line : sourceLines) {
//      line_index++;
//      int line_number = 1 + originalLineNumbers.get(line_index);
//
//      // Strip whitespace only from the left side (which will strip all of
//      // it, if the entire line is whitespace).  We want to preserve any
//      // special escaped whitespace on the right side.
//      line = leftTrim(line);
//
//      // If line is empty, or starts with '#', it's a comment
//      if (line.isEmpty() || line.charAt(0) == '#')
//        continue;
//
//      if (!RegExp.patternMatchesString(TOKENNAME_EXPR, line))
//        throw badArg("Syntax error:", line_number, quote(line));
//
//      int pos = line.indexOf(":");


    p5("parse script:", INDENT, script);

    parseExpressions(script, true);
//    //var scanner = new Scanner(getDfa(), script);
//
//    String tokenName = line.substring(0, pos).trim();
//
//    String expr = line.substring(pos + 1);
//    log("parsing regex:", tokenName);
//
//    // Give it the next available token id, if it's not an anonymous token; else -1
//
//    int token_id = -1;
//    if (tokenName.charAt(0) != '_') {
//      if (next_token_id == MAX_TOKEN_DEF)
//        throw badArg("Too many token definitions");
//      token_id = next_token_id;
//      next_token_id++;
//    }
//    var rex = new RegParse(token_id, tokenName);
//
//    rex.parse(expr, tokenNameMap, line_number);
//
//    if (tokenNameMap.containsKey(tokenName))
//      throw badArg("Duplicate token name", line_number, line);
//
//    tokenNameMap.put(tokenName, rex);
//
//    if (rex.id() < 0)
//      continue;
//
//    if (ToknUtils.acceptsEmptyString(rex.startState(), rex.endState()))
//      throw badArg("Zero-length tokens accepted:", line_number, line);
//
//    token_records.add(rex);
//    if (verbose())
//      log(ToknUtils.dumpStateMachine(rex.startState(), "regex for", tokenName));
//  }

    OurState combined = combineNFAs(mTokenRecords);
    if (
        verbose())

      log(ToknUtils.dumpStateMachine(combined, "combined regex state machines"));

    NFAToDFA builder = new NFAToDFA();
    if (false) // takes too long on sizeable token files
      builder.setVerbose(

          verbose());
    OurState dfa = builder.convertNFAToDFA(combined);
    if (

        verbose())

      log(ToknUtils.dumpStateMachine(dfa, "nfa to dfa"));

    List<String> redundantTokenNames = applyRedundantTokenFilter(mTokenRecords, dfa);
    if (

        nonEmpty(redundantTokenNames))

      badArg("Subsumed token(s) found (move them lower down in the .rxp file!):", redundantTokenNames);

    var jsmap =
        constructOldDFAJSMap(mTokenRecords, dfa);
    return

        convertOldDFAJSMapToCompactDFA(jsmap);
  }


  public List<String> tokenNames() {
    checkState(mTokenIds != null, "not yet available");
    return mTokenIds;
  }

//  private static ArrayList<String> parseLines(String script, ArrayList<Integer> originalLineNumbers) {
//
//    todo("better to parse these using the new regexp dfa, partitioning to tokens that start with 'xxxx:'");
//
//    ArrayList<String> sourceLines = arrayList();
//
//    // Join lines that have been ended with '\' to their following lines;
//    // only do this if there's an odd number of '\' at the end
//
//    StringBuilder accum = null;
//
//    int accum_start_line = -1;
//
//    int originalLineNumber = 0;
//
//    for (String line : split(script, '\n')) {
//      originalLineNumber++;
//
//      int trailing_backslash_count = 0;
//      while (true) {
//        if (line.length() <= trailing_backslash_count)
//          break;
//        int j = line.length() - 1 - trailing_backslash_count;
//        if (j < 0)
//          break;
//        if (line.charAt(j) != '\\')
//          break;
//        trailing_backslash_count++;
//      }
//
//      if (accum == null) {
//        accum = new StringBuilder();
//        accum_start_line = originalLineNumber;
//      }
//
//      if ((trailing_backslash_count & 1) == 1) {
//        accum.append(line.substring(0, line.length() - 1));
//      } else {
//        accum.append(line);
//        sourceLines.add(accum.toString());
//        if (originalLineNumbers != null)
//          originalLineNumbers.add(accum_start_line);
//        accum = null;
//      }
//    }
//
//    if (accum != null)
//      badArg("Incomplete final line:", INDENT, script);
//
//    return sourceLines;
//  }

  private static String leftTrim(String s) {
    String r = (s + "|").trim();
    return r.substring(0, r.length() - 1);
  }

  // Regex for token names preceding regular expressions
  private static Pattern TOKENNAME_EXPR = RegExp.pattern("[_A-Za-z][_A-Za-z0-9]*\\s*:\\s*.*");

  /**
   * This constructs a JSMap representing the OLD (non-compact) DFA.
   *
   * We'll need to convert it to the compact form...
   */
  private JSMap constructOldDFAJSMap(List<RegParse> token_records, OurState startState) {

    // These optimizations are only useful to reduce the size of the DFA files on disk,
    // and only by about 20%.  In memory, they have no effect; so for simplicity in
    // parsing the files, probably best to omit them.

    var withOpts = false;

    var withOptA = withOpts;
    var withOptB = withOpts;

    JSMap m = map();

    m.put("version", dfaConfig().version());

    JSList list = list();
    mTokenIds = arrayList();
    for (RegParse ent : token_records) {
      list.add(ent.name());
      mTokenIds.add(ent.name());
    }
    m.put("tokens", list);

    List<OurState> reachable = ToknUtils.reachableStates(startState);
    OurState finalState = null;

    Map<OurState, Integer> stateIndexMap = hashMap();
    List<OurState> orderedStates = arrayList();

    int index = 0;
    for (OurState s : reachable) {
      stateIndexMap.put(s, index);
      orderedStates.add(s);
      index++;
      if (!s.finalState())
        continue;
      checkState(finalState == null, "multiple final states");
      finalState = s;
    }
    checkState(stateIndexMap.get(startState) == 0, "unexpected start state index");
    checkState(finalState != null, "no final state found");
    int finalStateIndex = stateIndexMap.get(finalState);
    m.put("final", finalStateIndex);

    JSList states = list();
    for (OurState s : orderedStates) {
      JSList stateDesc = list();

      int edgeIndex = INIT_INDEX;
      for (OurEdge edge : s.edges()) {
        edgeIndex++;
        int[] cr = edge.codeSets();
        checkArgument(cr.length >= 2);

        JSList out = list();
        for (int i = 0; i < cr.length; i += 2) {
          int a = cr[i];
          int b = cr[i + 1];

          // Optimization:  if b==a+1, represent ...a,b,... as ...(double)a,....

          if (withOptA && b == a + 1)
            out.add((double) a);
          else {
            out.add(a);
            out.add(b);
          }
        }

        // Optimization: if resulting list has only one element, store that as a scalar
        if (withOptB && out.size() == 1)
          stateDesc.addUnsafe(out.getUnsafe(0));
        else
          stateDesc.add(out);

        int destStateIndex = stateIndexMap.get(edge.destinationState());

        // Optimization: if last edge, and destination state is the final state, omit it
        if (edgeIndex == s.edges().size() - 1 && destStateIndex == finalStateIndex)
          continue;

        stateDesc.add(destStateIndex);
      }
      states.add(stateDesc);
    }
    m.put("states", states);
    return m;
  }

  /**
   * Combine the individual NFAs constructed for the token definitions into one
   * large NFA, each augmented with an edge labelled with the appropriate token
   * identifier to let the tokenizer see which token led to the final state.
   */
  private OurState combineNFAs(List<RegParse> token_records) {

    // Create a new distinguished start state
    //
    OurState start_state = new OurState();
    for (RegParse regParse : token_records) {

      StatePair newStates = ToknUtils.duplicateNFA(regParse.startState(), regParse.endState());

      OurState dupStart = newStates.start;

      // Transition from the expression's end state (not a final state)
      // to a new final state, with the transitioning edge
      // labelled with the token id (actually, a transformed token id to distinguish
      // it from character codes)
      OurState dupEnd = newStates.end;
      OurState dupfinal_state = new OurState(true);

      CodeSet cs = CodeSet.withValue(OurState.tokenIdToEdgeLabel(regParse.id()));
      ToknUtils.addEdge(dupEnd, cs.elements(), dupfinal_state);

      // Add an e-transition from the start state to this expression's start

      ToknUtils.addEps(start_state, dupStart);
    }
    return start_state;
  }

  /**
   * Determine if any tokens are redundant, and report an error if so
   */
  private List<String> applyRedundantTokenFilter(List<RegParse> token_records, OurState start_state) {
    Set<Integer> recognizedTokenIdsSet = treeSet();
    for (OurState state : ToknUtils.reachableStates(start_state)) {
      for (OurEdge edge : state.edges()) {
        if (!edge.destinationState().finalState())
          continue;
        int token_id = OurState.edgeLabelToTokenId(edge.codeSets()[0]);
        recognizedTokenIdsSet.add(token_id);
      }
    }

    List<String> unrecognized = arrayList();
    for (RegParse rec : token_records) {
      if (recognizedTokenIdsSet.contains(rec.id()))
        continue;
      unrecognized.add(rec.name());
    }

    return unrecognized;
  }

  private String parsePredefinedExpressions() {
    String content = Files.readString(this.getClass(), "predef_expr.txt");
    return content;
  }

  private List<String> mTokenIds;
}
