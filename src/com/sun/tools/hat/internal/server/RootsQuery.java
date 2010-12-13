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


class RootsQuery extends QueryHandler {
    private enum Sorters implements Function<ReferenceChain, Comparable<?>>,
            Comparator<ReferenceChain> {
        BY_ROOT_TYPE {
            @Override
            public Integer apply(ReferenceChain chain) {
                return chain.getObj().getRoot().getType();
            }
        },

        BY_DEPTH {
            @Override
            public Integer apply(ReferenceChain chain) {
                return chain.getDepth();
            }
        };

        private final Ordering<ReferenceChain> ordering;

        private Sorters() {
            this.ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(ReferenceChain lhs, ReferenceChain rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    private final boolean includeWeak;

    public RootsQuery(boolean includeWeak) {
        this.includeWeak = includeWeak;
    }

    public void run() {
        long id = parseHex(query);
        JavaHeapObject target = snapshot.findThing(id);
        if (target == null) {
            startHtml("Object not found for rootset");
            error("object not found");
            endHtml();
            return;
        }
        if (includeWeak) {
            startHtml("Rootset references to " + target
                        + " (includes weak refs)");
        } else {
            startHtml("Rootset references to " + target
                        + " (excludes weak refs)");
        }
        out.flush();

        ReferenceChain[] refs
            = snapshot.rootsetReferencesTo(target, includeWeak);
        Arrays.sort(refs, new Comparator<ReferenceChain>() {
            public int compare(ReferenceChain left, ReferenceChain right) {
                return ComparisonChain.start()
                        // More interesting values are *higher*
                        .compare(right, left, Sorters.BY_ROOT_TYPE)
                        .compare(left, right, Sorters.BY_DEPTH)
                        .result();
            }
        });

        out.print("<h1>References to ");
        printThing(target);
        out.println("</h1>");
        int lastType = Root.INVALID_TYPE;
        for (ReferenceChain ref : refs) {
            Root root = ref.getObj().getRoot();
            if (root.getType() != lastType) {
                lastType = root.getType();
                out.print("<h2>");
                print(root.getTypeName() + " References");
                out.println("</h2>");
            }
            out.print("<h3>");
            printRoot(root);
            if (root.getReferer() != null) {
                out.print("<small> (from ");
                printThingAnchorTag(root.getReferer().getId());
                print(root.getReferer().toString());
                out.print(")</a></small>");

            }
            out.print(" :</h3>");
            while (ref != null) {
                ReferenceChain next = ref.getNext();
                JavaHeapObject obj = ref.getObj();
                print("--> ");
                printThing(obj);
                if (next != null) {
                    print(" (" +
                          obj.describeReferenceTo(next.getObj(), snapshot)
                          + ":)");
                }
                out.println("<br>");
                ref = next;
            }
        }

        out.println("<h2>Other queries</h2>");

        if (includeWeak) {
            printAnchorStart();
            out.print("roots/");
            printHex(id);
            out.print("\">");
            out.println("Exclude weak refs</a><br>");
            endHtml();
        }

        if (!includeWeak) {
            printAnchorStart();
            out.print("allRoots/");
            printHex(id);
            out.print("\">");
            out.println("Include weak refs</a><br>");
        }
    }

}
