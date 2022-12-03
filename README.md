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

