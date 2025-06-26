/**
 * MIT License
 *
 * Copyright (c) 2021 Jeff Sember
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/
package dfa;

public class Edge {

  public Edge(CodeSet codeSet, State destState) {
    this(null, codeSet, destState);
  }


  public Edge(State sourceState, CodeSet codeSet, State destState) {
    mSourceState = sourceState;
    mCodeSet = codeSet;
    mDest = destState;
  }

  // !!!! source state is unused!
  public State sourceState() {
    return mSourceState;
  }

  public State destinationState() {
    return mDest;
  }


  public int[] codeSets() {
    return mCodeSet.elements();
  }

  public CodeSet codeSet() {
    return mCodeSet;
  }

  private State mDest;
  private CodeSet mCodeSet;
  private State mSourceState;

}
