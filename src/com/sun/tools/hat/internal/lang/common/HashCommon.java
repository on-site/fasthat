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

package com.sun.tools.hat.internal.lang.common;

import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

/**
 * Common functions for dealing with hashes.
 *
 * @author Chris Jester-Young
 */
public final class HashCommon {
    /**
     * An interface for objects that receive key-value pairs during a
     * hash-walking operation.
     */
    public interface KeyValueVisitor {
        /**
         * Called once for each hash entry during a hash-walking operation.
         *
         * @param key the key in the hash entry
         * @param value the value in the hash entry
         */
        void visit(JavaThing key, JavaThing value);
    }

    /**
     * Disable instantiation for static class.
     */
    private HashCommon() {}

    /**
     * Walks through the given hash table, invoking the given visitor for
     * each entry found. The hash table is specified as a list of objects
     * that conform to the {@code HashMap.Entry} layout; in other words,
     * has at least the {@code key}, {@code value}, and {@code next} fields.
     * (The {@code hash} field is not used by this function.)
     *
     * @param table the hash table to walk
     * @param keyField the name of the {@code key} field
     * @param valueField the name of the {@code value} field
     * @param nextField the name of the {@code next} field
     * @param visitor the visitor to call for each hash entry found
     */
    public static void walkHashTable(Iterable<JavaObject> table, String keyField,
            String valueField, String nextField, KeyValueVisitor visitor) {
        for (JavaObject element : table) {
            for (JavaObject bucket = element; bucket != null;
                    bucket = Models.getFieldObject(bucket, nextField)) {
                visitor.visit(bucket.getField(keyField), bucket.getField(valueField));
            }
        }
    }
}
