/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.jexl3.junit.Asserter;
import org.apache.commons.lang3.SystemProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArithmeticTest extends JexlTestCase {
    public static class Arithmetic132 extends JexlArithmetic {
        public Arithmetic132() {
            super(false);
        }

        @Override
        public Object divide(final Object left, final Object right) {
            if (left == null && right == null) {
                return controlNullNullOperands(JexlOperator.DIVIDE);
            }
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                final BigDecimal l = toBigDecimal(left);
                final BigDecimal r = toBigDecimal(right);
                if (BigDecimal.ZERO.equals(r)) {
                    return divideZero(l);
                }
                final BigDecimal result = l.divide(r, getMathContext());
                return narrowBigDecimal(left, right, result);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                final double l = toDouble(left);
                final double r = toDouble(right);
                return Double.valueOf(l / r);
            }
            // otherwise treat as integers
            final BigInteger l = toBigInteger(left);
            final BigInteger r = toBigInteger(right);
            if (BigInteger.ZERO.equals(r)) {
                return divideZero(l);
            }
            final BigInteger result = l.divide(r);
            return narrowBigInteger(left, right, result);
        }

        protected double divideZero(final BigDecimal x) {
            final int ls = x.signum();
            if (ls < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            if (ls > 0) {
                return Double.POSITIVE_INFINITY;
            }
            return Double.NaN;
        }

        protected double divideZero(final BigInteger x) {
            final int ls = x.signum();
            if (ls < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            if (ls > 0) {
                return Double.POSITIVE_INFINITY;
            }
            return Double.NaN;
        }

        @Override
        public Object mod(final Object left, final Object right) {
            if (left == null && right == null) {
                return controlNullNullOperands(JexlOperator.MOD);
            }
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                final BigDecimal l = toBigDecimal(left);
                final BigDecimal r = toBigDecimal(right);
                if (BigDecimal.ZERO.equals(r)) {
                    return divideZero(l);
                }
                final BigDecimal remainder = l.remainder(r, getMathContext());
                return narrowBigDecimal(left, right, remainder);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                final double l = toDouble(left);
                final double r = toDouble(right);
                return Double.valueOf(l % r);
            }
            // otherwise treat as integers
            final BigInteger l = toBigInteger(left);
            final BigInteger r = toBigInteger(right);
            final BigInteger result = l.mod(r);
            if (BigInteger.ZERO.equals(r)) {
                return divideZero(l);
            }
            return narrowBigInteger(left, right, result);
        }
    }
    // an arithmetic that knows how to deal with vars
    public static class ArithmeticPlus extends JexlArithmetic {
        public ArithmeticPlus(final boolean strict) {
            super(strict);
        }

        public Var add(final Var lhs, final Var rhs) {
            return new Var(lhs.value + rhs.value);
        }

        public Var and(final Var lhs, final Var rhs) {
            return new Var(lhs.value & rhs.value);
        }

        public Var complement(final Var arg) {
            return new Var(~arg.value);
        }

        public Boolean contains(final Var lhs, final Var rhs) {
            return lhs.toString().contains(rhs.toString());
        }

        public Var divide(final Var lhs, final Var rhs) {
            return new Var(lhs.value / rhs.value);
        }

        public Boolean endsWith(final Var lhs, final Var rhs) {
            return lhs.toString().endsWith(rhs.toString());
        }

        public boolean equals(final Var lhs, final Var rhs) {
            return lhs.value == rhs.value;
        }

        public boolean greaterThan(final Var lhs, final Var rhs) {
            return lhs.value > rhs.value;
        }

        public boolean greaterThanOrEqual(final Var lhs, final Var rhs) {
            return lhs.value >= rhs.value;
        }

        public boolean lessThan(final Var lhs, final Var rhs) {
            return lhs.value < rhs.value;
        }

        public boolean lessThanOrEqual(final Var lhs, final Var rhs) {
            return lhs.value <= rhs.value;
        }

        public Var mod(final Var lhs, final Var rhs) {
            return new Var(lhs.value / rhs.value);
        }

        public Var multiply(final Var lhs, final Var rhs) {
            return new Var(lhs.value * rhs.value);
        }

        public Object negate(final String str) {
            final int length = str.length();
            final StringBuilder strb = new StringBuilder(str.length());
            for (int c = length - 1; c >= 0; --c) {
                strb.append(str.charAt(c));
            }
            return strb.toString();
        }

        public Var negate(final Var arg) {
            return new Var(-arg.value);
        }

        public Object not(final Var x) {
            throw new NullPointerException("make it fail");
        }

        public Var or(final Var lhs, final Var rhs) {
            return new Var(lhs.value | rhs.value);
        }

        public Var shiftLeft(final Var lhs, final Var rhs) {
            return new Var(lhs.value << rhs.value);
        }

        public Var shiftRight(final Var lhs, final Var rhs) {
            return new Var(lhs.value >> rhs.value);
        }

        public Var shiftRightUnsigned(final Var lhs, final Var rhs) {
            return new Var(lhs.value >>> rhs.value);
        }

        public Boolean startsWith(final Var lhs, final Var rhs) {
            return lhs.toString().startsWith(rhs.toString());
        }

        public Object subtract(final String x, final String y) {
            final int ix = x.indexOf(y);
            if (ix < 0) {
                return x;
            }
            final StringBuilder strb = new StringBuilder(x.substring(0, ix));
            strb.append(x.substring(ix + y.length()));
            return strb.toString();
        }

        public Var subtract(final Var lhs, final Var rhs) {
            return new Var(lhs.value - rhs.value);
        }

        public Var xor(final Var lhs, final Var rhs) {
            return new Var(lhs.value ^ rhs.value);
        }
    }

    // an arithmetic that fail systematically with vars
    public static class ArithmeticFail extends JexlArithmetic {
        public ArithmeticFail(final boolean strict) {
            super(strict);
        }

        @Override
        public Object add(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object and(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object complement(final Object arg) {
            throw new ArithmeticException(Objects.toString(arg));
        }

        @Override
        public Object decrement(final Object arg) {
            throw new ArithmeticException(Objects.toString(arg));
        }

        @Override
        public Object increment(final Object arg) {
            throw new ArithmeticException(Objects.toString(arg));
        }

        @Override
        public Boolean contains(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object divide(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Boolean endsWith(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean equals(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean strictEquals(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean greaterThan(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean greaterThanOrEqual(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean lessThan(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean lessThanOrEqual(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object mod(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object multiply(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object negate(final Object arg) {
            throw new ArithmeticException(Objects.toString(arg));
        }

        @Override
        public Object not(final Object arg) {
            throw new ArithmeticException(Objects.toString(arg));
        }

        @Override
        public Object or(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object shiftLeft(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object shiftRight(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object shiftRightUnsigned(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Boolean startsWith(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object subtract(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public Object xor(final Object lhs, final Object rhs) {
            throw new ArithmeticException(lhs + " o " + rhs);
        }

        @Override
        public boolean toBoolean(Object x) {
            throw new ArithmeticException(Objects.toString(x));
        }
    }

    @Test
    public void testFailAllOperators() {
        String[] scripts = new String[]{
            "(x, y)->{ x < y }",
            "(x, y)->{ x <= y }",
            "(x, y)->{ x > y }",
            "(x, y)->{ x >= y }",
            "(x, y)->{ x == y }",
            "(x, y)->{ x != y }",
            "(x, y)->{ x === y }",
            "(x, y)->{ x !== y }",
            "(x, y)->{ x % y }",
            "(x, y)->{ x * y }",
            "(x, y)->{ x + y }",
            "(x, y)->{ x - y }",
            "(x, y)->{ x ^ y }",
            "(x, y)->bitwiseXor(x,y)",
            "(x, y)->{ x || y }",
            "(x, y)->{ x | y }",
            "(x, y)->bitwiseOr(x,y)",
            "(x, y)->{ x << y }",
            "(x, y)->{ x >> y }",
            "(x, y)->{ x >>> y }",
            "(x, y)->{ x & y }",
            "(x, y)->{ x && y }",
            "(x, y)->bitwiseAnd(x,y)",
            "(x, y)->{ x =^ y }",
            "(x, y)->{ x !^ y }",
            "(x, y)->{ x =$ y }",
            "(x, y)->{ x !$ y }",
            "(x, y)->{ x =~ y }",
            "(x, y)->{ x !~ y }",
            "(x, ignore)->{ -x }",
            "(x, ignore)->{ +x }",
            "(x, ignore)->{ --x }",
            "(x, ignore)->{ ++x }",
            "(x, ignore)->{ x-- }",
            "(x, ignore)->{ x++ }"
        };
        final JexlEngine jexl = new JexlBuilder().cache(64).arithmetic(new ArithmeticFail(true)).create();
        final JexlContext jc = new EmptyTestContext();
        for(String src : scripts) {
            JexlScript script = jexl.createScript(src);
            try {
                Object result = script.execute(jc, new Var(42), new Var(43));
                Assert.fail(src);
            } catch(JexlException xjexl) {
                Assert.assertNotNull(jexl);
            }
        }
    }

    public static class Callable173 {
        public Object call(final Integer... arg) {
            return arg[0] * arg[1];
        }
        public Object call(final String... arg) {
            return 42;
        }
    }
    public static class EmptyTestContext extends MapContext implements JexlContext.NamespaceResolver {
        public static int log(final Object fmt, final int... arr) {
            //System.out.println(String.format(fmt.toString(), arr));
            return arr == null ? 0 : arr.length;
        }

        public static int log(final Object fmt, final Object... arr) {
            //System.out.println(String.format(fmt.toString(), arr));
            return arr == null ? 0 : arr.length;
        }

        @Override
        public Object resolveNamespace(final String name) {
            return this;
        }
    }
    public static class Var {
        int value;

        Var(final int v) {
            value = v;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    public static class XmlArithmetic extends JexlArithmetic {
        public XmlArithmetic(final boolean astrict) {
            super(astrict);
        }

        public XmlArithmetic(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
            super(astrict, bigdContext, bigdScale);
        }

        public boolean empty(final org.w3c.dom.Element elt) {
            return !elt.hasAttributes() && !elt.hasChildNodes();
        }

        public int size(final org.w3c.dom.Element elt) {
            return elt.getChildNodes().getLength();
        }
    }

    /** A small delta to compare doubles. */
    private static final double EPSILON = 1.e-6;

    static private void assertArithmeticException(final java.util.function.Supplier<Object> fun) {
        try {
            Assert.assertNull(fun.get());
        } catch (final ArithmeticException xany) {
            Assert.assertNotNull(xany);
        }
    }

    static private void assertNullOperand(final java.util.function.Supplier<Object> fun) {
        try {
            Assert.assertNull(fun.get());
        } catch (final JexlArithmetic.NullOperand xany) {
            Assert.assertNotNull(xany);
        }
    }

    private static Document getDocument(final String xml) throws Exception {
        final DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final InputStream stringInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return xmlBuilder.parse(stringInputStream);
    }

    /**
     * Returns the Java version as an int value.
     * @return the Java version as an int value (8, 9, etc.)
     */
    private static int getJavaVersion() {
        String version = SystemProperties.getJavaVersion();
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int sep = version.indexOf(".");
        if (sep < 0) {
            sep = version.indexOf("-");
        }
        if (sep > 0) {
            version = version.substring(0, sep);
        }
        return Integer.parseInt(version);
    }

    private final Asserter asserter;

    private final JexlArithmetic jexla;

    private final JexlArithmetic jexlb;

    private final Date date = new Date();

    public ArithmeticTest() {
        super("ArithmeticTest");
        asserter = new Asserter(JEXL);
        jexla = JEXL.getArithmetic();
        final JexlOptions options = new JexlOptions();
        options.setStrictArithmetic(false);
        jexlb = jexla.options(options);
    }

    void checkEmpty(final Object x, final boolean expect) {
        final JexlScript s0 = JEXL.createScript("empty(x)", "x");
        boolean empty = (Boolean) s0.execute(null, x);
        Assert.assertEquals(expect, empty);
        final JexlScript s1 = JEXL.createScript("empty x", "x");
        empty = (Boolean) s1.execute(null, x);
        Assert.assertEquals(expect, empty);
        final JexlScript s2 = JEXL.createScript("x.empty()", "x");
        empty = (Boolean) s2.execute(null, x);
        Assert.assertEquals(expect, empty);
    }

    protected void runOverload(final JexlEngine jexl, final JexlContext jc) {
        JexlScript script;
        Object result;

        script = jexl.createScript("(x, y)->{ x < y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(43), new Var(42));
        Assert.assertEquals(false, result);

        script = jexl.createScript("(x, y)->{ x <= y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(41), new Var(44));
        Assert.assertEquals(true, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(false, result);

        script = jexl.createScript("(x, y)->{ x > y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(43), new Var(42));
        Assert.assertEquals(true, result);

        script = jexl.createScript("(x, y)->{ x >= y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(41), new Var(44));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(true, result);

        script = jexl.createScript("(x, y)->{ x == y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(41), new Var(44));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(false, result);

        script = jexl.createScript("(x, y)->{ x != y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(44), new Var(44));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 44, 44);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(true, result);

        script = jexl.createScript("(x, y)->{ x % y }");
        result = script.execute(jc, 4242, 100);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(4242), new Var(100));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x * y }");
        result = script.execute(jc, 6, 7);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(6), new Var(7));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x + y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x - y }");
        result = script.execute(jc, 49, 7);
        Assert.assertEquals(42, result);
        result = script.execute(jc, "foobarquux", "bar");
        Assert.assertEquals("fooquux", result);
        result = script.execute(jc, 50, 8);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(50), new Var(8));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x)->{ -x }");
        result = script.execute(jc, -42);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(-42));
        Assert.assertEquals(42, ((Var) result).value);
        result = script.execute(jc, "pizza");
        Assert.assertEquals("azzip", result);
        result = script.execute(jc, -142);
        Assert.assertEquals(142, result);

        script = jexl.createScript("(x)->{ ~x }");
        result = script.execute(jc, -1);
        Assert.assertEquals(0L, result);
        result = script.execute(jc, new Var(-1));
        Assert.assertEquals(0L, ((Var) result).value);
        result = script.execute(jc, new Var(-42));
        Assert.assertEquals(41, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x ^ y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(36L, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(36L, ((Var) result).value);
        // legacy
        script = jexl.createScript("(x, y)->bitwiseXor(x,y)");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(36L, result);

        script = jexl.createScript("(x, y)->{ x | y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(39L, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(39L, ((Var) result).value);
        // legacy
        script = jexl.createScript("(x, y)->bitwiseOr(x,y)");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(39L, result);

        script = jexl.createScript("(x, y)->{ x << y }");
        result = script.execute(jc, 35, 1);
        Assert.assertEquals(70L, result);
        result = script.execute(jc, new Var(35), new Var(1));
        Assert.assertEquals(70L, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x >> y }");
        result = script.execute(jc, 42, 1);
        Assert.assertEquals(21L, result);
        result = script.execute(jc, new Var(42), new Var(1));
        Assert.assertEquals(21, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x >>> y }");
        result = script.execute(jc, 84, 2);
        Assert.assertEquals(21L, result);
        result = script.execute(jc, new Var(84), new Var(2));
        Assert.assertEquals(21, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x & y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(3L, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(3L, ((Var) result).value);
        // legacy
        script = jexl.createScript("(x, y)->bitwiseAnd(x,y)");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(3L, result);

        script = jexl.createScript("(x, y)->{ x =^ y }");
        result = script.execute(jc, 3115, 31);
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(31));
        Assert.assertTrue((Boolean) result);

        script = jexl.createScript("(x, y)->{ x !^ y }");
        result = script.execute(jc, 3115, 31);
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(31));
        Assert.assertFalse((Boolean) result);

        script = jexl.createScript("(x, y)->{ x =$ y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(15));
        Assert.assertTrue((Boolean) result);

        script = jexl.createScript("(x, y)->{ x !$ y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(15));
        Assert.assertFalse((Boolean) result);

        script = jexl.createScript("(x, y)->{ x =~ y }");
        result = script.execute(jc, 3155, 15);
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(3155), new Var(15));
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(15), new Var(3155));
        Assert.assertTrue((Boolean) result);

        script = jexl.createScript("(x, y)->{ x !~ y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(3155), new Var(15));
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(15), new Var(3155));
        Assert.assertFalse((Boolean) result);

        script = jexl.createScript("(x)->{ !x }");
        try {
            result = script.execute(jc, new Var(-42));
            Assert.fail("should fail");
        } catch (final JexlException xany) {
            Assert.assertTrue(xany instanceof JexlException.Operator);
        }
    }

    @Before
    @Override
    public void setUp() {
    }

    // JEXL-24: doubles with exponent
    @Test
    public void test2DoubleLiterals() {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 42.0e1D; b = 42.0E+2D; c = 42.0e-1d; d = 42.0E-2d; e=10e10; f= +1.e1; g=1e1; }";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(Double.valueOf("42.0e+1"), ctxt.get("a"));
        Assert.assertEquals(Double.valueOf("42.0e+2"), ctxt.get("b"));
        Assert.assertEquals(Double.valueOf("42.0e-1"), ctxt.get("c"));
        Assert.assertEquals(Double.valueOf("42.0e-2"), ctxt.get("d"));
        Assert.assertEquals(Double.valueOf("10e10"), ctxt.get("e"));
        Assert.assertEquals(Double.valueOf("10"), ctxt.get("f"));
        Assert.assertEquals(Double.valueOf("10"), ctxt.get("g"));
    }

    @Test
    public void testAddWithStringsLenient() {
        final JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic(false)).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("'a' + 0");
        result = script.execute(null);
        Assert.assertEquals("a0", result);

        script = jexl.createScript("0 + 'a' ");
        result = script.execute(null);
        Assert.assertEquals("0a", result);

        script = jexl.createScript("0 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals(1.2d, (Double) result, EPSILON);

        script = jexl.createScript("'1.2' + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("1.2 + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("1.2 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("'1.2' + 0 ");
        result = script.execute(null);
        Assert.assertEquals(1.2d, (Double) result, EPSILON);

        script = jexl.createScript("'1.2' + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);
    }

    @Test
    public void testAddWithStringsStrict() {
        final JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic(true)).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("'a' + 0");
        result = script.execute(null);
        Assert.assertEquals("a0", result);

        script = jexl.createScript("0 + 'a' ");
        result = script.execute(null);
        Assert.assertEquals("0a", result);

        script = jexl.createScript("0 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("01.2", result);

        script = jexl.createScript("'1.2' + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);

        script = jexl.createScript("1.2 + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("1.2 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);

        script = jexl.createScript("'1.2' + 0 ");
        result = script.execute(null);
        Assert.assertEquals("1.20", result);

        script = jexl.createScript("'1.2' + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);

    }

    @Test
    public void testArithmeticPlus() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(64).arithmetic(new ArithmeticPlus(false)).create();
        final JexlContext jc = new EmptyTestContext();
        runOverload(jexl, jc);
        runOverload(jexl, jc);
    }

    @Test
    public void testArithmeticPlusNoCache() {
        final JexlEngine jexl = new JexlBuilder().cache(0).arithmetic(new ArithmeticPlus(false)).create();
        final JexlContext jc = new EmptyTestContext();
        runOverload(jexl, jc);
    }

    @Test
    public void testAtomicBoolean() {
        // in a condition
        JexlScript e = JEXL.createScript("if (x) 1 else 2;", "x");
        final JexlContext jc = new MapContext();
        final AtomicBoolean ab = new AtomicBoolean(false);
        Object o;
        o = e.execute(jc, ab);
        Assert.assertEquals("Result is not 2", Integer.valueOf(2), o);
        ab.set(true);
        o = e.execute(jc, ab);
        Assert.assertEquals("Result is not 1", Integer.valueOf(1), o);
        // in a binary logical op
        e = JEXL.createScript("x && y", "x", "y");
        ab.set(true);
        o = e.execute(jc, ab, Boolean.FALSE);
        Assert.assertFalse((Boolean) o);
        ab.set(true);
        o = e.execute(jc, ab, Boolean.TRUE);
        Assert.assertTrue((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab, Boolean.FALSE);
        Assert.assertFalse((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab, Boolean.FALSE);
        Assert.assertFalse((Boolean) o);
        // in arithmetic op
        e = JEXL.createScript("x + y", "x", "y");
        ab.set(true);
        o = e.execute(jc, ab, 10);
        Assert.assertEquals(11, o);
        o = e.execute(jc, 10, ab);
        Assert.assertEquals(11, o);
        o = e.execute(jc, ab, 10.d);
        Assert.assertEquals(11.d, (Double) o, EPSILON);
        o = e.execute(jc, 10.d, ab);
        Assert.assertEquals(11.d, (Double) o, EPSILON);

        final BigInteger bi10 = BigInteger.TEN;
        ab.set(false);
        o = e.execute(jc, ab, bi10);
        Assert.assertEquals(bi10, o);
        o = e.execute(jc, bi10, ab);
        Assert.assertEquals(bi10, o);

        final BigDecimal bd10 = BigDecimal.TEN;
        ab.set(false);
        o = e.execute(jc, ab, bd10);
        Assert.assertEquals(bd10, o);
        o = e.execute(jc, bd10, ab);
        Assert.assertEquals(bd10, o);

        // in a (the) monadic op
        e = JEXL.createScript("!x", "x");
        ab.set(true);
        o = e.execute(jc, ab);
        Assert.assertFalse((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab);
        Assert.assertTrue((Boolean) o);

        // in a (the) monadic op
        e = JEXL.createScript("-x", "x");
        ab.set(true);
        o = e.execute(jc, ab);
        Assert.assertFalse((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab);
        Assert.assertTrue((Boolean) o);
    }

    @Test
    public void testBigDecimal() throws Exception {
        asserter.setVariable("left", new BigDecimal(2));
        asserter.setVariable("right", new BigDecimal(6));
        asserter.assertExpression("left + right", new BigDecimal(8));
        asserter.assertExpression("right - left", new BigDecimal(4));
        asserter.assertExpression("right * left", new BigDecimal(12));
        asserter.assertExpression("right / left", new BigDecimal(3));
        asserter.assertExpression("right % left", new BigDecimal(0));
    }

    @Test
    public void testBigdOp() {
        final BigDecimal sevendot475 = new BigDecimal("7.475");
        final BigDecimal SO = new BigDecimal("325");
        final JexlContext jc = new MapContext();
        jc.set("SO", SO);

        final String expr = "2.3*SO/100";

        final Object evaluate = JEXL.createExpression(expr).evaluate(jc);
        Assert.assertEquals(sevendot475, evaluate);
    }

    // JEXL-24: big decimals with exponent
    @Test
    public void testBigExponentLiterals() {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 42.0e1B; b = 42.0E+2B; c = 42.0e-1B; d = 42.0E-2b; e=4242.4242e1b}";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(new BigDecimal("42.0e+1"), ctxt.get("a"));
        Assert.assertEquals(new BigDecimal("42.0e+2"), ctxt.get("b"));
        Assert.assertEquals(new BigDecimal("42.0e-1"), ctxt.get("c"));
        Assert.assertEquals(new BigDecimal("42.0e-2"), ctxt.get("d"));
        Assert.assertEquals(new BigDecimal("4242.4242e1"), ctxt.get("e"));
    }

    @Test
    public void testBigInteger() throws Exception {
        asserter.setVariable("left", new BigInteger("2"));
        asserter.setVariable("right", new BigInteger("6"));
        asserter.assertExpression("left + right", new BigInteger("8"));
        asserter.assertExpression("right - left", new BigInteger("4"));
        asserter.assertExpression("right * left", new BigInteger("12"));
        asserter.assertExpression("right / left", new BigInteger("3"));
        asserter.assertExpression("right % left", new BigInteger("0"));
    }

    // JEXL-24: big integers and big decimals
    @Test
    public void testBigLiterals() {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 10H; b = 10h; c = 42.0B; d = 42.0b;}";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(new BigInteger("10"), ctxt.get("a"));
        Assert.assertEquals(new BigInteger("10"), ctxt.get("b"));
        Assert.assertEquals(new BigDecimal("42.0"), ctxt.get("c"));
        Assert.assertEquals(new BigDecimal("42.0"), ctxt.get("d"));
    }

    @Test
    public void testBigLiteralValue() {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final JexlExpression e = JEXL.createExpression("9223372036854775806.5B");
        final String res = String.valueOf(e.evaluate(ctxt));
        Assert.assertEquals("9223372036854775806.5", res);
    }

    /**
     * test some simple mathematical calculations
     */
    @Test
    public void testCalculations() throws Exception {
        asserter.setStrict(true, false);
        /*
         * test new null coersion
         */
        asserter.setVariable("imanull", null);
        asserter.assertExpression("imanull + 2", Integer.valueOf(2));
        asserter.assertExpression("imanull + imanull", Integer.valueOf(0));
        asserter.setVariable("foo", Integer.valueOf(2));

        asserter.assertExpression("foo + 2", Integer.valueOf(4));
        asserter.assertExpression("3 + 3", Integer.valueOf(6));
        asserter.assertExpression("3 + 3 + foo", Integer.valueOf(8));
        asserter.assertExpression("3 * 3", Integer.valueOf(9));
        asserter.assertExpression("3 * 3 + foo", Integer.valueOf(11));
        asserter.assertExpression("3 * 3 - foo", Integer.valueOf(7));

        /*
         * test parenthesized exprs
         */
        asserter.assertExpression("(4 + 3) * 6", Integer.valueOf(42));
        asserter.assertExpression("(8 - 2) * 7", Integer.valueOf(42));

        /*
         * test some floaty stuff
         */
        asserter.assertExpression("3 * \"3.0\"", Double.valueOf(9));
        asserter.assertExpression("3 * 3.0", Double.valueOf(9));

        /*
         * test / and %
         */
        asserter.setStrict(false, false);
        asserter.assertExpression("6 / 3", Integer.valueOf(6 / 3));
        asserter.assertExpression("6.4 / 3", Double.valueOf(6.4 / 3));
        asserter.assertExpression("0 / 3", Integer.valueOf(0 / 3));
        asserter.assertExpression("3 / 0", Double.valueOf(0));
        asserter.assertExpression("4 % 3", Integer.valueOf(1));
        asserter.assertExpression("4.8 % 3", Double.valueOf(4.8 % 3));

    }

    @Test
    public void testCoerceBigDecimal() {
        final JexlArithmetic ja = JEXL.getArithmetic();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H; }";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(BigDecimal.valueOf(34), ja.toBigDecimal(ctxt.get("a")));
        Assert.assertEquals(BigDecimal.valueOf(45.), ja.toBigDecimal(ctxt.get("b")));
        Assert.assertEquals(BigDecimal.valueOf(56.), ja.toBigDecimal(ctxt.get("c")));
        Assert.assertEquals(BigDecimal.valueOf(67), ja.toBigDecimal(ctxt.get("d")));
        Assert.assertEquals(BigDecimal.valueOf(78), ja.toBigDecimal(ctxt.get("e")));
        Assert.assertEquals(BigDecimal.valueOf(10), ja.toBigDecimal("10"));
        Assert.assertEquals(BigDecimal.valueOf(1.), ja.toBigDecimal(true));
        Assert.assertEquals(BigDecimal.valueOf(0.), ja.toBigDecimal(false));
        // BigDecimal precision is kept when used as argument
        BigDecimal a42 = BigDecimal.valueOf(42);
        BigDecimal a49 = BigDecimal.valueOf(49);
        JexlScript bde = JEXL.createScript("a * 6 / 7", "a");
        Assert.assertEquals(a42, bde.execute(null, a49));
        bde = JEXL.createScript("(a - 12) / 12", "a");
        MathContext mc = ja.getMathContext();
        BigDecimal b56 = BigDecimal.valueOf(56);
        BigDecimal b12 = BigDecimal.valueOf(12);
        BigDecimal b3dot666 = b56.subtract(b12, mc).divide(b12, mc);
        Assert.assertEquals(b3dot666, bde.execute(null, b56));
    }

    @Test
    public void testCoerceBigInteger() {
        final JexlArithmetic ja = JEXL.getArithmetic();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H; }";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(BigInteger.valueOf(34), ja.toBigInteger(ctxt.get("a")));
        Assert.assertEquals(BigInteger.valueOf(45), ja.toBigInteger(ctxt.get("b")));
        Assert.assertEquals(BigInteger.valueOf(56), ja.toBigInteger(ctxt.get("c")));
        Assert.assertEquals(BigInteger.valueOf(67), ja.toBigInteger(ctxt.get("d")));
        Assert.assertEquals(BigInteger.valueOf(78), ja.toBigInteger(ctxt.get("e")));
        Assert.assertEquals(BigInteger.valueOf(10), ja.toBigInteger("10"));
        Assert.assertEquals(BigInteger.valueOf(1), ja.toBigInteger(true));
        Assert.assertEquals(BigInteger.valueOf(0), ja.toBigInteger(false));
    }

    @Test
    public void testCoerceDouble() {
        final JexlArithmetic ja = JEXL.getArithmetic();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H; }";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(34, ja.toDouble(ctxt.get("a")), EPSILON);
        Assert.assertEquals(45, ja.toDouble(ctxt.get("b")), EPSILON);
        Assert.assertEquals(56, ja.toDouble(ctxt.get("c")), EPSILON);
        Assert.assertEquals(67, ja.toDouble(ctxt.get("d")), EPSILON);
        Assert.assertEquals(78, ja.toDouble(ctxt.get("e")), EPSILON);
        Assert.assertEquals(10d, ja.toDouble("10"), EPSILON);
        Assert.assertEquals(1.D, ja.toDouble(true), EPSILON);
        Assert.assertEquals(0.D, ja.toDouble(false), EPSILON);
    }

    @Test
    public void testCoerceInteger() {
        final JexlArithmetic ja = JEXL.getArithmetic();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H;";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(34, ja.toInteger(ctxt.get("a")));
        Assert.assertEquals(45, ja.toInteger(ctxt.get("b")));
        Assert.assertEquals(56, ja.toInteger(ctxt.get("c")));
        Assert.assertEquals(67, ja.toInteger(ctxt.get("d")));
        Assert.assertEquals(78, ja.toInteger(ctxt.get("e")));
        Assert.assertEquals(10, ja.toInteger("10"));
        Assert.assertEquals(1, ja.toInteger(true));
        Assert.assertEquals(0, ja.toInteger(false));
    }

    @Test
    public void testCoerceLong() {
        final JexlArithmetic ja = JEXL.getArithmetic();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H;";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(34L, ja.toLong(ctxt.get("a")));
        Assert.assertEquals(45L, ja.toLong(ctxt.get("b")));
        Assert.assertEquals(56L, ja.toLong(ctxt.get("c")));
        Assert.assertEquals(67L, ja.toLong(ctxt.get("d")));
        Assert.assertEquals(78L, ja.toLong(ctxt.get("e")));
        Assert.assertEquals(10L, ja.toLong("10"));
        Assert.assertEquals(1L, ja.toLong(true));
        Assert.assertEquals(0L, ja.toLong(false));
    }

    @Test
    public void testCoercions() throws Exception {
        asserter.assertExpression("1", Integer.valueOf(1)); // numerics default to Integer
        asserter.assertExpression("5L", Long.valueOf(5));

        asserter.setVariable("I2", Integer.valueOf(2));
        asserter.setVariable("L2", Long.valueOf(2));
        asserter.setVariable("L3", Long.valueOf(3));
        asserter.setVariable("B10", BigInteger.TEN);

        // Integer & Integer => Integer
        asserter.assertExpression("I2 + 2", Integer.valueOf(4));
        asserter.assertExpression("I2 * 2", Integer.valueOf(4));
        asserter.assertExpression("I2 - 2", Integer.valueOf(0));
        asserter.assertExpression("I2 / 2", Integer.valueOf(1));

        // Integer & Long => Long
        asserter.assertExpression("I2 * L2", Long.valueOf(4));
        asserter.assertExpression("I2 / L2", Long.valueOf(1));

        // Long & Long => Long
        asserter.assertExpression("L2 + 3", Long.valueOf(5));
        asserter.assertExpression("L2 + L3", Long.valueOf(5));
        asserter.assertExpression("L2 / L2", Long.valueOf(1));
        asserter.assertExpression("L2 / 2", Long.valueOf(1));

        // BigInteger
        asserter.assertExpression("B10 / 10", BigInteger.ONE);
        asserter.assertExpression("B10 / I2", new BigInteger("5"));
        asserter.assertExpression("B10 / L2", new BigInteger("5"));
    }

    @Test
    public void testCompare() {
        // JEXL doesn't support more than one operator in the same expression, for example: 1 == 1 == 1
        final Object[] EXPRESSIONS = {
                // Basic compare
                "1 == 1", true,
                "1 === 1", true,
                "1.d === 1", false,
                "1 != 1", false,
                "1 != 2", true,
                "1 > 2", false,
                "1 >= 2", false,
                "1 < 2", true,
                "1 <= 2", true,
                // Int <-> Float Coercion
                "1.0 == 1", true,
                "1 == 1.0", true,
                "1.1 != 1", true,
                "1.1 < 2", true,
                // Big Decimal <-> Big Integer Coercion
                "1.0b == 1h", true,
                "1.0b !== 1h", true,
                "1h == 1.0b", true,
                "1.1b != 1h", true,
                "1.1b < 2h", true,
                // Mix all type of numbers
                "1l == 1.0", true, // long and int
                "1l !== 1.0", true, // long and int
                "1.0d == 1.0f", true, // double and float
                "1l == 1.0b", true,
                "1l == 1h", true,
                "1.0d == 1.0b", true,
                "1.0f == 1.0b", true,
                "1.0d == 1h", true,
                "1.0f == 1h", true,
                // numbers and strings
                "'1' == 1", true,
                "'1' == 1l", true,
                "'1' == 1h", true,
                "'' == 0", true, // empty string is coerced to zero (ECMA compliance)
                "'1.0' == 1", true,
                "'1.0' == 1.0f", true,
                "'1.0' == 1.0d", true,
                "'1.0' == 1.0b", true,
                "'1.01' == 1.01", true,
                "'1.01' == 1", false,
                "'1.01' == 1b", false,
                "'1.01' == 1h", false,
                "'1.00001' == 1b", false,
                "'1.00001' == 1h", false,
                "'1.00000001' == 1", false,
                "'1.00000001' == 1b", false,
                "'1.00000001' == 1h", false,
                "1.0 >= '1'", true,
                "1.0 > '1'", false,
                "1.0 == 'a'", false,
                "10 == 'a'", false,
                "10 > 'a'", ArithmeticException.class,
                "10.0 > 'a'", ArithmeticException.class,
                "'a' <= 10b", ArithmeticException.class,
                "'a' >= 1h", ArithmeticException.class
        };
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new EmptyTestContext();
        JexlExpression expression;

        for (int e = 0; e < EXPRESSIONS.length; e += 2) {
            final String stext = (String) EXPRESSIONS[e];
            final Object expected = EXPRESSIONS[e + 1];
            expression = jexl.createExpression(stext);
            try {
                final Object result = expression.evaluate(jc);
                Assert.assertEquals("failed on " + stext, expected, result);
            } catch(JexlException jexlException) {
                Throwable cause = jexlException.getCause();
                if (cause == null || !cause.getClass().equals(expected)) {
                    Assert.fail(stext);
                }
            }
        }
    }

    @Test
    public void testDivClass() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new MapContext();
        final Object ra = jexl.createExpression("463.0d / 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        final Object r0 = jexl.createExpression("463.0B / 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        final Object r1 = jexl.createExpression("463.0B / 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    /**
     *
     * if silent, all arith exception return 0.0
     * if not silent, all arith exception throw
     */
    @Test
    public void testDivideByZero() throws Exception {
        final Map<String, Object> vars = new HashMap<>();
        final JexlEvalContext context = new JexlEvalContext(vars);
        final JexlOptions options = context.getEngineOptions();
        options.setStrictArithmetic(true);
        vars.put("aByte", Byte.valueOf((byte) 1));
        vars.put("aShort", Short.valueOf((short) 2));
        vars.put("aInteger", Integer.valueOf(3));
        vars.put("aLong", Long.valueOf(4));
        vars.put("aFloat", Float.valueOf((float) 5.5));
        vars.put("aDouble", Double.valueOf(6.6));
        vars.put("aBigInteger", new BigInteger("7"));
        vars.put("aBigDecimal", new BigDecimal("8.8"));

        vars.put("zByte", Byte.valueOf((byte) 0));
        vars.put("zShort", Short.valueOf((short) 0));
        vars.put("zInteger", Integer.valueOf(0));
        vars.put("zLong", Long.valueOf(0));
        vars.put("zFloat", Float.valueOf(0));
        vars.put("zDouble", Double.valueOf(0));
        vars.put("zBigInteger", new BigInteger("0"));
        vars.put("zBigDecimal", new BigDecimal("0"));

        final String[] tnames = {
            "Byte", "Short", "Integer", "Long",
            "Float", "Double",
            "BigInteger", "BigDecimal"
        };
        // number of permutations this will generate
        final int PERMS = tnames.length * tnames.length;

        final JexlEngine jexl = JEXL;
        // for non-silent, silent...
        for (int s = 0; s < 2; ++s) {
            final boolean strict = s != 0;
            options.setStrict(true);
            options.setStrictArithmetic(strict);
            int zthrow = 0;
            int zeval = 0;
            // for vars of all types...
            for (final String vname : tnames) {
                // for zeros of all types...
                for (final String zname : tnames) {
                    // divide var by zero
                    final String expr = "a" + vname + " / " + "z" + zname;
                    try {
                        final JexlExpression zexpr = jexl.createExpression(expr);
                        final Object nan = zexpr.evaluate(context);
                        // check we have a zero & increment zero count
                        if (nan instanceof Number) {
                            final double zero = ((Number) nan).doubleValue();
                            if (zero == 0.0) {
                                zeval += 1;
                            }
                        }
                    } catch (final Exception any) {
                        // increment the exception count
                        zthrow += 1;
                    }
                }
            }
            if (strict) {
                Assert.assertEquals("All expressions should have thrown " + zthrow + "/" + PERMS, zthrow, PERMS);
            } else {
                Assert.assertEquals("All expressions should have zeroed " + zeval + "/" + PERMS, zeval, PERMS);
            }
        }
        debuggerCheck(jexl);
    }

    @Test
    public void testDivideEdges() {
        assertNullOperand(() -> jexla.divide(null, null));
        Assert.assertEquals(0, jexlb.divide(null, null));
        assertNullOperand(() -> jexla.divide(null, null));
        Assert.assertEquals(0, jexlb.mod(null, null));
        assertArithmeticException(() -> jexla.divide(1, 0));
        assertArithmeticException(() -> jexla.divide(1L, 0L));
        assertArithmeticException(() -> jexla.divide(1f, 0f));
        assertArithmeticException(() -> jexla.divide(1d, 0d));
        assertArithmeticException(() -> jexla.divide(BigInteger.ONE, BigInteger.ZERO));
        assertArithmeticException(() -> jexla.divide(BigInteger.ONE, BigDecimal.ZERO));
    }

    @Test
    public void testEmpty() throws Exception {
        final Object[] SCRIPTS = {
            "var x = null; log('x = %s', x);", 0,
            "var x = 'abc'; log('x = %s', x);", 1,
            "var x = 333; log('x = %s', x);", 1,
            "var x = [1, 2]; log('x = %s', x);", 2,
            "var x = ['a', 'b']; log('x = %s', x);", 2,
            "var x = {1:'A', 2:'B'}; log('x = %s', x);", 1,
            "var x = null; return empty(x);", true,
            "var x = ''; return empty(x);", true,
            "var x = 'abc'; return empty(x);", false,
            "var x = 0; return empty(x);", true,
            "var x = 333; return empty(x);", false,
            "var x = []; return empty(x);", true,
            "var x = [1, 2]; return empty(x);", false,
            "var x = ['a', 'b']; return empty(x);", false,
            "var x = [...]; return empty(x);", true,
            "var x = [1, 2,...]; return empty(x);", false,
            "var x = {:}; return empty(x);", true,
            "var x = {1:'A', 2:'B'}; return empty(x);", false,
            "var x = {}; return empty(x);", true,
            "var x = {'A','B'}; return empty(x);", false
        };
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new EmptyTestContext();
        JexlScript script;

        for (int e = 0; e < SCRIPTS.length; e += 2) {
            final String stext = (String) SCRIPTS[e];
            final Object expected = SCRIPTS[e + 1];
            script = jexl.createScript(stext);
            final Object result = script.execute(jc);
            Assert.assertEquals("failed on " + stext, expected, result);
        }
    }

    @Test
    public void testEmptyDouble()  {
        Object x;
        x = JEXL.createScript("4294967296.d").execute(null);
        Assert.assertEquals(4294967296.0d, (Double) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("4294967296.0d").execute(null);
        Assert.assertEquals(4294967296.0d, (Double) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("0.0d").execute(null);
        Assert.assertEquals(0.0d, (Double) x, EPSILON);
        checkEmpty(x, true);
        x = Double.NaN;
        checkEmpty(x, true);

    }

    @Test
    public void testEmptyFloat()  {
        Object x;
        x = JEXL.createScript("4294967296.f").execute(null);
        Assert.assertEquals(4294967296.0f, (Float) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("4294967296.0f").execute(null);
        Assert.assertEquals(4294967296.0f, (Float) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("0.0f").execute(null);
        Assert.assertEquals(0.0f, (Float) x, EPSILON);
        checkEmpty(x, true);
        x = Float.NaN;
        checkEmpty(x, true);
    }

    @Test
    public void testEmptyLong()  {
        Object x;
        x = JEXL.createScript("new('java.lang.Long', 4294967296)").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("new Long(4294967296)").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("new('java.lang.Long', '4294967296')").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("4294967296l").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("4294967296L").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        checkEmpty(x, false);
        x = JEXL.createScript("0L").execute(null);
        Assert.assertEquals(0, ((Long) x).longValue());
        checkEmpty(x, true);
    }

    @Test
    public void testInfiniteArithmetic() throws Exception {
        final Map<String, Object> ns = new HashMap<>();
        ns.put("math", Math.class);
        final JexlEngine jexl = new JexlBuilder().arithmetic(new Arithmetic132()).namespaces(ns).create();

        Object evaluate = jexl.createExpression("1/0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("-1/0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("1.0/0.0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("-1.0/0.0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("math:abs(-42)").evaluate(null);
        Assert.assertEquals(42, evaluate);

        evaluate =  jexl.createExpression("42B / 7").evaluate(null);
        Assert.assertEquals(6, evaluate);

        evaluate =  jexl.createExpression("42.7B / 7").evaluate(null);
        Assert.assertEquals(BigDecimal.valueOf(6.1d), evaluate);
    }

    @Test
    public void testIntegerCoercionEdges() {
        assertNullOperand(() -> jexla.toBoolean(null));
        Assert.assertTrue(jexla.toBoolean(date));
        // int coercions
        assertNullOperand(() -> jexla.toInteger(null));
        Assert.assertEquals(0, jexlb.toInteger(null));
        assertArithmeticException(() -> jexla.toInteger(date));
        Assert.assertEquals(0, jexla.toInteger(Double.NaN));
        Assert.assertEquals(0, jexla.toInteger(""));
        Assert.assertEquals('b', jexla.toInteger('b'));
        Assert.assertEquals(1, jexla.toInteger(new AtomicBoolean(true)));
        Assert.assertEquals(0, jexla.toInteger(new AtomicBoolean(false)));

        // long coercions
        assertNullOperand(() -> jexla.toLong(null));
        Assert.assertEquals(0L, jexlb.toLong(null));
        assertArithmeticException(() -> jexla.toLong(date));
        Assert.assertEquals(0L, jexla.toLong(Double.NaN));
        Assert.assertEquals(0L, jexla.toLong(""));
        Assert.assertEquals('b', jexla.toLong('b'));
        Assert.assertEquals(1L, jexla.toLong(new AtomicBoolean(true)));
        Assert.assertEquals(0L, jexla.toLong(new AtomicBoolean(false)));
    }

    @Test
    public void testIsFloatingPointPattern() {
        final JexlArithmetic ja = new JexlArithmetic(true);

        Assert.assertFalse(ja.isFloatingPointNumber("floating point"));
        Assert.assertFalse(ja.isFloatingPointNumber("a1."));
        Assert.assertFalse(ja.isFloatingPointNumber("b1.2"));
        Assert.assertFalse(ja.isFloatingPointNumber("-10.2a-34"));
        Assert.assertFalse(ja.isFloatingPointNumber("+10.2a+34"));
        Assert.assertFalse(ja.isFloatingPointNumber("0"));
        Assert.assertFalse(ja.isFloatingPointNumber("1"));
        Assert.assertFalse(ja.isFloatingPointNumber("12A"));
        Assert.assertFalse(ja.isFloatingPointNumber("2F3"));
        Assert.assertFalse(ja.isFloatingPointNumber("23"));
        Assert.assertFalse(ja.isFloatingPointNumber("+3"));
        Assert.assertFalse(ja.isFloatingPointNumber("+34"));
        Assert.assertFalse(ja.isFloatingPointNumber("+3-4"));
        Assert.assertFalse(ja.isFloatingPointNumber("+3.-4"));
        Assert.assertFalse(ja.isFloatingPointNumber("3ee4"));

        Assert.assertTrue(ja.isFloatingPointNumber("0."));
        Assert.assertTrue(ja.isFloatingPointNumber("1."));
        Assert.assertTrue(ja.isFloatingPointNumber("1.2"));
        Assert.assertTrue(ja.isFloatingPointNumber("1.2e3"));
        Assert.assertTrue(ja.isFloatingPointNumber("2e3"));
        Assert.assertTrue(ja.isFloatingPointNumber("+2e-3"));
        Assert.assertTrue(ja.isFloatingPointNumber("+23E-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+23.E-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-23.4E+45"));
        Assert.assertTrue(ja.isFloatingPointNumber("1.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("10.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+10.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-10.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("10.2e-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("10.2e+34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-10.2e-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+10.2e+34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-10.2E-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+10.2E+34"));
    }

    @Test
    public void testJexl173() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new MapContext();
        final Callable173 c173 = new Callable173();
        JexlScript e = jexl.createScript( "c173(9, 6)", "c173" );
        Object result = e.execute(jc, c173);
        Assert.assertEquals(54, result);
        e = jexl.createScript( "c173('fourty', 'two')", "c173" );
        result = e.execute(jc, c173);
        Assert.assertEquals(42, result);

    }

    @Test
    public void testLeftNullOperand() throws Exception {
        asserter.setVariable("left", null);
        asserter.setVariable("right", Integer.valueOf(8));
        asserter.setStrict(true);
        asserter.failExpression("left + right", ".*null.*");
        asserter.failExpression("left - right", ".*null.*");
        asserter.failExpression("left * right", ".*null.*");
        asserter.failExpression("left / right", ".*null.*");
        asserter.failExpression("left % right", ".*null.*");
        asserter.failExpression("left & right", ".*null.*");
        asserter.failExpression("left | right", ".*null.*");
        asserter.failExpression("left ^ right", ".*null.*");
        asserter.failExpression("left < right", ".*null.*");
        asserter.failExpression("left <= right", ".*null.*");
        asserter.failExpression("left > right", ".*null.*");
        asserter.failExpression("left >= right", ".*null.*");
    }

    @Test
    public void testLeftNullOperand2() throws Exception {
        asserter.setVariable("x.left", null);
        asserter.setVariable("right", Integer.valueOf(8));
        asserter.setStrict(true);
        asserter.failExpression("x.left + right", ".*null.*");
        asserter.failExpression("x.left - right", ".*null.*");
        asserter.failExpression("x.left * right", ".*null.*");
        asserter.failExpression("x.left / right", ".*null.*");
        asserter.failExpression("x.left % right", ".*null.*");
        asserter.failExpression("x.left & right", ".*null.*");
        asserter.failExpression("x.left | right", ".*null.*");
        asserter.failExpression("x.left ^ right", ".*null.*");
        asserter.failExpression("x.left < right", ".*null.*");
        asserter.failExpression("x.left <= right", ".*null.*");
        asserter.failExpression("x.left > right", ".*null.*");
        asserter.failExpression("x.left >= right", ".*null.*");
    }

    // JEXL-24: long integers (and doubles)
    @Test
    public void testLongLiterals() {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrictArithmetic(true);
        final String stmt = "{a = 10L; b = 10l; c = 42.0D; d = 42.0d; e=56.3F; f=56.3f; g=63.5; h=0x10; i=010; j=0x10L; k=010l}";
        final JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(10L, ctxt.get("a"));
        Assert.assertEquals(10L, ctxt.get("b"));
        Assert.assertEquals(42.0D, ctxt.get("c"));
        Assert.assertEquals(42.0d, ctxt.get("d"));
        Assert.assertEquals(56.3f, ctxt.get("e"));
        Assert.assertEquals(56.3f, ctxt.get("f"));
        Assert.assertEquals(63.5d, ctxt.get("g"));
        Assert.assertEquals(0x10, ctxt.get("h"));
        Assert.assertEquals(010, ctxt.get("i")); // octal 010
        Assert.assertEquals(0x10L, ctxt.get("j")); // octal 010L
        Assert.assertEquals(010L, ctxt.get("k"));
    }

    @Test
    public void testMinusClass() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new MapContext();
        final Object ra = jexl.createExpression("463.0d - 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        final Object r0 = jexl.createExpression("463.0B - 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        final Object r1 = jexl.createExpression("463.0B - 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testMinusMinusPostfix() throws Exception {
        asserter.setVariable("aByte", Byte.valueOf((byte) 2));
        asserter.setVariable("aShort", Short.valueOf((short) 3));
        asserter.setVariable("anInteger", Integer.valueOf(4));
        asserter.setVariable("aLong", Long.valueOf(5));
        asserter.setVariable("aFloat", Float.valueOf((float) 6.6));
        asserter.setVariable("aDouble", Double.valueOf(7.7));
        asserter.setVariable("aBigInteger", new BigInteger("8"));
        asserter.setVariable("aBigDecimal", new BigDecimal("9.9"));
        asserter.setVariable("aString", "forty-two");

        asserter.assertExpression("aByte--",Byte.valueOf((byte) 2));
        asserter.assertExpression("aShort--", Short.valueOf((short) 3));
        asserter.assertExpression("anInteger--", Integer.valueOf(4));
        asserter.assertExpression("aLong--", Long.valueOf(5));
        asserter.assertExpression("aFloat--", Float.valueOf((float) 6.6));
        asserter.assertExpression("aDouble--", Double.valueOf(7.7));
        asserter.assertExpression("aBigInteger--", new BigInteger("8"));
        asserter.assertExpression("aBigDecimal--", new BigDecimal("9.9"));

        asserter.assertExpression("aByte", Byte.valueOf((byte) 1));
        asserter.assertExpression("aShort", Short.valueOf((short) 2));
        asserter.assertExpression("anInteger", Integer.valueOf(3));
        asserter.assertExpression("aLong", Long.valueOf(4));
        asserter.assertExpression("aFloat", Float.valueOf((float) 5.6));
        asserter.assertExpression("aDouble", Double.valueOf(6.7));
        asserter.assertExpression("aBigInteger", new BigInteger("7"));
        asserter.assertExpression("aBigDecimal", new BigDecimal("8.9"));

        asserter.failExpression("aString--", "--", String::contains);
    }

    @Test
    public void testMinusMinusPrefix() throws Exception {
        asserter.setVariable("aByte", Byte.valueOf((byte) 2));
        asserter.setVariable("aShort", Short.valueOf((short) 3));
        asserter.setVariable("anInteger", Integer.valueOf(4));
        asserter.setVariable("aLong", Long.valueOf(5));
        asserter.setVariable("aFloat", Float.valueOf((float) 6.6));
        asserter.setVariable("aDouble", Double.valueOf(7.7));
        asserter.setVariable("aBigInteger", new BigInteger("8"));
        asserter.setVariable("aBigDecimal", new BigDecimal("9.9"));
        asserter.setVariable("aString", "forty-two");

        asserter.assertExpression("--aByte", Byte.valueOf((byte) 1));
        asserter.assertExpression("--aShort", Short.valueOf((short) 2));
        asserter.assertExpression("--anInteger", Integer.valueOf(3));
        asserter.assertExpression("--aLong", Long.valueOf(4));
        asserter.assertExpression("--aFloat", Float.valueOf((float) 5.6));
        asserter.assertExpression("--aDouble", Double.valueOf(6.7));
        asserter.assertExpression("--aBigInteger", new BigInteger("7"));
        asserter.assertExpression("--aBigDecimal", new BigDecimal("8.9"));

        asserter.failExpression("aString--", "--", String::contains);
    }

    @Test
    public void testModEdge() {
        assertNullOperand(() -> jexla.mod(null, null));
        Assert.assertEquals(0, jexlb.mod(null, null));
        assertArithmeticException(() -> jexla.mod(1, 0));
        assertArithmeticException(() -> jexla.mod(1L, 0L));
        assertArithmeticException(() -> jexla.mod(1f, 0f));
        assertArithmeticException(() -> jexla.mod(1d, 0d));
        assertArithmeticException(() -> jexla.mod(BigInteger.ONE, BigInteger.ZERO));
        assertArithmeticException(() -> jexla.mod(BigInteger.ONE, BigDecimal.ZERO));
        assertNullOperand(() -> jexla.divide(null, null));
    }

    /**
     * JEXL-156.
     */
    @Test
    public void testMultClass(){
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new MapContext();
        final Object ra = jexl.createExpression("463.0d * 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        final Object r0 = jexl.createExpression("463.0B * 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        final Object r1 = jexl.createExpression("463.0B * 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testNaN() {
        final Map<String, Object> ns = new HashMap<>();
        ns.put("double", Double.class);
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("#NaN");
        result = script.execute(null);
        Assert.assertTrue(Double.isNaN((Double) result));
        script = jexl.createScript("NaN");
        result = script.execute(null);
        Assert.assertTrue(Double.isNaN((Double) result));
        script = jexl.createScript("double:isNaN(#NaN)");
        result = script.execute(null);
        Assert.assertTrue((Boolean) result);
        script = jexl.createScript("double:isNaN(NaN)");
        result = script.execute(null);
        Assert.assertTrue((Boolean) result);
    }

    @Test
    public void testNarrowBigInteger() throws Exception {
        final List<String> ls = Arrays.asList("zero", "one", "two");
        asserter.setVariable("list",ls);
        asserter.assertExpression("a -> list.get(a)", "zero", BigInteger.ZERO);
        asserter.assertExpression("a -> list.get(a)", "one", BigInteger.ONE);
        asserter.assertExpression("a -> list.get(2H)", "two");
        BigInteger b42 = BigInteger.valueOf(42);
        asserter.setVariable("bi10", BigInteger.valueOf(10));
        asserter.setVariable("bi420", BigInteger.valueOf(420));
        asserter.assertExpression("420 / bi10", b42);
        asserter.assertExpression("420l / bi10", b42);
        asserter.assertExpression("bi420 / 420", BigInteger.ONE);
        asserter.assertExpression("bi420 / 420l", BigInteger.ONE);
        asserter.assertExpression("bi420 / 420H", BigInteger.ONE);
    }

    @Test
    public void testNarrowBigDecimal() throws Exception {
        final List<String> ls = Arrays.asList("zero", "one", "two");
        asserter.setVariable("list", ls);
        asserter.assertExpression("a -> list.get(a)", "zero", BigDecimal.ZERO);
        asserter.assertExpression("a -> list.get(a)", "one", BigDecimal.ONE);
        asserter.assertExpression("a -> list.get(2B)", "two");
        BigDecimal bd42 = BigDecimal.valueOf(42);
        asserter.setVariable("bd10", BigDecimal.valueOf(10d));
        asserter.setVariable("bd420",BigDecimal.valueOf(420d));
        asserter.assertExpression("420 / bd10", bd42);
        asserter.assertExpression("420l / bd10", bd42);
        asserter.assertExpression("420H / bd10", bd42);
        asserter.assertExpression("bd420 / 10", bd42);
        asserter.assertExpression("bd420 / 10H", bd42);
        asserter.assertExpression("bd420 / 10B", bd42);
    }

    @Test
    public void testNullArgs() {
        final JexlEngine jexl =  new JexlBuilder().arithmetic(new JexlArithmetic(true) {
            @Override public boolean isStrict(final JexlOperator op) {
                return JexlOperator.ADD == op? false: super.isStrict(op);
            }
        }).create();
        final JexlScript script = jexl.createScript("'1.2' + x ", "x");
        final Object result = script.execute(null);
        Assert.assertEquals("1.2", result);
    }

    @Test
    public void testNullOperand() throws Exception {
        asserter.setVariable("right", null);
        asserter.failExpression("~right", ".*null.*");
    }

    @Test
    public void testNullOperands() throws Exception {
        asserter.setVariable("left", null);
        asserter.setVariable("right", null);
        asserter.failExpression("left + right", ".*null.*");
        asserter.failExpression("left - right", ".*null.*");
        asserter.failExpression("left * right", ".*null.*");
        asserter.failExpression("left / right", ".*null.*");
        asserter.failExpression("left % right", ".*null.*");
        asserter.failExpression("left & right", ".*null.*");
        asserter.failExpression("left | right", ".*null.*");
        asserter.failExpression("left ^ right", ".*null.*");
    }

    @Test
    public void testOperatorsEdges() {
        assertNullOperand(() -> jexla.multiply(null, null));
        Assert.assertEquals(0, jexlb.multiply(null, null));
        assertNullOperand(() -> jexla.add(null, null));
        Assert.assertEquals(0, jexlb.add(null, null));
        assertNullOperand(() -> jexla.subtract(null, null));
        Assert.assertEquals(0, jexlb.subtract(null, null));

        Assert.assertTrue(jexla.contains(null, null));
        Assert.assertFalse(jexla.contains(true, null));
        Assert.assertFalse(jexla.contains(null, true));
        Assert.assertTrue(jexla.endsWith(null, null));
        Assert.assertFalse(jexla.endsWith(true, null));
        Assert.assertFalse(jexla.endsWith(null, true));
        Assert.assertTrue(jexla.startsWith(null, null));
        Assert.assertFalse(jexla.startsWith(true, null));
        Assert.assertFalse(jexla.startsWith(null, true));
        Assert.assertTrue(jexla.isEmpty(null));
    }

    @Test
    public void testOption() {
        final Map<String, Object> vars = new HashMap<>();
        final JexlEvalContext context = new JexlEvalContext(vars);
        final JexlOptions options = context.getEngineOptions();
        options.setStrictArithmetic(true);
        final JexlScript script = JEXL.createScript("0 + '1.2' ");
        Object result;

        options.setStrictArithmetic(true);
        result = script.execute(context);
        Assert.assertEquals("01.2", result);

        options.setStrictArithmetic(false);
        result = script.execute(context);
        Assert.assertEquals(1.2d, (Double) result, EPSILON);
    }

    @Test
    public void testOverflows() throws Exception {
        asserter.assertExpression("1 + 2147483647", Long.valueOf("2147483648"));
        asserter.assertExpression("3 + " + (Long.MAX_VALUE - 2),  BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        asserter.assertExpression("-2147483648 - 1", Long.valueOf("-2147483649"));
        asserter.assertExpression("-3 + " + (Long.MIN_VALUE + 2),  BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE));
        asserter.assertExpression("1 + 9223372036854775807", new BigInteger("9223372036854775808"));
        asserter.assertExpression("-1 + (-9223372036854775808)", new BigInteger("-9223372036854775809"));
        asserter.assertExpression("-9223372036854775808 - 1", new BigInteger("-9223372036854775809"));
        final BigInteger maxl = BigInteger.valueOf(Long.MAX_VALUE);
        asserter.assertExpression(maxl + " * " + maxl , maxl.multiply(maxl));
    }

    @Test
    public void testPlusClass() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext jc = new MapContext();
        final Object ra = jexl.createExpression("463.0d + 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        final Object r0 = jexl.createExpression("463.0B + 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        final Object r1 = jexl.createExpression("463.0B + 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testPlusPlusPostfix() throws Exception {
        asserter.setVariable("aByte", Byte.valueOf((byte) 0));
        asserter.setVariable("aShort", Short.valueOf((short) 1));
        asserter.setVariable("anInteger", Integer.valueOf(2));
        asserter.setVariable("aLong", Long.valueOf(3));
        asserter.setVariable("aFloat", Float.valueOf((float) 4.4));
        asserter.setVariable("aDouble", Double.valueOf(5.5));
        asserter.setVariable("aBigInteger", new BigInteger("6"));
        asserter.setVariable("aBigDecimal", new BigDecimal("7.7"));
        asserter.setVariable("aString", "forty-two");

        asserter.assertExpression("aByte++", Byte.valueOf((byte) 0));
        asserter.assertExpression("aShort++", Short.valueOf((short) 1));
        asserter.assertExpression("anInteger++", Integer.valueOf(2));
        asserter.assertExpression("aLong++", Long.valueOf(3));
        asserter.assertExpression("aFloat++", Float.valueOf((float) 4.4));
        asserter.assertExpression("aDouble++", Double.valueOf(5.5));
        asserter.assertExpression("aBigInteger++", new BigInteger("6"));
        asserter.assertExpression("aBigDecimal++", new BigDecimal("7.7"));

        asserter.assertExpression("aByte", Byte.valueOf((byte) 1));
        asserter.assertExpression("aShort", Short.valueOf((short) 2));
        asserter.assertExpression("anInteger", Integer.valueOf(3));
        asserter.assertExpression("aLong", Long.valueOf(4));
        asserter.assertExpression("aFloat", Float.valueOf((float) 5.4));
        asserter.assertExpression("aDouble", Double.valueOf(6.5));
        asserter.assertExpression("aBigInteger", new BigInteger("7"));
        asserter.assertExpression("aBigDecimal", new BigDecimal("8.7"));

        asserter.failExpression("aString++", "++", String::contains);
    }

    @Test
    public void testPlusPlusPrefix() throws Exception {
        asserter.setVariable("aByte", Byte.valueOf((byte) 0));
        asserter.setVariable("aShort", Short.valueOf((short) 1));
        asserter.setVariable("anInteger", Integer.valueOf(2));
        asserter.setVariable("aLong", Long.valueOf(3));
        asserter.setVariable("aFloat", Float.valueOf((float) 4.4));
        asserter.setVariable("aDouble", Double.valueOf(5.5));
        asserter.setVariable("aBigInteger", new BigInteger("6"));
        asserter.setVariable("aBigDecimal", new BigDecimal("7.7"));
        asserter.setVariable("aString", "forty-two");

        asserter.assertExpression("++aByte", Byte.valueOf((byte) 1));
        asserter.assertExpression("++aShort", Short.valueOf((short) 2));
        asserter.assertExpression("++anInteger", Integer.valueOf(3));
        asserter.assertExpression("++aLong", Long.valueOf(4));
        asserter.assertExpression("++aFloat", Float.valueOf((float) 5.4));
        asserter.assertExpression("++aDouble", Double.valueOf(6.5));
        asserter.assertExpression("++aBigInteger", new BigInteger("7"));
        asserter.assertExpression("++aBigDecimal", new BigDecimal("8.7"));

        asserter.failExpression("++aString", "++", String::contains);
    }

    @Test
    public void testRealCoercionEdges() throws Exception {
        assertNullOperand(() -> jexla.toDouble(null));
        Assert.assertEquals(0.0d, jexlb.toDouble(null), EPSILON);
        Assert.assertEquals(32.0d, jexlb.toDouble((char) 32), EPSILON);
        assertArithmeticException(() -> jexla.toDouble(date));
        Assert.assertTrue(Double.isNaN(jexla.toDouble("")));
        Assert.assertEquals("", jexla.toString(Double.NaN));

        assertNullOperand(() -> jexla.toBigInteger(null));
        assertArithmeticException(() -> jexla.toBigInteger(date));
        Assert.assertEquals(BigInteger.ZERO, jexla.toBigInteger(Double.NaN));
        Assert.assertEquals(BigInteger.ZERO, jexla.toBigInteger(""));
        Assert.assertEquals(BigInteger.ZERO, jexla.toBigInteger((char) 0));

        assertNullOperand(() -> jexla.toBigDecimal(null));
        assertArithmeticException(() -> jexla.toBigDecimal(date));

        Assert.assertEquals(BigDecimal.ZERO, jexla.toBigDecimal(Double.NaN));
        Assert.assertEquals(BigDecimal.ZERO, jexla.toBigDecimal(""));
        Assert.assertEquals(BigDecimal.ZERO, jexla.toBigDecimal((char) 0));

        Double d64d3 = new Double( 6.4 / 3 );
        Assert.assertEquals(d64d3, ((Number) JEXL.createExpression("6.4 / 3").evaluate(null)).doubleValue(), EPSILON);
        asserter.assertExpression("6.4 / 3", d64d3);
        Assert.assertEquals(d64d3, ((Number) JEXL.createExpression("6.4 / 3d").evaluate(null)).doubleValue(), EPSILON);
        asserter.assertExpression("6.4 / 3d", d64d3);
        Assert.assertEquals(64d / 3, ((Number) JEXL.createExpression("64d / 3").evaluate(null)).doubleValue(), EPSILON);
        asserter.assertExpression("64 / 3d", 64 / 3d);
    }

    @Test
    public void testRightNullOperand() throws Exception {
        asserter.setVariable("left", Integer.valueOf(9));
        asserter.setVariable("right", null);
        asserter.failExpression("left + right", ".*null.*");
        asserter.failExpression("left - right", ".*null.*");
        asserter.failExpression("left * right", ".*null.*");
        asserter.failExpression("left / right", ".*null.*");
        asserter.failExpression("left % right", ".*null.*");
        asserter.failExpression("left & right", ".*null.*");
        asserter.failExpression("left | right", ".*null.*");
        asserter.failExpression("left ^ right", ".*null.*");
        asserter.failExpression("left < right", ".*null.*");
        asserter.failExpression("left <= right", ".*null.*");
        asserter.failExpression("left > right", ".*null.*");
        asserter.failExpression("left >= right", ".*null.*");
    }

    @Test
    public void testRightNullOperand2() throws Exception {
        asserter.setVariable("left", Integer.valueOf(9));
        asserter.setVariable("y.right", null);
        asserter.failExpression("left + y.right", ".*null.*");
        asserter.failExpression("left - y.right", ".*null.*");
        asserter.failExpression("left * y.right", ".*null.*");
        asserter.failExpression("left / y.right", ".*null.*");
        asserter.failExpression("left % y.right", ".*null.*");
        asserter.failExpression("left & y.right", ".*null.*");
        asserter.failExpression("left | y.right", ".*null.*");
        asserter.failExpression("left ^ y.right", ".*null.*");
        asserter.failExpression("left < y.right", ".*null.*");
        asserter.failExpression("left <= y.right", ".*null.*");
        asserter.failExpression("left > y.right", ".*null.*");
        asserter.failExpression("left >= y.right", ".*null.*");
    }

    /**
     * test some simple mathematical calculations
     */
    @Test
    public void testUnaryMinus() throws Exception {
        asserter.setVariable("aByte", Byte.valueOf((byte) 1));
        asserter.setVariable("aShort", Short.valueOf((short) 2));
        asserter.setVariable("anInteger", Integer.valueOf(3));
        asserter.setVariable("aLong", Long.valueOf(4));
        asserter.setVariable("aFloat", Float.valueOf((float) 5.5));
        asserter.setVariable("aDouble", Double.valueOf(6.6));
        asserter.setVariable("aBigInteger", new BigInteger("7"));
        asserter.setVariable("aBigDecimal", new BigDecimal("8.8"));

        // loop to allow checking caching of constant numerals (debug)
        for(int i = 0 ; i < 2; ++i) {
            asserter.assertExpression("-3", Integer.valueOf("-3"));
            asserter.assertExpression("-3.0", Double.valueOf("-3.0"));
            asserter.assertExpression("-aByte", Byte.valueOf((byte) -1));
            asserter.assertExpression("-aShort", Short.valueOf((short) -2));
            asserter.assertExpression("-anInteger", Integer.valueOf(-3));
            asserter.assertExpression("-aLong", Long.valueOf(-4));
            asserter.assertExpression("-aFloat", Float.valueOf((float) -5.5));
            asserter.assertExpression("-aDouble", Double.valueOf(-6.6));
            asserter.assertExpression("-aBigInteger", new BigInteger("-7"));
            asserter.assertExpression("-aBigDecimal", new BigDecimal("-8.8"));
        }
    }

    @Test
    public void testUnaryopsEdges() {
        assertArithmeticException(() -> jexla.positivize(date));
        assertNullOperand(() -> jexla.positivize(null));
        Assert.assertNull(jexlb.positivize(null));
        Assert.assertEquals(42, jexla.positivize((char) 42));
        Assert.assertEquals(Boolean.TRUE, jexla.positivize(Boolean.TRUE));
        Assert.assertEquals(Boolean.FALSE, jexla.positivize(Boolean.FALSE));
        Assert.assertEquals(Boolean.TRUE, jexla.positivize(new AtomicBoolean(true)));
        Assert.assertEquals(Boolean.FALSE, jexla.positivize(new AtomicBoolean(false)));

        assertNullOperand(() -> jexla.negate(null));
        Assert.assertNull(jexlb.negate(null));
        assertArithmeticException(() -> jexla.negate(date));
        Assert.assertEquals(Boolean.FALSE, jexla.negate(Boolean.TRUE));
        Assert.assertEquals(Boolean.TRUE, jexla.negate(Boolean.FALSE));
    }

    /**
     * test some simple mathematical calculations
     */
    @Test
    public void testUnaryPlus() throws Exception {
        asserter.setVariable("aByte", Byte.valueOf((byte) 1));
        asserter.setVariable("aShort", Short.valueOf((short) 2));
        asserter.setVariable("anInteger", Integer.valueOf(3));
        asserter.setVariable("aLong", Long.valueOf(4));
        asserter.setVariable("aFloat", Float.valueOf((float) 5.5));
        asserter.setVariable("aDouble", Double.valueOf(6.6));
        asserter.setVariable("aBigInteger", new BigInteger("7"));
        asserter.setVariable("aBigDecimal", new BigDecimal("8.8"));

        // loop to allow checking caching of constant numerals (debug)
        for(int i = 0 ; i < 2; ++i) {
            asserter.assertExpression("+3", Integer.valueOf("3"));
            asserter.assertExpression("+3.0", Double.valueOf("3.0"));
            asserter.assertExpression("+aByte", Integer.valueOf(1));
            asserter.assertExpression("+aShort", Integer.valueOf(2));
            asserter.assertExpression("+anInteger", Integer.valueOf(3));
            asserter.assertExpression("+aLong", Long.valueOf(4));
            asserter.assertExpression("+aFloat", Float.valueOf((float) 5.5));
            asserter.assertExpression("+aDouble", Double.valueOf(6.6));
            asserter.assertExpression("+aBigInteger", new BigInteger("7"));
            asserter.assertExpression("+aBigDecimal", new BigDecimal("8.8"));
        }
    }

    @Test
    public void testUndefinedVar() throws Exception {
        asserter.failExpression("objects[1].status", ".*variable 'objects' is undefined.*");
    }

    public static class InstanceofContext extends MapContext implements JexlContext.ClassNameResolver {
        @Override
        public String resolveClassName(String name) {
            if ("Double".equals(name)) {
                return Double.class.getName();
            }
            return null;
        }
    }

    @Test
    public void testInstanceOf0() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(false).create();
        final JexlContext ctxt = new InstanceofContext();
        runInstanceof(jexl, ctxt);
    }

    @Test
    public void testInstanceOf1() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(false).imports("java.lang").create();
        runInstanceof(jexl, null);
    }

    private void runInstanceof(JexlEngine jexl, JexlContext ctxt) {
        Object r = jexl.createExpression("3.0 instanceof 'Double'").evaluate(ctxt);
        Assert.assertTrue((Boolean) r);
        r = jexl.createExpression("'3.0' !instanceof 'Double'").evaluate(ctxt);
        Assert.assertTrue((Boolean) r);
        JexlScript script = jexl.createScript("x instanceof y", "x", "y");
        r = script.execute(ctxt, "foo", String.class);
        Assert.assertTrue((Boolean) r);
        r = script.execute(ctxt, 42.0, Double.class);
        Assert.assertTrue((Boolean) r);
        script = jexl.createScript("x !instanceof y", "x", "y");
        r = script.execute(ctxt, "foo", Double.class);
        Assert.assertTrue((Boolean) r);
        r = script.execute(ctxt, 42.0, String.class);
        Assert.assertTrue((Boolean) r);
    }

    /**
     * Inspired by JEXL-16{1,2}.
     */
    @Test
    public void testXmlArithmetic() throws Exception {
        Document xml;
        Node x;
        Boolean empty;
        int size;
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(false).arithmetic(new XmlArithmetic(false)).create();
        final JexlScript e0 = jexl.createScript("x.empty()", "x");
        final JexlScript e1 = jexl.createScript("empty(x)", "x");
        final JexlScript s0 = jexl.createScript("x.size()", "x");
        final JexlScript s1 = jexl.createScript("size(x)", "x");

        empty = (Boolean) e1.execute(null, (Object) null);
        Assert.assertTrue(empty);
        size = (Integer) s1.execute(null, (Object) null);
        Assert.assertEquals(0, size);

        try {
            final Object xx = e0.execute(null, (Object) null);
            Assert.assertNull(xx);
        } catch (final JexlException.Variable xvar) {
            Assert.assertNotNull(xvar);
        }
        try {
            final Object xx = s0.execute(null, (Object) null);
            Assert.assertNull(xx);
        } catch (final JexlException.Variable xvar) {
            Assert.assertNotNull(xvar);
        }
        final JexlOptions options = ctxt.getEngineOptions();
        options.setSafe(true);
        final Object x0 = e0.execute(ctxt, (Object) null);
        Assert.assertNull(x0);
        final Object x1 = s0.execute(ctxt, (Object) null);
        Assert.assertNull(x1);

        xml = getDocument("<node info='123'/>");
        x = xml.getLastChild();
        empty = (Boolean) e0.execute(null, x);
        Assert.assertFalse(empty);
        empty = (Boolean) e1.execute(null, x);
        Assert.assertFalse(empty);
        size = (Integer) s0.execute(null, x);
        Assert.assertEquals(0, size);
        size = (Integer) s1.execute(null, x);
        Assert.assertEquals(0, size);
        xml = getDocument("<node><a/><b/></node>");
        x = xml.getLastChild();
        empty = (Boolean) e0.execute(null, x);
        Assert.assertFalse(empty);
        empty = (Boolean) e1.execute(null, x);
        Assert.assertFalse(empty);
        size = (Integer) s0.execute(null, x);
        Assert.assertEquals(2, size);
        size = (Integer) s1.execute(null, x);
        Assert.assertEquals(2, size);
        xml = getDocument("<node/>");
        x = xml.getLastChild();
        empty = (Boolean) e0.execute(null, x);
        Assert.assertTrue(empty);
        empty = (Boolean) e1.execute(null, x);
        Assert.assertTrue(empty);
        size = (Integer) s0.execute(null, x);
        Assert.assertEquals(0, size);
        size = (Integer) s1.execute(null, x);
        Assert.assertEquals(0, size);
        xml = getDocument("<node info='123'/>");
        NamedNodeMap nnm = xml.getLastChild().getAttributes();
        Attr info = (Attr) nnm.getNamedItem("info");
        Assert.assertEquals("123", info.getValue());

        // JEXL-161
        final JexlContext jc = new MapContext();
        jc.set("x", xml.getLastChild());
        final String y = "456";
        jc.set("y", y);
        final JexlScript s = jexl.createScript("x.attribute.info = y");
        Object r;
        try {
            r = s.execute(jc);
            nnm = xml.getLastChild().getAttributes();
            info = (Attr) nnm.getNamedItem("info");
            Assert.assertEquals(y, r);
            Assert.assertEquals(y, info.getValue());
        } catch (final JexlException.Property xprop) {
            // test fails in java > 11 because modules, etc; need investigation
            Assert.assertTrue(xprop.getMessage().contains("info"));
            Assert.assertTrue(getJavaVersion() > 11);
        }
    }
}
