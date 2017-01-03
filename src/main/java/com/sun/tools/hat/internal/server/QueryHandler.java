/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2010, 2011, 2012 On-Site.com.
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

import java.io.PrintWriter;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import com.sun.tools.hat.internal.annotations.ViewGetter;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.util.Misc;

/**
 *
 * @author      Bill Foote
 */


public abstract class QueryHandler implements Runnable {
    protected String path;
    protected String urlStart;
    protected String query;
    protected PrintWriter out;
    protected Server server;
    protected Snapshot snapshot;
    protected ImmutableListMultimap<String, String> params;
    protected boolean rawMode;

    public boolean isRawMode() {
        return rawMode;
    }

    @ViewGetter
    public boolean getHasNewSet() {
        return snapshot.getHasNewSet();
    }

    @ViewGetter
    public Snapshot getSnapshot() {
        return snapshot;
    }

    public String getPath() {
        return path;
    }

    @ViewGetter
    public String getQuery() {
        return query;
    }

    public String getHttpStatus() {
        return "200 OK";
    }

    public List<String> getHeaders() {
        return ImmutableList.<String>builder()
                .add("Content-Type: text/html; charset=UTF-8")
                .add("Cache-Control: no-cache")
                .add("Pragma: no-cache")
                .build();
    }

    void setPath(String s) {
        path = s;
    }

    void setUrlStart(String s) {
        urlStart = s;
    }

    void setQuery(String s) {
        query = s;
    }

    void setOutput(PrintWriter o) {
        this.out = o;
    }

    void setServer(Server server) {
        this.server = server;
    }

    void setSnapshot(Snapshot ss) {
        this.snapshot = ss;
    }

    void setParams(ImmutableListMultimap<String, String> params) {
        this.params = params;
        rawMode = params.containsKey("raw");
    }

    protected long parseHex(String value) {
        return Misc.parseHex(value);
    }

    protected void print(String str) {
        out.print(Misc.encodeHtml(str));
    }

    protected void println(String str) {
        out.println(Misc.encodeHtml(str));
    }

    protected JavaClass resolveClass(String name, boolean valueRequired) {
        if (name == null && !valueRequired) {
            return null;
        }
        return Preconditions.checkNotNull(snapshot.findClass(name),
                "class not found: %s", name);
    }
}
