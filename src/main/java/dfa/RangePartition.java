package dfa;

import static js.base.Tools.*;

import java.util.List;
import java.util.Set;

import static dfa.Util.*;

/**
 * A data structure that transforms a set of CodeSets to a disjoint set of them,
 * such that no two range sets overlap.
 *
 * This is to improve the efficiency of the NFA => DFA algorithm, which involves
 * gathering information about what states are reachable on certain characters.
 * We can't afford to treat each character as a singleton, since the ranges can
 * be quite large. Hence, we want to treat ranges of characters as single
 * entities; this will only work if no two such ranges overlap.
 *
 * It works by starting with a tree whose node is labelled with the maximal
 * superset of character values. Then, for each edge in the NFA, performs a DFS
 * on this tree, splitting any node that only partially intersects any one set
 * that appears in the edge label. The running time is O(n log k), where n is
 * the size of the NFA, and k is the height of the resulting tree.
 *
 * We encourage k to be small by sorting the NFA edges by their label
 * complexity.
 */
final class RangePartition {

  /**
   * A node within a RangePartition tree
   */
  private static class RPNode {
    public RPNode(CodeSet codeSet) {
      this.codeSet = codeSet;
    }

    CodeSet codeSet;
    List<RPNode> children = arrayList();
  }

  public RangePartition() {
    mUniqueCodeSets = hashSet();
    // Make the root node hold the largest possible CodeSet. 
    // We want to be able to include all the token ids as well.

    mRootNode = buildNode(CodeSet.withRange(-MAX_TOKEN_DEF, codeMax()));
    // Add epsilon immediately, so it's always in its own subset
    addSet(CodeSet.epsilon());
  }

  public void addSet(CodeSet codeSet) {
    checkState(!mPrepared);
    mUniqueCodeSets.add(codeSet);
  }

  private void prepare() {
    checkState(!mPrepared);

    // Construct partition from previously added sets

    List<CodeSet> setsList = arrayList();
    setsList.addAll(mUniqueCodeSets);

    // Sort set by cardinality: probably get a more balanced tree
    // if larger sets are processed first
    setsList.sort((a, b) -> Integer.compare(b.elements().length, a.elements().length));

    for (CodeSet s : setsList)
      addSetAux(s, mRootNode);

    mPrepared = true;
  }

  /**
   * Apply the partition to a code set
   *
   * @return array of subsets from the partition whose union equals the code set
   * (this array will be the single element s if no partitioning was
   * necessary)
   */
  public List<CodeSet> apply(CodeSet codeSet) {
    if (!mPrepared)
      prepare();
    List<CodeSet> list = arrayList();
    applyAux(mRootNode, codeSet.dup(), list);

    // Sort the list of subsets by their first elements
    list.sort((a, b) -> Integer.compare(a.elements()[0], b.elements()[0]));
    return list;
  }

  private void applyAux(RPNode n, CodeSet s, List<CodeSet> list) {
    if (n.children.isEmpty()) {
      // Verify that this set equals the input set
      checkState(s.equals(n.codeSet));
      push(list, s);
    } else {
      for (RPNode m : n.children) {
        CodeSet s1 = s.intersect(m.codeSet);
        if (s1.isEmpty())
          continue;
        applyAux(m, s1, list);
        s = s.difference(m.codeSet);
        if (s.isEmpty())
          break;
      }
    }
  }

  private RPNode buildNode(CodeSet codeSet) {
    return new RPNode(codeSet);
  }

  /**
   * Add a set to the tree, extending the tree as necessary to maintain a
   * (disjoint) partition
   */
  private void addSetAux(CodeSet s, RPNode n) {

    //      #
    //      # The algorithm is this:
    //      #
    //      # add (s, n)    # add set s to node n; s must be subset of n.set
    //      #   if n.set = s, return
    //      #   if n is leaf:
    //      #     x = n.set - s
    //      #     add x,y as child sets of n
    //      #   else
    //      #     for each child m of n:
    //      #       t = intersect of m.set and s
    //      #       if t is nonempty, add(t, m)
    //      #
    if (n.codeSet.equals(s))
      return;

    if (n.children.isEmpty()) {
      CodeSet x = n.codeSet.difference(s);
      push(n.children, buildNode(x));
      push(n.children, buildNode(s));
    } else {
      for (RPNode m : n.children) {
        CodeSet t = m.codeSet.intersect(s);
        if (!t.isEmpty()) {
          addSetAux(t, m);
        }
      }
    }
  }

  private boolean mPrepared;
  private RPNode mRootNode;
  private Set<CodeSet> mUniqueCodeSets;

}
