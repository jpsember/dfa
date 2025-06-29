package dfa;

import js.base.BaseObject;
import js.base.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static js.base.Tools.*;
import static dfa.Util.*;

public class BinaryOper extends BaseObject {

  // 'OR' is not useful, as the alternation operator '|' serves this purpose already.
  //
  public enum OperationCode {
    AND, MINUS,
  }

  public static NFA aMinusB(NFA a, NFA b) {
    var oper = new BinaryOper(a, b, OperationCode.MINUS);
    return oper.result();
  }

  public static NFA aAndB(NFA a, NFA b) {
    var oper = new BinaryOper(a, b, OperationCode.AND);
    return oper.result();
  }

  private static long encodeFactorIds(int a, int b) {
    checkArgument(a > 0 && b > 0);
    return a | (((long) b) << 32);
  }

  private static int factorAId(long encoded) {
    return (int) encoded;
  }

  private static int factorBId(long encoded) {
    return (int) (encoded >> 32);
  }


  // Maps product ids to encoded pair of factor ids
  //
  private Map<Integer, Long> mProductIdToFactorIdPairMap = hashMap();

  // Maps state ids to states
  //
  private Map<Integer, State> mStateIdToStateMap = hashMap();

  // Stack of unexamined product states
  //
  private List<State> mSearchFrontier = arrayList();

  // Map of encoded pair of factor ids to product state
  private Map<Long, State> mFactorIdPairToProductStateMap = hashMap();

  private void extendFrontier(State state) {
    mSearchFrontier.add(state);
  }

  private State popFrontier() {
    return pop(mSearchFrontier);
  }

  private Pair<State, State> getFactorStates(State productState) {
    var encoded = encodedFactorIdPairForProductState(productState);
    if (false)
      log("encoded id pair for product state:", productState.id(), idPairToStr(encoded));
    return pair(stateWithId(factorAId(encoded)), stateWithId(factorBId(encoded)));
  }

  private long encodedFactorIdPairForProductState(State productState) {
    var encoded = mProductIdToFactorIdPairMap.get(productState.id());
    checkNotNull(encoded, "can't find product state in factor ids map:", productState.id());
    return encoded;
  }

  private State stateWithId(int id) {
    var state = mStateIdToStateMap.get(id);
    checkNotNull(state, "no state found for id:", id);
    return state;
  }

  private void addStatesToMap(Collection<State> states) {
    for (var s : states) {
      mStateIdToStateMap.put(s.id(), s);
    }
  }

  private BinaryOper(NFA a, NFA b, OperationCode oper) {
    mA = a;
    mB = b;
    mOper = oper;
  }

  private OperationCode mOper;

  private NFA mA, mB;
  private NFA mResult;

