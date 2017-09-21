/*
 * Copyright (c) 2015, 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.math.integer.min;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.MPCException;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.conditional.ConditionalSelect;
import dk.alexandra.fresco.lib.math.integer.min.MinInfFrac.MinInfOutput;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows to indicate that certain fractions should be regarded as having infinite value. I.e., to
 * indicate that those fractions should never be chosen as the minimum. We do this by taking for
 * each fraction a infinity indicator bit which can than be used to adjust any comparison result for
 * the indicated fractions. This solves a problem in the Simplex solver where we need to find the
 * minimum fraction larger than 0, in a list of fractions.
 *
 * This improves on a previous solution that simply tried to set all fractions
 * smaller than or equal to 0 to a very large value (essentially assuming this
 * value would be a good approximation of infinity). Such a solution however,
 * turns out to be prone to overflow problems, and picking the very larger
 * value, is also non-trivial.
 */
public class MinInfFrac implements Computation<MinInfOutput, ProtocolBuilderNumeric> {

  private final ArrayList<Frac> fs;


  /**
   * Constructs a protocol finding the minimum of a list of fractions. For
   * each fraction a 0/1 value should be given to indicate whether or not that
   * fraction should be disregarded when finding the minimum (similar to
   * setting that fraction to a value of infinity).
   *
   * @param ns input - a list of numerators
   * @param ds input - a list of denominators
   * @param infs input - a list of infinity indicators (should be a 0/1 value, 1 indicating
   * infinity)
   */
  public MinInfFrac(
      List<DRes<SInt>> ns,
      List<DRes<SInt>> ds,
      List<DRes<SInt>> infs) {
    if (ns.size() == ds.size() && ns.size() == infs.size()) {
      this.fs = new ArrayList<>();
      Iterator<DRes<SInt>> nsIterator = ns.iterator();
      Iterator<DRes<SInt>> dsIterator = ds.iterator();
      Iterator<DRes<SInt>> infsIterator = infs.iterator();
      while (nsIterator.hasNext()) {
        fs.add(new Frac(nsIterator.next(), dsIterator.next(), infsIterator.next()));
      }
    } else {
      throw new MPCException("Sizes of input arrays does not match");
    }
  }

