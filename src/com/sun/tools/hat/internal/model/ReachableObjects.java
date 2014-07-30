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

package com.sun.tools.hat.internal.model;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Ordering;

/**
 * @author      A. Sundararajan
 */

public class ReachableObjects {
    public ReachableObjects(JavaHeapObject root,
                            final ReachableExcludes excludes) {
        this.root = root;

        final Set<JavaHeapObject> bag = new HashSet<>();
        final Set<String> fieldsExcluded = new HashSet<>();
        final Set<String> fieldsUsed = new HashSet<>();
        JavaHeapObjectVisitor visitor = new AbstractJavaHeapObjectVisitor() {
            @Override
            public void visit(JavaHeapObject t) {
                // Size is zero for things like integer fields
                if (t != null && t.getSize() > 0 && !bag.contains(t)) {
                    bag.add(t);
                    t.visitReferencedObjects(this);
                }
            }

            @Override
            public boolean mightExclude() {
                return excludes != null;
            }

            @Override
            public boolean exclude(JavaClass clazz, JavaField f) {
                if (excludes == null) {
                    return false;
                }
                String nm = clazz.getName() + "." + f.getName();
                if (excludes.isExcluded(nm)) {
                    fieldsExcluded.add(nm);
                    return true;
                } else {
                    fieldsUsed.add(nm);
                    return false;
                }
            }
        };
        // Put the closure of root and all objects reachable from root into
        // bag (depth first), but don't include root:
        visitor.visit(root);
        bag.remove(root);

        this.reachables = bag.stream().sorted(Ordering.natural().reverse().onResultOf(JavaThing::getSize)
                .compound(Ordering.natural())).toArray(JavaThing[]::new);

        this.totalSize = bag.stream().mapToLong(JavaThing::getSize).sum()
                + root.getSize();

        excludedFields = getElements(fieldsExcluded);
        usedFields = getElements(fieldsUsed);
    }

    public JavaHeapObject getRoot() {
        return root;
    }

    public JavaThing[] getReachables() {
        return reachables;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String[] getExcludedFields() {
        return excludedFields;
    }

    public String[] getUsedFields() {
        return usedFields;
    }

    private static String[] getElements(Set<String> set) {
        return set.stream().sorted().toArray(String[]::new);
    }

    private final JavaHeapObject root;
    private final JavaThing[] reachables;
    private final String[]  excludedFields;
    private final String[]  usedFields;
    private final long totalSize;
}
