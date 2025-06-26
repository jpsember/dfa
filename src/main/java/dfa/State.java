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

import java.util.ArrayList;
import java.util.List;

import static js.base.Tools.*;

public final class State implements Comparable<State> {

  public State(boolean finalState, List<Edge> edges) {
    mId = sNextId++;
    mFinalState = finalState;
    if (edges == null)
      edges = arrayList();
    setEdges(edges);
  }

  public State() {
    this(false, null);
  }

  public State(boolean finalState) {
    this(finalState, null);
  }

  @Override
  public int compareTo(State other) {
    return Integer.compare(id(), other.id());
  }

  public List<Edge> edges() {
    return mEdges;
  }

  public boolean finalState() {
    return mFinalState;
  }

  public void setEdges(List<Edge> edges) {
    var edg = new ArrayList<Edge>(edges.size());
    edg.addAll(edges);
    mEdges = edg;
  }

  public void setFinal(boolean flag) {
    mFinalState = flag;
  }

  public int id() {
    return mId;
  }

  public void setId(int id) {
    mId = id;
  }

  public String toString(boolean includeEdges) {
    StringBuilder sb = new StringBuilder();
    sb.append(id());
    sb.append(finalState() ? '*' : ' ');
    if (includeEdges) {
      sb.append("=>");
      for (Edge e : edges()) {
        sb.append(" ");
        sb.append(e.destinationState().id());
      }
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(false);
  }

  public static void resetIds() {
    sNextId = 100;
  }

  public static void setIds(int nextMinValue) {
    if (sNextId > nextMinValue) {
      int mod = sNextId % 100;
      if (mod > 0)
        nextMinValue = sNextId - mod + 100;
      else
        nextMinValue = sNextId;
    }
    sNextId = nextMinValue;
  }

  public static String toString(Iterable<State> states) {
    StringBuilder sb = new StringBuilder("(");
    int index = INIT_INDEX;
    for (State s : states) {
      index++;
      if (index != 0)
        sb.append(' ');
      sb.append(s.id());
    }
    sb.append(')');
    return sb.toString();
  }

  public static void bumpIds() {
    setIds(0);
  }

  private static int sNextId = 100;

  public static final int EPSILON = -1;

  /**
   * One plus the maximum code represented
   */
  public static final int CODEMAX = 256;

  public static int edgeLabelToTokenId(int edgeLabel) {
    return EPSILON - 1 - edgeLabel;
  }

  public static int tokenIdToEdgeLabel(int tokenId) {
    // Note: this does the same calculation as edgeLabelToTokenId
    return EPSILON - 1 - tokenId;
  }

  private int mId;
  private List<Edge> mEdges;
  private boolean mFinalState;

}
