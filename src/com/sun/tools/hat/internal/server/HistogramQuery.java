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

package com.sun.tools.hat.internal.server;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.util.Misc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

/**
 * Prints histogram sortable by class name, count and size.
 *
 */
public class HistogramQuery extends QueryHandler {
    private enum Sorters implements Function<JavaClass, Comparable<?>>,
            Comparator<JavaClass> {
        BY_NAME {
            @Override
            public String apply(JavaClass clazz) {
                return clazz.getName();
            }
        };

        private final Ordering<JavaClass> ordering;

        private Sorters() {
            ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(JavaClass lhs, JavaClass rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    private static abstract class MetricsProvider {
        private final JavaClass[] classes;

        protected MetricsProvider(JavaClass[] classes) {
            this.classes = classes;
        }

        public abstract int getCount(JavaClass clazz);
        public abstract long getSize(JavaClass clazz);
        public int getRefCount(JavaClass clazz) {throw new UnsupportedOperationException();}

        /*
         * If Java had first-class methods, these functions would not be
         * necessary. :-(
         */
        public Function<JavaClass, Integer> getCountMethod() {
            return new Function<JavaClass, Integer>() {
                public Integer apply(JavaClass clazz) {return getCount(clazz);}
            };
        }
        public Function<JavaClass, Long> getSizeMethod() {
            return new Function<JavaClass, Long>() {
                public Long apply(JavaClass clazz) {return getSize(clazz);}
            };
        }
        public Function<JavaClass, Integer> getRefCountMethod() {
            return new Function<JavaClass, Integer>() {
                public Integer apply(JavaClass clazz) {return getRefCount(clazz);}
            };
        }

        public JavaClass[] getClasses() {return classes.clone();}
        public boolean hasRefCount() {return false;}
    }

    private static class GlobalMetricsProvider extends MetricsProvider {
        public GlobalMetricsProvider(Snapshot snapshot) {
            super(snapshot.getClassesArray());
        }

        @Override
        public int getCount(JavaClass clazz) {
            return clazz.getInstancesCount(false);
        }

        @Override
        public long getSize(JavaClass clazz) {
            return clazz.getTotalInstanceSize();
        }
    }

    private static class RefereeMetricsProvider extends MetricsProvider {
        private enum GetClass implements Function<JavaHeapObject, JavaClass> {
            INSTANCE;

            @Override
            public JavaClass apply(JavaHeapObject obj) {
                return obj.getClazz();
            }
        }

        final ImmutableMultimap<JavaClass, JavaHeapObject> referrers;
        final ImmutableMultiset<JavaClass> references;

        private RefereeMetricsProvider(ImmutableMultimap<JavaClass, JavaHeapObject> referrers,
                ImmutableMultiset<JavaClass> references) {
            super(referrers.keySet().toArray(new JavaClass[0]));
            this.referrers = referrers;
            this.references = references;
        }

        public static RefereeMetricsProvider make(Collection<JavaClass> referees) {
            Iterator<JavaClass> iter = referees.iterator();
            Preconditions.checkArgument(iter.hasNext());
            Iterable<JavaHeapObject> instances = iter.next().getInstances(false);
            while (iter.hasNext()) {
                instances = getReferrers(instances, Predicates.compose(
                        Predicates.equalTo(iter.next()), GetClass.INSTANCE));
            }
            return new RefereeMetricsProvider(
                    Multimaps.index(getReferrers(instances), GetClass.INSTANCE),
                    getReferences(instances).keys());
        }

        @Override
        public int getCount(JavaClass clazz) {
            return referrers.get(clazz).size();
        }

        @Override
        public long getSize(JavaClass clazz) {
            Collection<JavaHeapObject> subset = referrers.get(clazz);
            if (!clazz.isArray()) {
                return (long) clazz.getInstanceSize() * subset.size();
            }
            long size = 0;
            for (JavaHeapObject instance : subset) {
                size += instance.getSize();
            }
            return size;
        }

        @Override
        public int getRefCount(JavaClass clazz) {
            return references.count(clazz);
        }

        @Override
        public boolean hasRefCount() {
            return true;
        }

        private static ImmutableSet<JavaHeapObject> getReferrers(
                Iterable<JavaHeapObject> instances) {
            ImmutableSet.Builder<JavaHeapObject> builder = ImmutableSet.builder();
            for (JavaHeapObject instance : instances) {
                builder.addAll(instance.getReferers());
            }
            return builder.build();
        }

        private static ImmutableSet<JavaHeapObject> getReferrers(
                Iterable<JavaHeapObject> instances, Predicate<JavaHeapObject> filter) {
            ImmutableSet.Builder<JavaHeapObject> builder = ImmutableSet.builder();
            for (JavaHeapObject instance : instances) {
                builder.addAll(Sets.filter(instance.getReferers(), filter));
            }
            return builder.build();
        }

        private static ImmutableMultimap<JavaClass, JavaHeapObject> getReferences(
                Iterable<JavaHeapObject> instances) {
            ImmutableSetMultimap.Builder<JavaClass, JavaHeapObject> builder = ImmutableSetMultimap.builder();
            for (JavaHeapObject instance : instances) {
                for (JavaHeapObject referrer : instance.getReferers()) {
                    builder.put(referrer.getClazz(), instance);
                }
            }
            return builder.build();
        }
    }

    public void run() {
        List<JavaClass> referees = Lists.transform(params.get("referee"),
                new Function<String, JavaClass>() {
            @Override
            public JavaClass apply(String name) {
                JavaClass result = snapshot.findClass(name);
                Preconditions.checkNotNull(result, "class not found: %s", name);
                return result;
            }
        });
        MetricsProvider metrics;
        if (referees.isEmpty()) {
            metrics = new GlobalMetricsProvider(snapshot);
        } else {
            metrics = RefereeMetricsProvider.make(referees);
        }

        Comparator<JavaClass> comparator;
        if (query.equals("count")) {
            comparator = Ordering.natural().reverse().onResultOf(metrics.getCountMethod());
        } else if (query.equals("class")) {
            comparator = Sorters.BY_NAME;
        } else if (query.equals("size") || !metrics.hasRefCount()) {
            comparator = Ordering.natural().reverse().onResultOf(metrics.getSizeMethod());
        } else {
            comparator = Ordering.natural().reverse().onResultOf(metrics.getRefCountMethod());
        }
        JavaClass[] classes = metrics.getClasses();
        Arrays.sort(classes, comparator);

        startHtml("Heap Histogram");

        printBreadcrumbs(query, referees);
        out.println("<p align='center'>");
        out.println("<b><a href='/'>All Classes (excluding platform)</a></b>");
        out.println("</p>");

        out.println("<table align=center border=1>");
        out.println("<tr>");
        printHeader("class", "Class", referees, null);
        if (metrics.hasRefCount()) {
            printHeader("refCount", "Reference Count", referees, null);
        }
        printHeader("count", "Instance Count", referees, null);
        printHeader("size", "Total Size", referees, null);
        out.println("</tr>");
        for (JavaClass clazz : classes) {
            out.print("<tr><td>");
            printClass(clazz);
            if (referees.isEmpty()) {
                out.printf(" (%s)", formatLink(query, "refs", null, clazz));
            } else {
                out.printf(" (%s, %s)",
                        formatLink(query, "top", null, clazz),
                        formatLink(query, "chain", referees, clazz));
            }
            out.println("</td>");
            if (metrics.hasRefCount()) {
                out.printf("<td>%s</td>%n", metrics.getRefCount(clazz));
            }
            out.printf("<td>%s</td>%n", metrics.getCount(clazz));
            out.printf("<td>%s</td></tr>%n", metrics.getSize(clazz));
        }
        out.println("</table>");

        endHtml();
    }

    private void printBreadcrumbs(String pathInfo, List<JavaClass> referees) {
        if (!referees.isEmpty()) {
            out.print("<p align='center'>");
            out.print(formatLink(pathInfo, referees.get(0).getName(), null,
                    referees.get(0)));
            int size = referees.size();
            for (int i = 1; i < size; ++i) {
                out.printf(" &rarr; %s", formatLink(pathInfo, referees.get(i).getName(),
                        referees.subList(0, i + 1), null));
            }
            out.println("</p>");
        }
    }

    private void printHeader(String pathInfo, String label, Collection<JavaClass> referees,
            JavaClass tail) {
        out.printf("<th>%s</th>", formatLink(pathInfo, label, referees, tail));
    }

    private static String formatLink(String pathInfo, String label,
            Collection<JavaClass> referees, JavaClass tail) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        fmt.format("<a href='/histo/%s?", encodeForURL(pathInfo));
        if (referees != null) {
            for (JavaClass referee : referees) {
                fmt.format("referee=%s&", encodeForURL(referee.getIdString()));
            }
        }
        if (tail != null) {
            fmt.format("referee=%s&", encodeForURL(tail.getIdString()));
        }
        sb.setLength(sb.length() - 1);
        fmt.format("'>%s</a>", Misc.encodeHtml(label));
        return sb.toString();
    }
}
