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

package com.sun.tools.hat.internal.lang.jruby12;

import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.ModelFactoryFactory;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.Snapshot;

/**
 * Model factory for JRuby 1.2.
 *
 * @author Chris K. Jester-Young
 */
public class JRuby12 implements ModelFactory {
    public enum Factory implements ModelFactoryFactory {
        INSTANCE;

        @Override
        public boolean isSupported(Snapshot snapshot) {
            /*
             * BTW, feel free to relax this to enable other versions of
             * JRuby to work, if you are sure the models here work for
             * them too.
             */
            JavaClass constants = snapshot.findClass("org.jruby.runtime.Constants");
            return Models.checkStaticString(constants, "VERSION", "1.2.");
        }

        @Override
        public ModelFactory newFactory(Snapshot snapshot) {
            return isSupported(snapshot) ? new JRuby12(snapshot) : null;
        }
    }

    private final JavaClass constantsClass;
    private final JavaClass stringClass;
    private final JavaClass objectClass;

    private JRuby12(Snapshot snapshot) {
        constantsClass = Models.grabClass(snapshot, "org.jruby.runtime.Constants");
        stringClass = Models.grabClass(snapshot, "org.jruby.RubyString");
        objectClass = Models.grabClass(snapshot, "org.jruby.RubyObject");
    }

    @Override
    public Model newModel(JavaThing thing) {
        JavaObject obj = Models.safeCast(thing, JavaObject.class);
        if (obj != null) {
            if (obj.getClazz() == stringClass) {
                return JRubyString.make(obj);
            } else if (obj.getClazz() == objectClass) {
                return new JRubyObject(obj);
            }
            // TODO Implement array, hash, and other JRuby types.
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("JRuby %s (%s)",
                Models.getStaticString(constantsClass, "VERSION"),
                Models.getStaticString(constantsClass, "REVISION"));
    }
}
