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

import java.util.Arrays;
import java.util.Comparator;

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.*;

/**
 *
 * @author      Bill Foote
 */


class AllRootsQuery extends QueryHandler {
    private enum Sorters implements Function<Root, Comparable<?>>,
            Comparator<Root> {
        BY_TYPE {
            @Override
            public Integer apply(Root root) {
                return root.getType();
            }
        },

        BY_DESCRIPTION {
            @Override
            public String apply(Root root) {
                return root.getDescription();
            }
        };

        private final Ordering<Root> ordering;

        private Sorters() {
            this.ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(Root lhs, Root rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    public AllRootsQuery() {
    }

    public void run() {
        startHtml("All Members of the Rootset");

        Root[] roots = snapshot.getRootsArray();
        Arrays.sort(roots, new Comparator<Root>() {
            public int compare(Root left, Root right) {
                return ComparisonChain.start()
                        // More interesting values are *higher*
                        .compare(right, left, Sorters.BY_TYPE)
                        .compare(left, right, Sorters.BY_DESCRIPTION)
                        .result();
            }
        });

        int lastType = Root.INVALID_TYPE;

        for (Root root : roots) {
            if (root.getType() != lastType) {
                lastType = root.getType();
                out.print("<h2>");
                print(root.getTypeName() + " References");
                out.println("</h2>");
            }

            printRoot(root);
            if (root.getReferer() != null) {
                out.print("<small> (from ");
                printThingAnchorTag(root.getReferer().getId());
                print(root.getReferer().toString());
                out.print(")</a></small>");
            }
            out.print(" :<br>");

            JavaThing t = snapshot.findThing(root.getId());
            if (t != null) {    // It should always be
                print("--> ");
                printThing(t);
                out.println("<br>");
            }
        }

        out.println("<h2>Other Queries</h2>");
        out.println("<ul>");
        out.println("<li>");
        printAnchorStart();
        out.print("\">");
        print("Show All Classes");
        out.println("</a>");
        out.println("</ul>");

        endHtml();
    }
}
