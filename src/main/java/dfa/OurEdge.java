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

/**
 * This is our own, DISTINCT class, and no longer using the one (which may not exist soon)
 * in the core library; so I'm renaming it.
 */
public class OurEdge {

  public OurEdge(int[] codeSets, OurState destState) {
    this(null, codeSets, destState);
  }

  public OurEdge(OurState sourceState, int[] codeSets, OurState destState) {
    mSourceState = sourceState;
    mCodeSets = codeSets;
    mDest = destState;
  }

  public OurState sourceState() {
    return mSourceState;
  }

  public OurState destinationState() {
    return mDest;
  }

  public int[] codeSets() {
    return mCodeSets;
  }

  private OurState mDest;
  private int[] mCodeSets;
  private OurState mSourceState;

}
