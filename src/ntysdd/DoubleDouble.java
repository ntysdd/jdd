package ntysdd;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 用两个double变量来表示一个数，有效数字大约有32位
 * 适用情况：当需要较高精度，而又不想使用BigDecimal的时候
 * 注意：
 * 这个类目前没有完整测试上溢、下溢、NaN等情况，
 * 也不保证中间计算能保留所有精度，不保证计算结果正确到最后一位
 */
public final strictfp class DoubleDouble {
    public static final DoubleDouble ZERO = DoubleDouble.valueOf(0.0);
    public static final DoubleDouble ONE = DoubleDouble.valueOf(1.0);
    public static final DoubleDouble TWO = DoubleDouble.valueOf(2.0);
    // 在一个内部使用二进制小数的类里设置这样一个常量有点没有道理
    // 考虑到常用的BigDecimal类中有这个常量，这里也设置一下
    public static final DoubleDouble TEN = DoubleDouble.valueOf(10.0);

    private final double first;
    private final double second;
    // 用来缓存String表示
    private String toStringCache;

    private static final MethodHandle FMA_METHOD;

    static {
        Method fma = null;
        try {
            fma = Math.class.getMethod("fma", double.class, double.class, double.class);
        } catch (NoSuchMethodException ignore) {
        }
        MethodHandle fmaHandle = null;
        if (fma != null) {
            try {
                fmaHandle = MethodHandles.publicLookup().unreflect(fma);
            } catch (IllegalAccessException ignore) {
            }
        }
        FMA_METHOD = fmaHandle;
    }

    /**
     * 将double转为DoubleDouble
     */
    public static DoubleDouble valueOf(double x) {
        return new DoubleDouble(x);
    }

    /**
     * 将long转为DoubleDouble
     */
    public static DoubleDouble valueOf(long x) {
        if (canLongBeConvertedToDoubleExactly(x)) {
            return valueOf((double) x);
        }
        int part1 = ((int) x & 0x7fffffff);
        long part2 = x - part1;
        return add((double) part1, (double) part2);
    }

    private DoubleDouble(double v) {
        if (Double.isNaN(v)) {
            first = Double.NaN;
            second = Double.NaN;
        } else {
            first = v;
            second = 0;
        }
    }

    /**
     * 计算两个double的和，结果表示为DoubleDouble
     */
    public static DoubleDouble add(double lhs, double rhs) {
        double v1 = lhs;
        double v2 = rhs;
        if (Math.abs(v2) > Math.abs(v1)) {
            double t = v2;
            v2 = v1;
            v1 = t;
        }
        double f = v1 + v2;
        double s = (v1 - f) + v2;
        return new DoubleDouble(f, s);
    }

    /**
     * 计算一个DoubleDouble和一个double的和，返回DoubleDouble
     */
    public DoubleDouble add(double rhs) {
        if (rhs == 0) {
            double first = this.first;
            if (first == 0) {
                // 0 + 0的特殊形式，需要考虑±0的问题
                double result = first + rhs;
                if (Double.doubleToRawLongBits(result) == Double.doubleToRawLongBits(first)) {
                    return this;
                } else {
                    return DoubleDouble.valueOf(result);
                }
            } else {
                return this;
            }
        }
        double first = this.first;
        double second = this.second;
        DoubleDouble t = add(first, rhs);
        double t2 = second + t.second;
        return add(t.first, t2);
    }

    /**
     * 相反数
     */
    public DoubleDouble neg() {
        double first = this.first;
        double second = this.second;
        if (first == 0) {
            return new DoubleDouble(-first, 0.0);
        }
        return new DoubleDouble(-first, -second);
    }

    /**
     * 计算两个DoubleDouble的差，返回DoubleDouble
     */
    public DoubleDouble sub(DoubleDouble rhs) {
        return this.add(rhs.neg());
    }

    /**
     * 计算一个DoubleDouble和一个double的差，返回DoubleDouble
     */
    public DoubleDouble sub(double rhs) {
        return this.add(-rhs);
    }


    private static class Pair {
        final double v1;
        final double v2;

        Pair(double v1, double v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    private static final long POW_2_27 = (long) StrictMath.pow(2, 27);
    private static Pair split2(double v) {
        double c = POW_2_27 + 1;
        double vp = v * c;
        double v1 = (v - vp) + vp;
        double v2 = v - v1;
        return new Pair(v1, v2);
    }

    private static final double POW_2_970 = StrictMath.pow(2, 970);
    private static final double POW_2_minus_53 = StrictMath.pow(2, -53);
    /**
     * 计算两个double的积，结果表示为DoubleDouble
     */
    public static DoubleDouble mul(double lhs, double rhs) {
        double r1 = lhs * rhs;
        if (r1 == 0.0) {
            // 如果lhs或者rhs为0，结果为0
            // 即使因为下溢导致结果为0，也只能这么处理
            return DoubleDouble.valueOf(r1);
        }
        if (lhs == 1.0 || lhs == -1.0 || rhs == 1.0 || rhs == -1.0) {
            return DoubleDouble.valueOf(r1);
        }
        if (Double.isInfinite(r1)) {
            return new DoubleDouble(r1, 0);
        }
        if (FMA_METHOD != null) {
            double r2;
            try {
                r2 = (double) FMA_METHOD.invokeExact(lhs, rhs, -r1);
            } catch (Throwable e) {
                throw new AssertionError(e);
            }
            return new DoubleDouble(r1, r2);
        }
        int shift = 0;
        if (Math.abs(lhs) >= POW_2_970) {
            lhs *= POW_2_minus_53;
            shift += 53;
        }
        if (Math.abs(rhs) >= POW_2_970) {
            rhs *= POW_2_minus_53;
            shift += 53;
        }
        Pair s1 = split2(lhs);
        Pair s2 = split2(rhs);
        double c = s1.v1 * s2.v1 - lhs * rhs;
        c += s1.v1 * s2.v2;
        c += s1.v2 * s2.v1;
        c += s1.v2 * s2.v2;
        if (shift == 0) {
            return new DoubleDouble(lhs * rhs, c);
        } else {
            double scale;
            if (shift == 53) {
                scale = (double) POW_2_53;
            } else {
                scale = StrictMath.pow(2, shift);
            }
            return new DoubleDouble(lhs * rhs * scale,
                    c * scale);
        }
    }

    /**
     * 计算两个DoubleDouble的和，返回DoubleDouble
     */
    public DoubleDouble add(DoubleDouble rhs) {
        double x1 = this.first;
        double x2 = rhs.first;
        if (x1 == 0 || x2 == 0) {
            if (x1 == 0 && x2 == 0) {
                // 处理±0的问题
                double res = x1 + x2;
                if (Double.doubleToRawLongBits(x1) == Double.doubleToRawLongBits(res)) {
                    return this;
                } else {
                    return rhs;
                }
            } else if (x2 == 0) {
                return this;
            } else {
                return rhs;
            }
        }
        double x3 = this.second;
        double x4 = rhs.second;
        DoubleDouble t1 = add(x1, x2);
        DoubleDouble t2 = add(t1.second, x3);
        DoubleDouble t3 = add(t1.first, t2.first);
        DoubleDouble t4 = add(t2.second, t3.second);
        DoubleDouble t5 = add(t3.first, x4);
        DoubleDouble t6 = add(t5.second, t4.first);
        double t7 = t6.second + t4.second;
        DoubleDouble t8 = add(t7, t6.first);
        return add(t5.first, t8.first);
    }

    /**
     * 计算一个DoubleDouble和一个double的积，返回DoubleDouble
     */
    public DoubleDouble mul(double rhs) {
        double first = this.first;
        if (first == 0 || rhs == 0) {
            // 处理±0
            if (first == 0 && Math.copySign(1, rhs) > 0) {
                return this;
            }
            return DoubleDouble.valueOf(first * rhs);
        }
        if (rhs == 1) {
            return this;
        }
        if (rhs == -1) {
            return this.neg();
        }
        DoubleDouble r1 = mul(first, rhs);
        DoubleDouble r2 = mul(this.second, rhs);
        DoubleDouble result = r1.add(r2);
        if (result.first == 0) {
            // 处理±0
            double res = Math.copySign(0, this.first * rhs);
            if (Double.doubleToRawLongBits(res) == Double.doubleToRawLongBits(first)) {
                return this;
            }
            return DoubleDouble.valueOf(res);
        }
        return result;
    }

    /**
     * 计算一个DoubleDouble和一个double的商，返回DoubleDouble
     */
    public DoubleDouble div(double rhs) {
        double r1 = this.first / rhs;
        if (Double.isNaN(r1)) {
            if (Double.isNaN(this.first)) {
                return this;
            }
            return new DoubleDouble(Double.NaN);
        }
        DoubleDouble m = mul(r1, rhs);
        DoubleDouble r = m.neg().add(this.first);
        DoubleDouble rr = r.add(this.second);
        double r2 = rr.first / rhs;
        DoubleDouble m2 = mul(r2, rhs);
        DoubleDouble k = m2.neg().add(rr.first);
        k = k.add(rr.second);
        double r3 = k.first / rhs;
        return add(r1, r2).add(r3);
    }

    /**
     * 计算一个double的倒数
     */
    public static DoubleDouble reciprocal(double value) {
        if (value == 0 || Double.isInfinite(value)) {
            return new DoubleDouble(1.0 / value, 0);
        }
        if (Double.isNaN(value)) {
            return DoubleDouble.valueOf(value);
        }
        if (Math.abs(Math.scalb(value, -Math.getExponent(value))) == 1) {
            // value是2的整数幂
            double r = 1.0 / value;
            return new DoubleDouble(r, 0);
        }

        return DoubleDouble.ONE.div(value);
    }

    private DoubleDouble(double first, double second) {
        if (Double.isNaN(first) || Double.isNaN(second)) {
            this.first = Double.NaN;
            this.second = Double.NaN;
        } else {
            // assert Math.abs(first) > Math.abs(second) || first == 0 && second == 0
            if (first == 0 || second == 0) {
                this.first = first;
                this.second = 0;
            } else {
                this.first = first;
                this.second = second;
            }
        }
    }

    /**
     * 返回内部表示的第一部分
     */
    public double getFirst() {
        return first;
    }

    /**
     * 返回内部表示的第二部分
     */
    public double getSecond() {
        return second;
    }

    /**
     * 转成String
     * 注意：
     * 不保证返回的String能够精确表示这个数
     * 不保证返回的表示具体形式
     */
    @Override
    public String toString() {
        String toStringCache = this.toStringCache;
        if (toStringCache != null) {
            return toStringCache;
        }
        if (first == 0 && second == 0) {
            if (Math.copySign(1, first) < 0) {
                return this.toStringCache = "-0";
            } else {
                return this.toStringCache = "0";
            }
        }
        if (Double.isNaN(first) || Double.isNaN(second)) {
            return this.toStringCache = Double.toString(Double.NaN);
        }
        if (Double.isInfinite(first)) {
            return this.toStringCache = Double.toString(first);
        }
        if (Double.isInfinite(second)) {
            return this.toStringCache = Double.toString(second);
        }
        BigDecimal bd = new BigDecimal(first).add(new BigDecimal(second));
        long longValue = bd.longValue();
        if (bd.compareTo(BigDecimal.valueOf(longValue)) == 0) {
            return this.toStringCache = Long.toString(longValue);
        }
        MathContext mathContext = MathContext.DECIMAL128;
        return this.toStringCache = bd.round(mathContext)
                .stripTrailingZeros()
                .toString();
    }

    /**
     * 转成BigDecimal
     * 值为Inf或者NaN时抛出异常
     */
    public BigDecimal toBigDecimal() {
        return new BigDecimal(first)
                .add(new BigDecimal(second)).stripTrailingZeros();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DoubleDouble)) {
            return false;
        }
        DoubleDouble that = (DoubleDouble) o;
        return Double.compare(this.first, that.first) == 0
                && Double.compare(this.second, that.second) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    public DoubleDouble add(long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            return this.add((double) rhs);
        }
        DoubleDouble rhs2 = DoubleDouble.valueOf(rhs);
        return this.add(rhs2);
    }

    public static DoubleDouble add(long lhs, double rhs) {
        return add(rhs, lhs);
    }

    public static DoubleDouble add(double lhs, long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            return add((double) lhs, (double) rhs);
        }
        return DoubleDouble.valueOf(rhs).add(lhs);
    }

    public static DoubleDouble add(long lhs, long rhs) {
        long value = lhs + rhs;
        if (((lhs ^ value) & (rhs ^ value)) < 0) {
            // 溢出
            return valueOf(lhs).add(rhs);
        }
        return valueOf(value);
    }

    public DoubleDouble sub(long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            return this.sub((double) rhs);
        }
        DoubleDouble rhs2 = DoubleDouble.valueOf(rhs);
        return this.sub(rhs2);
    }

    public DoubleDouble mul(long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            return this.mul((double) rhs);
        }
        // DoubleDouble和DoubleDouble的乘法还没有实现
        // 先用BigDecimal来处理
        BigDecimal bd = this.toBigDecimal();
        bd = bd.multiply(BigDecimal.valueOf(rhs));
        double f = bd.doubleValue();
        double s = bd.subtract(new BigDecimal(f)).doubleValue();
        if (f == 0) {
            return this.mul(Math.copySign(0, rhs));
        }
        return new DoubleDouble(f, s);
    }

    public DoubleDouble div(long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            return this.div((double) rhs);
        }
        // DoubleDouble和DoubleDouble的除法还没有实现
        // 先用BigDecimal来处理
        BigDecimal bd = this.toBigDecimal();
        bd = bd.divide(BigDecimal.valueOf(rhs), new MathContext(100, RoundingMode.HALF_EVEN));
        double f = bd.doubleValue();
        double s = bd.subtract(new BigDecimal(f)).doubleValue();
        if (f == 0) {
            return this.mul(Math.copySign(0, rhs));
        }
        return new DoubleDouble(f, s);
    }

    public static DoubleDouble reciprocal(long value) {
        if (canLongBeConvertedToDoubleExactly(value)) {
            return DoubleDouble.reciprocal((double) value);
        }
        return DoubleDouble.valueOf(1).div(value);
    }

    private static final long POW_2_53 = (long) StrictMath.pow(2, 53);
    private static boolean canLongBeConvertedToDoubleExactly(long x) {
        if (-POW_2_53 <= x && x <= POW_2_53) {
            return true;
        }
        long v = (long) (double) x;
        if (v == Long.MAX_VALUE) {
            // 发生溢出
            // Long.MIN_VALUE的绝对值是2的整数幂，所以不返回false
            // 当x不是Long.MIN_VALUE而v是Long.MIN_VALUE时，后续的比较会失败
            return false;
        }
        return v == x;
    }
}
