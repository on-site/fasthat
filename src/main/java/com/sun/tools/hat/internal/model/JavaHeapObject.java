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

import com.google.common.collect.ImmutableSet;
import com.sun.tools.hat.internal.util.Misc;


/**
 *
 * @author      Bill Foote
 */

/**
 * Represents an object that's allocated out of the Java heap.  It occupies
 * memory in the VM, and is the sort of thing that in a JDK 1.1 VM had
 * a handle.  It can be a
 * JavaClass, a JavaObjectArray, a JavaValueArray or a JavaObject.
 */

public abstract class JavaHeapObject extends JavaThing {
    private ImmutableSet.Builder<JavaHeapObject> builder = ImmutableSet.builder();
    private ImmutableSet<JavaHeapObject> referers;

    public abstract JavaClass getClazz();
    @Override public abstract int getSize();
    public abstract long getId();

    /**
     * Do any initialization this thing needs after its data is read in.
     * Subclasses that override this should call super.resolve().
     */
    public void resolve(Snapshot snapshot) {
        StackTrace trace = snapshot.getSiteTrace(this);
        if (trace != null) {
            trace.resolve(snapshot);
        }
    }

    void setupReferers() {
        if (referers == null) {
            referers = builder.build();
            builder = null;
        }
    }


    /**
     * @return the id of this thing as hex string
     */
    public String getIdString() {
        return Misc.toHex(getId());
    }

    @Override
    public String toString() {
        return getClazz().getName() + "@" + getIdString();
    }

    /**
     * @return the StackTrace of the point of allocation of this object,
     *          or null if unknown
     */
    public StackTrace getAllocatedFrom() {
        return getClazz().getSiteTrace(this);
    }

    public boolean isNew() {
        return getClazz().isNew(this);
    }

    void setNew(boolean flag) {
        getClazz().setNew(this, flag);
    }

    /**
     * Tell the visitor about all of the objects we refer to
     */
    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
        v.visit(getClazz());
    }

    void addReferenceFrom(JavaHeapObject other) {
        builder.add(other);
    }

    void addReferenceFromRoot(Root r) {
        getClazz().addReferenceFromRoot(r, this);
    }

    /**
     * If the rootset includes this object, return a Root describing one
     * of the reasons why.
     */
    public Root getRoot() {
        return getClazz().getRoot(this);
    }

    /**
     * Tell who refers to us.
     *
     * @return a list of JavaHeapObject instances
     */
    public ImmutableSet<JavaHeapObject> getReferers() {
        if (referers == null) {
            throw new IllegalStateException("not resolved: " + getIdString());
        }
        return referers;
    }

    /**
     * Given other, which the caller promises is in referers, determines if
     * the reference is only a weak reference.
     */
    public boolean refersOnlyWeaklyTo(Snapshot ss, JavaThing other) {
        return false;
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
    public String describeReferenceTo(JavaThing target, Snapshot ss) {
        return "??";
    }

    @Override
    public boolean isHeapAllocated() {
        return true;
    }

}
