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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.ReferenceChain;
import com.sun.tools.hat.internal.server.view.JavaThingView;
import com.sun.tools.hat.internal.server.view.ReferenceChainSet;
import com.sun.tools.hat.internal.util.StreamIterable;

/**
 *
 * @author      Bill Foote
 */


class RootsQuery extends MustacheQueryHandler {
    private JavaThingView target;

    public boolean getIncludeWeak() {
        return params.containsKey("weak");
    }

    public JavaThingView getTarget() {
        if (target == null) {
            target = new JavaThingView(this, snapshot.findThing(parseHex(query)));
        }

        return target;
    }

    public Iterable<ReferenceChainSet> getReferenceChainSets() {
        // More interesting values are *higher*
        return new StreamIterable<>(Multimaps.<Integer, ReferenceChain>index(snapshot.rootsetReferencesTo(getTarget().toJavaHeapObject(), getIncludeWeak()), chain -> chain.getObj().getRoot().getType())
                .asMap().entrySet().stream()
                .sorted(Ordering.natural().reverse().onResultOf(entry -> entry.getKey()))
                .map(entry -> new ReferenceChainSet(this, entry)));
    }
}