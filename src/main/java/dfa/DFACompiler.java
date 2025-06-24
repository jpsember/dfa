package dfa;

import java.util.List;
import java.util.Map;
import java.util.Set;

import js.base.BaseObject;
import js.file.Files;
import js.json.JSList;
import js.json.JSMap;
import js.parsing.DFA;
import js.parsing.Scanner;

import static js.base.Tools.*;
import static dfa.Util.*;

public final class DFACompiler extends BaseObject {

  private List<TokenDefinition> mTokenRecords;
  // Maps token name to token entry
  private Map<String, TokenDefinition> mTokenNameMap;
  private int next_token_id;


  private void parseExpressions(String script, boolean debug) {
    var scanner = new Scanner(getDfa(), script);
    while (scanner.hasNext()) {
      var exprId = scanner.read(TokenDefinitionParser.T_TOKENID);
      var tokenName = chomp(exprId.text(), ":");

      // Give it the next available token id, if it's not an anonymous token; else -1

      int token_id = -1;
      if (tokenName.charAt(0) != '_') {
        if (next_token_id == MAX_TOKEN_DEF)
          throw badArg("Too many token definitions");
        token_id = next_token_id;
        next_token_id++;
      }
      var rex = new TokenDefinition(token_id, tokenName);
      if (mTokenNameMap.containsKey(tokenName))
        throw exprId.failWith("Duplicate token name");

      rex.parse(scanner, mTokenNameMap);

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
    mTokenRecords = arrayList();
    mTokenNameMap = hashMap();

    // Parse the predefined expressions, and insert those lines before the current ones
    {
      var predefExpr = Files.readString(this.getClass(), "predef_expr.txt");
      if (false && alert("GETTING RID OF ALL PREDEFINEDS")) {
        predefExpr = "";
      }
      // Pretty confident these are working, so disable logging
      parseExpressions(predefExpr, false);
    }

    parseExpressions(script, true);

    State combined = combineNFAs(mTokenRecords);
    if (verbose())
      log(ToknUtils.dumpStateMachine(combined, "combined regex state machines"));

    NFAToDFA builder = new NFAToDFA();
    State dfa = builder.convertNFAToDFA(combined);
    if (verbose())
      log(ToknUtils.dumpStateMachine(dfa, "nfa to dfa"));

    List<String> redundantTokenNames = applyRedundantTokenFilter(mTokenRecords, dfa);
    if (nonEmpty(redundantTokenNames))
      badArg("Subsumed token(s) found (move them lower down in the .rxp file!):", redundantTokenNames);

    todo("!can we construct directly to a compact dfa here?");
    var jsmap =
        constructOldDFAJSMap(mTokenRecords, dfa);
    return
        convertOldDFAJSMapToCompactDFA(jsmap);
  }


  public List<String> tokenNames() {
    checkState(mTokenIds != null, "not yet available");
    return mTokenIds;
  }

  /**
   * This constructs a JSMap representing the OLD (non-compact) DFA.
   *
   * We'll need to convert it to the compact form...
   */
  private JSMap constructOldDFAJSMap(List<TokenDefinition> token_records, State startState) {
    JSMap m = map();

    m.put("version", dfaConfig().version());

    JSList list = list();
    mTokenIds = arrayList();
    for (TokenDefinition ent : token_records) {
      list.add(ent.name());
      mTokenIds.add(ent.name());
    }
    m.put("tokens", list);

    List<State> reachable = ToknUtils.reachableStates(startState);
    State finalState = null;

    Map<State, Integer> stateIndexMap = hashMap();
    List<State> orderedStates = arrayList();

    int index = 0;
    for (State s : reachable) {
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
    for (State s : orderedStates) {
      JSList stateDesc = list();

      int edgeIndex = INIT_INDEX;
      for (Edge edge : s.edges()) {
        edgeIndex++;
        int[] cr = edge.codeSets();
        checkArgument(cr.length >= 2);

        JSList out = list();
        for (int i = 0; i < cr.length; i += 2) {
          int a = cr[i];
          int b = cr[i + 1];

          out.add(a);
          out.add(b);
        }

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
  private State combineNFAs(List<TokenDefinition> token_records) {

    // Create a new distinguished start state
    //
    State start_state = new State();
    for (TokenDefinition regParse : token_records) {

      StatePair newStates = ToknUtils.duplicateNFA(regParse.startState(), regParse.endState());

      State dupStart = newStates.start;

      // Transition from the expression's end state (not a final state)
      // to a new final state, with the transitioning edge
      // labelled with the token id (actually, a transformed token id to distinguish
      // it from character codes)
      State dupEnd = newStates.end;
      State dupfinal_state = new State(true);

      CodeSet cs = CodeSet.withValue(State.tokenIdToEdgeLabel(regParse.id()));
      ToknUtils.addEdge(dupEnd, cs.elements(), dupfinal_state);

      // Add an e-transition from the start state to this expression's start

      ToknUtils.addEps(start_state, dupStart);
    }
    return start_state;
  }

  /**
   * Determine if any tokens are redundant, and report an error if so
   */
  private List<String> applyRedundantTokenFilter(List<TokenDefinition> token_records, State start_state) {
    Set<Integer> recognizedTokenIdsSet = treeSet();
    for (State state : ToknUtils.reachableStates(start_state)) {
      for (Edge edge : state.edges()) {
        if (!edge.destinationState().finalState())
          continue;
        int token_id = State.edgeLabelToTokenId(edge.codeSets()[0]);
        recognizedTokenIdsSet.add(token_id);
      }
    }

    List<String> unrecognized = arrayList();
    for (TokenDefinition rec : token_records) {
      if (recognizedTokenIdsSet.contains(rec.id()))
        continue;
      unrecognized.add(rec.name());
    }

    return unrecognized;
  }

  private List<String> mTokenIds;
}
