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
import java.util.Set;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.server.view.BreadcrumbsView;
import com.sun.tools.hat.internal.server.view.JavaThingView;
import com.sun.tools.hat.internal.server.view.ReferrerSet;
import com.sun.tools.hat.internal.util.Misc;

/**
 *
 * @author      Bill Foote
 */


class InstancesQuery extends MustacheQueryHandler {
    private final boolean includeSubclasses;
    private final boolean newObjects;
    private JavaThingView javaClass;
    private ReferrerSet referrers;
    private Collection<JavaThingView> objects;

    public InstancesQuery(boolean includeSubclasses, boolean newObjects) {
        this.includeSubclasses = includeSubclasses;
        this.newObjects = newObjects;
    }

    private JavaThingView getJavaClass() {
        if (javaClass == null) {
            javaClass = new JavaThingView(this, resolveClass(query, true));
        }

        return javaClass;
    }

    private boolean isReferee() {
        return Boolean.parseBoolean(Iterables.getOnlyElement(params.get("referee"), "false"));
    }

    public String getTitle() {
        return String.format("%s%s of %s%s",
                             newObjects ? "New " : "",
                             isReferee() ? "Referees" : "Instances",
                             getJavaClass().getName(),
                             includeSubclasses ? " (including subclasses)" : "");
    }

    public BreadcrumbsView getBreadcrumbs() {
        return new BreadcrumbsView(this, path, getJavaClass(), getReferrers());
    }

    public ReferrerSet getReferrers() {
        if (referrers == null) {
            List<JavaClass> classes = Lists.transform(params.get("referrer"), referrer -> resolveClass(referrer, false));
            referrers = new ReferrerSet(this, Lists.transform(classes, referrer -> new JavaThingView(this, referrer)));
        }

        return referrers;
    }

    public Collection<JavaThingView> getObjects() {
        if (objects == null) {
            List<JavaThingView> referrers = getReferrers().getReferrers();
            Set<JavaHeapObject> result = Misc.getInstances(getJavaClass().toJavaClass(), includeSubclasses, Lists.transform(referrers, JavaThingView::toJavaClass));

            if (isReferee()) {
                int size = referrers.size();
                JavaClass prev = size > 1 ? referrers.get(size - 2).toJavaClass() : getJavaClass().toJavaClass();
                result = Misc.getRefereesByClass(result, prev);
            }

            if (newObjects) {
                result = Sets.filter(result, JavaHeapObject::isNew);
            }

            objects = ImmutableSet.copyOf(Collections2.transform(result, obj -> new JavaThingView(this, obj)));
        }

        return objects;
    }

    public long getNumInstances() {
        return getObjects().size();
    }

    public long getTotalSize() {
        return getObjects().stream().mapToLong(JavaThingView::getSize).sum();
    }
}
