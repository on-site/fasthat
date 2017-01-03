/*
 * Copyright © 2011, 2012 On-Site.com.
 * Copyright © 2015 Chris Jester-Young.
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

package com.sun.tools.hat.internal.lang.openjdk;

import java.util.List;

import com.google.common.collect.ImmutableMap;

import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.common.HashCommon;
import com.sun.tools.hat.internal.lang.common.SimpleMapModel;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.util.Suppliers;

public class JavaHash extends SimpleMapModel {
    private static ImmutableMap<JavaThing, JavaThing> getMapImpl(Iterable<JavaObject> table) {
        final ImmutableMap.Builder<JavaThing, JavaThing> builder = ImmutableMap.builder();
        HashCommon.walkHashTable(table, "key", "value", "next", builder::put);
        return builder.build();
    }

    private JavaHash(ModelFactory factory, Iterable<JavaObject> table) {
        super(factory, Suppliers.memoize(() -> getMapImpl(table)));
    }

    public static JavaHash make(ModelFactory factory, JavaObject hash) {
        List<JavaObject> table = Models.getFieldObjectArray(hash, "table", JavaObject.class);
        return table == null ? null : new JavaHash(factory, table);
    }
}
