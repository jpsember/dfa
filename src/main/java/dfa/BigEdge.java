package dfa;

/**
 * A subclass of Edge that supports source states, useful during DFA
 * construction
 */
final class BigEdge extends OurEdge {

  public BigEdge(OurState sourceState, int[] codeSet, OurState destState) {
    super(codeSet, destState);
    mSourceState = sourceState;
  }

  @Override
  public OurState sourceState() {
    return mSourceState;
  }

  private final OurState mSourceState;

}
