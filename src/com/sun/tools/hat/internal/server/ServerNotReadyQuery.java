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

import com.sun.tools.hat.internal.parser.LoadProgress;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

class ServerNotReadyQuery extends QueryHandler {
    private static final String MB = "MB";
    private static final String GB = "GB";
    private final LoadProgress loadProgress;

    public ServerNotReadyQuery(LoadProgress loadProgress) {
        this.loadProgress = loadProgress;
    }

    @Override
    public void run() {
        startHtml("Server Not Ready");
        printMemoryUsage();
        loadProgress.each(p -> printProgress(p));
        out.println("<meta http-equiv=\"refresh\" content=\"1\" />");
        endHtml();
    }

    private void printProgress(LoadProgress.ProgressElement progress) {
        out.println("<p>");
        println(progress.getLoadString());
        out.println("</p>");
    }

    private void printMemoryUsage() {
        MemoryUsage memory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        String size = MB;
        double used = (double) memory.getUsed() / 1024.0 / 1024.0;
        double total = (double) memory.getMax() / 1024.0 / 1024.0;
        double percentUsed = 100.0 * used / total;

        if (total >= 1024.0) {
            size = GB;
            used /= 1024.0;
            total /= 1024.0;
        }

        out.println("<p>");
        out.println(String.format("<b>Heap Utilization:</b> %1.2f%s / %1.2f%s (%1.1f%%)", used, size, total, size, percentUsed));
        out.println("</p>");
    }
}
