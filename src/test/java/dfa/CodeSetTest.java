/**
 * MIT License
 * 
 * Copyright (c) 2022 Jeff Sember
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
 * 
 **/
package dfa;

import static js.base.Tools.*;
import static org.junit.Assert.*;

import java.util.BitSet;

import org.junit.Test;

import js.data.IntArray;
import js.parsing.State;
import js.testutil.MyTestCase;

public class CodeSetTest extends MyTestCase {

  @Test
  public void add2() {
    prep();
    add(10, 13);
    add(9);
    equ("9 13");
  }

  @Test
  public void addRandom() {
    BitSet bitSet = new BitSet();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < 1000; i++) {
      bitSet.clear();

      prep();

      final int max = 100;
      final int offset = -max / 2;
      final int maxChunkLen = (i % 10) + 1;
      for (int j = 0; j < 50; j++) {
        int u = random().nextInt(max - maxChunkLen);
        int v = u + (int) (random().nextFloat() * random().nextFloat() * maxChunkLen) + 1;

        add(u + offset, v + offset);
        bitSet.set(u, v);
      }

      sb.setLength(0);
      int streakLen = 0;
      for (int j = 0; j <= max; j++) {
        if (bitSet.get(j))
          streakLen++;
        else {
          if (streakLen > 0) {
            sb.append(' ');
            sb.append(j - streakLen + offset);
            sb.append(' ');
            sb.append(j + offset);
            streakLen = 0;
          }
        }
      }
      String result = sb.toString().trim();
      log(result);
      equ(result);
    }
  }

  @Test
  public void add() {
    prep();

    add(72, 81);
    equ("72 81");

    add(50);
    equ("50 51 72 81");

    add(75, 77);
    equ("50 51 72 81");

    add(72, 78);
    equ("50 51 72 81");

    add(70, 78);
    equ("50 51 70 81");

    add(60);
    equ("50 51 60 61 70 81");

    add(40);
    equ("40 41 50 51 60 61 70 81");

    add(41);
    equ("40 42 50 51 60 61 70 81");

    add(81);
    equ("40 42 50 51 60 61 70 82");

    add(83);
    equ("40 42 50 51 60 61 70 82 83 84");

    add(49, 84);
    equ("40 42 49 84");

    add(39, 86);
    equ("39 86");
  }

  @Test
  public void intersect() {
    prep();
    add(39, 86);
    swap();
    add(50, 70);
    isect();
    equ("50 70");

    swap();
    add(20, 25);
    add(35, 51);
    add(62, 68);
    add(72, 80);
    isect();
    equ("50 51 62 68");

    prep();
    swap();
    add(50, 70);
    isect();
    equ("");

    add(50, 70);
    swap();
    add(50, 70);
    isect();
    equ("50 70");

    prep();
    add(20, 25);
    swap();
    add(25, 30);
    isect();
    equ("");
  }

  @Test
  public void difference() {
    prep();
    add(20, 30);
    add(40, 50);
    swap();

    add(20, 80);
    diff();
    equ("30 40 50 80");

    prep();
    add(19, 32);
    diff();
    equ("19 20 30 32");

    prep();
    add(30, 40);
    diff();
    equ("30 40");

    prep();
    add(20, 30);
    add(40, 50);
    diff();
    equ("");

    prep();
    add(19, 30);
    add(40, 50);
    diff();
    equ("19 20");

    prep();
    add(20, 30);
    add(40, 51);
    diff();
    equ("50 51");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalRange() {
    prep();
    add(60, 50);
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalRange2() {
    prep();
    add(60, 60);
  }

  @Test
  public void testNegate() {

    prep();
    add(10, 15);
    add(20, 25);
    add(30);
    add(40, 45);
    equ("10 15 20 25 30 31 40 45");
    neg(22, 37);
    equ("10 15 20 22 25 30 31 37 40 45");
    neg(25, 27);
    equ("10 15 20 22 27 30 31 37 40 45");
    neg(15, 20);
    equ("10 22 27 30 31 37 40 45");

    prep();
    add(10, 22);
    neg(0, State.CODEMAX);
    equ("0 10 22 256");

    prep();
    add(10, 20);
    neg(10, 20);
    equ("");

    prep();
    add(10, 20);
    add(30, 40);
    neg(5, 10);
    equ("5 20 30 40");

    prep();
    add(10, 20);
    add(30, 40);
    neg(25, 30);
    equ("10 20 25 40");

    prep();
    add(10, 20);
    add(30, 40);
    neg(40, 50);
    equ("10 20 30 50");

    prep();
    add(10, 20);
    add(30, 40);
    neg(41, 50);
    equ("10 20 30 40 41 50");

    prep();
    add(10, 20);
    add(30, 40);
    neg(15, 35);
    equ("10 15 20 30 35 40");
  }

  private void neg(int lower, int upper) {
    mSet0 = mSet0.negate(lower, upper);
  }

  private void add(int lower, int upper) {
    mSet0.add(lower, upper);
  }

  private void add(int value) {
    mSet0.add(value);
  }

  private void equ(String expr) {
    IntArray.Builder b = IntArray.newBuilder();
    if (nonEmpty(expr)) {
      for (String s : split(expr, ' '))
        b.add(Integer.parseInt(s));
    }
    assertArrayEquals(b.array(), mSet0.elements());
  }

  private void prep() {
    mSet0 = new CodeSet();
  }

  private CodeSet mSet0;
  private CodeSet mSet1;

  private void isect() {
    mSet0 = mSet0.intersect(mSet1);
  }

  private void diff() {
    mSet0 = mSet0.difference(mSet1);
  }

  private void swap() {
    mSet1 = mSet0;
    prep();
  }

}
