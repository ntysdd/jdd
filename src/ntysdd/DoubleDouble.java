package ntysdd;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * 用两个double变量来表示一个数，有效数字大约有32位
 * 适用情况：当需要较高精度，而又不想使用BigDecimal的时候
 * 注意：
 * 这个类目前没有完整测试上溢、下溢、NaN等情况，
 * 也不保证中间计算能保留所有精度，不保证计算结果正确到最后一位
 */
public final strictfp class DoubleDouble {
    public static final DoubleDouble ZERO = new DoubleDouble(0.0, 0.0);
    public static final DoubleDouble ONE = new DoubleDouble(1.0, 0.0);
    public static final DoubleDouble TWO = new DoubleDouble(2.0, 0.0);
    // 在一个内部使用二进制小数的类里设置这样一个常量有点没有道理
    // 考虑到常用的BigDecimal类中有这个常量，这里也设置一下
    public static final DoubleDouble TEN = new DoubleDouble(10.0, 0.0);

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
        if (x == 1) {
            return ONE;
        }
        if (x == 2) {
            return TWO;
        }
        if (x == 10) {
            return TEN;
        }
        if (Double.doubleToRawLongBits(x) == 0) {
            return ZERO;
        }
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
        double f = lhs + rhs;
        if (Double.isInfinite(f)) {
            return new DoubleDouble(f);
        }
        if (Math.abs(lhs) >= Math.abs(rhs)) {
            return new DoubleDouble(f, rhs - (f - lhs));
        } else {
            return new DoubleDouble(f, lhs - (f - rhs));
        }
    }

    /**
     * 计算一个DoubleDouble和一个double的和，返回DoubleDouble
     */
    public DoubleDouble add(double rhs) {
        double first = this.first;
        if (rhs == 0) {
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
        if (first == 0.0) {
            double res = first + rhs;
            return DoubleDouble.valueOf(res);
        }
        if (Double.isInfinite(first) && first == rhs) {
            return this;
        }
        if (this.second == 0) {
            DoubleDouble res = DoubleDouble.add(first, rhs);
            if (this.equals(res)) {
                return this;
            }
            return res;
        }
        if (Double.isNaN(first)) {
            return this;
        }
        if (Double.isNaN(rhs)) {
            return DoubleDouble.valueOf(Double.NaN);
        }
        if (Double.isInfinite(first) || Double.isInfinite(rhs)) {
            return DoubleDouble.valueOf(first + rhs);
        }
        double second = this.second;
        DoubleDouble t = add(first, rhs);
        if (t.first == 0) {
            return new DoubleDouble(second);
        }
        if (t.second == 0 && second + t.first == t.first) {
            return new DoubleDouble(t.first, second);
        }
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
        if (Double.isInfinite(r1)) {
            return new DoubleDouble(r1);
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
        if (Double.isNaN(r1)) {
            return new DoubleDouble(Double.NaN);
        }
        if (Math.abs(lhs) == 1 || Math.abs(lhs) == 0.5 || Math.abs(lhs) == 2
                || Math.abs(rhs) == 1 || Math.abs(rhs) == 0.5 || Math.abs(rhs) == 2) {
            // 这些常见值走快速路径
            return DoubleDouble.valueOf(r1);
        }
        double mantissa1 = Math.scalb(lhs, -Math.getExponent(lhs));
        double mantissa2 = Math.scalb(rhs, -Math.getExponent(rhs));
        if (Math.abs(mantissa1) == 1.0 || Math.abs(mantissa2) == 1.0) {
            return DoubleDouble.valueOf(r1);
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
        if (Double.isInfinite(x1)) {
            if (Double.isFinite(x2) || x1 == x2) {
                return this;
            }
            return DoubleDouble.valueOf(Double.NaN);
        }
        double x3 = this.second;
        double x4 = rhs.second;

        double[] v = {x1, x2, x3, x4};
        sortByAbsMaxFirst(v);

        double s1 = 0;
        double s2 = 0;
        double s3 = 0;

        for (double x : v) {
            s3 += x;
            DoubleDouble t = add(s2, s3);
            s3 = t.second;
            s2 = t.first;
            t = add(s1, s2);
            s2 = t.second;
            s1 = t.first;
        }
        return add(s1, s2).add(s3);
    }

    /*
     * 将数组按照绝对值排序，绝对值最大的排在最开头，绝对值最小的排在最末尾
     * 对于不含NaN的小数组，采用插排
     * 对于大数组，交给Arrays.sort(T[], Comparator)排序
     */
    private static void sortByAbsMaxFirst(double[] v) {
        OUT:
        if (v.length <= 20) {
            // 插排
            for (int i = 0; i < v.length; i++) {
                double val = v[i];
                if (Double.isNaN(val)) {
                    // 发现含有NaN，则跳到外面，用Arrays.sort(T[], Comparator)来排序
                    break OUT;
                }
                if (i == 0) {
                    continue;
                }
                double key = Math.abs(val);
                int j = i - 1;
                while (j >= 0 && Math.abs(v[j]) < key) {
                    v[j + 1] = v[j];
                    j--;
                }
                v[j + 1] = val;
            }
            return;
        }

        // 为了能匹配API的签名，这里生成了很多包装对象
        Double[] t = new Double[v.length];
        for (int i = 0; i < v.length; i++) {
            t[i] = v[i];
        }
        Arrays.sort(t, Comparator.comparing(x -> -Math.abs(x)));
        for (int i = 0; i < v.length; i++) {
            v[i] = t[i];
        }
    }

    /**
     * 计算一个DoubleDouble和一个double的积，返回DoubleDouble
     */
    public DoubleDouble mul(double rhs) {
        double first = this.first;
        if (first == 0 || rhs == 0) {
            double res = first * rhs;
            // 处理±0
            if (Double.doubleToLongBits(first) == Double.doubleToLongBits(res)) {
                return this;
            }
            return DoubleDouble.valueOf(res);
        }
        if (rhs == 1) {
            return this;
        }
        if (rhs == -1) {
            return this.neg();
        }
        if (this.second == 0) {
            return mul(first, rhs);
        }
        if (Double.isNaN(first)) {
            return this;
        }
        if (Double.isNaN(rhs)) {
            return DoubleDouble.valueOf(Double.NaN);
        }
        DoubleDouble r1 = mul(first, rhs);
        DoubleDouble r2 = mul(this.second, rhs);
        DoubleDouble result;
        if (r1.second != 0 || r2.second != 0) {
            result = r1.add(r2);
        } else {
            result = add(r1.first, r2.first);
        }
        if (result.first == 0) {
            // 处理±0
            double res = Math.copySign(0, first * rhs);
            return DoubleDouble.valueOf(res);
        }
        return result;
    }

    /**
     * 计算一个DoubleDouble和一个DoubleDouble的积，返回DoubleDouble
     */
    public DoubleDouble mul(DoubleDouble rhs) {
        if (rhs.second == 0 || Double.isNaN(rhs.first)
                || Double.isInfinite(rhs.first)
                || this.first == 0 || Double.isNaN(this.first)
                || Double.isInfinite(this.first)) {
            return this.mul(rhs.first);
        }
        if (this.second == 0) {
            return rhs.mul(this.first);
        }
        double x1 = this.first;
        double x2 = rhs.first;
        double x3 = this.second;
        double x4 = rhs.second;

        DoubleDouble[] v = {
                mul(x1, x2),
                mul(x1, x4),
                mul(x2, x3),
                mul(x3, x4)
        };
        DoubleDouble sum = ZERO;
        DoubleDouble c = ZERO;
        for (DoubleDouble x : v) {
            c = c.add(x);
            DoubleDouble s1 = sum.add(c);
            c = c.sub(s1.sub(sum));
            sum = s1;
        }
        return sum;
    }

    /**
     * 计算一个DoubleDouble和一个double的商，返回DoubleDouble
     */
    public DoubleDouble div(double rhs) {
        double first = this.first;
        if (first == 0 && rhs != 0) {
            if (Double.isNaN(rhs)) {
                return DoubleDouble.valueOf(Double.NaN);
            }
            if (Double.isFinite(rhs)) {
                return this.mul(rhs);
            } else {
                return this.mul(1.0 / rhs);
            }
        }
        if (Double.isInfinite(rhs)) {
            return this.mul(1.0 / rhs);
        }
        if (Double.isInfinite(first)) {
            if (Double.isFinite(rhs) && rhs != 0) {
                return this.mul(rhs);
            }
        }
        if (rhs == 1.0) {
            return this;
        }
        if (Double.isNaN(first)) {
            return this;
        }
        if (rhs == 0) {
            double res = first / rhs;
            if (Double.doubleToLongBits(res) == Double.doubleToLongBits(this.first)) {
                return this;
            }
            return DoubleDouble.valueOf(res);
        }
        if (Math.abs(Math.scalb(rhs, -Math.getExponent(rhs))) == 1) {
            // rhs是2的整数次幂，且1.0 / rhs不会导致无穷
            return this.mul(1.0 / rhs);
        }
        double r1 = first / rhs;
        if (!Double.isFinite(r1)) {
            return new DoubleDouble(r1);
        }
        DoubleDouble m = mul(r1, rhs);
        DoubleDouble r = m.neg().add(first);
        DoubleDouble rr = r.add(this.second);
        double r2 = rr.first / rhs;
        DoubleDouble m2 = mul(r2, rhs);
        DoubleDouble k = m2.neg().add(rr.first);
        k = k.add(rr.second);
        double r3 = k.first / rhs;
        return add(r1, r2).add(r3);
    }

    /**
     * 计算一个DoubleDouble和一个DoubleDouble的商，返回DoubleDouble
     */
    public DoubleDouble div(DoubleDouble rhs) {
        if (rhs.second == 0 || Double.isNaN(rhs.first)
                || Double.isInfinite(rhs.first)
                || this.first == 0 || Double.isNaN(this.first)
                || Double.isInfinite(this.first)) {
            return this.div(rhs.first);
        }
        if (ONE.equals(this)) {
            return reciprocal(rhs);
        }

        // reciprocal0和reciprocal1共有1.0 / first的大约53 * 3 = 159位二进制数
        double reciprocal0 = 1.0 / rhs.first;
        DoubleDouble reciprocal1 = DoubleDouble.ONE.sub(mul(reciprocal0, rhs.first)).div(rhs.first);

        // 上面计算的倒数只考虑了first，用泰勒级数修正second带来的的影响
        DoubleDouble eps = DoubleDouble.valueOf(rhs.second).div(rhs.first);
        DoubleDouble epsDivFirst = eps.div(rhs.first);
        // 这一项已经很小，不需要很高精度
        double eps2 = eps.first * eps.first;

        double[] v = {reciprocal0,
                -epsDivFirst.first,
                reciprocal1.first,
                -epsDivFirst.second,
                eps2 * reciprocal0,
                reciprocal1.second};
        sortByAbsMaxFirst(v);
        double s1 = 0;
        double s2 = 0;
        double s3 = 0;
        for (double x : v) {
            DoubleDouble t;
            s3 += x;
            t = add(s2, s3);
            s3 = t.second;
            s2 = t.first;
            t = add(s1, s2);
            s2 = t.second;
            s1 = t.first;
        }

        double t1 = this.first;
        double t2 = this.second;

        DoubleDouble[] parts = {
                mul(s1, t1),
                mul(s2, t1),
                mul(s3, t1),
                mul(s1, t2),
                mul(s2, t2),
                mul(s3, t2),
        };
        v = new double[parts.length * 2];
        for (int i = 0; i < parts.length; i++) {
            DoubleDouble dd = parts[i];
            v[i] = dd.first;
            v[i + parts.length] = dd.second;
        }
        sortByAbsMaxFirst(v);
        s1 = 0;
        s2 = 0;
        s3 = 0;

        for (double x : v) {
            DoubleDouble t;
            s3 += x;
            t = add(s2, s3);
            s3 = t.second;
            s2 = t.first;
            t = add(s1, s2);
            s2 = t.second;
            s1 = t.first;
        }
        DoubleDouble res = add(s1, s2).add(s3);
        if (res.getFirst() == 0) {
            // 需要考虑±0的问题
            return DoubleDouble.valueOf(Math.copySign(0, this.first) * Math.copySign(0, rhs.first));
        }
        return res;
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

        double f = 1.0 / value;
        if (Double.isInfinite(f)) {
            return new DoubleDouble(f, 0);
        }

        double r;
        if (FMA_METHOD != null) {
            try {
                r = (double) FMA_METHOD.invokeExact(f, -value, 1.0);
            } catch (Throwable e) {
                throw new AssertionError(e);
            }
        } else {
            r = mul(f, -value).add(ONE).first;
        }
        DoubleDouble k = mul(f, r);
        double k2 = k.first * r;
        return k.add(k2).add(f);
    }

    public static DoubleDouble sqrt(double value) {
        if (value == 0) {
            // 这里要注意±0问题
            if (Double.doubleToRawLongBits(value) == 0) {
                return ZERO;
            }
            return DoubleDouble.valueOf(value);
        }
        if (value == 1) {
            return ONE;
        }
        double f0 = Math.sqrt(value);
        if (!(Double.isFinite(f0))) {
            return DoubleDouble.valueOf(f0);
        }
        DoubleDouble k = mul(f0, f0);
        double t = k.sub(value).first;
        if (t == 0) {
            return DoubleDouble.valueOf(f0);
        }

        DoubleDouble eps = DoubleDouble.valueOf(t).div(f0).mul(-0.5);
        double eps2 = t * t / (k.first * f0) * (1.0 / -8.0);
        return eps.add(eps2).add(f0);
    }

    public static DoubleDouble cbrt(DoubleDouble value) {
        if (value.second == 0) {
            return cbrt(value.first);
        }
        double f0 = Math.cbrt(value.first);
        if (!(Double.isFinite(f0))) {
            return DoubleDouble.valueOf(f0);
        }
        DoubleDouble h = mul(f0, f0).mul(-f0).add(value).div(value);
        DoubleDouble eps = h.mul(f0).div(3);
        double eps2 = f0 * h.first * h.first * (2.0 / 9.0);
        return eps.add(eps2).add(f0);
    }

    public static DoubleDouble cbrt(double value) {
        if (value == 0) {
            if (Double.doubleToRawLongBits(value) == 0) {
                return ZERO;
            }
            return DoubleDouble.valueOf(value);
        }
        if (value == 1) {
            return ONE;
        }
        double f0 = Math.cbrt(value);
        if (!(Double.isFinite(f0))) {
            return DoubleDouble.valueOf(f0);
        }
        DoubleDouble h = mul(f0, f0).mul(-f0).add(value).div(value);
        DoubleDouble eps = h.mul(f0).div(3);
        double eps2 = f0 * h.first * h.first * (2.0 / 9.0);
        return eps.add(eps2).add(f0);
    }

    private DoubleDouble(double first, double second) {
        if (Double.isNaN(first) || Double.isNaN(second)) {
            this.first = Double.NaN;
            this.second = Double.NaN;
        } else {
            if (Double.isInfinite(first)) {
                this.first = first;
                this.second = 0;
                return;
            }
            if (first == 0 && second != 0) {
                throw new AssertionError();
            }
            if (first + second != first) {
                throw new AssertionError();
            }
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
            throw new AssertionError();
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
        if (Double.isNaN(first) || Double.isInfinite(first)) {
            // this是NaN或者无穷，不需要考虑精度问题
            return this.mul((double) rhs);
        }
        return this.mul(valueOf(rhs));
    }

    public static DoubleDouble mul(long lhs, long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            if (canLongBeConvertedToDoubleExactly(lhs)) {
                return DoubleDouble.mul((double) lhs, (double) rhs);
            }
            return DoubleDouble.valueOf(lhs).mul((double) rhs);
        }
        return DoubleDouble.valueOf(rhs).mul(lhs);
    }

    public static DoubleDouble mul(long lhs, double rhs) {
        return DoubleDouble.valueOf(lhs).mul(rhs);
    }

    public static DoubleDouble mul(double lhs, long rhs) {
        return mul(rhs, lhs);
    }

    public DoubleDouble div(long rhs) {
        if (canLongBeConvertedToDoubleExactly(rhs)) {
            return this.div((double) rhs);
        }
        if (Double.isNaN(first) || Double.isInfinite(first)) {
            // this是NaN或者无穷，不需要考虑精度问题
            return this.div((double) rhs);
        }
        if (this.equals(ONE)) {
            return reciprocal(DoubleDouble.valueOf(rhs));
        }
        return this.div(valueOf(rhs));
    }

    public static DoubleDouble reciprocal(long value) {
        if (canLongBeConvertedToDoubleExactly(value)) {
            return DoubleDouble.reciprocal((double) value);
        }
        return DoubleDouble.valueOf(1).div(value);
    }

    public static DoubleDouble reciprocal(DoubleDouble value) {
        double first = value.first;
        if (first == 0 || Double.isInfinite(first)) {
            return DoubleDouble.valueOf(1.0 / first);
        }
        double second = value.second;
        if (second == 0) {
            return reciprocal(first);
        }
        if (Double.isNaN(first)) {
            return DoubleDouble.valueOf(Double.NaN);
        }

        // reciprocal0和reciprocal1共有1.0 / first的大约53 * 3 = 159位二进制数
        double reciprocal0 = 1.0 / first;
        DoubleDouble reciprocal1 = DoubleDouble.ONE.sub(mul(reciprocal0, first)).div(first);

        // 上面计算的倒数只考虑了value.first，用泰勒级数修正value.second带来的的影响
        DoubleDouble eps = DoubleDouble.valueOf(second).div(first);
        DoubleDouble epsDivFirst = eps.div(first);
        // 这一项已经很小，不需要很高精度
        double eps2 = eps.first * eps.first;

        double[] v = {reciprocal0,
                -epsDivFirst.first,
                reciprocal1.first,
                -epsDivFirst.second,
                eps2 * reciprocal0,
                reciprocal1.second};

        DoubleDouble sum = ZERO;

        for (int i = v.length - 1; i >= 0; i--) {
            double x = v[i];
            sum = sum.add(x);
        }

        return sum;
    }

    public static DoubleDouble sqrt(long value) {
        if (canLongBeConvertedToDoubleExactly(value)) {
            return sqrt((double) value);
        }
        if (value < 0) {
            return DoubleDouble.valueOf(Double.NaN);
        }
        return sqrt(DoubleDouble.valueOf(value));
    }

    public static DoubleDouble cbrt(long value) {
        if (canLongBeConvertedToDoubleExactly(value)) {
            return cbrt((double) value);
        }
        return cbrt(DoubleDouble.valueOf(value));
    }

    public static DoubleDouble sqrt(DoubleDouble value) {
        if (value.second == 0) {
            return sqrt(value.first);
        }
        double x0 = Math.sqrt(value.first);
        DoubleDouble h = mul(-x0, x0).add(value).div(value);
        DoubleDouble eps = h.mul(x0 * 0.5);
        double eps2 = 0.375 * x0 * h.first * h.first;
        return eps.add(eps2).add(x0);
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

    public static DoubleDouble log(double value) {
        if (!(value >= 0)) {
            return DoubleDouble.valueOf(Double.NaN);
        }
        if (value == 0) {
            return DoubleDouble.valueOf(Double.NEGATIVE_INFINITY);
        }
        if (value == Double.POSITIVE_INFINITY) {
            return valueOf(Double.POSITIVE_INFINITY);
        }
        int exponent = Math.getExponent(value);
        double normalized = Math.scalb(value, -exponent);
        if (exponent == Double.MIN_EXPONENT - 1) {
            // subnormal
            int exponent2 = Math.getExponent(normalized);
            exponent += exponent2;
            normalized = Math.scalb(value, -exponent);
        }

        // 1.0 <= normalized && normalized < 2
        Triple result;
        if (normalized <= 1.99 || exponent != -1) {
            result = Log.log1p(normalized - 1);
        } else {
            // normalized > 1.99 && normalized < 2
            double reciprocal = 2 / normalized;
            double val = reciprocal - 1;
            result = Log.log1p(val);

            DoubleDouble ke = DoubleDouble.mul(reciprocal, value).add(-1.0);
            double k = ke.first;
            DoubleDouble k2 = DoubleDouble.mul(k, k);
            if (ke.second != 0) {
                k2 = k2.add(2 * k * ke.second);
            }
            Triple lk = new Triple(k2);
            lk.v1 *= -0.5;
            lk.v2 *= -0.5;
            lk.dirtyAdd(k * k * k / 3);
            lk.dirtyAdd(k);

            lk.dirtyAdd(-result.v3);
            lk.dirtyAdd(-result.v2);
            lk.dirtyAdd(-result.v1);

            return new DoubleDouble(lk.v1, lk.v2);
        }
        Triple log2 = new Triple(0.6931471805599453);
        log2.v2 = 2.3190468138462996E-17;
        log2.v3 = 5.707708438416212E-34;
        log2.dirtyMul((double) exponent);
        result.dirtyAdd(log2.v3);
        result.dirtyAdd(log2.v2);
        result.dirtyAdd(log2.v1);
        return new DoubleDouble(result.v1, result.v2);
    }

    private static class Triple {
        double v1;
        double v2;
        double v3;

        public Triple() {
        }

        public Triple(double x) {
            this.v1 = x;
            if (Double.isNaN(x)) {
                this.v1 = Double.NaN;
                this.v2 = Double.NaN;
                this.v3 = Double.NaN;
            }
        }

        public Triple(DoubleDouble x) {
            this.v1 = x.first;
            this.v2 = x.second;
            if (Double.isNaN(x.first)) {
                this.v1 = Double.NaN;
                this.v2 = Double.NaN;
                this.v3 = Double.NaN;
            }
        }

        public Triple(Triple x) {
            this.v1 = x.v1;
            this.v2 = x.v2;
            this.v3 = x.v3;
        }

        public void renormalize() {
            final double ov1 = this.v1;
            final double ov2 = this.v2;
            final double ov3 = this.v3;
            if (!(Double.isFinite(ov1) && Double.isFinite(ov2) && Double.isFinite(ov3))) {
                if (Double.isNaN(ov1) || Double.isNaN(ov2) || Double.isNaN(ov3)) {
                    this.v1 = Double.NaN;
                    this.v2 = Double.NaN;
                    this.v3 = Double.NaN;
                    return;
                }
                if (Double.isInfinite(ov1) || Double.isInfinite(ov2) || Double.isInfinite(ov3)) {
                    double res = ov1 + ov2 + ov3;
                    if (Double.isNaN(res)) {
                        this.v1 = Double.NaN;
                        this.v2 = Double.NaN;
                        this.v3 = Double.NaN;
                        return;
                    }
                    this.v1 = res;
                    this.v2 = 0;
                    this.v3 = 0;
                    return;
                }
                throw new AssertionError();
            }
            if (ov2 == 0 && ov3 == 0) {
                if (Double.doubleToRawLongBits(ov2) == 0
                        && Double.doubleToRawLongBits(ov3) == 0) {
                    return;
                }
                this.v2 = 0;
                this.v3 = 0;
                return;
            }
            if (Math.abs(ov1) * 0.25 <= Math.abs(ov2)
                    || Math.abs(ov2) * 0.25 <= Math.abs(ov3)
            ) {
                // 大小关系很奇怪
                BigDecimal bd1 = new BigDecimal(ov1);
                BigDecimal bd2 = new BigDecimal(ov2);
                BigDecimal bd3 = new BigDecimal(ov3);
                BigDecimal res = bd1.add(bd2).add(bd3);
                double lv1 = res.doubleValue();
                BigDecimal r = res.subtract(new BigDecimal(lv1));
                double lv2 = r.doubleValue();
                double lv3 = r.subtract(new BigDecimal(lv2)).doubleValue();
                if (Double.isInfinite(lv1)) {
                    this.v1 = lv1;
                    this.v2 = 0;
                    this.v3 = 0;
                    return;
                }
                this.v1 = lv1;
                this.v2 = lv2;
                this.v3 = lv3;
                return;
            }

            DoubleDouble t = DoubleDouble.add(this.v2, this.v3);
            DoubleDouble t2 = DoubleDouble.add(this.v1, t.getFirst());
            DoubleDouble t3 = DoubleDouble.add(t2.getSecond(), t.getSecond());
            this.v1 = t2.getFirst();
            this.v2 = t3.getFirst();
            this.v3 = t3.getSecond();
        }

        private static double fma(double a, double b, double c) {
            if (FMA_METHOD != null) {
                try {
                    return (double) FMA_METHOD.invokeExact(a, b, c);
                } catch (Throwable e) {
                    throw new AssertionError(e);
                }
            }
            return DoubleDouble.mul(a, b).add(c).getFirst();
        }

        // 用Triple来求和
        // 注意会进行排序
        public static Triple sum(double[] v) {
            Triple res = new Triple();
            OUT:
            if (v.length != 0) {
                DoubleDouble.sortByAbsMaxFirst(v);
                if (Double.isNaN(v[v.length - 1])) {
                    res.v1 = Double.NaN;
                    res.v2 = Double.NaN;
                    res.v3 = Double.NaN;
                    break OUT;
                }
                double sum1 = 0;
                double sum2 = 0;
                double sum3 = 0;
                for (double x : v) {
                    if (x == 0) {
                        // 因为前面按照绝对值排序了，所以后面都是0.0（或者NaN）
                        break;
                    }
                    DoubleDouble t;
                    sum3 += x;
                    t = DoubleDouble.add(sum2, sum3);
                    sum2 = t.getFirst();
                    sum3 = t.getSecond();
                    t = DoubleDouble.add(sum1, sum2);
                    sum1 = t.getFirst();
                    sum2 = t.getSecond();
                }

                res.v1 = sum1;
                res.v2 = sum2;
                res.v3 = sum3;
            }
            return res;
        }

        public static Triple mul(double x1, double x2, double x3) {
            DoubleDouble t1 = DoubleDouble.mul(x1, x2);
            DoubleDouble t2 = DoubleDouble.mul(t1.getFirst(), x3);
            DoubleDouble t3 = DoubleDouble.mul(t1.getSecond(), x3);
            double[] v = { t2.getFirst(),
                    t3.getFirst(),
                    t2.getSecond(),
                    t3.getSecond()
            };
            return sum(v);
        }

        public void dirtyMul(double m) {
            DoubleDouble d1 = DoubleDouble.mul(this.v1, m);
            DoubleDouble d2 = DoubleDouble.mul(this.v2, m);
            DoubleDouble d3 = DoubleDouble.mul(this.v3, m);
            double[] v = {
                    d1.getFirst(),
                    d2.getFirst(),
                    d1.getSecond(),
                    d2.getSecond(),
                    d3.getFirst(),
                    d3.getSecond()
            };
            Triple sum = sum(v);
            this.v1 = sum.v1;
            this.v2 = sum.v2;
            this.v3 = sum.v3;
        }

        public void dirtyAdd(double x) {
            if (this.v1 == 0) {
                // assert this.v2 == 0 && this.v3 == 0
                this.v1 = x;
                this.v2 = 0;
                this.v3 = 0;
                return;
            }
            if (this.v2 == 0) {
                // assert this.v3 == 0
                DoubleDouble t = DoubleDouble.add(this.v1, x);
                this.v1 = t.getFirst();
                this.v2 = t.getSecond();
                this.v3 = 0;
                return;
            }
            if (this.v3 == 0) {
                DoubleDouble t = DoubleDouble.add(x, this.v1);
                if (t.getFirst() == 0) {
                    this.v1 = this.v2;
                    this.v2 = 0;
                    this.v3 = 0;
                    return;
                }
                DoubleDouble t2 = DoubleDouble.add(t.getSecond(), this.v2);
                this.v1 = t.getFirst();
                this.v2 = t2.getFirst();
                this.v3 = t2.getSecond();
                renormalize();
                return;
            }
            DoubleDouble t1 = DoubleDouble.add(x, this.v1);
            if (t1.getSecond() == 0 && Math.abs(t1.getFirst()) >= Math.abs(this.v1)) {
                this.v1 = t1.getFirst();
                return;
            }
            if (t1.getFirst() == 0) {
                this.v1 = this.v2;
                this.v2 = this.v3;
                this.v3 = 0;
                return;
            }
            DoubleDouble t2 = DoubleDouble.add(this.v2, t1.getSecond());
            double t3 = t2.getSecond() + this.v3;
            DoubleDouble t4 = DoubleDouble.add(t2.getFirst(), t3);

            DoubleDouble r1 = DoubleDouble.add(t1.getFirst(), t4.getFirst());
            DoubleDouble r2 = DoubleDouble.add(r1.getSecond(), t4.getSecond());

            this.v1 = r1.getFirst();
            this.v2 = r2.getFirst();
            this.v3 = r2.getSecond();
        }

        public void dirtyDiv(double d) {
            double r1 = this.v1 / d;
            double k = fma(-d, r1, this.v1);
            DoubleDouble t = DoubleDouble.add(this.v2, k);
            double k2 = t.getSecond() + this.v3;
            DoubleDouble t2 = DoubleDouble.add(t.getFirst(), k2);

            double r2 = t2.getFirst() / d;
            k = fma(-d, r2, t2.getFirst());

            double r3 = (k + t2.getSecond()) / d;

            this.v1 = r1;
            this.v2 = r2;
            this.v3 = r3;
        }

        // 计算1/(1+x)-1
        public static Triple reciprocal1p(double x) {
            double r1 = Math.expm1(-Math.log1p(x));
            DoubleDouble r1p1 = DoubleDouble.add(1.0, r1);
            DoubleDouble xp1 = DoubleDouble.add(1.0, x);
            Triple h = new Triple(r1p1);
            h.dirtyMul(new Triple(xp1));
            h.dirtyAdd(-1);
            h.v1 *= -1;
            h.v2 *= -1;
            h.v3 *= -1;

            double h2 = h.v1 * h.v1;

            h.dirtyMul(new Triple(r1p1));

            h.dirtyAdd(h2 * (r1 + 1));
            h.dirtyAdd(r1);

            return h;
        }

        public void dirtyMul(Triple rhs) {
            // 注意最后再修改，防止参数被意外修改
            DoubleDouble d1 = DoubleDouble.mul(this.v1, rhs.v1);
            DoubleDouble d2 = DoubleDouble.mul(this.v1, rhs.v2);
            DoubleDouble d3 = DoubleDouble.mul(this.v1, rhs.v3);
            DoubleDouble d4 = DoubleDouble.mul(this.v2, rhs.v1);
            DoubleDouble d5 = DoubleDouble.mul(this.v2, rhs.v2);
            DoubleDouble d6 = DoubleDouble.mul(this.v2, rhs.v3);
            DoubleDouble d7 = DoubleDouble.mul(this.v3, rhs.v1);
            DoubleDouble d8 = DoubleDouble.mul(this.v3, rhs.v2);
            DoubleDouble d9 = DoubleDouble.mul(this.v3, rhs.v3);
            double[] v = {
                    d1.getFirst(),
                    d2.getFirst(),
                    d4.getFirst(),
                    d1.getSecond(),
                    d5.getFirst(),
                    d4.getSecond(),
                    d2.getSecond(),
                    d5.getSecond(),
                    d3.getFirst(),
                    d3.getSecond(),
                    d6.getFirst(),
                    d7.getFirst(),
                    d6.getSecond(),
                    d7.getSecond(),
                    d8.getFirst(),
                    d8.getSecond(),
                    d9.getFirst(),
                    d9.getSecond()
            };
            Triple sum = sum(v);
            this.v1 = sum.v1;
            this.v2 = sum.v2;
            this.v3 = sum.v3;
        }

        public void sqrt() {
            double v1 = this.v1;
            double v2 = this.v2;
            double v3 = this.v3;

            double r1 = Math.sqrt(v1);
            DoubleDouble k = DoubleDouble.add(v2, v3).add(fma(r1, -r1, v1));
            DoubleDouble r = k.mul(0.5).div(r1);
            double k1 = k.getFirst();
            double r2 = (k1 * k1) / (r1 * r1 * r1) * (-0.125);

            DoubleDouble s1 = DoubleDouble.add(r1, r.first);
            double c = r.second + r2;
            this.v1 = s1.first;
            this.v2 = s1.second;
            this.v3 = 0;
            dirtyAdd(c);
        }

        public BigDecimal toBigDecimal() {
            return new BigDecimal(v1)
                    .add(new BigDecimal(v2))
                    .add(new BigDecimal(v3))
                    .stripTrailingZeros();
        }

        @Override
        public String toString() {
            if (Double.isNaN(this.v1) || Double.isNaN(this.v2) || Double.isNaN(this.v3)) {
                return Double.toString(Double.NaN);
            }
            if (Double.isInfinite(this.v1) || Double.isInfinite(this.v2) || Double.isInfinite(this.v3)) {
                return Double.toString(this.v1 + this.v2 + this.v3);
            }
            BigDecimal bd = toBigDecimal();
            return bd.round(new MathContext(51))
                    .stripTrailingZeros()
                    .toString();
        }
    }

    private static class Log {
        private static final double[] coefficients = {
                20564.141339293772,
                -0.40742573671663634,
                0.2857174507900288,
                -1.20149472161068E-11,
                0.4,
                -1.5906358982810946E-23,
        };

        public static Triple log1p(double value) {
            if (!(0 <= value && value <= 1)) {
                throw new AssertionError();
            }
            Triple vt = new Triple(value);
            vt.dirtyAdd(1);
            for (int i = 0; i < 16; i++) {
                vt.sqrt();
            }
            Triple vtc = new Triple(vt);
            vtc.dirtyAdd(-1);
            double value1 = vtc.v1;

            double x = value1 / (2 + value1);

            Triple tInv = new Triple(DoubleDouble.add(1, -x));
            Triple reciprocalX = Triple.reciprocal1p(x);
            reciprocalX.dirtyAdd(1);
            tInv.dirtyMul(reciprocalX);

            Triple vtMulInv = new Triple(vt);
            vtMulInv.dirtyMul(tInv);

            Triple s2 = Triple.mul(x, x, x);
            s2.dirtyDiv(3);

            DoubleDouble t1 = DoubleDouble.mul(x, coefficients[0]);
            Triple k = new Triple(t1);
            k.dirtyAdd(coefficients[1]);

            for (int i = 2, coefficientsLength = coefficients.length; i < coefficientsLength; i++) {
                double c = coefficients[i];
                k.dirtyMul(x);
                k.dirtyAdd(c);
            }
            k.dirtyMul(x);
            k.dirtyMul(x);
            k.dirtyMul(x);
            k.dirtyMul(x);

            k.dirtyAdd(x * 2);
            k.dirtyAdd(s2.v3 * 2);
            k.dirtyAdd(s2.v2 * 2);
            k.dirtyAdd(s2.v1 * 2);


            // 修正value与(1+x)/(1-x)中的细微差别
            vtMulInv.dirtyAdd(-1);
            double vs1 = vtMulInv.v1;
            double vs2 = vtMulInv.v2;

            k.dirtyAdd(vs1);
            k.dirtyAdd(vs2 + vs1 * vs1 * (-0.5));

            k.v1 *= 65536;
            k.v2 *= 65536;
            k.v3 *= 65536;

            return k;
        }
    }
}
