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

package com.sun.tools.hat.internal.util;

import java.lang.reflect.Field;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Drop-in replacement for {@link com.google.common.base.Suppliers#memoize}
 * that can be unmemoized on demand. This pokes Guava internals via
 * reflection, so it may stop working in future Guava versions.
 *
 * @author Chris Jester-Young
 */
@SuppressWarnings("unchecked")
public class Suppliers {
    private static final Class<?> MEMOIZING_SUPPLIER_CLASS = getMemoizingSupplierClass();
    private static final Field INITIALIZED_FIELD = getField("initialized");
    private static final Field VALUE_FIELD = getField("value");

    private static final LoadingCache<Supplier<?>, Supplier<?>> MEMOIZERS
            = CacheBuilder.newBuilder().softValues().build(
              CacheLoader.from(com.google.common.base.Suppliers::memoize));

    private Suppliers() {}

    private static Class<?> getMemoizingSupplierClass() {
        try {
            return Class.forName("com.google.common.base.Suppliers$MemoizingSupplier");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Field getField(String name) {
        try {
            Field field = MEMOIZING_SUPPLIER_CLASS.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static <T> Supplier<T> memoize(Supplier<T> supplier) {
        return (Supplier<T>) MEMOIZERS.getUnchecked(supplier);
    }

    private static void unmemoize(Supplier<?> supplier) {
        MEMOIZING_SUPPLIER_CLASS.cast(supplier);
        try {
            synchronized (supplier) {
                INITIALIZED_FIELD.setBoolean(supplier, false);
                VALUE_FIELD.set(supplier, null);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void unmemoizeAll() {
        MEMOIZERS.asMap().values().forEach(Suppliers::unmemoize);
    }
}
