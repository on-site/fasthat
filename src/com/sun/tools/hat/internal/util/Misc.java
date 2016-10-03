/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2010, 2011 On-Site.com.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package com.sun.tools.hat.internal.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;

/**
 * Miscellaneous functions I couldn't think of a good place to put.
 *
 * @author      Bill Foote
 */


public class Misc {
    private static final String digits = "0123456789abcdef";

    public final static String toHex(int addr) {
        StringBuilder sb = new StringBuilder("0x");
        for (int s = 28; s >= 0; s -= 4) {
            sb.append(digits.charAt((addr >>> s) & 0xf));
        }
        return sb.toString();
    }

    public final static String toHex(long addr) {
        return "0x" + Long.toHexString(addr);
    }

    public final static long parseHex(String value) {
        long result = 0;
        if (value.length() < 2 || value.charAt(0) != '0' ||
            value.charAt(1) != 'x') {
            return -1L;
        }
        for(int i = 2; i < value.length(); i++) {
            result *= 16;
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                result += (ch - '0');
            } else if (ch >= 'a' && ch <= 'f') {
                result += (ch - 'a') + 10;
            } else if (ch >= 'A' && ch <= 'F') {
                result += (ch - 'A') + 10;
            } else {
                throw new NumberFormatException("" + ch
                                        + " is not a valid hex digit");
            }
        }
        return result;
    }

    public static String encodeHtml(String str) {
        final int len = str.length();
        StringBuilder buf = new StringBuilder();
        IntConsumer numeric = c -> buf.append("&#").append(c).append(';');
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == '<') {
                buf.append("&lt;");
            } else if (ch == '>') {
                buf.append("&gt;");
            } else if (ch == '"') {
                buf.append("&quot;");
            } else if (ch == '&') {
                buf.append("&amp;");
            } else if (Character.isHighSurrogate(ch) && ++i < len) {
                char ch2 = str.charAt(i);
                if (Character.isLowSurrogate(ch2)) {
                    numeric.accept(Character.toCodePoint(ch, ch2));
                } else {
                    // Invalid, just print out what we have.
                    numeric.accept(ch);
                    numeric.accept(ch2);
                }
            } else if (ch < ' ' || ch == '\'' || ch > 127) {
                numeric.accept(ch);
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    public static ImmutableSet<JavaHeapObject> getReferrers(
            Iterable<JavaHeapObject> instances) {
        ImmutableSet.Builder<JavaHeapObject> builder = ImmutableSet.builder();
        for (JavaHeapObject instance : instances) {
            builder.addAll(instance.getReferers());
        }
        return builder.build();
    }

    public static ImmutableSet<JavaHeapObject> getReferrers(
            Iterable<JavaHeapObject> instances, Predicate<JavaHeapObject> filter) {
        ImmutableSet.Builder<JavaHeapObject> builder = ImmutableSet.builder();
        for (JavaHeapObject instance : instances) {
            builder.addAll(Sets.filter(instance.getReferers(), filter));
        }
        return builder.build();
    }

    public static ImmutableSet<JavaHeapObject> getReferrersByClass(
            Iterable<JavaHeapObject> instances, JavaClass clazz) {
        return getReferrers(instances, instance -> instance.getClazz() == clazz);
    }

    public static ImmutableSet<JavaHeapObject> getReferees(
            Iterable<JavaHeapObject> instances, final Predicate<JavaHeapObject> filter) {
        final ImmutableSet.Builder<JavaHeapObject> builder = ImmutableSet.builder();
        for (JavaHeapObject instance : instances) {
            instance.visitReferencedObjects(obj -> {
                if (filter.apply(obj)) {
                    builder.add(obj);
                }
            });
        }
        return builder.build();
    }

    public static ImmutableSet<JavaHeapObject> getRefereesByClass(
            Iterable<JavaHeapObject> instances, JavaClass clazz) {
        return getReferees(instances, instance -> instance.getClazz() == clazz);
    }

    public static ImmutableSet<JavaHeapObject> getInstances(JavaClass clazz,
            boolean includeSubclasses, Iterable<JavaClass> referrers) {
        Iterable<JavaHeapObject> instances = clazz.getInstances(includeSubclasses);
        if (referrers != null) {
            for (JavaClass referrer : referrers) {
                instances = getReferrersByClass(instances, referrer);
            }
        }
        return ImmutableSet.copyOf(instances);
    }

    private static final long ONE_SECOND = 1000L;
    private static final long ONE_MINUTE = ONE_SECOND * 60L;
    private static final long ONE_HOUR = ONE_MINUTE * 60L;

    public static String formatTime(long millis) {
        long hours = millis / ONE_HOUR;
        millis %= ONE_HOUR;
        long minutes = millis / ONE_MINUTE;
        millis %= ONE_MINUTE;
        long seconds = millis / ONE_SECOND;
        millis %= ONE_SECOND;
        return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    public static String pluralize(int value, String base) {
        if (value == 1) {
            return base;
        } else {
            return base + "s";
        }
    }

    public static String encodeForURL(String s) {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Should never happen
            throw new AssertionError(ex);
        }
        return s;
    }
}