  @Override
  public DRes<MinInfOutput> buildComputation(ProtocolBuilderNumeric builder) {
    DRes<SInt> one = builder.numeric().known(BigInteger.ONE);
    if (fs.size() == 1) { // The trivial case
      return () -> {
        ArrayList<DRes<SInt>> result = new ArrayList<>();
        result.add(one);
        Frac frac = fs.get(0);
        return new MinInfOutput(frac.n, frac.d, frac.inf, result);
      };
    }
    ArrayList<DRes<SInt>> cs = new ArrayList<>(fs.size());
    for (@SuppressWarnings("unused") Frac f : fs) {
      cs.add(null);
    }
    return builder.seq(seq -> () -> new IterationState(fs, 0))
        .whileLoop(
            state -> (state.fs.size() > 1),
            (seq, state) -> {
              //TODO Clean up method
              int layer = state.layer;
              final List<Frac> fs = state.fs;
              //Find min
              int sizeOfTmpC = fs.size() / 2;
              int nextLength = sizeOfTmpC + (fs.size() % 2);
              List<DRes<Frac>> tmpFs = new ArrayList<>(nextLength);
              List<DRes<SInt>> tmpCs = new ArrayList<>(sizeOfTmpC);
              for (int i = 0; i < sizeOfTmpC; i++) {
                tmpCs.add(null);
              }
              for (int i = 0; i < sizeOfTmpC; i++) {
                tmpFs.add(null);
              }
              seq.par((par) -> {
                for (int i = 0; i < sizeOfTmpC; i++) {
                  int finalI = i;
                  tmpFs.set(i,
                      par.seq((innerSeq) -> innerSeq.seq(seq1 ->
                              () -> null
                          ).pairInPar(
                          (seq11, ignored) -> seq11
                              .numeric().mult(fs.get(finalI * 2).n, fs.get(finalI * 2 + 1).d),
                          (seq12, ignored) -> seq12
                              .numeric().mult(fs.get(finalI * 2 + 1).n, fs.get(finalI * 2).d)
                          ).seq((seq13, pair) -> {
                            SInt p1 = pair.getFirst();
                            SInt p2 = pair.getSecond();
                            Numeric numeric = seq13.numeric();
                        DRes<SInt> tmpC = seq13.comparison()
                            .compareLEQLong(() -> p1, () -> p2);
                        DRes<SInt> notInf0 = numeric
                            .sub(BigInteger.ONE, fs.get(finalI * 2).inf);
                            tmpC = numeric.mult(notInf0, tmpC);
                            tmpC = seq13
                                .seq(new ConditionalSelect(fs.get(finalI * 2 + 1).inf,
                                    fs.get(finalI * 2 + 1).inf, tmpC));
                            DRes<SInt> c = tmpC;
                            tmpCs.set(finalI, c);
                            DRes<SInt> rn = seq13
                                .seq(
                                    new ConditionalSelect(c, fs.get(finalI * 2).n,
                                        fs.get(finalI * 2 + 1).n));
                            DRes<SInt> rd = seq13
                                .seq(
                                    new ConditionalSelect(c, fs.get(finalI * 2).d,
                                        fs.get(finalI * 2 + 1).d));
                            DRes<SInt> rinf = numeric.mult(
                                fs.get(finalI * 2).inf, fs.get(finalI * 2 + 1).inf);
                            return () -> new Frac(rn, rd, rinf);
                          })
                      )
                  );
                }
                return () -> null;
              });

              if (fs.size() % 2 == 1) {
                tmpFs.add(fs.get(fs.size() - 1));
              }

              // Updated Cs
              int offset = 1 << (layer + 1);
              if (layer == 0) {
                seq.par((par) -> {
                  for (int i = 0; i < sizeOfTmpC; i++) {
                    int finalI = i;
                    par.seq((innerSeq) -> {
                      DRes<SInt> c = tmpCs.get(finalI);
                      DRes<SInt> notC = innerSeq.numeric().sub(BigInteger.ONE, c);

                      cs.set(finalI * 2, c);
                      cs.set(finalI * 2 + 1, notC);
                      return () -> null;
                    });
                  }
                  if (cs.size() % 2 == 1) {
                    cs.set(cs.size() - 1, one);
                  }
                  return () -> null;
                });
              } else {
                seq.par((par) -> {
                  for (int i = 0; i < sizeOfTmpC; i++) {
                    DRes<SInt> c = tmpCs.get(i);
                    for (int j = i * offset; j < i * offset + offset / 2; j++) {
                      cs.set(j, par.numeric().mult(c, cs.get(j)));
                    }
                    int finalI = i;
                    par.seq((innerSeq) -> {
                      DRes<SInt> notC = innerSeq.numeric().sub(BigInteger.ONE, c);
                      innerSeq.par((innerPar) -> {
                        int limit =
                            (finalI + 1) * offset > cs.size() ? cs.size() : (finalI + 1) * offset;
                        for (int j = finalI * offset + offset / 2; j < limit; j++) {
                          cs.set(j, innerPar.numeric().mult(notC, cs.get(j)));
                        }
                        return () -> null;
                      });
                      return () -> null;
                    });
                  }
                  return () -> null;
                });
              }

              return () -> new IterationState(
                  tmpFs.stream().map(DRes::out).collect(Collectors.toList()),
                  layer + 1);
            }).seq((sequentialProtocolBuilder, iterationState) -> {
          Frac frac = iterationState.fs.get(0);
          return () -> new MinInfOutput(frac.n, frac.d, frac.inf, cs);
        });

  }

  /**
   * Helper class to represent a fraction consisting of a numerator denominator
   * and an infinity indicator.
   */
  private class Frac implements DRes<Frac> {

    DRes<SInt> n, d, inf;

    Frac(DRes<SInt> n, DRes<SInt> d, DRes<SInt> inf) {
      super();
      this.n = n;
      this.d = d;
      this.inf = inf;
    }

    @Override
    public Frac out() {
      return this;
    }
  }

  public static class MinInfOutput {

    public final DRes<SInt> nm, dm, infm;
    public final ArrayList<DRes<SInt>> cs;

    public MinInfOutput(
        DRes<SInt> nm,
        DRes<SInt> dm,
        DRes<SInt> infm,
        ArrayList<DRes<SInt>> cs) {
      this.nm = nm;
      this.dm = dm;
      this.infm = infm;
      this.cs = cs;
    }
  }

  private static class IterationState implements DRes<IterationState> {

    private final List<Frac> fs;
    private final int layer;

    private IterationState(List<Frac> fs, int layer) {
      this.fs = fs;
      this.layer = layer;
    }

    @Override
    public IterationState out() {
      return this;
    }
  }
}