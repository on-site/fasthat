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
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.*;

import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author      Bill Foote
 */


class InstancesCountQuery extends QueryHandler {
    private enum NonPlatformPredicate implements Predicate<JavaClass> {
        INSTANCE;

        @Override
        public boolean apply(JavaClass clazz) {
            return !PlatformClasses.isPlatformClass(clazz);
        }
    }

    private enum Sorters implements Function<JavaClass, Comparable<?>>,
            Comparator<JavaClass> {
        BY_INSTANCE_COUNT {
            @Override
            public Integer apply(JavaClass cls) {
                return cls.getInstancesCount(false);
            }
        },

        BY_ARRAY_TYPE {
            @Override
            public Boolean apply(JavaClass cls) {
                return cls.getName().startsWith("[");
            }
        },

        BY_NAME {
            @Override
            public String apply(JavaClass cls) {
                return cls.getName();
            }
        };

        private final Ordering<JavaClass> ordering;

        private Sorters() {
            this.ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(JavaClass lhs, JavaClass rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    private final boolean excludePlatform;

    public InstancesCountQuery(boolean excludePlatform) {
        this.excludePlatform = excludePlatform;
    }

    public void run() {
        if (excludePlatform) {
            startHtml("Instance Counts for All Classes (excluding platform)");
        } else {
            startHtml("Instance Counts for All Classes (including platform)");
        }

        JavaClass[] classes = snapshot.getClassesArray();
        if (excludePlatform) {
            classes = Collections2.filter(Arrays.asList(classes),
                    NonPlatformPredicate.INSTANCE).toArray(new JavaClass[0]);
        }
        Arrays.sort(classes, new Comparator<JavaClass>() {
            public int compare(JavaClass lhs, JavaClass rhs) {
                return ComparisonChain.start()
                        // Sort from biggest to smallest
                        .compare(rhs, lhs, Sorters.BY_INSTANCE_COUNT)
                        .compare(lhs, rhs, Sorters.BY_ARRAY_TYPE)
                        .compare(lhs, rhs, Sorters.BY_NAME)
                        .result();
            }
        });

        long totalSize = 0;
        long instances = 0;
        for (JavaClass clazz : classes) {
            int count = clazz.getInstancesCount(false);
            print("" + count);
            printAnchorStart();
            print("instances/" + encodeForURL(clazz));
            out.print("\"> ");
            if (count == 1) {
                print("instance");
            } else {
                print("instances");
            }
            out.print("</a> ");
            if (snapshot.getHasNewSet()) {
                int newInst = 0;
                for (JavaHeapObject obj : clazz.getInstances(false)) {
                    if (obj.isNew()) {
                        newInst++;
                    }
                }
                print("(");
                printAnchorStart();
                print("newInstances/" + encodeForURL(clazz));
                out.print("\">");
                print("" + newInst + " new");
                out.print("</a>) ");
            }
            print("of ");
            printClass(clazz);
            out.println("<br>");
            instances += count;
            totalSize += clazz.getTotalInstanceSize();
        }
        out.println("<h2>Total of " + instances + " instances occupying " + totalSize + " bytes.</h2>");

        out.println("<h2>Other Queries</h2>");
        out.println("<ul>");

        out.print("<li>");
        printAnchorStart();
        if (!excludePlatform) {
            out.print("showInstanceCounts/\">");
            print("Show instance counts for all classes (excluding platform)");
        } else {
            out.print("showInstanceCounts/includePlatform/\">");
            print("Show instance counts for all classes (including platform)");
        }
        out.println("</a>");

        out.print("<li>");
        printAnchorStart();
        out.print("allClassesWithPlatform/\">");
        print("Show All Classes (including platform)");
        out.println("</a>");

        out.print("<li>");
        printAnchorStart();
        out.print("\">");
        print("Show All Classes (excluding platform)");
        out.println("</a>");

        out.println("</ul>");

        endHtml();
    }


}
