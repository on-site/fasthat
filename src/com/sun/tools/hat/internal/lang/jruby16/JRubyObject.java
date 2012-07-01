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

package com.sun.tools.hat.internal.lang.jruby16;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.ObjectModel;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

class JRubyObject extends ObjectModel {
    private static class GetVariableNames extends CacheLoader<JavaObject, ImmutableList<String>> {
        @Override
        public ImmutableList<String> load(JavaObject rubyClass) {
            ImmutableList<JavaObject> names = Models.getFieldObjectArray(rubyClass,
                    "variableNames", JavaObject.class);
            return ImmutableList.copyOf(Lists.transform(names,
                    Models.GetStringValue.INSTANCE));
        }
    }

    private static final LoadingCache<JavaObject, ImmutableList<String>> VARIABLE_NAME_CACHE
            = CacheBuilder.newBuilder().softValues().build(new GetVariableNames());

    private static class PropertiesSupplier implements Supplier<ImmutableMap<String, JavaThing>> {
        private final JavaObject obj;
        private final JavaObject rubyClass;

        public PropertiesSupplier(JavaObject obj, JavaObject rubyClass) {
            this.obj = obj;
            this.rubyClass = rubyClass;
        }

        @Override
        public ImmutableMap<String, JavaThing> get() {
            List<String> names = VARIABLE_NAME_CACHE.getUnchecked(rubyClass);
            if (names.isEmpty())
                return ImmutableMap.of();
            List<JavaThing> values = Models.getFieldObjectArray(obj, "varTable", JavaThing.class);
            ImmutableMap.Builder<String, JavaThing> builder = ImmutableMap.builder();
            Iterator<JavaThing> iter = values.iterator();
            for (String name : names) {
                if (!iter.hasNext())
                    break;
                builder.put(name, iter.next());
            }
            return builder.build();
        }
    }

    private final JavaObject obj;
    private final Supplier<ImmutableMap<String, JavaThing>> properties;

    public JRubyObject(JavaObject obj) {
        this.obj = obj;
        this.properties = Suppliers.memoize(new PropertiesSupplier(obj, getClassObject()));
    }

    @Override
    public String getClassName() {
        JavaObject cls = getClassObject();
        String name = Models.getFieldString(cls, "classId");
        return name != null ? name : "#<Class:" + cls.getIdString() + ">";
    }

    @Override
    public JavaObject getClassObject() {
        return Models.getFieldObject(obj, "metaClass");
    }

    @Override
    public Map<String, JavaThing> getProperties() {
        return properties.get();
    }
}
