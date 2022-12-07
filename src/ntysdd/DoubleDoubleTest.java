package ntysdd;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

import static ntysdd.DoubleDouble.*;

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

        assertSameObject(ZERO, DoubleDouble.valueOf(0));
        assertSameObject(ZERO, DoubleDouble.valueOf(0.0));
        assertSameObject(ONE, DoubleDouble.valueOf(1));
        assertSameObject(ONE, DoubleDouble.valueOf(1.0));
        assertSameObject(TWO, DoubleDouble.valueOf(2));
        assertSameObject(TWO, DoubleDouble.valueOf(2.0));
        assertSameObject(TEN, DoubleDouble.valueOf(10));
        assertSameObject(TEN, DoubleDouble.valueOf(10.0));

        assertEquals(NEG_ZERO, ZERO.neg());
        if (!NEG_ZERO.toString().startsWith("-")) {
            throw new AssertionError();
        }

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

        assertEquals(TWO, ONE.add(ONE));
        assertEquals(TWO.neg(), ONE.neg().add(ONE.neg()));
        assertEquals(ZERO, ONE.add(ONE.neg()));
        assertEquals(DoubleDouble.valueOf(Long.MAX_VALUE).add(1), ONE.add(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Long.MAX_VALUE).add(ONE), ONE.add(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Long.MIN_VALUE).neg(), ONE.add(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Long.MAX_VALUE).neg(), ONE.add(Long.MIN_VALUE));
        assertEquals(DoubleDouble.valueOf(Long.MIN_VALUE).add(-1),
                DoubleDouble.valueOf(Long.MAX_VALUE).add(2).neg());
        assertEquals(DoubleDouble.add(-1, Long.MIN_VALUE),
                DoubleDouble.valueOf(Long.MAX_VALUE).add(2).neg());
        assertEquals(DoubleDouble.add(-1, Long.MIN_VALUE + 1),
                DoubleDouble.valueOf(Long.MAX_VALUE).add(1).neg());
        assertEquals(DoubleDouble.add(-1.0, Long.MIN_VALUE),
                DoubleDouble.valueOf(Long.MAX_VALUE).add(2).neg());
        assertEquals(DoubleDouble.add(-1.0, Long.MIN_VALUE + 1),
                DoubleDouble.valueOf(Long.MAX_VALUE).add(1).neg());
        assertEquals(ZERO, DoubleDouble.add(1, Long.MIN_VALUE).sub(1 + Long.MIN_VALUE));
    }

    public static void test005() {
        final DoubleDouble POS_INF = DoubleDouble.valueOf(Double.POSITIVE_INFINITY);
        final DoubleDouble NEG_INF = DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);
        if (POS_INF.equals(NEG_INF)) {
            throw new AssertionError();
        }
        assertEquals(NEG_INF, POS_INF.neg());
        assertEquals(POS_INF, NEG_INF.neg());

        assertEquals(POS_INF, ZERO.add(Double.POSITIVE_INFINITY));
        assertEquals(POS_INF, NEG_ZERO.add(Double.POSITIVE_INFINITY));
        assertEquals(NEG_INF, ZERO.add(Double.NEGATIVE_INFINITY));
        assertEquals(NEG_INF, NEG_ZERO.add(Double.NEGATIVE_INFINITY));

        assertEquals(POS_INF, DoubleDouble.add(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(NEG_INF,
                DoubleDouble.add(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.add(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.add(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));

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

        assertEquals(POS_INF, DoubleDouble.valueOf(Long.MAX_VALUE).add(Double.POSITIVE_INFINITY));
        assertEquals(NEG_INF, DoubleDouble.valueOf(Long.MAX_VALUE).add(Double.NEGATIVE_INFINITY));

        assertEquals(Double.toString(Double.POSITIVE_INFINITY), POS_INF.toString());
        assertEquals(Double.toString(Double.NEGATIVE_INFINITY), NEG_INF.toString());
    }

    public static void test006() {
        final DoubleDouble POS_INF = DoubleDouble.valueOf(Double.POSITIVE_INFINITY);
        final DoubleDouble NEG_INF = DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);
        final DoubleDouble NaN = DoubleDouble.valueOf(Double.NaN);
        assertEquals(NaN, NaN);
        assertEquals(NaN, NaN.neg());
        assertEquals("NaN", NaN.toString());

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
        assertEquals(NaN, DoubleDouble.valueOf(Long.MAX_VALUE).add(Double.NaN));
        assertEquals(NaN, DoubleDouble.valueOf(-Long.MAX_VALUE).add(Double.NaN));
    }

    public static void test007() {
        if (System.identityHashCode(ZERO.toString()) != System.identityHashCode(ZERO.toString())) {
            throw new AssertionError();
        }
        long value = Long.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            DoubleDouble v = DoubleDouble.valueOf(value - i);
            assertEquals(value - i, v.toBigDecimal().longValueExact());
            assertEquals(Long.toString(value - i), v.toString());
        }
        value = Long.MIN_VALUE;
        for (int i = 0; i < 10; i++) {
            DoubleDouble v = DoubleDouble.valueOf(value + i);
            assertEquals(value + i, v.toBigDecimal().longValueExact());
            assertEquals(Long.toString(value + i), v.toString());
        }
        value = (long)StrictMath.pow(2, 53) + 10;
        for (int i = 0; i < 20; i++) {
            DoubleDouble v = DoubleDouble.valueOf(value - i);
            assertEquals(value - i, v.toBigDecimal().longValueExact());
            assertEquals(Long.toString(value - i), v.toString());
        }
        value = -((long)StrictMath.pow(2, 53) + 10);
        for (int i = 0; i < 20; i++) {
            DoubleDouble v = DoubleDouble.valueOf(value + i);
            assertEquals(value + i, v.toBigDecimal().longValueExact());
            assertEquals(Long.toString(value + i), v.toString());
        }
    }

    public static void test008() {
        assertEquals(ZERO, ZERO.sub(ZERO));
        assertEquals(ZERO, ZERO.sub(0));
        assertEquals(ZERO, ZERO.sub(0.0));
        assertEquals(ZERO, ZERO.sub(NEG_ZERO));
        assertEquals(ZERO, ZERO.sub(-0.0));
        assertEquals(ZERO, NEG_ZERO.sub(NEG_ZERO));
        assertEquals(ZERO, NEG_ZERO.sub(-0.0));
        assertEquals(NEG_ZERO, NEG_ZERO.sub(ZERO));
        assertEquals(NEG_ZERO, NEG_ZERO.sub(0));
        assertEquals(NEG_ZERO, NEG_ZERO.sub(0.0));

        assertEquals(ONE.neg(), ZERO.sub(ONE));
        assertEquals(ONE.neg(), ZERO.sub(1));
    }

    public static void test009() {
        final DoubleDouble DIV_1_3 = ONE.div(3);
        final DoubleDouble DIV_1_3_0 = ONE.div(3.0);
        final DoubleDouble RECIPROCAL_3 = DoubleDouble.reciprocal(3);
        final DoubleDouble RECIPROCAL_3_0 = DoubleDouble.reciprocal(3.0);

        assertEquals(DIV_1_3, DIV_1_3_0);
        assertEquals(DIV_1_3_0, RECIPROCAL_3);
        assertEquals(RECIPROCAL_3, RECIPROCAL_3_0);

        assertEquals("0.", DIV_1_3.toString().substring(0, 2));
        assertEquals("33333333333333333333333333333333",
                DIV_1_3.toString().substring(2, 2 + 32));

        assertEquals(2.0 / 3.0, DIV_1_3.mul(2).getFirst());
        assertEquals("66666666666666666666666666666666",
                DIV_1_3.mul(2).toString().substring(2, 2 + 32));
    }

    public static void test010() {
        assertEquals(ZERO, DoubleDouble.mul(1E-300, 1E-300));
        assertEquals(NEG_ZERO, DoubleDouble.mul(-1E-300, 1E-300));
        assertEquals(NEG_ZERO, DoubleDouble.mul(1E-300, -1E-300));
        assertEquals(ZERO, DoubleDouble.mul(-1E-300, -1E-300));

        final DoubleDouble POS_INF = DoubleDouble.valueOf(Double.POSITIVE_INFINITY);
        final DoubleDouble NEG_INF = DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);

        assertEquals(POS_INF, DoubleDouble.reciprocal(0.0));
        assertEquals(ONE.div(4.4E-323), DoubleDouble.reciprocal(4.4E-323));
        assertEquals(POS_INF, DoubleDouble.valueOf(100).div(0.0));
        assertEquals(POS_INF, DoubleDouble.valueOf(-100).div(-0.0));
        assertEquals(POS_INF, DoubleDouble.reciprocal(0));
        assertEquals(NEG_INF, DoubleDouble.reciprocal(-0.0));
        assertEquals(NEG_INF, DoubleDouble.valueOf(-100).div(0.0));
        assertEquals(NEG_INF, DoubleDouble.valueOf(100).div(-0.0));
        assertEquals(ZERO, DoubleDouble.reciprocal(Double.POSITIVE_INFINITY));
        assertEquals(NEG_ZERO, DoubleDouble.reciprocal(Double.NEGATIVE_INFINITY));

        assertEquals(POS_INF, DoubleDouble.mul(1E300, 1E300));
        assertEquals(NEG_INF, DoubleDouble.mul(-1E300, 1E300));
        assertEquals(NEG_INF, DoubleDouble.mul(1E300, -1E300));
        assertEquals(POS_INF, DoubleDouble.mul(-1E300, -1E300));
        assertEquals(DoubleDouble.valueOf(Double.NaN), DoubleDouble.mul(Double.NaN, 0));
        assertEquals(DoubleDouble.valueOf(Double.NaN), DoubleDouble.mul(0, Double.NaN));
        assertEquals(ONE, DoubleDouble.mul(32.0, 0.03125));

        assertEquals(DoubleDouble.mul(1E-300, 1E300),
                DoubleDouble.mul(1E300, 1E-300));
        assertEquals(1.0, DoubleDouble.mul(1E300, 1E-300).getFirst());
        if (Math.abs(DoubleDouble.mul(1E300, 1E-300).sub(1).getFirst()) > 7.75638521E-17) {
            throw new AssertionError();
        }


        assertEquals(ZERO, ONE.mul(0.0));
        assertEquals(NEG_ZERO, ONE.mul(-0.0));
        assertEquals(NEG_ZERO, ONE.neg().mul(0.0));
        assertEquals(ZERO, ONE.neg().mul(-0.0));

        assertEquals(ZERO, ZERO.mul(0.0));
        assertEquals(NEG_ZERO, ZERO.mul(-0.0));
        assertEquals(NEG_ZERO, NEG_ZERO.mul(0.0));
        assertEquals(ZERO, NEG_ZERO.mul(-0.0));

        assertEquals(ONE, ONE.mul(1.0));
        assertEquals(DoubleDouble.valueOf(Integer.MAX_VALUE), ONE.mul(Integer.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Integer.MIN_VALUE), ONE.mul(Integer.MIN_VALUE));
        assertEquals(DoubleDouble.valueOf(Long.MAX_VALUE), ONE.mul(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Long.MIN_VALUE), ONE.mul(Long.MIN_VALUE));
        assertEquals(ONE.neg(), ONE.mul(-1.0));

        assertEquals(DoubleDouble.valueOf((long) Integer.MAX_VALUE * Integer.MAX_VALUE),
                DoubleDouble.valueOf(Integer.MAX_VALUE).mul(Integer.MAX_VALUE));
        assertEquals(ZERO, DoubleDouble.valueOf(1E-300).div(3).mul(1E-300 / 7));
        assertEquals(NEG_ZERO, DoubleDouble.valueOf(-1E-300).div(3).mul(1E-300 / 7));
        assertEquals(NEG_ZERO, DoubleDouble.valueOf(1E-300).div(3).mul(-1E-300 / 7));
        assertEquals(ZERO, DoubleDouble.valueOf(-1E-300).div(3).mul(-1E-300 / 7));

        assertEquals(DoubleDouble.valueOf(Double.NaN), DoubleDouble.valueOf(Long.MAX_VALUE)
                .mul(Double.NaN));
        assertEquals(DoubleDouble.valueOf(Double.NaN), DoubleDouble.valueOf(Double.NaN)
                .mul(Long.MAX_VALUE));
    }

    public static void test011() {
        DoubleDouble DD_1_3 = ONE.div(3);
        DoubleDouble DD_1_7 = ONE.div(7);
        if (Math.abs(DD_1_3.mul(3).sub(1).getFirst()) > 1E-32) {
            throw new AssertionError();
        }
        if (Math.abs(DD_1_7.mul(7).sub(1).getFirst()) > 1E-32) {
            throw new AssertionError();
        }
        if (Math.abs(DD_1_3.mul(1.0 / 7.0).mul(21).sub(1).getFirst()) > 6E-17) {
            throw new AssertionError();
        }
        assertEquals(ZERO, DD_1_3.mul(1E-200).mul(1E-300));
        assertEquals(NEG_ZERO, DD_1_3.mul(1E-200).mul(-1E-300));
    }

    public static void test012() {
        assertEquals(ZERO, ZERO.div(1));
        assertEquals(NEG_ZERO, ZERO.div(-1));
        assertEquals(ONE.div(3).div(-1), ONE.div(-3).div(1));
        assertEquals(ONE.div(3).div(StrictMath.pow(2, -100)),
                ONE.div(3).mul(StrictMath.pow(2, 100)));

        DoubleDouble DIV_1_LONG_MAX = ONE.div(Long.MAX_VALUE);
        assertEquals(DIV_1_LONG_MAX, DoubleDouble.reciprocal(Long.MAX_VALUE));
        if (Math.abs(DIV_1_LONG_MAX.mul(Long.MAX_VALUE).sub(1).getFirst()) > 1.1755E-38) {
            throw new AssertionError();
        }
        assertEquals(ZERO, DoubleDouble.valueOf(Double.MIN_VALUE).div(Long.MAX_VALUE));
        assertEquals(NEG_ZERO, DoubleDouble.valueOf(Double.MIN_VALUE).div(-Long.MAX_VALUE));
        assertEquals(NEG_ZERO, DoubleDouble.valueOf(-Double.MIN_VALUE).div(Long.MAX_VALUE));
        assertEquals(ZERO, DoubleDouble.valueOf(-Double.MIN_VALUE).div(-Long.MAX_VALUE));

        assertEquals(DoubleDouble.valueOf(Double.NaN), DoubleDouble.valueOf(Double.NaN)
                .div(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Double.POSITIVE_INFINITY), DoubleDouble.valueOf(Double.POSITIVE_INFINITY)
                .div(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Double.NEGATIVE_INFINITY), DoubleDouble.valueOf(Double.NEGATIVE_INFINITY)
                .div(Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(Double.NEGATIVE_INFINITY), DoubleDouble.valueOf(Double.POSITIVE_INFINITY)
                .div(Long.MIN_VALUE + 1));
        assertEquals(DoubleDouble.valueOf(Double.POSITIVE_INFINITY), DoubleDouble.valueOf(Double.NEGATIVE_INFINITY)
                .div(Long.MIN_VALUE + 1));

        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.valueOf(Double.NaN).div(3));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                ZERO.div(Double.NaN));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                NEG_ZERO.div(Double.NaN));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.valueOf(3).div(Double.NaN));

        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.reciprocal(Double.NaN));
        assertEquals(DoubleDouble.valueOf(32),
                DoubleDouble.reciprocal(0.03125));
    }

    public static void test013() {
        assertEquals(ZERO, DoubleDouble.mul(0.0, 0.0));
        assertEquals(NEG_ZERO, DoubleDouble.mul(-0.0, 0.0));
        assertEquals(NEG_ZERO, DoubleDouble.mul(0.0, -0.0));
        assertEquals(ZERO, DoubleDouble.mul(-0.0, -0.0));

        assertEquals(ZERO, DoubleDouble.ZERO.mul(Long.MAX_VALUE));
        assertEquals(NEG_ZERO, DoubleDouble.ZERO.mul(-Long.MAX_VALUE));
        assertEquals(NEG_ZERO, NEG_ZERO.mul(Long.MAX_VALUE));
        assertEquals(ZERO, NEG_ZERO.mul(-Long.MAX_VALUE));

        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(Double.POSITIVE_INFINITY, 0.0));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(Double.POSITIVE_INFINITY, -0.0));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(Double.NEGATIVE_INFINITY, 0.0));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(Double.NEGATIVE_INFINITY, -0.0));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(0.0, Double.POSITIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(-0.0, Double.POSITIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(0.0, Double.NEGATIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN),
                DoubleDouble.mul(-0.0, Double.NEGATIVE_INFINITY));

        assertEquals(ONE, DoubleDouble.mul(1, 1));
        assertEquals(DoubleDouble.valueOf(Long.MAX_VALUE), DoubleDouble.mul(Long.MAX_VALUE, 1));
        assertEquals(DoubleDouble.valueOf(Long.MAX_VALUE), DoubleDouble.mul(1, Long.MAX_VALUE));
        assertEquals(DoubleDouble.valueOf(StrictMath.pow(2, 126))
                        .sub(StrictMath.pow(2, 64)),
                DoubleDouble.mul(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    public static void test014() {
        DoubleDouble POS_INF = valueOf(Double.POSITIVE_INFINITY);
        DoubleDouble NEG_INF = valueOf(Double.NEGATIVE_INFINITY);
        assertEquals(POS_INF, POS_INF.div(0.0));
        assertEquals(NEG_INF, POS_INF.div(-0.0));
        assertEquals(NEG_INF, NEG_INF.div(0.0));
        assertEquals(POS_INF, NEG_INF.div(-0.0));

        assertEquals(ZERO, ZERO.div(Double.POSITIVE_INFINITY));
        assertEquals(NEG_ZERO, ZERO.div(Double.NEGATIVE_INFINITY));
        assertEquals(NEG_ZERO, NEG_ZERO.div(Double.POSITIVE_INFINITY));
        assertEquals(ZERO, NEG_ZERO.div(Double.NEGATIVE_INFINITY));

        assertEquals(POS_INF, POS_INF.div(100));
        assertEquals(NEG_INF, POS_INF.div(-100));
        assertEquals(NEG_INF, NEG_INF.div(100));
        assertEquals(POS_INF, NEG_INF.div(-100));

        assertEquals(DoubleDouble.valueOf(Double.NaN), POS_INF.div(Double.POSITIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN), NEG_INF.div(Double.POSITIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN), POS_INF.div(Double.NEGATIVE_INFINITY));
        assertEquals(DoubleDouble.valueOf(Double.NaN), NEG_INF.div(Double.NEGATIVE_INFINITY));
    }

    public static void test015() {
        DoubleDouble zero = DoubleDouble.valueOf(0);
        DoubleDouble negZero = DoubleDouble.valueOf(-0.0);
        DoubleDouble POS_INF = valueOf(Double.POSITIVE_INFINITY);
        DoubleDouble NEG_INF = valueOf(Double.NEGATIVE_INFINITY);

        assertSameObject(zero, zero.add(0.0));
        assertSameObject(zero, zero.add(-0.0));
        assertSameObject(zero, zero.add(0));
        assertSameObject(zero, zero.add(ZERO));
        assertSameObject(zero, zero.add(negZero));
        assertSameObject(zero, negZero.add(zero));

        assertSameObject(ONE, zero.add(ONE));
        assertSameObject(ONE, ONE.add(zero));
        assertSameObject(ONE, ONE.add(0));
        assertSameObject(ONE, ONE.add(0.0));
        assertSameObject(POS_INF, POS_INF.add(POS_INF));
        assertSameObject(POS_INF, POS_INF.add(Double.POSITIVE_INFINITY));
        assertSameObject(POS_INF, POS_INF.add(1));
        assertSameObject(POS_INF, POS_INF.add(-Long.MAX_VALUE));
        assertSameObject(POS_INF, zero.add(POS_INF));
        assertSameObject(POS_INF, POS_INF.add(zero));
        assertSameObject(POS_INF, POS_INF.add(0));
        assertSameObject(POS_INF, POS_INF.add(0.0));
        assertSameObject(NEG_INF, NEG_INF.add(NEG_INF));
        assertSameObject(NEG_INF, NEG_INF.add(Double.NEGATIVE_INFINITY));
        assertSameObject(NEG_INF, NEG_INF.add(1));
        assertSameObject(NEG_INF, NEG_INF.add(Long.MAX_VALUE));
        assertSameObject(NEG_INF, zero.add(NEG_INF));
        assertSameObject(NEG_INF, NEG_INF.add(zero));
        assertSameObject(NEG_INF, NEG_INF.add(0));
        assertSameObject(NEG_INF, NEG_INF.add(0.0));

        assertSameObject(negZero, negZero.add(-0.0));
        assertSameObject(negZero, negZero.add(DoubleDouble.valueOf(-0.0)));

        assertSameObject(ONE, negZero.add(ONE));
        assertSameObject(ONE, ONE.add(negZero));
        assertSameObject(POS_INF, negZero.add(POS_INF));
        assertSameObject(POS_INF, POS_INF.add(negZero));
        assertSameObject(POS_INF, POS_INF.add(-0.0));
        assertSameObject(NEG_INF, negZero.add(NEG_INF));
        assertSameObject(NEG_INF, NEG_INF.add(negZero));
        assertSameObject(NEG_INF, NEG_INF.add(-0.0));
    }

    public static void test016() {
        DoubleDouble DD_LON_MAX = DoubleDouble.valueOf(Long.MAX_VALUE);
        DoubleDouble zero = ZERO;
        DoubleDouble POS_INF = DoubleDouble.valueOf(Double.POSITIVE_INFINITY);
        DoubleDouble NEG_INF = DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);

        assertSameObject(DD_LON_MAX, DD_LON_MAX.mul(1));
        assertSameObject(DD_LON_MAX, DD_LON_MAX.mul(1.0));
        assertEquals(zero, zero.mul(1));
        assertEquals(zero, zero.mul(0));
        assertEquals(zero, zero.mul(1.0));
        assertEquals(zero, zero.mul(Long.MAX_VALUE));


        assertEquals(POS_INF, POS_INF.mul(1));
        assertEquals(POS_INF, POS_INF.mul(10));
        assertEquals(POS_INF, POS_INF.mul(Long.MAX_VALUE));

        assertEquals(NEG_INF, NEG_INF.mul(1));
        assertEquals(NEG_INF, NEG_INF.mul(10));
        assertEquals(NEG_INF, NEG_INF.mul(Long.MAX_VALUE));
    }

    public static void test999() {
        Random random = new Random(0);
        long count = 0;
        for (int i = 0; i < 10000; i++) {
            double r1 = random.nextDouble();
            double r2 = random.nextDouble();
            double r3 = random.nextDouble() / StrictMath.pow(2, 53);
            double r4 = random.nextDouble() / StrictMath.pow(2, 53);

            DoubleDouble d1 = DoubleDouble.add(r1, r3);
            DoubleDouble d2 = DoubleDouble.add(r2, r4);
            int shift = random.nextInt(30);
            d2 = d2.mul(StrictMath.pow(2, -shift));
            if (!Objects.equals(refAdd(d1, d2), d1.add(d2))) {
                count++;
            }
            if (!Objects.equals(refSub(d1, d2), d1.sub(d2))) {
                count++;
            }
            if (!Objects.equals(refMul(d1, d2), d1.mul(d2))) {
                count++;
            }
            if (!Objects.equals(refDiv(d1, d2), d1.div(d2))) {
                count++;
            }
            if (!Objects.equals(refDiv(ONE, d2), DoubleDouble.reciprocal(d2))) {
                count++;
            }
        }
        assertEquals(0L, count);
    }

    private static DoubleDouble refAdd(DoubleDouble d1, DoubleDouble d2) {
        if (Double.doubleToRawLongBits(-0.0) == Double.doubleToRawLongBits(d1.getFirst())
                && Double.doubleToRawLongBits(-0.0) == Double.doubleToRawLongBits(d2.getFirst())) {
            return DoubleDouble.valueOf(-0.0);
        }
        return fromBigDecimal(d1.toBigDecimal().add(d2.toBigDecimal()));
    }

    private static DoubleDouble refSub(DoubleDouble d1, DoubleDouble d2) {
        if (Double.doubleToRawLongBits(-0.0) == Double.doubleToRawLongBits(d1.getFirst())
                && Double.doubleToRawLongBits(0.0) == Double.doubleToRawLongBits(d2.getFirst())) {
            return DoubleDouble.valueOf(-0.0);
        }
        return fromBigDecimal(d1.toBigDecimal().subtract(d2.toBigDecimal()));
    }

    private static DoubleDouble refMul(DoubleDouble d1, DoubleDouble d2) {
        return fromBigDecimal(d1.toBigDecimal().multiply(d2.toBigDecimal()));
    }

    private static DoubleDouble refDiv(DoubleDouble d1, DoubleDouble d2) {
        return fromBigDecimal(d1.toBigDecimal().divide(
                d2.toBigDecimal(), new MathContext(40 * 3 + 10)));
    }

    private static DoubleDouble fromBigDecimal(BigDecimal bd) {
        double f1 = bd.doubleValue();
        double f2 = bd.subtract(new BigDecimal(f1)).doubleValue();
        return DoubleDouble.add(f1, f2);
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

    private static void assertSameObject(Object expected, Object actual) {
        if (expected == null) {
            String msg = "expected should not be null";
            throw new AssertionError(msg);
        }
        if (expected != actual) {
            String msg = "expected: " + expected + ", actual: " + actual;
            throw new AssertionError(msg);
        }
    }
}
