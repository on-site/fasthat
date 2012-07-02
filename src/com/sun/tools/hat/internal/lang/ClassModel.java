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

package com.sun.tools.hat.internal.lang;

import java.util.Collection;

import com.sun.tools.hat.internal.model.JavaObject;

/**
 * A class model models a single class, which has superclasses, properties
 * (fields/instance variables), and methods.
 *
 * @author Chris K. Jester-Young
 */
public abstract class ClassModel extends AbstractModel {
    public ClassModel(ModelFactory factory) {
        super(factory);
    }

    @Override
    public void visit(ModelVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Returns this model's associated class object.
     *
     * @return the class object for this class
     */
    public abstract JavaObject getClassObject();

    /**
     * Returns the fully-qualified name of this class. Same concept as
     * {@link Class#getName}, though not necessarily the same details.
     *
     * @return the fully-qualified name of this class
     */
    public abstract String getName();

    /**
     * Returns the simple name of this class. Same concept as
     * {@link Class#getSimpleName}, though not necessarily the same
     * details.
     *
     * @return the simple name of this class
     */
    public abstract String getSimpleName();

    /**
     * Returns the superclasses of this class. In Java, this includes
     * interfaces. In Ruby, this includes included modules.
     *
     * @return superclasses of this class
     */
    public abstract Collection<ClassModel> getSuperclasses();

    /**
     * Returns the property names of this class. In Java, this refers
     * to fields, and in Ruby, this refers to instance variables.
     *
     * @return the property names of this class
     */
    public abstract Collection<String> getPropertyNames();
}
