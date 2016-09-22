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

import java.util.Map;
import java.util.function.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.ScalarModel;
import com.sun.tools.hat.internal.lang.common.SimpleScalarModel;
import com.sun.tools.hat.internal.lang.openjdk.JavaHash;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.Snapshot;

/**
 * Common, version-independent components of version-specific JRuby
 * model factories.
 *
 * @author Chris Jester-Young
 */
public abstract class JRuby implements ModelFactory {
    private final JavaThing nullThing;
    private final JavaClass constantsClass;
    private final JavaClass classClass;
    private final JavaClass moduleWrapperClass;
    private final JavaThing never;
    private final JavaThing undef;

    private final ScalarModel nilScalar = new SimpleScalarModel(this, "nil");
    private final ScalarModel falseScalar = new SimpleScalarModel(this, "false");
    private final ScalarModel trueScalar = new SimpleScalarModel(this, "true");
    private final ScalarModel neverScalar = new SimpleScalarModel(this, "<never>");
    private final ScalarModel undefScalar = new SimpleScalarModel(this, "<undef>");

    private final Map<JavaClass, Function<JavaObject, Model>> dispatchMap;
    private final LoadingCache<JavaObject, JRubyClass> classCache
            = CacheBuilder.newBuilder().softValues().build(CacheLoader.from(this::makeClassRaw));

    public JRuby(Snapshot snapshot) {
        Function<String, JavaClass> lookup = name -> Models.grabClass(snapshot, name);
        nullThing = snapshot.getNullThing();
        constantsClass = lookup.apply("org.jruby.runtime.Constants");
        classClass = lookup.apply("org.jruby.RubyClass");
        moduleWrapperClass = lookup.apply("org.jruby.IncludedModuleWrapper");
        JavaClass basicObjectClass = lookup.apply("org.jruby.RubyBasicObject");
        never = basicObjectClass.getStaticField("NEVER");
        undef = basicObjectClass.getStaticField("UNDEF");

        dispatchMap = new ImmutableMap.Builder<JavaClass, Function<JavaObject, Model>>()
                .put(lookup.apply("org.jruby.MetaClass"), this::lookupClass)
                .put(classClass, this::lookupClass)
                .put(lookup.apply("org.jruby.RubyModule"), this::lookupClass)
                .put(lookup.apply("org.jruby.RubyString"), this::makeString)
                .put(lookup.apply("org.jruby.RubySymbol"), this::makeSymbol)
                .put(lookup.apply("org.jruby.RubyNil"), this::makeNil)
                .put(lookup.apply("org.jruby.RubyBoolean$False"), this::makeFalse)
                .put(lookup.apply("org.jruby.RubyBoolean$True"), this::makeTrue)
                .put(lookup.apply("org.jruby.RubyBoolean"), this::makeBoolean)
                .put(basicObjectClass, this::makeBasicObject)
                .put(lookup.apply("org.jruby.RubyFixnum"), this::makeFixnum)
                .put(lookup.apply("org.jruby.RubyFloat"), this::makeFloat)
                .put(lookup.apply("org.jruby.RubyObject"), this::makeObject)
                .put(lookup.apply("org.jruby.RubyStruct"), this::makeStruct)
                .put(lookup.apply("org.jruby.RubyArray"), this::makeArray)
                .put(lookup.apply("org.jruby.RubyHash"), this::makeHash)
                // TODO Implement other JRuby types.
                .build();
    }

    @Override
    public Map<JavaClass, Function<JavaObject, Model>> getDispatchMap() {
        return dispatchMap;
    }

    protected JRubyClass makeClassRaw(JavaObject obj) {
        return new JRubyClass(this, obj);
    }

    public final JRubyClass lookupClass(JavaObject obj) {
        return classCache.getUnchecked(obj);
    }

    protected JRubyString makeString(JavaObject obj) {
        return JRubyString.make(this, obj);
    }

    protected JRubySymbol makeSymbol(JavaObject obj) {
        return JRubySymbol.make(this, obj);
    }

    protected ScalarModel makeNil(JavaObject obj) {
        return nilScalar;
    }

    protected ScalarModel makeFalse(JavaObject obj) {
        return falseScalar;
    }

    protected ScalarModel makeTrue(JavaObject obj) {
        return trueScalar;
    }

    protected ScalarModel makeBoolean(JavaObject obj) {
        JavaInt flags = Models.getFieldThing(obj, "flags", JavaInt.class);
        return flags == null ? null
                : (flags.value & 1) != 0 ? falseScalar : trueScalar;
    }

    protected ScalarModel makeBasicObject(JavaObject obj) {
        return obj == never ? neverScalar
                : obj == undef ? undefScalar
                : null;
    }

    protected JRubyFixnum makeFixnum(JavaObject obj) {
        return JRubyFixnum.make(this, obj);
    }

    protected JRubyFloat makeFloat(JavaObject obj) {
        return JRubyFloat.make(this, obj);
    }

    protected JRubyObject makeObject(JavaObject obj) {
        return new JRubyObject(this, obj);
    }

    protected JRubyStruct makeStruct(JavaObject obj) {
        return new JRubyStruct(this, obj);
    }

    protected JRubyArray makeArray(JavaObject obj) {
        return JRubyArray.make(this, obj);
    }

    protected JavaHash makeHash(JavaObject obj) {
        return JavaHash.make(this, obj);
    }

    public JavaThing getNullThing() {
        return nullThing;
    }

    public boolean isClassClass(JavaClass cls) {
        return cls == classClass;
    }

    public boolean isModuleWrapperClass(JavaClass cls) {
        return cls == moduleWrapperClass;
    }

    @Override
    public String toString() {
        return String.format("JRuby %s (%s)",
                Models.getStaticString(constantsClass, "VERSION"),
                Models.getStaticString(constantsClass, "REVISION"));
    }
}
