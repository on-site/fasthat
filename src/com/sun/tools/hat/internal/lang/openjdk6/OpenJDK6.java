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

package com.sun.tools.hat.internal.lang.openjdk6;

import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.ModelFactoryFactory;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaObjectArray;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.Snapshot;

/**
 * Model factory for OpenJDK 6.
 *
 * @author Chris K. Jester-Young
 */
public class OpenJDK6 implements ModelFactory {
    public enum Factory implements ModelFactoryFactory {
        INSTANCE;

        @Override
        public boolean isSupported(Snapshot snapshot) {
            /*
             * BTW, feel free to relax this to enable other JDK versions
             * to work, if you are sure that the models here work for them
             * too.
             */
            JavaClass version = snapshot.findClass("sun.misc.Version");
            return (Models.checkStaticString(version, "java_runtime_name", "Java(TM) SE")
                    || Models.checkStaticString(version, "java_runtime_name", "OpenJDK"))
                    && Models.checkStaticString(version, "java_version", "1.6.0_");
        }

        @Override
        public ModelFactory newFactory(Snapshot snapshot) {
            return new OpenJDK6(snapshot);
        }
    }

    private final JavaClass versionClass;
    private final JavaClass concHashMapClass;
    private final JavaClass hashMapClass;
    private final JavaClass hashtableClass;
    private final JavaClass arrayListClass;
    private final JavaClass vectorClass;
    private final JavaClass linkedListClass;

    private OpenJDK6(Snapshot snapshot) {
        versionClass = Models.grabClass(snapshot, "sun.misc.Version");
        concHashMapClass = Models.grabClass(snapshot, "java.util.concurrent.ConcurrentHashMap");
        hashMapClass = Models.grabClass(snapshot, "java.util.HashMap");
        hashtableClass = Models.grabClass(snapshot, "java.util.Hashtable");
        arrayListClass = Models.grabClass(snapshot, "java.util.ArrayList");
        vectorClass = Models.grabClass(snapshot, "java.util.Vector");
        linkedListClass = Models.grabClass(snapshot, "java.util.LinkedList");
    }

    @Override
    public Model newModel(JavaThing thing) {
        if (thing instanceof JavaObject) {
            JavaObject obj = (JavaObject) thing;
            // XXX The factory dispatch mechanism needs real improvement.
            JavaClass clazz = obj.getClazz();
            if (clazz.isString())
                return JavaString.make(obj);
            else if (clazz == concHashMapClass)
                return JavaConcHash.make(obj);
            else if (clazz == hashMapClass || clazz == hashtableClass)
                return JavaHash.make(obj);
            else if (clazz == arrayListClass)
                return JavaVector.make(obj, "size");
            else if (clazz == vectorClass)
                return JavaVector.make(obj, "elementCount");
            else if (clazz == linkedListClass)
                return JavaLinkedList.make(obj);
            // TODO Implement all the standard collection classes.
        }
        if (thing instanceof JavaObjectArray)
            return new JavaArray((JavaObjectArray) thing);
        return null;
    }

    @Override
    public String toString() {
        return String.format("OpenJDK %s (%s)",
                Models.getStaticString(versionClass, "java_version"),
                Models.getStaticString(versionClass, "java_runtime_version"));
    }
}
