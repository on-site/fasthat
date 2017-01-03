/*
 * Copyright © 2011 On-Site.com.
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

package com.sun.tools.hat.internal.lang;

import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

import java.util.Map;
import java.util.function.Function;

/**
 * Factory for language-specific models.
 *
 * @author Chris Jester-Young
 */
public interface ModelFactory {
    /**
     * Returns the dispatch map used by this model factory.
     *
     * @return the dispatch map used by this model factory
     */
    Map<JavaClass, Function<JavaObject, Model>> getDispatchMap();

    /**
     * Creates a model object for the given {@link JavaThing}. Returns
     * null if the given {@link JavaThing} is not supported.
     *
     * @param thing the object to create a model for
     * @return a model for {@code thing}, or null
     */
    default Model newModel(JavaThing thing) {
        if (thing instanceof JavaObject) {
            JavaObject obj = (JavaObject) thing;
            Map<JavaClass, Function<JavaObject, Model>> dispatchMap = getDispatchMap();
            JavaClass cls = obj.getClazz();
            if (dispatchMap.containsKey(cls))
                return dispatchMap.get(cls).apply(obj);
        }
        return null;
    }
}
