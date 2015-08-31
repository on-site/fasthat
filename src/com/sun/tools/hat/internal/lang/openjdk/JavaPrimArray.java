/*
 * Copyright © 2013 On-Site.com.
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

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Shorts;

import com.sun.tools.hat.internal.lang.common.SimpleCollectionModel;
import com.sun.tools.hat.internal.model.JavaBoolean;
import com.sun.tools.hat.internal.model.JavaByte;
import com.sun.tools.hat.internal.model.JavaChar;
import com.sun.tools.hat.internal.model.JavaDouble;
import com.sun.tools.hat.internal.model.JavaFloat;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaLong;
import com.sun.tools.hat.internal.model.JavaShort;
import com.sun.tools.hat.internal.model.JavaValue;
import com.sun.tools.hat.internal.model.JavaValueArray;

public class JavaPrimArray extends SimpleCollectionModel {
    public JavaPrimArray(OpenJDK factory, JavaValueArray array) {
        super(factory, Suppliers.memoize(
                () -> wrapElements(array.getElements()).collect(Collectors.toList())));
    }

    private static Stream<? extends JavaValue> wrapElements(Object elements) {
        if (elements instanceof boolean[])
            return Booleans.asList((boolean[]) elements).stream().map(JavaBoolean::new);
        if (elements instanceof byte[])
            return Bytes.asList((byte[]) elements).stream().map(JavaByte::new);
        if (elements instanceof char[])
            return Chars.asList((char[]) elements).stream().map(JavaChar::new);
        if (elements instanceof short[])
            return Shorts.asList((short[]) elements).stream().map(JavaShort::new);
        if (elements instanceof int[])
            return Arrays.stream((int[]) elements).mapToObj(JavaInt::new);
        if (elements instanceof long[])
            return Arrays.stream((long[]) elements).mapToObj(JavaLong::new);
        if (elements instanceof float[])
            return Floats.asList((float[]) elements).stream().map(JavaFloat::new);
        if (elements instanceof double[])
            return Arrays.stream((double[]) elements).mapToObj(JavaDouble::new);
        throw new AssertionError();
    }
}
