/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package com.sun.tools.hat.internal.server;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSortedSet;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.util.Suppliers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class is a helper that determines if a class is a "platform"
 * class or not.  It's a platform class if its name starts with one of
 * the prefixes to be found in /com/sun/tools/hat/resources/platform_names.txt.
 *
 * @author      Bill Foote
 */

public class PlatformClasses {
    private static final Supplier<ImmutableSortedSet<String>> NAMES_SUPPLIER = Suppliers.memoize(() -> {
        ImmutableSortedSet.Builder<String> builder = ImmutableSortedSet.naturalOrder();
        try (InputStream str = PlatformClasses.class.getResourceAsStream("/com/sun/tools/hat/resources/platform_names.txt")) {
            if (str != null) {
                try (BufferedReader rdr = new BufferedReader(new InputStreamReader(str))) {
                    String s;
                    while ((s = rdr.readLine()) != null) {
                        if (!s.isEmpty()) {
                            builder.add(s);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            // Shouldn't happen, and if it does, continuing
            // is the right thing to do anyway.
        }
        return builder.build();
    });

    public static ImmutableSortedSet<String> getNames() {
        return NAMES_SUPPLIER.get();
    }


    public static boolean isPlatformClass(JavaClass clazz) {
        // all classes loaded by bootstrap loader are considered
        // platform classes. In addition, the older name based filtering
        // is also done for compatibility.
        if (clazz.isBootstrap()) {
            return true;
        }

        String name = clazz.getName();
        // skip even the array classes of the skipped classes.
        if (name.startsWith("[")) {
            int index = name.lastIndexOf('[');
            if (index != -1) {
                if (name.charAt(index + 1) != 'L') {
                    // some primitive array.
                    return true;
                }
                // skip upto 'L' after the last '['.
                name = name.substring(index + 2);
            }
        }
        String floor = getNames().floor(name);
        return floor != null && name.startsWith(floor);
    }
}
