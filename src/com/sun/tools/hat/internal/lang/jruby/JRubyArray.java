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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.sun.tools.hat.internal.lang.CollectionModel;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaObjectArray;
import com.sun.tools.hat.internal.model.JavaThing;

public class JRubyArray extends CollectionModel {
    private enum GetObjectArrayElements implements Function<JavaObjectArray,
            ImmutableList<JavaThing>> {
        INSTANCE;

        @Override
        public ImmutableList<JavaThing> apply(JavaObjectArray arr) {
            return ImmutableList.copyOf(arr.getElements());
        }
    }

    private static final Map<JavaObjectArray, ImmutableList<JavaThing>> ELEMENT_CACHE
            = new MapMaker().softKeys().makeComputingMap(GetObjectArrayElements.INSTANCE);

    private final Collection<JavaThing> value;

    private JRubyArray(Collection<JavaThing> value) {
        this.value = value;
    }

    public static JRubyArray make(JavaObject obj) {
        JavaObjectArray arr = Models.getFieldThing(obj, "values", JavaObjectArray.class);
        JavaInt begin = Models.getFieldThing(obj, "begin", JavaInt.class);
        JavaInt length = Models.getFieldThing(obj, "realLength", JavaInt.class);
        if (arr == null || begin == null || length == null)
            return null;
        List<JavaThing> elements = ELEMENT_CACHE.get(arr);
        return new JRubyArray(elements.subList(begin.value, begin.value + length.value));
    }

    @Override
    public Collection<JavaThing> getCollection() {
        return value;
    }
}
