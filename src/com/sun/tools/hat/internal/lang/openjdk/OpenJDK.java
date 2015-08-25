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

import com.google.common.collect.ImmutableMap;
import com.sun.tools.hat.internal.lang.CollectionModel;
import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaObjectArray;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.JavaValueArray;
import com.sun.tools.hat.internal.model.Snapshot;

import java.util.Map;
import java.util.function.Function;

/**
 * Model factory for OpenJDK.
 *
 * @author Chris Jester-Young
 */
public abstract class OpenJDK implements ModelFactory {
    private final JavaThing nullThing;
    private final JavaClass versionClass;
    private final Map<JavaClass, Function<JavaObject, Model>> dispatchMap;

    public static boolean checkVersion(Snapshot snapshot, String prefix) {
        JavaClass version = snapshot.findClass("sun.misc.Version");
        return (Models.checkStaticString(version, "java_runtime_name", "Java(TM) SE")
                || Models.checkStaticString(version, "java_runtime_name", "OpenJDK"))
                && Models.checkStaticString(version, "java_version", prefix);
    }

    public OpenJDK(Snapshot snapshot) {
        Function<String, JavaClass> lookup = name -> Models.grabClass(snapshot, name);
        nullThing = snapshot.getNullThing();
        versionClass = lookup.apply("sun.misc.Version");

        dispatchMap = new ImmutableMap.Builder<JavaClass, Function<JavaObject, Model>>()
                .put(lookup.apply("java.lang.String"), this::makeString)
                .put(lookup.apply("java.util.concurrent.ConcurrentHashMap"), this::makeConcurrentHash)
                .put(lookup.apply("java.util.HashMap"), this::makeHash)
                .put(lookup.apply("java.util.Hashtable"), this::makeHash)
                .put(lookup.apply("java.util.ArrayList"), obj -> makeVector(obj, "size"))
                .put(lookup.apply("java.util.Vector"), obj -> makeVector(obj, "elementCount"))
                .put(lookup.apply("java.util.LinkedList"), this::makeLinkedList)
                 // TODO Implement all the standard collection classes.
                .build();
    }

    @Override
    public Map<JavaClass, Function<JavaObject, Model>> getDispatchMap() {
        return dispatchMap;
    }

    @Override
    public Model newModel(JavaThing thing) {
        Model result = ModelFactory.super.newModel(thing);
        if (result != null)
            return result;
        if (thing instanceof JavaObjectArray)
            return new JavaArray(this, (JavaObjectArray) thing);
        if (thing instanceof JavaValueArray)
            return new JavaPrimArray(this, (JavaValueArray) thing);
        return null;
    }

    protected JavaString makeString(JavaObject obj) {
        return JavaString.make(this, obj);
    }

    protected JavaConcHash makeConcurrentHash(JavaObject obj) {
        return JavaConcHash.make(this, obj);
    }

    protected JavaHash makeHash(JavaObject obj) {
        return JavaHash.make(this, obj);
    }

    protected JavaVector makeVector(JavaObject obj, String sizeField) {
        return JavaVector.make(this, obj, sizeField);
    }

    protected abstract CollectionModel makeLinkedList(JavaObject obj);

    public JavaThing getNullThing() {
        return nullThing;
    }

    @Override
    public String toString() {
        return String.format("OpenJDK %s (%s)",
                Models.getStaticString(versionClass, "java_version"),
                Models.getStaticString(versionClass, "java_runtime_version"));
    }
}
