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
 * Reads a single HTTP query from a socket, and starts up a QueryHandler
 * to server it.
 *
 * @author      Bill Foote
 */


import com.sun.tools.hat.internal.util.Misc;

import java.net.Socket;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class HttpHandler implements Runnable {
    public enum Method { INVALID, GET, POST; }

    private static final AtomicInteger nextRequestId = new AtomicInteger();
    private final Socket socket;
    private final int requestId = nextRequestId.incrementAndGet();
    protected PrintWriter out;

    public HttpHandler (Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            handleRequest();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected abstract QueryHandler requestHandler(Method method, String query);

    private void handleRequest() throws IOException {
        try (InputStream in = new BufferedInputStream(socket.getInputStream());
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(), "UTF-8")))) {
            this.out = out;
            boolean isProtocolError = false;
            Method method = Method.INVALID;

            switch (in.read()) {
            case 'G':
                if (in.read() != 'E' || in.read() != 'T' || in.read() != ' ') {
                    isProtocolError = true;
                } else {
                    method = Method.GET;
                }
                break;
            case 'P':
                if (in.read() != 'O' || in.read() != 'S' || in.read() != 'T' || in.read() != ' ') {
                    isProtocolError = true;
                } else {
                    method = Method.POST;
                }
                break;
            default:
                isProtocolError = true;
            }

            int data;
            StringBuilder queryBuf = new StringBuilder();
            while ((data = in.read()) != -1 && data != ' ') {
                char ch = (char) data;
                queryBuf.append(ch);
            }
            String query = queryBuf.toString();

            log("Incoming request: " + method + " " + query);
            long startTime = System.currentTimeMillis();

            try {
                if (isProtocolError) {
                    log("Protocol error");
                    processQuery(new ErrorQuery("Protocol error"));
                } else {
                    processQuery(method, query);
                }
            } finally {
                long endTime = System.currentTimeMillis();
                log("Request finished in: " + Misc.formatTime(endTime - startTime));
            }
        }
    }

    private void processQuery(Method method, String query) {
        QueryHandler handler = requestHandler(method, query);

        if (handler == null) {
            processQuery(new ErrorQuery("Query '" + query + "' not implemented"));
        } else {
            processQuery(handler);
        }
    }

    private void processQuery(QueryHandler handler) {
        out.printf("HTTP/1.0 %s%n", handler.getHttpStatus());

        for (String header : handler.getHeaders()) {
            out.println(header);
        }

        out.println();
        handler.setOutput(out);

        try {
            handler.run();
        } catch (RuntimeException ex) {
            log(ex);
            handler = new ErrorQuery(ex.getMessage());
            handler.setOutput(out);
            handler.run();
        }
    }

    protected void log(Exception e) {
        StringWriter stack = new StringWriter();
        PrintWriter writer = new PrintWriter(stack);
        writer.println("Error handling request");
        e.printStackTrace(writer);
        log(stack.toString());
    }

    protected void log(String message) {
        System.out.printf("[%d] %s%n", requestId, message);
    }
}
