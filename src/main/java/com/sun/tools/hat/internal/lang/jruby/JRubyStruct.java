/*
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

import java.util.Iterator;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.sun.tools.hat.internal.lang.CollectionModel;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.ObjectModel;
import com.sun.tools.hat.internal.lang.common.SimpleObjectModel;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

/**
 * Model for JRuby {@code struct}s.
 *
 * @author Chris Jester-Young
 */
public class JRubyStruct extends SimpleObjectModel {
    public JRubyStruct(JRuby factory, JavaObject obj) {
        super(factory, () -> JRubyObject.getClassModel(factory, obj),
                () -> JRubyObject.getEigenclassModel(factory, obj),
                Suppliers.memoize(() -> getMembers(factory, obj)));
    }

    private static ImmutableMap<String, JavaThing> getMembers(JRuby factory, JavaObject obj) {
        JavaObject cls = JRubyObject.getClassModel(factory, obj).getClassObject();
        JavaObject structClass = null;
        while (cls.getField("varTable") == factory.getNullThing()) {
            if (structClass == null)
                structClass = Models.getFieldObjectChain(cls, "runtime", "structClass");
            if (cls == structClass)
                break;
            cls = Models.getFieldObject(cls, "superClass");
        }
        ObjectModel classModel = factory.makeObject(cls);
        JavaObject member = (JavaObject) classModel.getProperties().get("__member__");
        CollectionModel memberModel = JRubyArray.make(factory, member);

        ImmutableList<JavaObject> values = Models.getFieldObjectArray(obj, "values", JavaObject.class);
        Iterator<JavaObject> iter = values.iterator();

        ImmutableMap.Builder<String, JavaThing> builder = ImmutableMap.builder();
        for (JavaThing symbol : memberModel.getCollection()) {
            builder.put(JRubySymbol.make(factory, (JavaObject) symbol).getValue(), iter.next());
        }
        return builder.build();
    }
}
