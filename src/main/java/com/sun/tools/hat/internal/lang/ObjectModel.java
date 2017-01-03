/*
 * Copyright (c) 2011, 2012 On-Site.com.
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

import java.util.Map;

import com.sun.tools.hat.internal.model.JavaThing;

/**
 * An object model models a single object, which has a class, optionally
 * an eigenclass, and a map of properties (fields/instance variables).
 *
 * @author Chris K. Jester-Young
 */
public interface ObjectModel extends Model {
    /**
     * Returns the class model associated with this object's class.
     *
     * @return the class model associated with this object's class
     */
    ClassModel getClassModel();

    /**
     * Returns the class model associated with this object's eigenclass,
     * if any. Ruby is likely to be the only language where objects have
     * eigenclasses.
     *
     * @return the class model associated with this object's eigenclass,
     *         or null
     */
    ClassModel getEigenclassModel();

    /**
     * Returns the properties (field names and values) attached to this
     * object. In Ruby's case, this means instance variables and their
     * values.
     *
     * @return properties attached to this object
     */
    Map<String, JavaThing> getProperties();

    @Override
    default <T> T visit(ModelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
