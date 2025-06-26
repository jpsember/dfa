package dfa;

import java.util.List;
import java.util.Map;
import java.util.Set;

import js.base.BaseObject;
import js.file.Files;
import js.parsing.DFA;
import js.parsing.Scanner;

import static js.base.Tools.*;
import static dfa.Util.*;

/**
 * Compiles a DFA from a script of token definitions
 */
public final class DFACompiler extends BaseObject {

  public DFA parse(String script) {
    mTokenRecords = arrayList();
    mTokenNameMap = hashMap();

    // Parse the predefined expressions, and insert those lines before the current ones
    {
      var predefExpr = Files.readString(this.getClass(), "predef_expr.txt");
      if (false && alert("GETTING RID OF ALL PREDEFINEDS")) {
        predefExpr = "";
      }
      parseExpressions(predefExpr);
    }

    parseExpressions(script);

    State combined = combineNFAs(mTokenRecords);
    if (verbose())
      log(ToknUtils.dumpStateMachine(combined, "combined regex state machines"));

    State startState;
    {
      NFAToDFA builder = new NFAToDFA();
      startState = builder.convertNFAToDFA(combined);
      if (verbose())
        log(ToknUtils.dumpStateMachine(startState, "nfa to dfa"));

      List<String> redundantTokenNames = applyRedundantTokenFilter(mTokenRecords, startState);
      if (nonEmpty(redundantTokenNames))
        badArg("Subsumed token(s) found (move them lower down in the .rxp file!):", redundantTokenNames);
    }

    var bld = createBuilder(mTokenRecords, startState);
    return bld.build();
  }

  private void parseExpressions(String script) {
    var scanner = new Scanner(getDfa(), script);
    while (scanner.hasNext()) {
      var exprId = scanner.read(TokenDefinitionParser.T_TOKENID);
      var tokenName = chomp(exprId.text(), ":");

      // Give it the next available token id, if it's not an anonymous token; else -1

      int token_id = -1;
      if (tokenName.charAt(0) != '_') {
        if (mNextTokenId == MAX_TOKEN_DEF)
          throw badArg("Too many token definitions");
        token_id = mNextTokenId;
        mNextTokenId++;
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

  private DFABuilder createBuilder(List<TokenDefinition> token_records, State startState) {
    var dfaBuilder = new DFABuilder();

    List<String> tokenNames = arrayList();
    for (TokenDefinition ent : token_records) {
      tokenNames.add(ent.name());
    }
    dfaBuilder.setTokenNames(tokenNames);

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

    // Construct new states from the renamed versions
    {
      List<State> newStates = arrayList();
      for (int i = 0; i < orderedStates.size(); i++) {
        newStates.add(new State());
      }

      for (State origState : orderedStates) {
        var newState = newStates.get(stateIndexMap.get(origState));
        List<Edge> newEdges = arrayList();
        for (Edge edge : origState.edges()) {
          // Construct a new edge from this edge
          var newDestStateIndex = stateIndexMap.get(edge.destinationState());
          var newEdge = new Edge(edge.codeSets(), newStates.get(newDestStateIndex));
          newEdges.add(newEdge);
        }
        newState.setEdges(newEdges);
      }
      dfaBuilder.setStates(newStates);
    }
    return dfaBuilder;
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

      NFA newStates = ToknUtils.duplicateNFA(regParse.startState(), regParse.endState());

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

  private List<TokenDefinition> mTokenRecords;
  // Maps token name to token entry
  private Map<String, TokenDefinition> mTokenNameMap;
  private int mNextTokenId;

}
