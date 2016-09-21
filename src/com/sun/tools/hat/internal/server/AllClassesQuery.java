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

import com.sun.tools.hat.internal.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author      Bill Foote
 */
class AllClassesQuery extends MustacheQueryHandler {
    private final boolean excludePlatform;
    private final boolean oqlSupported;
    private List<JavaPackage> packages;

    public AllClassesQuery(boolean excludePlatform, boolean oqlSupported) {
        this.excludePlatform = excludePlatform;
        this.oqlSupported = oqlSupported;
    }

    public String getTitle() {
        if (excludePlatform) {
            return "All Classes (excluding platform)";
        } else {
            return "All Classes (including platform)";
        }
    }

    public boolean getExcludePlatform() {
        return excludePlatform;
    }

    public boolean getOqlSupported() {
        return oqlSupported;
    }

    public List<JavaPackage> getPackages() {
        if (packages != null) {
            return packages;
        }

        packages = new ArrayList<>();
        JavaPackage lastPackage = null;

        for (JavaClass clazz : snapshot.getClasses()) {
            if (excludePlatform && PlatformClasses.isPlatformClass(clazz)) {
                continue;
            }

            String pkg = clazz.getPackageName();

            if (lastPackage == null || !pkg.equals(lastPackage.getName())) {
                lastPackage = new JavaPackage(pkg);
                packages.add(lastPackage);
            }

            lastPackage.getClasses().add(clazz);
        }

        return packages;
    }

    public static class JavaPackage {
        private final String name;
        private final List<JavaClass> classes = new ArrayList<>();

        public JavaPackage(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<JavaClass> getClasses() {
            return classes;
        }
    }
}
