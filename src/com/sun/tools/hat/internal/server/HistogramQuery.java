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

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.util.Misc;

import java.util.Collection;
import java.util.Comparator;

/**
 * Prints histogram sortable by class name, count and size.
 *
 */
public class HistogramQuery extends QueryHandler {
    private static abstract class MetricsProvider {
        private final Collection<JavaClass> classes;

        protected MetricsProvider(Collection<JavaClass> classes) {
            this.classes = classes;
        }

        public abstract int getCount(JavaClass clazz);
        public abstract long getSize(JavaClass clazz);
        public int getRefCount(JavaClass clazz) {throw new UnsupportedOperationException();}

        public Collection<JavaClass> getClasses() {return classes;}
        public boolean hasRefCount() {return false;}
    }

    private static class GlobalMetricsProvider extends MetricsProvider {
        public GlobalMetricsProvider(Snapshot snapshot) {
            super(snapshot.getClasses());
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
        final ImmutableMultimap<JavaClass, JavaHeapObject> referrers;
        final ImmutableMultiset<JavaClass> references;

        private RefereeMetricsProvider(ImmutableMultimap<JavaClass, JavaHeapObject> referrers,
                ImmutableMultiset<JavaClass> references) {
            super(referrers.keySet());
            this.referrers = referrers;
            this.references = references;
        }

        public static RefereeMetricsProvider make(JavaClass referee,
                Collection<JavaClass> referrers) {
            ImmutableSet<JavaHeapObject> instances = Misc.getInstances(referee,
                    false, referrers);
            return new RefereeMetricsProvider(
                    Multimaps.index(Misc.getReferrers(instances), JavaHeapObject::getClazz),
                    getReferences(instances).keys());
        }

        @Override
        public int getCount(JavaClass clazz) {
            return referrers.get(clazz).size();
        }

        @Override
        public long getSize(JavaClass clazz) {
            Collection<JavaHeapObject> subset = referrers.get(clazz);
            return clazz.isArray()
                    ? subset.stream().mapToLong(JavaHeapObject::getSize).sum()
                    : (long) clazz.getInstanceSize() * subset.size();
        }

        @Override
        public int getRefCount(JavaClass clazz) {
            return references.count(clazz);
        }

        @Override
        public boolean hasRefCount() {
            return true;
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

    @Override
    public void run() {
        JavaClass referee = resolveClass(Iterables.getOnlyElement(
                params.get("referee"), null), false);
        Collection<JavaClass> referrers = Collections2.transform(
                params.get("referrer"), referrer -> resolveClass(referrer, false));
        MetricsProvider metrics;
        if (referee == null) {
            metrics = new GlobalMetricsProvider(snapshot);
        } else {
            metrics = RefereeMetricsProvider.make(referee, referrers);
        }

        startHtml("Heap Histogram");

        printBreadcrumbs(query, referee, referrers);
        out.println("<p align='center'>");
        out.println("<b><a href='/allClasses/'>All Classes (excluding platform)</a></b>");
        out.println("</p>");

        out.println("<table align=center border=1>");
        out.println("<tr>");
        printHeader("class", "Class", referee, referrers);
        if (metrics.hasRefCount()) {
            printHeader("refCount", "Reference Count", referee, referrers);
        }
        printHeader("count", "Instance Count", referee, referrers);
        printHeader("size", "Total Size", referee, referrers);
        out.println("</tr>");

        Comparator<JavaClass> comparator;
        if (query.equals("count")) {
            comparator = Ordering.natural().reverse().onResultOf(metrics::getCount);
        } else if (query.equals("class")) {
            comparator = Ordering.natural().onResultOf(JavaClass::getName);
        } else if (query.equals("size") || !metrics.hasRefCount()) {
            comparator = Ordering.natural().reverse().onResultOf(metrics::getSize);
        } else {
            comparator = Ordering.natural().reverse().onResultOf(metrics::getRefCount);
        }

        metrics.getClasses().stream().sorted(comparator).forEach(clazz -> {
            out.print("<tr><td>");
            printClass(clazz);
            if (referee == null) {
                out.printf(" (%s)", formatLink(query, "refs", clazz, null, null));
            } else {
                out.printf(" (%s, %s)",
                        formatLink(query, "top", clazz, null, null),
                        formatLink(query, "chain", referee, referrers, clazz));
            }
            out.println("</td>");

            if (metrics.hasRefCount()) {
                String refCount = String.valueOf(metrics.getRefCount(clazz));
                ImmutableMultimap<String, String> params
                        = ImmutableMultimap.of("referee", "true");
                if (referee == null) {
                    out.printf("<td>%s</td>%n", formatLink("instances", null,
                            refCount, null, clazz, null, null, params));
                } else {
                    out.printf("<td>%s</td>%n", formatLink("instances", null,
                            refCount, null, referee, referrers, clazz, params));
                }
            }

            String count = String.valueOf(metrics.getCount(clazz));
            if (referee == null) {
                out.printf("<td>%s</td>%n", formatLink("instances", null,
                        count, null, clazz, null, null, null));
            } else {
                out.printf("<td>%s</td>%n", formatLink("instances", null,
                        count, null, referee, referrers, clazz, null));
            }
            out.printf("<td>%s</td></tr>%n", metrics.getSize(clazz));
        });
        out.println("</table>");

        endHtml();
    }

    private void printBreadcrumbs(String pathInfo, JavaClass referee,
            Collection<JavaClass> referrers) {
        super.printBreadcrumbs(path, pathInfo, "referee", referee,
                referrers, null);
    }

    private void printHeader(String pathInfo, String label, JavaClass referee,
            Collection<JavaClass> referrers) {
        out.printf("<th>%s</th>", formatLink(pathInfo, label, referee,
                referrers, null));
    }

    private String formatLink(String pathInfo, String label, JavaClass referee,
            Collection<JavaClass> referrers, JavaClass tail) {
        return formatLink(path, pathInfo, label, "referee", referee,
                referrers, tail, null);
    }
}
