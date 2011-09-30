/*
 * Copyright (c) 2011 On-Site.com.
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

package com.sun.tools.hat.internal.lang.jruby;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.hat.internal.lang.MapModel;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaObject;

public class JRubyHash extends MapModel {
    private final ImmutableMap<JavaObject, JavaObject> map;

    private JRubyHash(ImmutableMap<JavaObject, JavaObject> map) {
        this.map = map;
    }

    public static JRubyHash make(JavaObject hash) {
        List<JavaObject> table = Models.getFieldObjectArray(hash, "table", JavaObject.class);
        if (table == null)
            return null;
        final ImmutableMap.Builder<JavaObject, JavaObject> builder = ImmutableMap.builder();
        iterateTable(table, "key", "value", "next", new KeyValueVisitor() {
            @Override
            public void visit(JavaObject key, JavaObject value) {
                builder.put(key, value);
            }
        });
        return new JRubyHash(builder.build());
    }

    @Override
    public Map<JavaObject, JavaObject> getMap() {
        return map;
    }

    public interface KeyValueVisitor {
        void visit(JavaObject key, JavaObject value);
    }

    public static void iterateTable(List<JavaObject> table, String keyField,
            String valueField, String nextField, KeyValueVisitor visitor) {
        for (JavaObject element : table) {
            for (JavaObject bucket = element; bucket != null;
                    bucket = Models.getFieldObject(bucket, nextField)) {
                visitor.visit(Models.getFieldObject(bucket, keyField),
                        Models.getFieldObject(bucket, valueField));
            }
        }
    }
}
