package ntysdd;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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

    private DoubleDouble(double first, double second) {
        if (Double.isNaN(first) || Double.isNaN(second)) {
            this.first = Double.NaN;
            this.second = Double.NaN;
        } else {
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
}
