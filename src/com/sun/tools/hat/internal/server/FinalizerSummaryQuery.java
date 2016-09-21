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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.*;
import java.util.*;

public class FinalizerSummaryQuery extends QueryHandler {
    @Override
    public void run() {
        startHtml("Finalizer Summary");

        out.println("<p align='center'>");
        out.println("<b><a href='/allClasses/'>All Classes (excluding platform)</a></b>");
        out.println("</p>");

        printFinalizerSummary(snapshot.getFinalizerObjects());
        endHtml();
    }

    private void printFinalizerSummary(Collection<? extends JavaHeapObject> objs) {
        int count = 0;
        Multiset<JavaClass> bag = HashMultiset.create();

        for (JavaHeapObject obj : objs) {
            count++;
            bag.add(obj.getClazz());
        }

        out.println("<p align='center'>");
        out.println("<b>");
        out.println("Total ");
        if (count != 0) {
            out.print("<a href='/finalizerObjects/'>instances</a>");
        } else {
            out.print("instances");
        }
        out.println(" pending finalization: ");
        out.print(count);
        out.println("</b></p><hr>");

        if (count == 0) {
            return;
        }

        // calculate and print histogram
        out.println("<table border=1 align=center>");
        out.println("<tr><th>Count</th><th>Class</th></tr>");
        bag.entrySet().stream().sorted(Ordering.natural().reverse()
                .onResultOf(entry -> entry.getCount())).forEach(entry -> {
            out.println("<tr><td>");
            out.println(entry.getCount());
            out.println("</td><td>");
            printClass(entry.getElement());
            out.println("</td><tr>");
        });
        out.println("</table>");
    }
}
