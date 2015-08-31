/*
 * Copyright © 2011, 2012, 2013 On-Site.com.
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

import java.nio.CharBuffer;
import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaObjectArray;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.JavaValueArray;
import com.sun.tools.hat.internal.model.Snapshot;

/**
 * Common functionality used by language-specific models.
 *
 * @author Chris Jester-Young
 */
// I wish Java had package-and-subpackage private
public final class Models {
    /**
     * Disable instantiation for static class.
     */
    private Models() {}

    /**
     * Returns {@code obj} if it's an instance of class {@code cls},
     * otherwise null.
     *
     * <p>The curious use of the two type parameters is to encourage the
     * use of this function for downcasts only (as opposed to upcasts
     * or invalid casts). Since the hat models do not use interfaces,
     * sidecasts are not a use case we have to consider.
     *
     * @param <T> the type to cast from
     * @param <U> the type to cast to
     * @param obj the object to cast
     * @param cls the type key to cast with
     * @return {@code obj} cast to the given type, or null
     */
    public static <T, U extends T> U safeCast(T obj, Class<U> cls) {
        return cls.isInstance(obj) ? cls.cast(obj) : null;
    }

    private static JavaClass getClass(Snapshot snapshot, String... classNames) {
        for (String className : classNames) {
            JavaClass result = snapshot.findClass(className);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * Resolves the class object corresponding to the first found among
     * the given class names using the given snapshot. If none of the
     * class names can be found, throw an exception.
     *
     * @param snapshot the snapshot to use to look up the classes
     * @param classNames the names of the classes to look up
     * @return the class object corresponding to the first found class
     */
    public static JavaClass grabClass(Snapshot snapshot, String... classNames) {
        return Preconditions.checkNotNull(getClass(snapshot, classNames),
                "Cannot find class(es) " + Arrays.toString(classNames));
    }

    /**
     * Returns whether any of the given class names can be found in the
     * given snapshot.
     *
     * @param snapshot the snapshot to use to look up the classes
     * @param classNames the names of the classes to look up
     * @return true iff any of the given names can be found
     */
    public static boolean hasClass(Snapshot snapshot, String... classNames) {
        return getClass(snapshot, classNames) != null;
    }

    /**
     * Returns the value of string object {@code obj} as a {@link CharBuffer},
     * or null if {@code obj} is not a string object.
     *
     * @param obj the object to get the string value of
     * @return the string value of {@code obj} as a {@link CharBuffer}
     */
    public static CharBuffer getStringValueAsCharBuffer(JavaObject obj) {
        if (obj != null && obj.getClazz().isString()) {
            JavaValueArray value = safeCast(obj.getField("value"), JavaValueArray.class);
            JavaInt offset = safeCast(obj.getField("offset"), JavaInt.class);
            JavaInt count = safeCast(obj.getField("count"), JavaInt.class);
            if (value != null) {
                if (offset != null && count != null) {
                    return CharBuffer.wrap((char[]) value.getElements(), offset.value, count.value);
                } else {
                    return CharBuffer.wrap((char[]) value.getElements());
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of string object {@code obj}, or null if
     * {@code obj} is not a string object.
     *
     * @param obj the object to get the string value of
     * @return the string value of {@code obj}, or null
     */
    public static String getStringValue(JavaObject obj) {
        CharBuffer value = getStringValueAsCharBuffer(obj);
        return value != null ? value.toString() : null;
    }

    /**
     * Returns the value of array object {@code arr}, which contains
     * objects of type {@code T}, or null if at least one element of
     * {@code arr} is not of type {@code T}.
     *
     * @param arr the array to get {@code T} objects from
     * @param typeKey the type key for type {@code T}
     * @return the elements of {@code arr} if they're all of type {@code T}, or null
     */
    public static <T extends JavaThing> ImmutableList<T> getObjectArrayValue(
            JavaObjectArray arr, Class<T> typeKey) {
        if (arr != null) {
            ImmutableList.Builder<T> builder = ImmutableList.builder();
            for (JavaThing t : arr.getElements()) {
                if (t != null) {
                    if (!typeKey.isInstance(t))
                        return null;
                    builder.add(typeKey.cast(t));
                }
            }
            return builder.build();
        }
        return null;
    }

    /**
     * Returns the content of the static field {@code fieldName} in the
     * given class, if it is a string; otherwise returns null.
     *
     * @param clazz the class to get the static string field from
     * @param fieldName the name of the static string field
     * @return the contents of the field, as a string, or null
     */
    public static String getStaticString(JavaClass clazz, String fieldName) {
        return getStringValue(safeCast(clazz.getStaticField(fieldName),
                JavaObject.class));
    }

    /**
     * Returns whether the static field {@code fieldName} in the given class
     * is a string starting with {@code prefix}'s contents. Unlike {@link
     * #getStaticString}, this method does not throw for invalid arguments,
     * but simply returns false.
     *
     * @param clazz the class to get the static string field from
     * @param fieldName the name of the static string field
     * @param prefix the prefix to check for
     * @return true iff the field starts with the given prefix
     */
    public static boolean checkStaticString(JavaClass clazz, String fieldName,
            String prefix) {
        if (clazz != null) {
            String value = getStaticString(clazz, fieldName);
            if (value != null) {
                return value.startsWith(prefix);
            }
        }
        return false;
    }

    /**
     * Returns whether the given object has a field with the given
     * name. This will return true as long as the field exists, even
     * if its value is null.
     *
     * @param obj the object to check
     * @param field the field name to look for
     * @return true iff {@code obj} has a field of the given name
     */
    public static boolean hasField(JavaObject obj, String field) {
        /*
         * JavaObject.getField() returns null if the field doesn't
         * exist, and returns Snapshot.getNullThing() if the field
         * exists but has a null value.
         */
        return obj.getField(field) != null;
    }

    /**
     * Returns the first given field found (in the sense of
     * {@link #hasField}) in the given object.
     *
     * @param obj the object to search
     * @param fields the field names to look for
     * @return the first field that exists in {@code obj}, or null
     */
    public static String findField(JavaObject obj, String... fields) {
        for (String field : fields) {
            if (hasField(obj, field))
                return field;
        }
        return null;
    }

    /**
     * Convenience method for getting the given field from the given
     * object as a Java object.
     *
     * @param obj the object to use
     * @param field the field to get
     * @return the value of the field if it's a Java object, else null
     */
    public static JavaObject getFieldObject(JavaObject obj, String field) {
        return getFieldThing(obj, field, JavaObject.class);
    }

    /**
     * Convenience method for getting the given field from the given
     * object as a {@code T}.
     *
     * @param obj the object to use
     * @param field the field to get
     * @param typeKey the type key for {@code T}
     * @return the value of the field if it's a {@code T}, else null
     */
    public static <T extends JavaThing> T getFieldThing(JavaObject obj, String field,
            Class<T> typeKey) {
        return obj == null ? null : safeCast(obj.getField(field), typeKey);
    }

    /**
     * Convenience method for getting the string value of the given field
     * from the given object.
     *
     * @param obj the object to use
     * @param field the field to get
     * @return the string value of the field if it's a string, else null
     */
    public static String getFieldString(JavaObject obj, String field) {
        return getStringValue(getFieldObject(obj, field));
    }

    /**
     * Convenience method for getting the elements of the object array
     * value of the given field from the given object.
     *
     * @param obj the object to use
     * @param field the field to get
     * @param typeKey the type key for {@code T}
     * @return the elements of the field if it's an object array with all
     *         elements of type {@code T}, else null
     */
    public static <T extends JavaThing> ImmutableList<T> getFieldObjectArray(
            JavaObject obj, String field, Class<T> typeKey) {
        return getObjectArrayValue(getFieldThing(obj, field, JavaObjectArray.class),
                typeKey);
    }
}
