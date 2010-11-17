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

import java.util.AbstractList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    //
    // Who we refer to.  This is heavily optimized for space, because it's
    // well worth trading a bit of speed for less swapping.
    // referers and referersLen go through two phases:  Building and
    // resolved.  When building, referers might have duplicates, but can
    // be appended to.  When resolved, referers has no duplicates or
    // empty slots.
    //
    private JavaHeapObject[] referers = null;
    private int referersLen = 0;        // -1 when resolved

    public abstract JavaClass getClazz();
    public abstract int getSize();
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

    //
    //  Eliminate duplicates from referers, and size the array exactly.
    // This sets us up to answer queries.  See the comments around the
    // referers data member for details.
    //
    void setupReferers() {
        if (referersLen > 1) {
            // Copy referers to set, screening out duplicates
            Set<JavaThing> set = new HashSet<JavaThing>();
            for (int i = 0; i < referersLen; i++) {
                set.add(referers[i]);
            }

            // Now copy into the array
            referers = set.toArray(new JavaHeapObject[set.size()]);
        }
        referersLen = -1;
    }


    /**
     * @return the id of this thing as hex string
     */
    public String getIdString() {
        return Misc.toHex(getId());
    }

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
        if (referersLen == 0) {
            referers = new JavaHeapObject[1];        // It was null
        } else if (referersLen == referers.length) {
            JavaHeapObject[] copy = new JavaHeapObject[(3 * (referersLen + 1)) / 2];
            System.arraycopy(referers, 0, copy, 0, referersLen);
            referers = copy;
        }
        referers[referersLen++] = other;
        // We just append to referers here.  Measurements have shown that
        // around 10% to 30% are duplicates, so it's better to just append
        // blindly and screen out all the duplicates at once.
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
    public List<JavaHeapObject> getReferers() {
        if (referersLen != -1) {
            throw new IllegalStateException("not resolved: " + getIdString());
        }
        return new AbstractList<JavaHeapObject>() {
            @Override
            public int size() {
                return referers != null ? referers.length : 0;
            }

            @Override
            public JavaHeapObject get(int index) {
                return referers[index];
            }
        };
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

    public boolean isHeapAllocated() {
        return true;
    }

}
