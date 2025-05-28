package dfa;

import java.util.List;
import java.util.Map;

import static js.base.Tools.*;

/**
 * Maintains a map of old to new states
 */
final class StateRenamer {

  /**
   * Construct new versions of all reachable states, omitting edges
   * 
   * @param oldStartState
   *          old starting state
   */
  public void constructNewVersions(OurState oldStartState) {
    OurState.bumpDebugIds();
    List<OurState> oldStates = ToknUtils.reachableStates(oldStartState);
    for (OurState oldState : oldStates)
      put(oldState, null);
  }

  /**
   * Construct new versions of all reachable states, including edges
   * 
   * @param oldStartState
   *          old starting state
   */
  public void constructNewVersionsWithEdges(OurState oldStartState) {
    OurState.bumpDebugIds();
    List<OurState> oldStates = ToknUtils.reachableStates(oldStartState);
    for (OurState oldState : oldStates) {
      put(oldState, null);
    }
    for (OurState oldState : oldStates) {
      OurState newState = get(oldState);
      for (OurEdge e : oldState.edges()) {
        ToknUtils.addEdge(newState, e.codeSets(), get(e.destinationState()));
      }
    }
  }

  /**
   * Get list of old states, in the sequence that old->new mappings were added
   */
  public List<OurState> oldStates() {
    return mOldStateList;
  }

  /**
   * Map an old state to a new one
   * 
   * @param oldState
   * @param newStateOrNull
   *          new state; if null, creates a copy of the old state, but without
   *          any edges
   * @return new state
   */
  public OurState put(OurState oldState, OurState newStateOrNull) {
    checkArgument(oldState != null);
    OurState newState = newStateOrNull;
    if (newState == null)
      newState = new OurState(oldState.finalState());
    OurState prevMapping = mMap.put(oldState, newState);
    if (prevMapping != null)
      badState("state already had a mapping!", oldState, prevMapping, "; cannot remap to", newState);
    mOldStateList.add(oldState);
    return newState;
  }

  /**
   * Get the new state that has been mapped to an old one; throws exception if
   * no mapping exists
   */
  public OurState get(OurState oldState) {
    checkArgument(oldState != null);
    OurState newState = mMap.get(oldState);
    if (newState == null)
      badArg("no mapping found for key:", oldState);
    return newState;
  }

  private final Map<OurState, OurState> mMap = hashMap();
  private final List<OurState> mOldStateList = arrayList();
}
