package dk.alexandra.fresco.lib.fixed;

import dk.alexandra.fresco.lib.fixed.MathTests.TestFixedSign;
import dk.alexandra.fresco.suite.dummy.arithmetic.AbstractDummyArithmeticTest;
import org.junit.Test;

public class TestFixed extends AbstractDummyArithmeticTest {

  @Test
  public void test_Fixed_Input_Sequential() {
    runTest(new BasicFixedPointTests.TestInput<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Open_to_party_Sequential() {
    runTest(new BasicFixedPointTests.TestOpenToParty<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Known() {
    runTest(new BasicFixedPointTests.TestKnown<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Use_SInt() {
    runTest(new BasicFixedPointTests.TestUseSInt<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Add_Known() {
    runTest(new BasicFixedPointTests.TestAddKnown<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Add_Secret() {
    runTest(new BasicFixedPointTests.TestAdd<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Subtract_Secret() {
    runTest(new BasicFixedPointTests.TestSubtractSecret<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Sub_Known() {
    runTest(new BasicFixedPointTests.TestSubKnown<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Mult_Known() {
    runTest(new BasicFixedPointTests.TestMultKnown<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Mults() {
    runTest(new BasicFixedPointTests.TestMult<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Repeated_Multiplication() {
    runTest(
        new BasicFixedPointTests.TestRepeatedMultiplication<>(),
        new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Division_Secret_Divisor() {
    runTest(new BasicFixedPointTests.TestDiv<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Division_Known_Divisor() {
    runTest(
        new BasicFixedPointTests.TestDivisionKnownDivisor<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Division_Known_Negative_Divisor() {
    runTest(
        new BasicFixedPointTests.TestDivisionKnownNegativeDivisor<>(),
        new TestParameters().numParties(2));
  }

  @Test
  public void test_Close_Fixed_Matrix() {
    runTest(new LinearAlgebraTests.TestCloseFixedMatrix<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Close_And_Open_Fixed_Matrix() {
    runTest(new LinearAlgebraTests.TestCloseAndOpenMatrix<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Matrix_Addition() {
    runTest(new LinearAlgebraTests.TestMatrixAddition<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Matrix_Subtraction() {
    runTest(new LinearAlgebraTests.TestMatrixSubtraction<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Matrix_Multiplication() {
    runTest(
        new LinearAlgebraTests.TestMatrixMultiplication<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Matrix_Scale() {
    runTest(new LinearAlgebraTests.TestMatrixScale<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Matrix_Operate() {
    runTest(new LinearAlgebraTests.TestMatrixOperate<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Vector_Multiplication_Unmatched() {
    runTest(new LinearAlgebraTests.TestVectorMultUnmatchedDimensions<>(), new TestParameters());
  }

  @Test
  public void test_Fixed_Matrix_Multiplication_Unmatched() {
    runTest(new LinearAlgebraTests.TestMatrixMultUnmatchedDimensions<>(), new TestParameters());
  }

  @Test
  public void test_Fixed_Matrix_Addition_Unmatched() {
    runTest(new LinearAlgebraTests.TestAdditionUnmatchedDimensions<>(), new TestParameters());
  }

  @Test
  public void test_Fixed_Matrix_Transpose() {
    runTest(new LinearAlgebraTests.TestTransposeMatrix<>(), new TestParameters());
  }

  @Test
  public void test_Fixed_Exp() {
    runTest(new MathTests.TestExp<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Random_Element() {
    runTest(new MathTests.TestRandom<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Leq() {
    runTest(new BasicFixedPointTests.TestLeq<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Log() {
    runTest(new MathTests.TestLog<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_Sqrt() {
    runTest(new MathTests.TestSqrt<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Sum() {
    runTest(new MathTests.TestSum<>(), new TestParameters());
  }

  @Test
  public void test_inner_product() {
    runTest(new MathTests.TestInnerProduct<>(), new TestParameters());
  }

  @Test
  public void test_inner_product_known_part() {
    runTest(new MathTests.TestInnerProductPublicPart<>(), new TestParameters());
  }

  @Test
  public void test_inner_product_unmatched_dimensions() {
    runTest(new MathTests.TestInnerProductUnmatchedDimensions<>(), new TestParameters());
  }

  @Test
  public void test_inner_product_known_part_unmatched() {
    runTest(new MathTests.TestInnerProductPublicPartUnmatched<>(), new TestParameters());
  }

  @Test
  public void test_normalize_SFixed() {
    runTest(new NormalizeTests.TestNormalizeSFixed<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_normalize_power_SFixed() {
    runTest(new NormalizeTests.TestNormalizePowerSFixed<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_reciprocal() {
    runTest(new MathTests.TestReciprocal<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_two_power() {
    runTest(new MathTests.TestTwoPower<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_Fixed_sign() {
    runTest(new TestFixedSign<>(), new TestParameters().numParties(2));
  }

  @Test
  public void test_constant_Fixed_polynomial() {
    runTest(new MathTests.TestConstantPolynomial<>(), new TestParameters().numParties(2));
  }
}
