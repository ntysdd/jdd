package ntysdd;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import static ntysdd.DoubleDouble.ONE;
import static ntysdd.DoubleDouble.ZERO;

public class DoubleDoubleTest {
    private static final DoubleDouble NEG_ZERO = DoubleDouble.valueOf(-0.0);

    public static void test001() {
        assertEquals(0.0, DoubleDouble.valueOf(0.0).getFirst());
        assertEquals(0.0, DoubleDouble.valueOf(0.0).getSecond());
        assertEquals(-0.0, DoubleDouble.valueOf(-0.0).getFirst());
        assertEquals(0.0, DoubleDouble.valueOf(-0.0).getSecond());

        if (ZERO.equals(DoubleDouble.valueOf(-0.0))) {
            throw new AssertionError();
        }

        assertEquals(NEG_ZERO, ZERO.neg());

        assertEquals(ZERO, DoubleDouble.valueOf(0));
        assertEquals(ZERO, DoubleDouble.valueOf(0.0));
        assertEquals(ZERO, DoubleDouble.add(0.0, 0.0));
        assertEquals(DoubleDouble.ONE, DoubleDouble.valueOf(1));
        assertEquals(DoubleDouble.ONE, DoubleDouble.valueOf(1.0));
        assertEquals(DoubleDouble.ONE, DoubleDouble.add(1.0, 0.0));
        assertEquals(DoubleDouble.TWO, DoubleDouble.valueOf(2));
        assertEquals(DoubleDouble.TWO, DoubleDouble.valueOf(2.0));
        assertEquals(DoubleDouble.TWO, DoubleDouble.add(2.0, 0.0));
        assertEquals(DoubleDouble.TEN, DoubleDouble.valueOf(10));
        assertEquals(DoubleDouble.TEN, DoubleDouble.valueOf(10.0));
        assertEquals(DoubleDouble.TEN, DoubleDouble.add(10.0, 0.0));

        final DoubleDouble DD_LONG_MAX = DoubleDouble.valueOf(Long.MAX_VALUE);
        assertEquals(9.223372036854776E18, DD_LONG_MAX.getFirst());
        assertEquals(-1.0, DD_LONG_MAX.getSecond());

        final DoubleDouble DD_LONG_MIN = DoubleDouble.valueOf(Long.MIN_VALUE);
        assertEquals(-9.223372036854776E18, DD_LONG_MIN.getFirst());
        assertEquals(0.0, DD_LONG_MIN.getSecond());
    }

    public static void test002() {
        DoubleDouble d1 = DoubleDouble.add(1.0, 1E-20);
        DoubleDouble d2 = DoubleDouble.add(1E-20, 1.0);
        DoubleDouble d3 = DoubleDouble.add(1.0, 1E-30);
        DoubleDouble d4 = DoubleDouble.add(2.0, 1E-20);

        assertEquals(1.0, d1.getFirst());
        assertEquals(1E-20, d1.getSecond());

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertEquals(d1, d1);

        if (d1.equals(null)) {
            throw new AssertionError();
        }
        if (d1.equals(d3)) {
            throw new AssertionError();
        }
        if (d1.equals(d4)) {
            throw new AssertionError();
        }
        if (DoubleDouble.valueOf(1.0).equals(1.0)) {
            throw new AssertionError();
        }

        assertEquals(Double.MAX_VALUE, DoubleDouble.valueOf(Double.MAX_VALUE).getFirst());
        assertEquals(0.0, DoubleDouble.valueOf(Double.MAX_VALUE).getSecond());
        assertEquals(Double.MIN_VALUE, DoubleDouble.valueOf(Double.MIN_VALUE).getFirst());
        assertEquals(0.0, DoubleDouble.valueOf(Double.MIN_VALUE).getSecond());
        assertEquals(Double.MIN_NORMAL, DoubleDouble.valueOf(Double.MIN_NORMAL).getFirst());
        assertEquals(0.0, DoubleDouble.valueOf(Double.MIN_NORMAL).getSecond());
    }

