/*
 * Copyright (c) 2012, 2014 On-Site.com.
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

package com.sun.tools.hat.internal.lang.jruby17;

import java.util.Collection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaObject;

/**
 * Class model for JRuby. Mostly version-independent except for field
 * name differences.
 *
 * @author Chris K. Jester-Young
 */
public class JRubyClass extends com.sun.tools.hat.internal.lang.jruby.JRubyClass {
    private static class ClassCacheLoader extends CacheLoader<JavaObject, JRubyClass> {
        private final JRuby17 factory;

        public ClassCacheLoader(JRuby17 factory) {
            this.factory = factory;
        }

        @Override
        public JRubyClass load(JavaObject classObject) {
            return new JRubyClass(factory, classObject);
        }
    }

    private static class CacheCacheLoader extends CacheLoader<JRuby17, LoadingCache<JavaObject, JRubyClass>> {
        @Override
        public LoadingCache<JavaObject, JRubyClass> load(JRuby17 factory) {
            return CacheBuilder.newBuilder().softValues().build(
                    new ClassCacheLoader(factory));
        }
    }

    private static final LoadingCache<JRuby17, LoadingCache<JavaObject, JRubyClass>> CACHE
            = CacheBuilder.newBuilder().weakKeys().build(new CacheCacheLoader());

    private static final String[] BASE_NAME_FIELDS = {"baseName", "classId"};
    private static final String[] CACHED_NAME_FIELDS = {"cachedName",
            "calculatedName", "fullName"};
    private static final String[] ANONYMOUS_NAME_FIELDS = {"anonymousName",
            "bareName"};

    private final Supplier<ImmutableList<String>> propertyNamesSupplier
            = Suppliers.memoize(new PropertyNamesSupplier());

    private JRubyClass(JRuby17 factory, JavaObject classObject) {
        super(factory, classObject);
    }

    public static JRubyClass make(JRuby17 factory, JavaObject classObject) {
        return Models.findField(classObject, BASE_NAME_FIELDS) != null
                && Models.findField(classObject, CACHED_NAME_FIELDS) != null
                && Models.findField(classObject, ANONYMOUS_NAME_FIELDS) != null
                ? CACHE.getUnchecked(factory).getUnchecked(classObject) : null;
    }

    @Override
    protected LoadingCache<JavaObject, ? extends JRubyClass> getClassCache() {
        return CACHE.getUnchecked((JRuby17) getFactory());
    }

    private class PropertyNamesSupplier implements Supplier<ImmutableList<String>> {
        @Override
        public ImmutableList<String> get() {
            return ImmutableList.copyOf(Lists.transform(
                    Models.getFieldObjectArray(
                            Models.getFieldObject(getClassObject(), "variableTableManager"),
                            "variableNames", JavaObject.class),
                    Models.GetStringValue.INSTANCE));
        }
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertyNamesSupplier.get();
    }
}
