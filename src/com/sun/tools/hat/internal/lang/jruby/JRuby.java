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

import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.Snapshot;

/**
 * Common, version-independent components of version-specific JRuby
 * model factories.
 *
 * @author Chris K. Jester-Young
 */
public abstract class JRuby implements ModelFactory {
    private final JavaThing nullThing;
    private final JavaClass constantsClass;
    private final JavaClass classClass;
    private final JavaClass moduleClass;
    private final JavaClass moduleWrapperClass;
    private final JavaClass stringClass;
    private final JavaClass objectClass;
    private final JavaClass arrayClass;
    private final JavaClass hashClass;

    public JRuby(Snapshot snapshot) {
        nullThing = snapshot.getNullThing();
        constantsClass = Models.grabClass(snapshot, "org.jruby.runtime.Constants");
        classClass = Models.grabClass(snapshot, "org.jruby.RubyClass");
        moduleClass = Models.grabClass(snapshot, "org.jruby.RubyModule");
        moduleWrapperClass = Models.grabClass(snapshot, "org.jruby.IncludedModuleWrapper");
        stringClass = Models.grabClass(snapshot, "org.jruby.RubyString");
        objectClass = Models.grabClass(snapshot, "org.jruby.RubyObject");
        arrayClass = Models.grabClass(snapshot, "org.jruby.RubyArray");
        hashClass = Models.grabClass(snapshot, "org.jruby.RubyHash");
    }

    public JavaThing getNullThing() {
        return nullThing;
    }

    public JavaClass getConstantsClass() {
        return constantsClass;
    }

    public JavaClass getClassClass() {
        return classClass;
    }

    public JavaClass getModuleClass() {
        return moduleClass;
    }

    public JavaClass getModuleWrapperClass() {
        return moduleWrapperClass;
    }

    public JavaClass getStringClass() {
        return stringClass;
    }

    public JavaClass getObjectClass() {
        return objectClass;
    }

    public JavaClass getArrayClass() {
        return arrayClass;
    }

    public JavaClass getHashClass() {
        return hashClass;
    }

    @Override
    public String toString() {
        return String.format("JRuby %s (%s)",
                Models.getStaticString(constantsClass, "VERSION"),
                Models.getStaticString(constantsClass, "REVISION"));
    }
}
