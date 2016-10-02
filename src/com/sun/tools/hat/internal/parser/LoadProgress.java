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

import com.sun.tools.hat.internal.util.Misc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class LoadProgress implements Iterable<LoadProgress.ProgressElement> {
    private List<ProgressElement> elements = Collections.synchronizedList(new ArrayList<>());

    public void startLoadingStream(String heapFile, PositionDataInputStream stream) {
        addProgress(new StreamProgress(heapFile, stream));
    }

    public TickedProgress startTickedProgress(String name, int numTicks) {
        TickedProgress progress = new TickedProgress(name, numTicks);
        addProgress(progress);
        return progress;
    }

    private void addProgress(ProgressElement progress) {
        elements.add(progress);
        progress.start();
    }

    public void end() {
        synchronized (elements) {
            elements.get(elements.size() - 1).end();
        }
    }

    @Override
    public Iterator<ProgressElement> iterator() {
        return elements.iterator();
    }

    public void each(Consumer<ProgressElement> callback) {
        synchronized (elements) {
            for (ProgressElement progress : elements) {
                callback.accept(progress);
            }
        }
    }

    public static abstract class ProgressElement {
        private final long startTime;
        private volatile boolean ended = false;

        public ProgressElement() {
            this.startTime = System.currentTimeMillis();
        }

        protected abstract double getPercentDone();
        protected abstract String getLoadDescription();

        public void start() {
            System.out.println(String.format("----- Starting: %s -----", getLoadDescription()));
        }

        public String getLoadString() {
            double percentDone = getPercentDone();
            String loadTime = "unknown";
            long elapsed = getElapsedTime();

            if (elapsed > 0 && percentDone > 0.0) {
                double totalExpectedMillis = (elapsed / (percentDone / 100.0)) - elapsed;
                loadTime = Misc.formatTime((long) totalExpectedMillis);
            }

            return String.format("%s: %1.1f%%, estimated remaining load time: %s", getLoadDescription(), percentDone, loadTime);
        }

        private long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }

        public void end() {
            ended = true;
            System.out.println(String.format("Finished: %s in %s", getLoadDescription(), Misc.formatTime(getElapsedTime())));
        }

        protected boolean isEnded() {
            return ended;
        }
    }

    public static class StreamProgress extends ProgressElement {
        private final String heapFile;
        private final PositionDataInputStream stream;
        private final long length;

        public StreamProgress(String heapFile, PositionDataInputStream stream) {
            this.heapFile = heapFile;
            this.stream = stream;
            this.length = new File(heapFile).length();
        }

        @Override
        protected double getPercentDone() {
            if (isEnded()) {
                return 100.0;
            }

            return ((double) stream.position() / (double) length) * 100.0;
        }

        @Override
        protected String getLoadDescription() {
            return String.format("Loading %s", heapFile);
        }
    }

    public static class TickedProgress extends ProgressElement {
        private final String name;
        private final int numTicks;
        private final AtomicInteger progress = new AtomicInteger(0);

        public TickedProgress(String name, int numTicks) {
            this.name = name;
            this.numTicks = numTicks;
        }

        public void tick() {
            progress.incrementAndGet();
        }

        public int getPercentDoneInt() {
            return (int) getPercentDone();
        }

        @Override
        protected double getPercentDone() {
            if (isEnded()) {
                return 100.0;
            }

            return (progress.doubleValue() / (double) numTicks) * 100.0;
        }

        @Override
        protected String getLoadDescription() {
            return name;
        }
    }
}
