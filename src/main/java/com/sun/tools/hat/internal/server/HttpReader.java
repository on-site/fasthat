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

import com.google.common.collect.ImmutableList;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.oql.OQLEngine;

public class HttpReader extends HttpHandler {
    private final Server server;
    private final Snapshot snapshot;
    private final ImmutableList<HandlerRoute> routes = makeHandlerRoutes();

    private ImmutableList<HandlerRoute> makeHandlerRoutes() {
        ImmutableList.Builder<HandlerRoute> builder = ImmutableList.builder();

        if (OQLEngine.isOQLSupported()) {
            builder.add(new HandlerRoute("/oql/", OQLQuery::new),
                        new HandlerRoute("/oqlhelp/", OQLHelp::new));
        }
        builder.add(new HandlerRoute("/", IndexQuery::new),
                    new HandlerRoute("/allClasses/", AllClassesQuery::new),
                    new HandlerRoute("/showRoots/", AllRootsQuery::new),
                    new HandlerRoute("/showInstanceCounts/", InstancesCountQuery::new),
                    new HandlerRoute("/instances/*", InstancesQuery::new),
                    new HandlerRoute("/object/*", ObjectQuery::new),
                    new HandlerRoute("/class/*", ClassQuery::new),
                    new HandlerRoute("/roots/*", RootsQuery::new),
                    new HandlerRoute("/reachableFrom/*", ReachableQuery::new),
                    new HandlerRoute("/rootStack/*", RootStackQuery::new),
                    new HandlerRoute("/histo/*", HistogramQuery::new),
                    new HandlerRoute("/refsByType/*", RefsByTypeQuery::new),
                    new HandlerRoute("/finalizerSummary/", FinalizerSummaryQuery::new),
                    new HandlerRoute("/finalizerObjects/", FinalizerObjectsQuery::new),
                    new HandlerRoute("/loadDump/", HttpHandler.Method.POST, LoadDumpQuery::new),
                    new HandlerRoute("/dropDump/", HttpHandler.Method.POST, DropDumpQuery::new),
                    new HandlerRoute("/debug/unmemoize", HttpHandler.Method.POST, DebugQuery.Unmemoize::new),
                    new HandlerRoute("/debug/*", DebugQuery::new));
        return builder.build();
    }

    public HttpReader (Socket s, Server server) {
        super(s);
        this.server = server;
        this.snapshot = server.getSnapshot();
    }

    @Override
    protected QueryHandler requestHandler(HttpHandler.Method method, String query) {
        if (snapshot == null) {
            return new ErrorQuery("The heap snapshot is still being read.");
        }

        QueryHandler handler = HandlerRoute.requestHandler(routes, method, query);

        if (handler != null) {
            handler.setServer(server);
            handler.setSnapshot(snapshot);
        }

        return handler;
    }

}