  public NFA result() {
    if (mResult != null) return mResult;

    var a = mA;
    var b = mB;

    var a2 = toDFA(a);
    var b2 = toDFA(b);

    // Add all the states of the two DFAs to the state map
    addStatesToMap(a2.states);
    addStatesToMap(b2.states);

    // Partition the edge labels into disjoint codesets,
    // and construct new versions of the reachable states

    var par = new RangePartition();

    {
      par.addStateCodeSets(a2.states);
      par.addStateCodeSets(b2.states);
      // Replace existing edges with partitioned versions
      par.apply(a2.states);
      par.apply(b2.states);
    }

    // construct the product NFA of these two.
    var productStart = constructProductState(a2.startState, b2.startState);
    var productEnd = new State();

    // Initialize frontier to the start state
    extendFrontier(productStart);

    Set<CodeSet> workCodeSets = hashSet();

    // Ensure that every state has an edge for *every* possible edge label, by
    // adding edges to the sink state for missing ones.

    // This is actually only necessary for the MINUS operation

    if (mOper == OperationCode.MINUS) {
      workCodeSets.addAll(par.getPartition());
    }

    // Continue searching until the frontier is empty
    //
    while (!mSearchFrontier.isEmpty()) {
      var productState = popFrontier();

      // Determine the two factor states
      var factors = getFactorStates(productState);
      var fa = factors.first;
      var fb = factors.second;

      // Construct the set of labels that appear in either of the edges.
      // Construct an edge for each label in the set, sending to the sink state(s) where appropriate.

      // For MINUS, we've preconstructed the set of ALL possible edges.
      //
      // For the others, we only want to add those labels that appear on the two edges.
      //
      if (mOper != OperationCode.MINUS) {
        workCodeSets.clear();
        for (var x : fa.edges()) workCodeSets.add(x.codeSet());
        for (var x : fb.edges()) workCodeSets.add(x.codeSet());
      }

      for (var x : workCodeSets) {
        // determine target states for this label
        var aTarget = a2.sinkState;
        for (var y : fa.edges()) {
          if (x.equals(y.codeSet())) {
            aTarget = y.destinationState();
            break;
          }
        }
        var bTarget = b2.sinkState;
        for (var y : fb.edges()) {
          if (x.equals(y.codeSet())) {
            bTarget = y.destinationState();
            break;
          }
        }

        var destProductId = productId(aTarget, bTarget);

        // Look for an existing product state with this id.  If not found,
        // create one, and add it to the frontier

        var destProductState = mFactorIdPairToProductStateMap.get(destProductId);
        if (destProductState == null) {
          destProductState = constructProductState(aTarget, bTarget);
          mSearchFrontier.add(destProductState);
        }

        // Add edge to product graph

        var edge = new Edge(x, destProductState);
        productState.edges().add(edge);
      }
    }

    // for each product state that has been marked as a final state,
    // clear that flag, and add an epsilon edge to the end state
    for (var productState : mFactorIdPairToProductStateMap.values()) {
      if (productState.finalState()) {
        productState.setFinal(false);
        productState.edges().add(new Edge(CodeSet.epsilon(), productEnd));
      }
    }
    mResult = new NFA(productStart, productEnd);
    return mResult;
  }

  /**
   * Construct a product state representing two factor states, and add to the appropriate data structures
   */
  private State constructProductState(State a, State b) {
    // Set final state according to the binary operation

    boolean finalState;
    switch (mOper) {
      case MINUS:
        finalState = a.finalState() && !b.finalState();
        break;
      case AND:
        finalState = a.finalState() && b.finalState();
        break;
      default:
        throw notSupported();
    }

    var abProduct = new State(finalState);
    var existing = mStateIdToStateMap.put(abProduct.id(), abProduct);
    checkState(existing == null, "state already in map");
    var encodedIdPair = encodeFactorIds(a.id(), b.id());
    mProductIdToFactorIdPairMap.put(abProduct.id(), encodedIdPair);
    mFactorIdPairToProductStateMap.put(encodedIdPair, abProduct);
    return abProduct;
  }

  private static long productId(State a, State b) {
    var aId = a.id();
    var bId = b.id();
    return aId + (((long) bId) << 32L);
  }

  private static String idPairToStr(long encoded) {
    return String.format("[ %4d | %4d ]", factorAId(encoded), factorBId(encoded));
  }

  /**
   * Convert NFA to a DFA, and add a sink non-final state
   */
  private AugDFA toDFA(NFA nfa) {
    // We have to make the end state a final state
    nfa.end.setFinal(true);
    var xDFA = NFAToDFA.convert(nfa.start);
    var xAug = new AugDFA(xDFA);
    return xAug;
  }

  /**
   * A bookkeeping class for a DFA that includes a sink state (a non-final state with
   * no outgoing edges)
   */
  private static class AugDFA {

    AugDFA(State startState) {
      this.startState = startState;
      sinkState = new State();
      // Construct a list of states, including the sink state
      states = reachableStates(startState);
      states.add(sinkState);
    }

    State startState;
    State sinkState;
    List<State> states;
  }
}
