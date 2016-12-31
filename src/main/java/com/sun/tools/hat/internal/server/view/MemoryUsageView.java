/*
 * Copyright (c) 2016 On-Site.com.
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

package com.sun.tools.hat.internal.server.view;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import com.sun.tools.hat.internal.annotations.ViewGetter;
import com.sun.tools.hat.internal.server.QueryHandler;

/**
 * View model for memory usage information.
 *
 * @author Mike Virata-Stone
 */
public class MemoryUsageView extends ViewModel {
    private static final String MB = "MB";
    private static final String GB = "GB";

    private final String memUsed;
    private final String memTotal;
    private final String memPercent;

    public MemoryUsageView(QueryHandler handler) {
        super(handler);
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

        memUsed = String.format("%1.2f%s", used, size);
        memTotal = String.format("%1.2f%s", total, size);
        memPercent = String.format("%1.1f%%", percentUsed);
    }

    @ViewGetter
    public String getUsedMemory() {
        return memUsed;
    }

    @ViewGetter
    public String getTotalMemory() {
        return memTotal;
    }

    @ViewGetter
    public String getPercentMemoryUsed() {
        return memPercent;
    }
}
