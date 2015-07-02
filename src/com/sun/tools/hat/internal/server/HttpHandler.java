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


import java.net.Socket;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

import com.google.common.base.Strings;
import com.sun.tools.hat.internal.util.Misc;

public abstract class HttpHandler implements Runnable {
    private final Socket socket;
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

    protected abstract void handleRequest(String query) throws IOException;

    private void handleRequest() throws IOException {
        try (InputStream in = new BufferedInputStream(socket.getInputStream());
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(), "UTF-8")))) {
            this.out = out;
            out.println("HTTP/1.0 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Cache-Control: no-cache");
            out.println("Pragma: no-cache");
            out.println();
            if (in.read() != 'G' || in.read() != 'E'
                    || in.read() != 'T' || in.read() != ' ') {
                outputError("Protocol error");
            }
            int data;
            StringBuilder queryBuf = new StringBuilder();
            while ((data = in.read()) != -1 && data != ' ') {
                char ch = (char) data;
                queryBuf.append(ch);
            }
            String query = queryBuf.toString();
            handleRequest(query);
        }
    }

    protected void outputError(String msg) {
        out.println();
        out.println("<html><body bgcolor=\"#ffffff\">");
        out.println(Misc.encodeHtml(Strings.nullToEmpty(msg)));
        out.println("</body></html>");
    }

}
