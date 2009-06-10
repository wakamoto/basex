package org.basex.data;

import org.basex.util.Array;

/**
 * This class contains full-text positions.
 *
 * The data is stored as follows:
 * pos[i][pos0, ..., posn]
 * poi[i][poi0, ..., poin]
 * For each pos values, a poi value is stored.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public class FTPos {
  /** Pre value. */
  public int pre;
  /** Positions. */
  public int[] pos;
  /** Pointers. */
  public byte[] poi;

  /**
   * Constructor.
   * @param p pre value
   * @param ps positions
   * @param pi pointers
   */
  FTPos(final int p, final int[] ps, final byte[] pi) {
    pre = p;
    pos = ps;
    poi = pi;
  }

  /**
   * Returns the number of entries.
   * @return number of entries
   */
  public int size() {
    return pos.length;
  }

  /**
   * Merges the specified position arrays. 
   * @param ps positions
   * @param pi pointers
   */
  void union(final int[] ps, final byte[] pi) {
    // skip existing values
    if(Array.eq(pos, ps)) return;
    
    // merge entries with the same pre value
    final int prs = ps.length;
    final int pss = pos.length;
    final int[] ts = new int[prs + pss];
    final byte[] ti = new byte[ts.length];
    for(int i = 0, i0 = 0, i1 = 0; i < ts.length; i++) {
      final boolean s = i0 == prs || i1 < pss && pos[i1] < ps[i0];
      ts[i] = s ? pos[i1] : ps[i0];
      ti[i] = s ? poi[i1++] : pi[i0++];
    }
    pos = ts;
    poi = ti;
  }
}