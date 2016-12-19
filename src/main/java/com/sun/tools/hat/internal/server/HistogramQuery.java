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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.server.view.BreadcrumbsView;
import com.sun.tools.hat.internal.server.view.JavaThingView;
import com.sun.tools.hat.internal.server.view.Link;
import com.sun.tools.hat.internal.server.view.ReferrerSet;
import com.sun.tools.hat.internal.util.Misc;
import com.sun.tools.hat.internal.util.StreamIterable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Prints histogram sortable by class name, count and size.
 *
 */
public class HistogramQuery extends MustacheQueryHandler {
    private JavaThingView referee;
    private ReferrerSet referrers;
    private MetricsProvider metrics;

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

    public JavaThingView getReferee() {
        if (referee == null) {
            JavaClass javaClass = resolveClass(Iterables.getOnlyElement(params.get("referee"), null), false);
            referee = new JavaThingView(this, javaClass);
        }

        return referee;
    }

    public ReferrerSet getReferrers() {
        if (referrers == null) {
            List<JavaClass> classes = Lists.transform(params.get("referrer"), referrer -> resolveClass(referrer, false));
            referrers = new ReferrerSet(this, Lists.transform(classes, referrer -> new JavaThingView(this, referrer)));
        }

        return referrers;
    }

    private List<JavaClass> getReferrerClasses() {
        return Lists.transform(getReferrers().getReferrers(), JavaThingView::toJavaClass);
    }

    public BreadcrumbsView getBreadcrumbs() {
        return new BreadcrumbsView(this, path, query, "referee", getReferee(), getReferrers());
    }

    public MetricsProvider getMetrics() {
        if (metrics == null && getReferee().isNull()) {
            metrics = new GlobalMetricsProvider(snapshot);
        } else if (metrics == null) {
            metrics = RefereeMetricsProvider.make(getReferee().toJavaClass(), getReferrerClasses());
        }

        return metrics;
    }

    public Link getClassHeaderLink() {
        return new Link(this, "class", "Class", getReferee(), getReferrers());
    }

    public boolean hasRefCount() {
        return getMetrics().hasRefCount();
    }

    public Link getRefCountHeaderLink() {
        return new Link(this, "refCount", "Reference Count", getReferee(), getReferrers());
    }

    public Link getInstanceCountHeaderLink() {
        return new Link(this, "count", "Instance Count", getReferee(), getReferrers());
    }

    public Link getTotalSizeHeaderLink() {
        return new Link(this, "size", "Total Size", getReferee(), getReferrers());
    }

    public Iterable<HistogramRow> getHistogramRows() {
        MetricsProvider metrics = getMetrics();
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

        return new StreamIterable<>(metrics.getClasses().stream().sorted(comparator).map(HistogramRow::new));
    }

    public class HistogramRow {
        private final JavaThingView clazz;

        public HistogramRow(JavaClass clazz) {
            this.clazz = new JavaThingView(HistogramQuery.this, clazz);
        }

        public JavaThingView getJavaClass() {
            return clazz;
        }

        public Link getRefsLink() {
            return new Link(HistogramQuery.this, query, "refs", clazz, null, null);
        }

        public Link getTopLink() {
            return new Link(HistogramQuery.this, query, "top", clazz, null, null);
        }

        public Link getChainLink() {
            return new Link(HistogramQuery.this, query, "chain", getReferee(), getReferrers(), clazz);
        }

        public Link getRefCountInstancesLink() {
            String refCount = String.valueOf(metrics.getRefCount(clazz.toJavaClass()));
            ImmutableMultimap<String, String> params = ImmutableMultimap.of("referee", "true");

            if (getReferee().isNull()) {
                return new Link(HistogramQuery.this, "instances", null, refCount, null, clazz, null, null, params);
            } else {
                return new Link(HistogramQuery.this, "instances", null, refCount, null, getReferee(), getReferrers(), clazz, params);
            }
        }

        public Link getInstancesLink() {
            String count = String.valueOf(metrics.getCount(clazz.toJavaClass()));

            if (getReferee().isNull()) {
                return new Link(HistogramQuery.this, "instances", null, count, null, clazz, null, null, null);
            } else {
                return new Link(HistogramQuery.this, "instances", null, count, null, getReferee(), getReferrers(), clazz, null);
            }
        }

        public long getMetricsSize() {
            return getMetrics().getSize(clazz.toJavaClass());
        }
    }
}
