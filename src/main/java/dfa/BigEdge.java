package dfa;

import js.parsing.Edge;
import js.parsing.State;

/**
 * A subclass of Edge that supports source states, useful during DFA
 * construction
 */
final class BigEdge extends Edge {

  public BigEdge(State sourceState, int[] codeSet, State destState) {
    super(codeSet, destState);
    mSourceState = sourceState;
  }

  @Override
  public State sourceState() {
    return mSourceState;
  }

  private final State mSourceState;

}
