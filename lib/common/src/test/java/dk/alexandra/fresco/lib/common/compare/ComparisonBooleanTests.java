package dk.alexandra.fresco.lib.common.compare;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.binary.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.value.SBool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;

public class ComparisonBooleanTests {

  public static class CompareAndSwapTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderBinary> {

    public CompareAndSwapTest() {
    }

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderBinary> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderBinary>() {
        @Override
        public void test() throws Exception {
          List<Boolean> rawLeft = Arrays.asList(ByteAndBitConverter.toBoolean("ee"));
          List<Boolean> rawRight = Arrays.asList(ByteAndBitConverter.toBoolean("00"));

          Application<List<List<Boolean>>, ProtocolBuilderBinary> app =
              producer -> producer.seq(seq -> {
                List<DRes<SBool>> left =
                    rawLeft.stream().map(seq.binary()::known).collect(Collectors.toList());
                List<DRes<SBool>> right =
                    rawRight.stream().map(seq.binary()::known).collect(Collectors.toList());

                DRes<List<List<DRes<SBool>>>> compared =
                    new CompareAndSwap(left, right).buildComputation(seq);
                return compared;
              }).seq((seq, opened) -> {
                List<List<DRes<Boolean>>> result = new ArrayList<>();
                for (List<DRes<SBool>> entry : opened) {
                  result.add(entry.stream().map(DRes::out).map(seq.binary()::open)
                      .collect(Collectors.toList()));
                }

                return () -> result;
              }).seq((seq, opened) -> {
                List<List<Boolean>> result = new ArrayList<>();
                for (List<DRes<Boolean>> entry : opened) {
                  result.add(entry.stream().map(DRes::out).collect(Collectors.toList()));
                }

                return () -> result;
              });

          List<List<Boolean>> res = runApplication(app);

          Assert.assertEquals("00", ByteAndBitConverter.toHex(res.get(0)));
          Assert.assertEquals("ee", ByteAndBitConverter.toHex(res.get(1)));
        }
      };
    }
  }

  /**
   * Tests if the number 01010 > 01110 - then it reverses that.
   */
  public static class TestGreaterThan<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderBinary> {

    private boolean doAsserts = false;

    public TestGreaterThan() {}

    public TestGreaterThan(boolean doAsserts) {
      this.doAsserts = doAsserts;
    }

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderBinary> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderBinary>() {
        @Override
        public void test() throws Exception {
          Boolean[] comp1 = new Boolean[] {false, true, false, true, false};
          Boolean[] comp2 = new Boolean[] {false, true, true, true, false};

          Application<List<Boolean>, ProtocolBuilderBinary> app = producer -> producer.seq(seq -> {
            List<DRes<SBool>> in1 = BooleanHelper.known(comp1, seq.binary());
            List<DRes<SBool>> in2 = BooleanHelper.known(comp2, seq.binary());
            DRes<SBool> res1 = BinaryComparison.using(seq).greaterThan(in1, in2);
            DRes<SBool> res2 = BinaryComparison.using(seq).greaterThan(in2, in1);
            DRes<Boolean> open1 = seq.binary().open(res1);
            DRes<Boolean> open2 = seq.binary().open(res2);
            return () -> Arrays.asList(open1, open2);
          }).seq(
              (seq, opened) -> () -> opened.stream().map(DRes::out).collect(Collectors.toList()));

          List<Boolean> res = runApplication(app);

          if (doAsserts) {
            Assert.assertEquals(false, res.get(0));
            Assert.assertEquals(true, res.get(1));
          }
        }
      };
    }
  }

  /**
   * Tests if the number 01010 == 01110 and then checks if 01010 == 01010.
   */
  public static class TestEquality<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderBinary> {

    private boolean doAsserts = false;

    public TestEquality() {}

    public TestEquality(boolean doAsserts) {
      this.doAsserts = doAsserts;
    }

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderBinary> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderBinary>() {
        @Override
        public void test() throws Exception {
          Boolean[] comp1 = new Boolean[] {false, true, false, true, false};
          Boolean[] comp2 = new Boolean[] {false, true, true, true, false};

          Application<List<Boolean>, ProtocolBuilderBinary> app = producer -> producer.seq(seq -> {
            List<DRes<SBool>> in1 = BooleanHelper.known(comp1, seq.binary());
            List<DRes<SBool>> in2 = BooleanHelper.known(comp2, seq.binary());
            DRes<SBool> res1 = BinaryComparison.using(seq).equal(in1, in2);
            DRes<SBool> res2 = BinaryComparison.using(seq).equal(in1, in1);
            DRes<Boolean> open1 = seq.binary().open(res1);
            DRes<Boolean> open2 = seq.binary().open(res2);
            return () -> Arrays.asList(open1, open2);
          }).seq(
              (seq, opened) -> () -> opened.stream().map(DRes::out).collect(Collectors.toList()));

          List<Boolean> res = runApplication(app);

          if (doAsserts) {
            Assert.assertEquals(false, res.get(0));
            Assert.assertEquals(true, res.get(1));
          }
        }
      };
    }
  }

  /**
   * Tests if the number 01010 > 01110 - then it reverses that.   
   */
  public static class TestGreaterThanUnequalLength<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderBinary> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderBinary> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderBinary>() {
        @Override
        public void test() throws Exception {
          Boolean[] comp1 = new Boolean[] {false, true, false, true, false};
          Boolean[] comp2 = new Boolean[] {false, true, true};

          Application<List<Boolean>, ProtocolBuilderBinary> app = producer -> producer.seq(seq -> {
            List<DRes<SBool>> in1 = BooleanHelper.known(comp1, seq.binary());
            List<DRes<SBool>> in2 = BooleanHelper.known(comp2, seq.binary());
            DRes<SBool> res1 = BinaryComparison.using(seq).greaterThan(in1, in2);
            DRes<Boolean> open1 = seq.binary().open(res1);
            return () -> Collections.singletonList(open1);
          }).seq(
              (seq, opened) -> () -> opened.stream().map(DRes::out).collect(Collectors.toList()));

          try {
            runApplication(app);
          } catch (Exception e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
              throw e;
            }
          }
        }
      };
    }
  }
}
