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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.*;
import com.sun.tools.hat.internal.util.Misc;

import java.util.*;

/**
 * References by type summary
 *
 */
public class RefsByTypeQuery extends QueryHandler {
    private enum Sorters implements Function<Multiset.Entry<JavaClass>, Integer>,
            Comparator<Multiset.Entry<JavaClass>> {
        BY_COUNT {
            @Override
            public Integer apply(Multiset.Entry<JavaClass> entry) {
                return ~entry.getCount();
            }
        };

        private final Ordering<Multiset.Entry<JavaClass>> ordering;

        private Sorters() {
            ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(Multiset.Entry<JavaClass> lhs, Multiset.Entry<JavaClass> rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    public void run() {
        ClassResolver resolver = new ClassResolver(snapshot, true);
        JavaClass clazz = resolver.apply(query);
        Collection<JavaClass> referrers = Collections2.transform(
                params.get("referrer"), resolver);
        ImmutableSetMultimap.Builder<JavaClass, JavaHeapObject> rfrBuilder
                = ImmutableSetMultimap.builder();
        final ImmutableSetMultimap.Builder<JavaClass, JavaHeapObject> rfeBuilder
                = ImmutableSetMultimap.builder();
        for (final JavaHeapObject instance : Misc.getInstances(clazz, false, referrers)) {
            if (instance.getId() == -1) {
                continue;
            }
            for (JavaHeapObject ref : instance.getReferers()) {
                JavaClass cl = ref.getClazz();
                if (cl == null) {
                     System.out.println("null class for " + ref);
                     continue;
                }
                rfrBuilder.put(cl, instance);
            }
            instance.visitReferencedObjects(
                new AbstractJavaHeapObjectVisitor() {
                    public void visit(JavaHeapObject obj) {
                        rfeBuilder.put(obj.getClazz(), instance);
                    }
                }
            );
        } // for each instance

        startHtml("References by Type");
        out.println("<p align='center'>");
        printClass(clazz);
        if (clazz.getId() != -1) {
            out.println("[" + clazz.getIdString() + "]");
        }
        out.println("</p>");
        printBreadcrumbs(path, null, null, clazz, referrers, null);

        ImmutableMultiset<JavaClass> referrersStat = rfrBuilder.build().keys();
        if (!referrersStat.isEmpty()) {
            out.println("<h3 align='center'>Referrers by Type</h3>");
            print(referrersStat, clazz, referrers, true);
        }

        ImmutableMultiset<JavaClass> refereesStat = rfeBuilder.build().keys();
        if (!refereesStat.isEmpty()) {
            out.println("<h3 align='center'>Referees by Type</h3>");
            print(refereesStat, clazz, referrers, false);
        }

        endHtml();
    } // run

    private void print(Multiset<JavaClass> multiset, JavaClass primary,
            Collection<JavaClass> referrers, boolean supportsChaining) {
        out.println("<table border='1' align='center'>");
        List<Multiset.Entry<JavaClass>> entries = Lists.newArrayList(multiset.entrySet());
        Collections.sort(entries, Sorters.BY_COUNT);

        out.println("<tr><th>Class</th><th>Count</th></tr>");
        for (Multiset.Entry<JavaClass> entry : entries) {
            out.println("<tr><td>");
            JavaClass clazz = entry.getElement();
            printClass(clazz);
            if (supportsChaining) {
                out.printf(" (%s, %s)",
                        formatLink("top", clazz, null, null),
                        formatLink("chain", primary, referrers, clazz));
            } else {
                out.printf(" (%s)",
                        formatLink("refs", clazz, null, null));
            }
            out.println("</td><td>");
            out.println(entry.getCount());
            out.println("</td></tr>");
        }
        out.println("</table>");
    }

    private String formatLink(String label, JavaClass clazz,
            Collection<JavaClass> referrers, JavaClass tail) {
        return formatLink(path, null, label, null, clazz, referrers, tail, null);
    }
}
