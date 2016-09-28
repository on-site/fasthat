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

import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.server.view.JavaThingView;
import com.sun.tools.hat.internal.util.StreamIterable;

import java.util.Collection;

/**
 *
 * @author      Bill Foote
 */


class InstancesCountQuery extends MustacheQueryHandler {
    private final boolean excludePlatform;
    private Collection<JavaThingView> classes;

    public InstancesCountQuery(boolean excludePlatform) {
        this.excludePlatform = excludePlatform;
    }

    public String getTitle() {
        if (excludePlatform) {
            return "Instance Counts for All Classes (excluding platform)";
        } else {
            return "Instance Counts for All Classes (including platform)";
        }
    }

    public boolean getExcludePlatform() {
        return excludePlatform;
    }

    public boolean hasNewSet() {
        return snapshot.getHasNewSet();
    }

    public Iterable<JavaThingView> getClasses() {
        return new StreamIterable<>(getClassesCollection().stream()
                .sorted(Ordering.natural().reverse().onResultOf(JavaThingView::getInstancesCountWithoutSubclasses)
                .compound(Ordering.natural().onResultOf(JavaThingView::isArrayClass))
                .compound(Ordering.natural().onResultOf(JavaThingView::getName))));
    }

    public long getInstances() {
        return getClassesCollection().stream().mapToLong(cls -> cls.getInstancesCountWithoutSubclasses()).sum();
    }

    public long getTotalSize() {
        return getClassesCollection().stream().mapToLong(JavaThingView::getTotalInstanceSize).sum();
    }

    private Collection<JavaThingView> getClassesCollection() {
        if (classes != null) {
            return classes;
        }

        Collection<JavaClass> result = snapshot.getClasses();

        if (excludePlatform) {
            result = Collections2.filter(result, cls -> !PlatformClasses.isPlatformClass(cls));
        }

        classes = Collections2.transform(result, cls -> new JavaThingView(this, cls));
        return classes;
    }
}
