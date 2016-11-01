/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.server.view.BreadcrumbsView;
import com.sun.tools.hat.internal.server.view.JavaThingView;
import com.sun.tools.hat.internal.server.view.Link;
import com.sun.tools.hat.internal.server.view.ReferrerSet;
import com.sun.tools.hat.internal.util.Misc;
import com.sun.tools.hat.internal.util.StreamIterable;

import java.util.List;

/**
 * References by type summary
 *
 */
public class RefsByTypeQuery extends MustacheQueryHandler {
    private JavaThingView clazz;
    private ReferrerSet referrers;
    private List<JavaClass> rawReferrers;
    private ImmutableMultiset<JavaThingView> referrersStat;
    private ImmutableMultiset<JavaThingView> refereesStat;

    public JavaThingView getJavaClass() {
        if (clazz == null) {
            clazz = new JavaThingView(this, resolveClass(query, true));
        }

        return clazz;
    }

    private List<JavaClass> getRawReferrers() {
        if (rawReferrers == null) {
            rawReferrers = Lists.transform(params.get("referrer"), referrer -> resolveClass(referrer, false));
        }

        return rawReferrers;
    }

    private ReferrerSet getReferrers() {
        if (referrers == null) {
            referrers = new ReferrerSet(this, Lists.transform(getRawReferrers(), referrer -> new JavaThingView(this, referrer)));
        }

        return referrers;
    }

    public BreadcrumbsView getBreadcrumbs() {
        return new BreadcrumbsView(this, path, null, null, getJavaClass(), getReferrers());
    }

    public boolean hasReferrers() {
        return !getReferrersStat().isEmpty();
    }

    public boolean hasReferees() {
        return !getRefereesStat().isEmpty();
    }

    private ImmutableMultiset<JavaThingView> getReferrersStat() {
        cacheData();
        return referrersStat;
    }

    private ImmutableMultiset<JavaThingView> getRefereesStat() {
        cacheData();
        return refereesStat;
    }

    public Iterable<JavaClassWithLinks> getReferrersStatEntries() {
        return getStatEntries(getReferrersStat());
    }

    public Iterable<JavaClassWithLinks> getRefereesStatEntries() {
        return getStatEntries(getRefereesStat());
    }

    private Iterable<JavaClassWithLinks> getStatEntries(ImmutableMultiset<JavaThingView> multiset) {
        return new StreamIterable<>(multiset.entrySet().stream()
                .sorted(Ordering.natural().reverse().onResultOf(Multiset.Entry::getCount))
                .map(JavaClassWithLinks::new));
    }

    private void cacheData() {
        if (referrersStat != null && refereesStat != null) {
            return;
        }

        final ImmutableSetMultimap.Builder<JavaThingView, JavaThingView> rfrBuilder = ImmutableSetMultimap.builder();
        final ImmutableSetMultimap.Builder<JavaThingView, JavaThingView> rfeBuilder = ImmutableSetMultimap.builder();

        for (final JavaHeapObject instance : Misc.getInstances(getJavaClass().toJavaClass(), false, getRawReferrers())) {
            if (instance.getId() == -1) {
                continue;
            }
            for (JavaHeapObject ref : instance.getReferers()) {
                JavaClass cl = ref.getClazz();
                if (cl == null) {
                     System.out.println("null class for " + ref);
                     continue;
                }
                rfrBuilder.put(new JavaThingView(this, cl), new JavaThingView(this, instance));
            }
            instance.visitReferencedObjects(obj -> rfeBuilder.put(new JavaThingView(this, obj.getClazz()), new JavaThingView(this, instance)));
        }

        referrersStat = rfrBuilder.build().keys();
        refereesStat = rfeBuilder.build().keys();
    }

    public class JavaClassWithLinks {
        private final JavaThingView clazz;
        private final int count;

        public JavaClassWithLinks(Multiset.Entry<JavaThingView> entry) {
            this.clazz = entry.getElement();
            this.count = entry.getCount();
        }

        public JavaThingView getClazz() {
            return clazz;
        }

        public int getCount() {
            return count;
        }

        public Link getTopLink() {
            return link("top", clazz, null, null);
        }

        public Link getChainLink() {
            return link("chain", getJavaClass(), getReferrers(), clazz);
        }

        public Link getRefsLink() {
            return link("refs", clazz, null, null);
        }

        private Link link(String label, JavaThingView clazz, ReferrerSet referrers, JavaThingView tail) {
            return new Link(RefsByTypeQuery.this, path, null, label, null, clazz, referrers, tail, null);
        }
    }
}