    public static void test003() {
        assertEquals(ZERO, DoubleDouble.add(0.0, 0.0));
        assertEquals(ZERO, DoubleDouble.add(0.0, -0.0));
        assertEquals(ZERO, DoubleDouble.add(-0.0, 0.0));
        assertEquals(ZERO, DoubleDouble.add(0.0, 0));
        assertEquals(ZERO, DoubleDouble.add(-0.0, 0));
        assertEquals(ZERO, DoubleDouble.add(0, 0.0));
        assertEquals(ZERO, DoubleDouble.add(0, -0.0));
        assertEquals(ZERO, DoubleDouble.add(0, 0));

        assertEquals(NEG_ZERO, DoubleDouble.add(-0.0, -0.0));

        assertEquals(ZERO, ZERO.add(0));
        assertEquals(ZERO, ZERO.add(0.0));
        assertEquals(ZERO, ZERO.add(-0.0));

        assertEquals(ZERO, NEG_ZERO.add(0));
        assertEquals(ZERO, NEG_ZERO.add(0.0));
        assertEquals(NEG_ZERO, NEG_ZERO.add(-0.0));

        assertEquals(ZERO, ZERO.add(ZERO));
        assertEquals(ZERO, ZERO.add(NEG_ZERO));
        assertEquals(ZERO, NEG_ZERO.add(ZERO));
        assertEquals(NEG_ZERO, NEG_ZERO.add(NEG_ZERO));
    }

    public static void test004() {
        assertEquals(DoubleDouble.ONE, DoubleDouble.ONE.add(ZERO));
        assertEquals(DoubleDouble.ONE, DoubleDouble.ONE.add(NEG_ZERO));
        assertEquals(DoubleDouble.ONE, ZERO.add(DoubleDouble.ONE));
        assertEquals(DoubleDouble.ONE, NEG_ZERO.add(DoubleDouble.ONE));

        final DoubleDouble NEG_ONE = DoubleDouble.ONE.neg();
        assertEquals(-1.0, NEG_ONE.getFirst());
        assertEquals(0.0, NEG_ONE.getSecond());

        assertEquals(NEG_ONE, NEG_ONE.add(ZERO));
        assertEquals(NEG_ONE, NEG_ONE.add(NEG_ZERO));
        assertEquals(NEG_ONE, ZERO.add(NEG_ONE));
        assertEquals(NEG_ONE, NEG_ZERO.add(NEG_ONE));
    }

    public static void test005() {
        final DoubleDouble POS_INF = DoubleDouble.valueOf(Double.POSITIVE_INFINITY);
        final DoubleDouble NEG_INF = DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);
        if (POS_INF.equals(NEG_INF)) {
            throw new AssertionError();
        }
        assertEquals(NEG_INF, POS_INF.neg());
        assertEquals(POS_INF, NEG_INF.neg());

        assertEquals(POS_INF, POS_INF.add(ZERO));
        assertEquals(POS_INF, POS_INF.add(0.0));
        assertEquals(POS_INF, POS_INF.add(NEG_ZERO));
        assertEquals(POS_INF, POS_INF.add(-0.0));
        assertEquals(POS_INF, POS_INF.add(ONE));
        assertEquals(POS_INF, POS_INF.add(1));
        assertEquals(POS_INF, POS_INF.add(1.0));
        assertEquals(POS_INF, POS_INF.add(Double.POSITIVE_INFINITY));
        assertEquals(POS_INF, POS_INF.add(ONE.neg()));
        assertEquals(POS_INF, POS_INF.add(-1));
        assertEquals(POS_INF, POS_INF.add(-1.0));

        assertEquals(NEG_INF, NEG_INF.add(ZERO));
        assertEquals(NEG_INF, NEG_INF.add(0.0));
        assertEquals(NEG_INF, NEG_INF.add(NEG_ZERO));
        assertEquals(NEG_INF, NEG_INF.add(-0.0));
        assertEquals(NEG_INF, NEG_INF.add(ONE));
        assertEquals(NEG_INF, NEG_INF.add(1));
        assertEquals(NEG_INF, NEG_INF.add(1.0));
        assertEquals(NEG_INF, NEG_INF.add(Double.NEGATIVE_INFINITY));
        assertEquals(NEG_INF, NEG_INF.add(ONE.neg()));
        assertEquals(NEG_INF, NEG_INF.add(-1));
        assertEquals(NEG_INF, NEG_INF.add(-1.0));

