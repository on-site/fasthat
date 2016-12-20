/*
 * Copyright © 2012 On-Site.com.
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

package com.sun.tools.hat.internal.lang.jruby;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sun.tools.hat.internal.lang.ClassModel;
import com.sun.tools.hat.internal.lang.HasSimpleForm;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaObject;

/**
 * Class model for JRuby. Mostly version-independent except for field
 * name differences.
 *
 * @author Chris Jester-Young
 */
public class JRubyClass implements ClassModel, HasSimpleForm {
    private static final String[] BASE_NAME_FIELDS = {"baseName", "classId"};
    private static final String[] CACHED_NAME_FIELDS = {"cachedName",
        "calculatedName", "fullName"};
    private static final String[] ANONYMOUS_NAME_FIELDS = {"anonymousName",
        "bareName"};

    private final JRuby factory;
    private final JavaObject classObject;
    private final String baseNameField;
    private final String cachedNameField;
    private final String anonymousNameField;
    private final Supplier<String> nameSupplier
            = Suppliers.memoize(this::getNameImpl);
    private final Supplier<String> anonymousNameSupplier
            = Suppliers.memoize(this::getAnonymousNameImpl);
    private final Supplier<JRubyClass> realClassSupplier
            = Suppliers.memoize(this::getRealClassImpl);
    private final Supplier<ImmutableList<ClassModel>> superclassesSupplier
            = Suppliers.memoize(this::getSuperclassesImpl);
    private final Supplier<ImmutableList<String>> propertyNamesSupplier
            = Suppliers.memoize(this::getPropertyNamesImpl);

    protected JRubyClass(JRuby factory, JavaObject classObject) {
        this.factory = factory;
        this.classObject = classObject;
        baseNameField = Models.findField(classObject, BASE_NAME_FIELDS);
        cachedNameField = Models.findField(classObject, CACHED_NAME_FIELDS);
        anonymousNameField = Models.findField(classObject, ANONYMOUS_NAME_FIELDS);
    }

    @Override
    public JRuby getFactory() {
        return factory;
    }

    private boolean isAnonymous() {
        return Models.getFieldString(classObject, baseNameField) == null;
    }

    @Override
    public JavaObject getClassObject() {
        return classObject;
    }

    private boolean isClass() {
        return factory.isClassClass(classObject.getClazz());
    }

    // Based on RubyModule.calculateName()
    private String getNameImpl() {
        JavaObject objectClassObject = Models.getFieldObjectChain(classObject,
                "metaClass", "runtime", "objectClass");

        Deque<String> names = new ArrayDeque<>();
        for (JavaObject cls = classObject; cls != null && cls != objectClassObject;
             cls = Models.getFieldObject(cls, "parent")) {
            String name = Models.getFieldString(cls, baseNameField);
            if (name == null)
                name = factory.lookupClass(cls).getAnonymousName();
            names.addFirst(name);
        }
        return Joiner.on("::").join(names);
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

    // Based on RubyModule.calculateAnonymousName()
    private String getAnonymousNameImpl() {
        return String.format("#<%s:%#x>", isClass() ? "Class" : "Module",
                classObject.getId());
    }

    public String getAnonymousName() {
        return coalesce(Models.getFieldString(classObject, anonymousNameField),
                anonymousNameSupplier);
    }

    // Based on MetaClass.getRealClass()
    private JRubyClass getRealClassImpl() {
        JavaObject cls = classObject;
        while (cls != null && !factory.isClassClass(cls.getClazz())) {
            cls = Models.getFieldObject(cls, "superClass");
        }
        return factory.lookupClass(cls);
    }

    public JRubyClass getRealClass() {
        return realClassSupplier.get();
    }

    private ImmutableList<ClassModel> getSuperclassesImpl() {
        ImmutableList.Builder<ClassModel> builder = ImmutableList.builder();
        JavaObject cls = Models.getFieldObject(classObject, "superClass");
        while (cls != null) {
            if (factory.isClassClass(cls.getClazz())) {
                builder.add(factory.lookupClass(cls));
                break;
            } else if (factory.isModuleWrapperClass(cls.getClazz())) {
                JavaObject module = Models.getFieldObject(cls, "delegate");
                builder.add(factory.lookupClass(module));
            }
            cls = Models.getFieldObject(cls, "superClass");
        }
        return builder.build().reverse();
    }

    @Override
    public Collection<ClassModel> getSuperclasses() {
        return superclassesSupplier.get();
    }

    protected ImmutableList<String> getPropertyNamesImpl() {
        return ImmutableList.copyOf(Lists.transform(
                Models.getFieldObjectArray(getClassObject(), "variableNames", JavaObject.class),
                Models::getStringValue));
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertyNamesSupplier.get();
    }

    private static <T> T coalesce(T value, Supplier<? extends T> fallback) {
        return value != null ? value : fallback.get();
    }

    @Override
    public String getSimpleForm() {
        return '<' + (isClass() ? "class" : "module") + ':' + getSimpleName() + '>';
    }
}
