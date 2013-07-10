/*
 * Copyright (c) 2013 On-Site.com.
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

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.sun.tools.hat.internal.lang.AbstractCollectionModel;
import com.sun.tools.hat.internal.model.JavaBoolean;
import com.sun.tools.hat.internal.model.JavaByte;
import com.sun.tools.hat.internal.model.JavaChar;
import com.sun.tools.hat.internal.model.JavaDouble;
import com.sun.tools.hat.internal.model.JavaFloat;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaLong;
import com.sun.tools.hat.internal.model.JavaShort;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.JavaValueArray;

public class JavaPrimArray extends AbstractCollectionModel {
    private final Collection<JavaThing> collection;

    public JavaPrimArray(OpenJDK factory, JavaValueArray array) {
        super(factory);
        collection = createBackingList(array);
    }

    private static ImmutableList<JavaThing> createBackingList(JavaValueArray array) {
        ImmutableList.Builder<JavaThing> builder = ImmutableList.builder();
        Object elements = array.getElements();
        if (elements instanceof boolean[]) {
            for (boolean element : (boolean[]) elements)
                builder.add(new JavaBoolean(element));
        } else if (elements instanceof byte[]) {
            for (byte element : (byte[]) elements)
                builder.add(new JavaByte(element));
        } else if (elements instanceof char[]) {
            for (char element : (char[]) elements)
                builder.add(new JavaChar(element));
        } else if (elements instanceof short[]) {
            for (short element : (short[]) elements)
                builder.add(new JavaShort(element));
        } else if (elements instanceof int[]) {
            for (int element : (int[]) elements)
                builder.add(new JavaInt(element));
        } else if (elements instanceof long[]) {
            for (long element : (long[]) elements)
                builder.add(new JavaLong(element));
        } else if (elements instanceof float[]) {
            for (float element : (float[]) elements)
                builder.add(new JavaFloat(element));
        } else if (elements instanceof double[]) {
            for (double element : (double[]) elements)
                builder.add(new JavaDouble(element));
        } else {
            throw new AssertionError();
        }
        return builder.build();
    }

    @Override
    public Collection<JavaThing> getCollection() {
        return collection;
    }
}
