package ntysdd;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * 用两个double变量来表示一个数，有效数字大约有32位
 * 适用情况：当需要较高精度，而又不想使用BigDecimal的时候
 * 注意：
 * 这个类目前没有完整测试上溢、下溢、NaN等情况，
 * 也不保证中间计算能保留所有精度，不保证计算结果正确到最后一位
 */
public final strictfp class DoubleDouble {
    private final double first;
    private final double second;
    // 用来缓存String表示
    private String toStringCache;

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
        if (Math.abs(x) < (long) Math.pow(2, 53)) {
            return valueOf((double) x);
        }
        int part1 = ((int) x & 0x7fffffff);
        long part2 = x - part1;
        return valueOf((double) part1).add(valueOf((double) part2));
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

    private static class Pair {
        final double v1;
        final double v2;

        Pair(double v1, double v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    private static Pair split2(double x) {
        int exponent = Math.getExponent(x);
        int shift = exponent + 1022 + 27;
        double t1 = x;
        t1 = Math.scalb(t1, -shift);
        t1 = Math.scalb(t1, shift);
        double t2 = x - t1;
        return new Pair(t1, t2);
    }

    /**
     * 计算两个double的积，结果表示为DoubleDouble
     */
    public static DoubleDouble mul(double lhs, double rhs) {
        Pair s1 = split2(lhs);
        Pair s2 = split2(rhs);
        double r1 = lhs * rhs;
        double c = s1.v1 * s2.v1 - r1;
        c += s1.v1 * s2.v2;
        c += s1.v2 * s2.v1;
        c += s1.v2 * s2.v2;
        return new DoubleDouble(r1, c);
    }

    /**
     * 计算两个DoubleDouble的和，返回DoubleDouble
     */
    public DoubleDouble add(DoubleDouble rhs) {
        double x1 = this.first;
        double x2 = rhs.first;
        double x3 = this.second;
        double x4 = rhs.second;
        if (Math.abs(x2) > Math.abs(x1)) {
            double t = x2;
            x2 = x1;
            x1 = t;
            if (Math.abs(x4) > Math.abs(x2)) {
                t = x2;
                x2 = x4;
                x4 = t;
            }
        } else if (Math.abs(x3) > Math.abs(x2)) {
            double t = x2;
            x2 = x3;
            x3 = t;
        }
        if (Math.abs(x4) > Math.abs(x3)) {
            double t = x3;
            x3 = x4;
            x4 = t;
        }
        if (!(Math.abs(x1) >= Math.abs(x2) && Math.abs(x2) >= Math.abs(x3) && Math.abs(x3) >= Math.abs(x4))) {
            throw new AssertionError();
        }
        double[] v = {x1, x2, x3, x4};

        double s = 0;
        double c = 0;
        for (double x : v) {
            c -= x;
            double s1 = s - c;
            c = s1 - s + c;
            s = s1;
        }
        return new DoubleDouble(s, -c);
    }

    /**
     * 计算一个DoubleDouble和一个double的积，返回DoubleDouble
     */
    public DoubleDouble mul(double rhs) {
        DoubleDouble r1 = mul(this.first, rhs);
        DoubleDouble r2 = mul(this.second, rhs);
        return r1.add(r2);
    }

    /**
     * 计算一个DoubleDouble和一个double的商，返回DoubleDouble
     */
    public DoubleDouble div(double rhs) {
        double r1 = this.first / rhs;
        double rx = this.second / rhs;
        DoubleDouble m = mul(r1, rhs);
        DoubleDouble r = m.add(new DoubleDouble(-this.first, 0));
        double r2 = add(-r.first / rhs, rx).first;
        return new DoubleDouble(r1, r2);
    }

    private DoubleDouble(double first, double second) {
        if (Double.isNaN(first) || Double.isNaN(second)) {
            this.first = Double.NaN;
            this.second = Double.NaN;
        } else {
            if (first != 0 && second == 0) {
                // -0.0导致的问题太多，特殊处理一下
                this.first = first;
                this.second = 0;
                return;
            }
            this.first = first;
            this.second = second;
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
            return Long.toString(longValue);
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
}
