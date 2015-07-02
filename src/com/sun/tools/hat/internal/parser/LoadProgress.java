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

package com.sun.tools.hat.internal.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class LoadProgress {
    private List<StreamProgress> streams = Collections.synchronizedList(new ArrayList<>());

    public void startLoadingStream(String heapFile, PositionDataInputStream stream) {
        streams.add(new StreamProgress(heapFile, stream));
    }

    public void endLoadingStream() {
        synchronized (streams) {
            streams.get(streams.size() - 1).end();
        }
    }

    public void each(Consumer<StreamProgress> callback) {
        synchronized (streams) {
            for (StreamProgress progress : streams) {
                callback.accept(progress);
            }
        }
    }

    public static class StreamProgress {
        private final long startTime;
        private final String heapFile;
        private final PositionDataInputStream stream;
        private final long length;
        private boolean ended = false;

        public StreamProgress(String heapFile, PositionDataInputStream stream) {
            this.startTime = System.currentTimeMillis();
            this.heapFile = heapFile;
            this.stream = stream;
            this.length = new File(heapFile).length();
        }

        public synchronized void end() {
            ended = true;
        }

        public String getHeapFile() {
            return heapFile;
        }

        private synchronized boolean isEnded() {
            return ended;
        }

        public double getPercentDone() {
            if (isEnded()) {
                return 100.0;
            }

            return ((double) stream.position() / (double) length) * 100.0;
        }

        public String getLoadString() {
            double percentDone = getPercentDone();
            String loadTime = "unknown";
            long elapsed = System.currentTimeMillis() - startTime;

            if (elapsed > 0 && percentDone > 0.0) {
                double totalExpectedMillis = (elapsed / (percentDone / 100.0)) - elapsed;

                if (totalExpectedMillis > 60000.0) {
                    loadTime = String.format("%1.1f minutes", totalExpectedMillis / 60000.0);
                } else {
                    loadTime = String.format("%1.1f seconds", totalExpectedMillis / 1000.0);
                }
            }

            return String.format("%s is loading: %1.1f%%, estimated remaining load time: %s", getHeapFile(), percentDone, loadTime);
        }
    }
}
