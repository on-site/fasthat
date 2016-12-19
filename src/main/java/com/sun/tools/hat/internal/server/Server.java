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

package com.sun.tools.hat.internal.server;

import com.google.common.base.Preconditions;
import com.sun.tools.hat.internal.lang.guava.GuavaRuntime;
import com.sun.tools.hat.internal.lang.jruby12.JRuby12Runtime;
import com.sun.tools.hat.internal.lang.jruby16.JRuby16Runtime;
import com.sun.tools.hat.internal.lang.jruby17.JRuby17Runtime;
import com.sun.tools.hat.internal.lang.openjdk6.OpenJDK6Runtime;
import com.sun.tools.hat.internal.lang.openjdk7.OpenJDK7Runtime;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.model.ReachableExcludesImpl;
import com.sun.tools.hat.internal.parser.LoadProgress;
import com.sun.tools.hat.internal.parser.Reader;
import com.sun.tools.hat.internal.server.QueryListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Contains all the sever relevant objects and config.
 *
 * @author Mike Virata-Stone
 */
public class Server {
    private Thread serverThread;
    private QueryListener listener;
    private final LoadProgress loadProgress = new LoadProgress();
    private volatile Snapshot snapshot;
    private volatile boolean loadingSnapshot = false;

    private boolean parseOnly = false;
    private int port = 7000;
    private boolean callStack = true;
    private boolean calculateRefs = true;
    private boolean preCacheHistograms = true;
    private String heapsDir = ".";
    private String dump;
    private String baselineDump;
    private String excludeFileName;
    private int debugLevel = 0;

    public LoadProgress getLoadProgress() {
        return loadProgress;
    }

    public boolean isLoadingSnapshot() {
        return loadingSnapshot;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public boolean getParseOnly() {
        return parseOnly;
    }

    public int getPort() {
        return port;
    }

    public boolean getCallStack() {
        return callStack;
    }

    public boolean getCalculateRefs() {
        return calculateRefs;
    }

    public boolean getPreCacheHistograms() {
        return preCacheHistograms;
    }

    public String getHeapsDir() {
        return heapsDir;
    }

    public Path getHeapsPath() {
        return new File(heapsDir).toPath();
    }

    public String getDump() {
        return dump;
    }

    public String getBaselineDump() {
        return baselineDump;
    }

    public String getExcludeFileName() {
        return excludeFileName;
    }

    public File getExcludeFile() {
        if (excludeFileName != null) {
            return new File(excludeFileName);
        }

        return null;
    }

    public int getDebugLevel() {
        return debugLevel;
    }

    public void setParseOnly(boolean value) {
        parseOnly = value;
    }

    public void setPort(int value) {
        port = value;
    }

    public void setCallStack(boolean value) {
        callStack = value;
    }

    public void setCalculateRefs(boolean value) {
        calculateRefs = value;
    }

    public void setPreCacheHistograms(boolean value) {
        preCacheHistograms = value;
    }

    public void setHeapsDir(String value) {
        heapsDir = value;
    }

    public void setDumpParallel(final String dump, final String baselineDump) {
        if (isLoadingSnapshot()) {
            return;
        }

        serverThread = new Thread(() -> {
            try {
                setDump(dump, baselineDump);
            } catch (Exception e) {
                System.err.println("Error while loading dump!");
                e.printStackTrace(System.err);
            }
        });
        serverThread.setName("fasthat-dump-loader");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void setDump(String dump, String baselineDump) throws IOException {
        setDump(dump, baselineDump, true);
    }

    public void setDump(String dump, String baselineDump, boolean validateDumpPaths) throws IOException {
        if (validateDumpPaths) {
            Preconditions.checkArgument(allowedDumpPath(dump), "Heap dump is not in an allowed path to process: " + dump);
            Preconditions.checkArgument(allowedDumpPath(baselineDump), "Baseline heap dump is not in an allowed path to process: " + baselineDump);
        }

        this.dump = dump;
        this.baselineDump = baselineDump;

        if (dump == null) {
            this.snapshot = null;
            System.gc();
            System.out.println("Cleared snapshot.");
            return;
        } else {
            this.loadingSnapshot = true;
            this.snapshot = null;
        }

        try {
            System.out.printf("Reading from %s...%n", dump);
            Snapshot snapshot = Reader.readFile(loadProgress, dump, callStack, debugLevel);
            System.out.println("Snapshot read, resolving...");
            snapshot.resolve(loadProgress, calculateRefs, preCacheHistograms);
            System.out.println("Snapshot resolved.");

            File excludeFile = getExcludeFile();
            if (excludeFile != null) {
                snapshot.setReachableExcludes(new ReachableExcludesImpl(excludeFile));
            }

            if (baselineDump != null) {
                System.out.println("Reading baseline snapshot...");
                Snapshot baseline = Reader.readFile(loadProgress, baselineDump, false, debugLevel);
                baseline.resolve(loadProgress, false, false);
                System.out.println("Discovering new objects...");
                snapshot.markNewRelativeTo(baseline);
                baseline = null;    // Guard against conservative GC
            }

            snapshot.setUpModelFactories(OpenJDK6Runtime.INSTANCE,
                    OpenJDK7Runtime.INSTANCE, GuavaRuntime.INSTANCE,
                    JRuby12Runtime.INSTANCE, JRuby16Runtime.INSTANCE,
                    JRuby17Runtime.INSTANCE);
            this.snapshot = snapshot;
        } finally {
            this.loadingSnapshot = false;
        }
    }

    private boolean allowedDumpPath(String dump) {
        if (dump == null) {
            return true;
        }

        return new File(dump).toPath().startsWith(getHeapsPath());
    }

    public void setExcludeFileName(String value) {
        excludeFileName = value;
    }

    public void setDebugLevel(int value) {
        debugLevel = value;
    }

    public void start(String dump, String baselineDump) throws InterruptedException, IOException {
        if (listener != null) {
            throw new IllegalStateException("Start called multiple times!");
        }

        if (!parseOnly && debugLevel != 2) {
            listener = new QueryListener(this);
            serverThread = new Thread(listener);
            serverThread.setName("fasthat-query-listener");
            serverThread.setDaemon(true);
            serverThread.start();
            System.out.printf("Started HTTP server on port %d%n", port);
            System.out.println("Server is listening.");
        }

        setDump(dump, baselineDump, false);

        if ( debugLevel == 2 ) {
            System.out.println("No server, -debug 2 was used.");
            System.exit(0);
        }

        if (parseOnly) {
            // do not start web server.
            System.out.println("-parseonly is true, exiting..");
            System.exit(0);
        }

        System.out.println("Server is ready.");
        serverThread.join();
    }
}