        assertEquals(POS_INF, POS_INF.add(POS_INF));
        assertEquals(NEG_INF, NEG_INF.add(NEG_INF));
    }

    public static void test006() {
        final DoubleDouble POS_INF = DoubleDouble.valueOf(Double.POSITIVE_INFINITY);
        final DoubleDouble NEG_INF = DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);
        final DoubleDouble NaN = DoubleDouble.valueOf(Double.NaN);
        assertEquals(NaN, NaN);
        assertEquals(NaN, NaN.neg());

        assertEquals(Double.NaN, NaN.getFirst());
        assertEquals(Double.NaN, NaN.getSecond());
        assertEquals(NaN, POS_INF.add(NEG_INF));
        assertEquals(NaN, NEG_INF.add(POS_INF));

        assertEquals(NaN, NaN.add(NaN));
        assertEquals(NaN, NaN.add(ZERO));
        assertEquals(NaN, NaN.add(ONE));
        assertEquals(NaN, NaN.add(ONE.neg()));
        assertEquals(NaN, NaN.add(ONE.add(1E-20)));
        assertEquals(NaN, NaN.add(ONE.add(1E-20).neg()));
        assertEquals(NaN, NaN.add(POS_INF));
        assertEquals(NaN, NaN.add(NEG_INF));

        assertEquals(NaN, ZERO.add(NaN));
        assertEquals(NaN, ONE.add(NaN));
        assertEquals(NaN, ONE.neg().add(NaN));
        assertEquals(NaN, ONE.add(1E-20).add(NaN));
        assertEquals(NaN, ONE.add(1E-20).neg().add(NaN));
        assertEquals(NaN, POS_INF.add(NaN));
        assertEquals(NaN, NEG_INF.add(NaN));

        assertEquals(NaN, NaN.add(Double.NaN));
        assertEquals(NaN, ZERO.add(Double.NaN));
        assertEquals(NaN, NEG_ZERO.add(Double.NaN));
        assertEquals(NaN, ONE.add(Double.NaN));
        assertEquals(NaN, NaN.add(0.0));
        assertEquals(NaN, NaN.add(-0.0));
        assertEquals(NaN, NaN.add(1.0));
        assertEquals(NaN, NaN.add(-1.0));
        assertEquals(NaN, NaN.add(0));
        assertEquals(NaN, NaN.add(1));
        assertEquals(NaN, NaN.add(-1));
        assertEquals(NaN, NaN.add(Double.POSITIVE_INFINITY));
        assertEquals(NaN, NaN.add(Double.NEGATIVE_INFINITY));
        assertEquals(NaN, POS_INF.add(Double.NaN));
        assertEquals(NaN, NEG_INF.add(Double.NaN));

        assertEquals(NaN, DoubleDouble.add(Double.NaN, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(0.0, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(-0.0, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(1.0, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(-1.0, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(0, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(1, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(-1, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(Double.POSITIVE_INFINITY, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(Double.NaN, Double.POSITIVE_INFINITY));
        assertEquals(NaN, DoubleDouble.add(Double.NEGATIVE_INFINITY, Double.NaN));
        assertEquals(NaN, DoubleDouble.add(Double.NaN, Double.NEGATIVE_INFINITY));
    }

    public static void main(String[] args) throws Exception {
        Method[] methods = Arrays.stream(DoubleDoubleTest.class.getMethods())
                .filter(m -> m.getName().matches("test[0-9]+")
                        && m.getParameterCount() == 0)
                .sorted(Comparator.comparing(Method::getName))
                .toArray(Method[]::new);
        for (Method method : methods) {
            method.invoke(null);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if ((expected == null) != (actual == null)) {
            String msg = "expected: " + expected + ", actual: " + actual;
            throw new AssertionError(msg);
        }
        if (expected != null && !expected.equals(actual)) {
            String msg = "expected: " + expected + ", actual: " + actual;
            throw new AssertionError(msg);
        }
    }
}
