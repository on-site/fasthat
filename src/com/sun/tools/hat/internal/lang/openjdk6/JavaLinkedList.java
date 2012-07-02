/*
 * Copyright (c) 2011, 2012 On-Site.com.
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

package com.sun.tools.hat.internal.lang.openjdk6;

import java.util.Collection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.sun.tools.hat.internal.lang.CollectionModel;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

class JavaLinkedList extends CollectionModel {
    private static class ListSupplier implements Supplier<ImmutableList<JavaThing>> {
        private final JavaObject header;

        public ListSupplier(JavaObject header) {
            this.header = header;
        }

        @Override
        public ImmutableList<JavaThing> get() {
            ImmutableList.Builder<JavaThing> builder = ImmutableList.builder();
            for (JavaObject entry = Models.getFieldObject(header, "next");
                    entry != header; entry = Models.getFieldObject(entry, "next")) {
                if (entry == null)
                    return null;
                builder.add(entry.getField("element"));
            }
            return builder.build();
        }
    }

    private final Supplier<ImmutableList<JavaThing>> items;

    private JavaLinkedList(OpenJDK6 factory, Supplier<ImmutableList<JavaThing>> items) {
        super(factory);
        this.items = items;
    }

    public static JavaLinkedList make(OpenJDK6 factory, JavaObject list) {
        JavaObject header = Models.getFieldObject(list, "header");
        return header == null ? null
                : new JavaLinkedList(factory, Suppliers.memoize(new ListSupplier(header)));
    }

    @Override
    public Collection<JavaThing> getCollection() {
        return items.get();
    }
}
