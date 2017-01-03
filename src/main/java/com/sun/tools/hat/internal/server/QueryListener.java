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

/**
 *
 * @author      Bill Foote
 */


import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import java.io.IOException;

import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.parser.LoadProgress;

public class QueryListener implements Runnable {

    private final Executor executor = Executors.newCachedThreadPool();
    private final Server server;

    public QueryListener(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            waitForRequests();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void waitForRequests() throws IOException {
        try (ServerSocket ss = new ServerSocket(server.getPort())) {
            while (true) {
                Socket s = ss.accept();
                Snapshot snapshot = server.getSnapshot();

                if (snapshot == null) {
                    executor.execute(new ServerNotReadyHttpReader(s, server));
                } else {
                    executor.execute(new HttpReader(s, server));
                }
            }
        }
    }

}
