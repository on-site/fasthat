/*
 * Copyright (c) 2012 On-Site.com.
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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sun.tools.hat.internal.lang.ClassModel;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaObject;

/**
 * Class model for JRuby. Mostly version-independent except for field
 * name differences.
 *
 * @author Chris K. Jester-Young
 */
public class JRubyClass extends ClassModel {
    private static class ClassCacheLoader extends CacheLoader<JavaObject, JRubyClass> {
        private final JRuby factory;

        public ClassCacheLoader(JRuby factory) {
            this.factory = factory;
        }

        @Override
        public JRubyClass load(JavaObject classObject) {
            return new JRubyClass(factory, classObject);
        }
    }

    private static class CacheCacheLoader extends CacheLoader<JRuby, LoadingCache<JavaObject, JRubyClass>> {
        @Override
        public LoadingCache<JavaObject, JRubyClass> load(JRuby factory) {
            return CacheBuilder.newBuilder().softValues().build(
                    new ClassCacheLoader(factory));
        }
    }

    private static final LoadingCache<JRuby, LoadingCache<JavaObject, JRubyClass>> CACHE
            = CacheBuilder.newBuilder().weakKeys().build(new CacheCacheLoader());

    private static final String[] BASE_NAME_FIELDS = {"baseName", "classId"};
    private static final String[] CACHED_NAME_FIELDS = {"cachedName",
        "calculatedName", "fullName"};
    private static final String[] ANONYMOUS_NAME_FIELDS = {"anonymousName",
        "bareName"};

    private final JavaObject classObject;
    private final String baseNameField;
    private final String cachedNameField;
    private final String anonymousNameField;
    private final Supplier<String> nameSupplier
            = Suppliers.memoize(new NameSupplier());
    private final Supplier<String> anonymousNameSupplier
            = Suppliers.memoize(new AnonymousNameSupplier());
    private final Supplier<ClassModel> realClassSupplier
            = Suppliers.memoize(new RealClassSupplier());
    private final Supplier<ImmutableList<ClassModel>> superclassesSupplier
            = Suppliers.memoize(new SuperclassesSupplier());
    private final Supplier<ImmutableList<String>> propertyNamesSupplier
            = Suppliers.memoize(new PropertyNamesSupplier());

    private JRubyClass(JRuby factory, JavaObject classObject) {
        super(factory);
        this.classObject = classObject;
        baseNameField = Models.findField(classObject, BASE_NAME_FIELDS);
        cachedNameField = Models.findField(classObject, CACHED_NAME_FIELDS);
        anonymousNameField = Models.findField(classObject, ANONYMOUS_NAME_FIELDS);
    }

    public static JRubyClass make(JRuby factory, JavaObject classObject) {
        return Models.findField(classObject, BASE_NAME_FIELDS) != null
                && Models.findField(classObject, CACHED_NAME_FIELDS) != null
                && Models.findField(classObject, ANONYMOUS_NAME_FIELDS) != null
                ? CACHE.getUnchecked(factory).getUnchecked(classObject) : null;
    }

    private LoadingCache<JavaObject, JRubyClass> getClassCache() {
        return CACHE.getUnchecked((JRuby) getFactory());
    }

    private boolean isAnonymous() {
        return Models.getFieldString(classObject, baseNameField) == null;
    }

    private class NameSupplier implements Supplier<String> {
        // Based on RubyModule.calculateName()
        @Override
        public String get() {
            Deque<String> names = new ArrayDeque<String>();
            LoadingCache<JavaObject, JRubyClass> cache = getClassCache();
            JavaObject cls = classObject;
            do {
                String name = Models.getFieldString(cls, baseNameField);
                if (name == null)
                    name = cache.getUnchecked(cls).getAnonymousName();
                names.addFirst(name);
                cls = Models.getFieldObject(cls, "parent");
            } while (cls != null && cls != getObjectClassObject());
            return Joiner.on("::").join(names);
        }

        private JavaObject getObjectClassObject() {
            JavaObject eigenclass = Models.getFieldObject(classObject, "metaClass");
            JavaObject runtime = Models.getFieldObject(eigenclass, "runtime");
            return Models.getFieldObject(runtime, "objectClass");
        }
    }

    @Override
    public String getName() {
        return isAnonymous() ? getAnonymousName()
                : coalesce(Models.getFieldString(classObject, cachedNameField),
                        nameSupplier);
    }

    @Override
    public String getSimpleName() {
        return isAnonymous() ? getAnonymousName()
                : Models.getFieldString(classObject, baseNameField);
    }

    private class AnonymousNameSupplier implements Supplier<String> {
        // Based on RubyModule.calculateAnonymousName()
        @Override
        public String get() {
            JRuby factory = (JRuby) getFactory();
            boolean isClass = classObject.getClazz() == factory.getClassClass();
            return String.format("#<%s:%#x>", isClass ? "Class" : "Module",
                    classObject.getId());
        }
    }

    public String getAnonymousName() {
        return coalesce(Models.getFieldString(classObject, anonymousNameField),
                anonymousNameSupplier);
    }

    private class RealClassSupplier implements Supplier<ClassModel> {
        // Based on MetaClass.getRealClass()
        @Override
        public ClassModel get() {
            JRuby factory = (JRuby) getFactory();
            JavaClass classClass = factory.getClassClass();
            JavaObject cls = classObject;
            while (cls != null && cls.getClazz() != classClass) {
                cls = Models.getFieldObject(cls, "superClass");
            }
            return getClassCache().getUnchecked(cls);
        }
    }

    public ClassModel getRealClass() {
        return realClassSupplier.get();
    }

    private class SuperclassesSupplier implements Supplier<ImmutableList<ClassModel>> {
        @Override
        public ImmutableList<ClassModel> get() {
            ImmutableList.Builder<ClassModel> builder = ImmutableList.builder();
            LoadingCache<JavaObject, JRubyClass> cache = getClassCache();
            JavaObject cls = Models.getFieldObject(classObject, "superClass");
            while (cls != null) {
                JRuby factory = (JRuby) getFactory();
                if (cls.getClazz() == factory.getClassClass()) {
                    builder.add(cache.getUnchecked(cls));
                    break;
                } else if (cls.getClazz() == factory.getModuleWrapperClass()) {
                    JavaObject module = Models.getFieldObject(cls, "delegate");
                    builder.add(cache.getUnchecked(module));
                }
                cls = Models.getFieldObject(cls, "superClass");
            }
            return builder.build().reverse();
        }
    }

    @Override
    public Collection<ClassModel> getSuperclasses() {
        return superclassesSupplier.get();
    }

    private class PropertyNamesSupplier implements Supplier<ImmutableList<String>> {
        @Override
        public ImmutableList<String> get() {
            return ImmutableList.copyOf(Lists.transform(
                    Models.getFieldObjectArray(classObject, "variableNames", JavaObject.class),
                    Models.GetStringValue.INSTANCE));
        }
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertyNamesSupplier.get();
    }

    private static <T> T coalesce(T value, Supplier<? extends T> fallback) {
        return value != null ? value : fallback.get();
    }
}
