/*
 * Copyright © 2016 On-Site.com.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this work. If not, see <http://www.gnu.org/licenses/>.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module. An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your
 * version of the library, but you are not obligated to do so. If you do
 * not wish to do so, delete this exception statement from your version.
 */
package com.sun.tools.hat.internal.server.view;

import com.sun.tools.hat.internal.annotations.ViewGetter;
import com.sun.tools.hat.internal.model.ReferenceChain;
import com.sun.tools.hat.internal.server.QueryHandler;

import java.util.Iterator;

/**
 * @author Mike Virata-Stone
 */
public class ReferenceChainView extends ViewModel {
    private final ReferenceChain reference;

    public ReferenceChainView(QueryHandler handler, ReferenceChain reference) {
        super(handler);
        this.reference = reference;
    }

    @ViewGetter
    public RootView getRoot() {
        return new RootView(handler, reference.getObj().getRoot());
    }

    @ViewGetter
    public JavaThingView getObject() {
        return new JavaThingView(handler, reference.getObj());
    }

    @ViewGetter
    public boolean hasNext() {
        return reference.getNext() != null;
    }

    @ViewGetter
    public String getDescriptionOfReference() {
        if (hasNext()) {
            return getObject().toJavaHeapObject().describeReferenceTo(reference.getNext().getObj(), handler.getSnapshot());
        }

        return null;
    }

    @ViewGetter
    public ReferenceChainIterable getReferences() {
        return new ReferenceChainIterable();
    }

    public class ReferenceChainIterable implements Iterable<ReferenceChainView> {
        @Override
        public ReferenceChainIterator iterator() {
            return new ReferenceChainIterator();
        }
    }

    public class ReferenceChainIterator implements Iterator<ReferenceChainView> {
        private ReferenceChainView next = ReferenceChainView.this;

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ReferenceChainView next() {
            ReferenceChainView result = next;

            if (result.reference.getNext() == null) {
                next = null;
            } else {
                next = new ReferenceChainView(handler, result.reference.getNext());
            }

            return result;
        }
    }
}
