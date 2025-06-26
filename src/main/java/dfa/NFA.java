package dfa;

/**
 * Represents an NFA, as references to its start and (single) end states
 */
final class NFA {

  NFA(State start, State end) {
    this.start = start;
    this.end = end;
  }

  final State start;
  final State end;
}