# jdd
DoubleDouble in Java

Sometimes you just want be a little bit more precise than float or double.

```
public static void main(String[] args) {
    DoubleDouble s = DoubleDouble.valueOf(3);
    System.out.println(s);
    DoubleDouble d = DoubleDouble.valueOf(1);
    d = d.div(7);
    System.out.println(d);
    s = s.add(d);
    System.out.println(s);
}

# 3
# 0.1428571428571428571428571428571424
# 3.142857142857142857142857142857139

```

# Explain
The DoubleDouble class represents a number with a pair of doubles.

For example, the number `1.0000000000000000000542101086242752217003726400434970855712890625` can be represented by `1.0 + 5.421010862427522E-20`.

Since each double has about 53 binary digits, you get about 53 * 2 == 106 digits (about 32 decimal digits) of accuracy with double-doubles (often you get more, because the second double has a sign bit and an exponent). You don't get larger exponents though, so when your normal double numbers overflows or underflows, you face similar problems with double-doubles.


# Some differences from normal float numbers

Double-doubles are made up of two-numbers, each with an exponent. There are some consquences.

The single most annoying one may be this: its hard to write a toString() method for DoubleDouble. For example, How can you represent `1.0 + 1.1830521861667747E-271` in a reasonable format? You either get weired format, or you get really long strings. Or you just say `1`, and ignore the insane small difference -- this does mean you lose the ability to restore the original double-double.

Double-doubles sometimes give you more precision than you expect (just like the insane example above), and this may surprise you.

You may think that just like that `1.0 / 3` multiplied by `3` restores to `1.0`, shouldn't `DoubleDouble.valueOf(1).div(3).mul(3)` gives `1`? Unfortunately, no. It gives `1 - 3.0814879110195774E-33`. That's because `DoubleDouble.valueOf(1).div(3)` really is `0.333333333333333333333333333333332306170696326807545036811763954705430113012454285126295872032642364501953125`, and `0.999999999999999999999999999999996918512088980422635110435291864116290339037362855378887616097927093505859375` multiplied by `3`, or `1 -3.081487911019577364889564708135883709660962637144621112383902072906494140625E-33`. The `mul` by double-double is too precise, gives an exact answer, so error introduced by `div` is not canceled.

By the way, even double doesn't restore to `1.0` always. For example, `1.0 / 49 * 49` gives `0.9999999999999999`.



# Why DoubleDouble is useful
DoubleDouble is useful when excess precision is useful. Even if all your data are of `double` format, it's still useful to have a more precise "working precision" -- this is what the IEEE "double-extended" format is for (it has a precision of 64 binary digits), and it's sometimes called `long double`. Unfortunately Java doesn't provide this format.

With DoubleDouble you get more precision than "long double" -- instead of 64 bits, you get 106 or even more.

For an example, consider this program:
```
    public static void main(String[] args) {
        double sum = 0;
        DoubleDouble sum2 = DoubleDouble.valueOf(0);
        BigDecimal sum3 = BigDecimal.ZERO;
        for (int i = 1; i < 1_000_000; i++) {
            double v = 1.0 / i;
            sum += v;
            sum2 = sum2.add(v);
            sum3 = sum3.add(new BigDecimal(v));
        }
        System.out.println(sum);
        System.out.println(sum2);
        System.out.println(sum3);
    }
    
# 14.39272572286499
# 14.39272572286572357721844463704971
# 14.392725722865723577218444637049708789700019906376837752759456634521484375
```

Look at the results, when summing 1_000_000 numbers, you lose 3 decimal digits if you don't care and use a plain `double`. In this example, all data have about the same order of magnitude. You will lose more if they are of different order of magnitude, interleaved with positive and negative numbers.

An insane example, `1E30 + 1.0 + (-1E30)` gives you `0.0`. And with DoubleDouble, you get the correct answer `1`.
