/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.hat;

import com.sun.tools.hat.internal.server.Server;

import java.io.IOException;
import java.io.File;

/**
 *
 * @author      Bill Foote
 */


public class Main {

    private static String VERSION_STRING = "fasthat version 2.1";

    private static void usage(String message) {
        if ( message != null ) {
            System.err.printf("ERROR: %s%n", message);
        }
        System.err.println("Usage:  fasthat [-stack <bool>] [-refs <bool>] [-histo <bool>] [-port <port>] [-heaps <dir>] [-baseline <file>] [-debug <int>] [-version] [-h|-help] [<file>]");
        System.err.println();
        System.err.println("\t-J<flag>          Pass <flag> directly to the runtime system. For");
        System.err.println("\t\t\t  example, -J-mx512m to use a maximum heap size of 512MB");
        System.err.println("\t-stack false:     Turn off tracking object allocation call stack.");
        System.err.println("\t-refs false:      Turn off tracking of references to objects");
        System.err.println("\t-histo false:     Turn off pre-caching of histogram sizes");
        System.err.println("\t-port <port>:     Set the port for the HTTP server.  Defaults to 7000");
        System.err.println("\t-exclude <file>:  Specify a file that lists data members that should");
        System.err.println("\t\t\t  be excluded from the reachableFrom query.");
        System.err.println("\t-heaps <dir>:     Indicate the heaps directory.  Defaults to the current");
        System.err.println("\t\t\t  directory.");
        System.err.println("\t-baseline <file>: Specify a baseline object dump.  Objects in");
        System.err.println("\t\t\t  both heap dumps with the same ID and same class will");
        System.err.println("\t\t\t  be marked as not being \"new\".");
        System.err.println("\t-debug <int>:     Set debug level.");
        System.err.println("\t\t\t    0:  No debug output");
        System.err.println("\t\t\t    1:  Debug hprof file parsing");
        System.err.println("\t\t\t    2:  Debug hprof file parsing, no server");
        System.err.println("\t-version          Report version number");
        System.err.println("\t-h|-help          Print this help and exit");
        System.err.println("\t<file>            The file to read");
        System.err.println();
        System.err.println("For a dump file that contains multiple heap dumps,");
        System.err.println("you may specify which dump in the file");
        System.err.println("by appending \"#<number>\" to the file name, e.g., \"foo.hprof#3\".");
        System.err.println();
        System.err.println("All boolean options default to \"true\"");
        System.exit(1);
    }

    //
    // Convert s to a boolean.  If it's invalid, abort the program.
    //
    private static boolean booleanValue(String s) {
        if ("true".equalsIgnoreCase(s)) {
            return true;
        } else if ("false".equalsIgnoreCase(s)) {
            return false;
        } else {
            usage("Boolean value must be true or false");
            return false;       // Never happens
        }
    }

    public static void main(final String[] args) throws InterruptedException, IOException {
        Server server = new Server();
        String baselineDump = null;
        String dump = null;

        for (int i = 0; i < args.length; i += 2) {
            if ("-version".equals(args[i])) {
                System.out.printf("%s (java version %s)%n", VERSION_STRING, System.getProperty("java.version"));
                System.exit(0);
            }

            if ("-h".equals(args[i]) || "-help".equals(args[i])) {
                usage(null);
            }

            if (i == (args.length - 1)) {
                dump = args[i];
                break;
            }
            String key = args[i];
            String value = args[i+1];
            if ("-stack".equals(key)) {
                server.setCallStack(booleanValue(value));
            } else if ("-refs".equals(key)) {
                server.setCalculateRefs(booleanValue(value));
            } else if ("-histo".equals(key)) {
                server.setPreCacheHistograms(booleanValue(value));
            } else if ("-port".equals(key)) {
                server.setPort(Integer.parseInt(value, 10));
            } else if ("-exclude".equals(key)) {
                server.setExcludeFileName(value);
            } else if ("-heaps".equals(key)) {
                server.setHeapsDir(value);
            } else if ("-baseline".equals(key)) {
                baselineDump = value;
            } else if ("-debug".equals(key)) {
                server.setDebugLevel(Integer.parseInt(value, 10));
            } else if ("-parseonly".equals(key)) {
                // Undocumented option. To be used for testing purpose only
                server.setParseOnly(booleanValue(value));
            }
        }

        File excludeFile = server.getExcludeFile();
        if (excludeFile != null && !excludeFile.exists()) {
            System.out.printf("Exclude file %s does not exist.  Aborting.%n", excludeFile);
            System.exit(1);
        }

        server.start(dump, baselineDump);
    }
}
