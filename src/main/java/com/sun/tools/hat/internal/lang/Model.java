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

/**
 * Base interface that all language-specific models implement.
 *
 * <p>In the current design of language-specific models, there are three
 * generic types: scalars, lists, and maps, represented by the interfaces
 * {@link ScalarModel}, {@link CollectionModel}, and {@link MapModel}. In order to
 * allow users of the class not to have to do a bunch of {@code instanceof}
 * tests, a visitor pattern is used instead.
 *
 * @author Chris K. Jester-Young
 */
public interface Model {
    /**
     * Returns the factory that created this model.
     *
     * @return the factory that created this model
     */
    ModelFactory getFactory();

    /**
     * Sends this model to the given visitor.
     *
     * @param visitor the visitor to send this model to
     */
    <T> T visit(ModelVisitor<T> visitor);
}
