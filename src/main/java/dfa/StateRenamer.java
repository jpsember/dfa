package dfa;

import java.util.List;
import java.util.Map;

import static js.base.Tools.*;
import static dfa.Util.*;

/**
 * Maintains a map of old to new states
 */
final class StateRenamer {

  /**
   * Construct new versions of all reachable states, including edges
   *
   * @param startState old starting state
   */
  public void constructNewVersionsWithEdges(State startState) {
    List<State> oldStates = reachableStates(startState);
    for (State oldState : oldStates) {
      put(oldState, null);
    }
    for (State oldState : oldStates) {
      State newState = get(oldState);
      for (Edge e : oldState.edges()) {
        addEdge(newState, e.codeSet(), get(e.destinationState()));
      }
    }
  }

  /**
   * Get list of old states, in the sequence that old->new mappings were added
   */
  public List<State> oldStates() {
    return mOldStateList;
  }

  /**
   * Map an old state to a new one
   *
   * @param oldState
   * @param newStateOrNull new state; if null, creates a copy of the old state, but without
   *                       any edges
   * @return new state
   */
  public State put(State oldState, State newStateOrNull) {
    checkArgument(oldState != null);
    State newState = newStateOrNull;
    if (newState == null)
      newState = new State(oldState.finalState());
    State prevMapping = mMap.put(oldState.id(), newState);
    if (prevMapping != null)
      badState("state already had a mapping!", oldState, prevMapping, "; cannot remap to", newState);
    mOldStateList.add(oldState);
    return newState;
  }

  /**
   * Get the new state that has been mapped to an old one; throws exception if
   * no mapping exists
   */
  public State get(State oldState) {
    checkArgument(oldState != null);
    State newState = mMap.get(oldState.id());
    if (newState == null)
      badArg("no mapping found for key:", oldState);
    return newState;
  }

  private final Map<Integer, State> mMap = hashMap();
  private final List<State> mOldStateList = arrayList();
}
