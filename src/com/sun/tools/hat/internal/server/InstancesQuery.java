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

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sun.tools.hat.internal.model.*;
import com.sun.tools.hat.internal.util.Misc;

/**
 *
 * @author      Bill Foote
 */


class InstancesQuery extends QueryHandler {

    private final boolean includeSubclasses;
    private final boolean newObjects;

    public InstancesQuery(boolean includeSubclasses) {
        this(includeSubclasses, false);
    }

    public InstancesQuery(boolean includeSubclasses, boolean newObjects) {
        this.includeSubclasses = includeSubclasses;
        this.newObjects = newObjects;
    }

    public void run() {
        ClassResolver resolver = new ClassResolver(snapshot, true);
        JavaClass clazz = resolver.apply(query);
        List<JavaClass> referrers = Lists.transform(
                params.get("referrer"), resolver);
        boolean referee = Boolean.parseBoolean(Iterables.getOnlyElement(
                params.get("referee"), "false"));
        String instancesOf;
        if (newObjects)
            instancesOf = referee ? "New referees" : "New instances";
        else
            instancesOf = referee ? "Referees" : "Instances";
        startHtml(String.format("%s of %s%s", instancesOf, clazz.getName(),
                includeSubclasses ? " (including subclasses)" : ""));
        if (referrers.isEmpty()) {
            out.print("<strong>");
            printClass(clazz);
            out.print("</strong><br><br>");
        } else {
            printBreadcrumbs(path, null, null, clazz, referrers, null);
        }
        Collection<JavaHeapObject> objects = Misc.getInstances(clazz,
                includeSubclasses, referrers);
        if (referee) {
            int size = referrers.size();
            JavaClass prev = size > 1 ? referrers.get(size - 2) : clazz;
            objects = Misc.getRefereesByClass(objects, prev);
        }
        long totalSize = 0;
        long instances = 0;
        for (JavaHeapObject obj : objects) {
            if (newObjects && !obj.isNew())
                continue;
            printThing(obj);
            out.println("<br>");
            totalSize += obj.getSize();
            instances++;
        }
        out.println("<h2>Total of " + instances + " instances occupying " + totalSize + " bytes.</h2>");
        endHtml();
    }
}
