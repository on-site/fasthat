/*
 * Copyright © 2011, 2012, 2013 On-Site.com.
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
import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.sun.tools.hat.internal.lang.AbstractMapModel;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.common.HashCommon;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

public class JavaConcHash extends AbstractMapModel {
    private static class MapSupplier implements Supplier<ImmutableMap<JavaThing, JavaThing>> {
        private final List<JavaObject> segments;

        public MapSupplier(List<JavaObject> segments) {
            this.segments = segments;
        }

        @Override
        public ImmutableMap<JavaThing, JavaThing> get() {
            final ImmutableMap.Builder<JavaThing, JavaThing> builder = ImmutableMap.builder();
            for (JavaObject segment : segments) {
                List<JavaObject> table = Models.getFieldObjectArray(segment, "table",
                        JavaObject.class);
                if (table != null)
                    HashCommon.walkHashTable(table, "key", "value", "next", builder::put);
            }
            return builder.build();
        }
    }

    private final Supplier<ImmutableMap<JavaThing, JavaThing>> map;

    private JavaConcHash(OpenJDK factory, Supplier<ImmutableMap<JavaThing, JavaThing>> map) {
        super(factory);
        this.map = map;
    }

    public static JavaConcHash make(OpenJDK factory, JavaObject chm) {
        List<JavaObject> segments = Models.getFieldObjectArray(chm, "segments",
                JavaObject.class);
        return segments == null ? null
                : new JavaConcHash(factory, Suppliers.memoize(new MapSupplier(segments)));
    }

    @Override
    public Map<JavaThing, JavaThing> getMap() {
        return map.get();
    }
}