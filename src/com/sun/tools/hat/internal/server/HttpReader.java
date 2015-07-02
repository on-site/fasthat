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
import java.net.URLDecoder;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.oql.OQLEngine;

public class HttpReader extends HttpHandler {
    private class EngineThreadLocal extends ThreadLocal<OQLEngine> {
        @Override
        protected OQLEngine initialValue() {
            return new OQLEngine(snapshot);
        }
    }

    private static class HandlerRoute {
        private static final Pattern SLASH = Pattern.compile("/");
        private static final Pattern AMPER = Pattern.compile("[&;]");

        private final String name;
        private final String[] parts;
        private final Supplier<QueryHandler> handlerFactory;

        public HandlerRoute(String name, Supplier<QueryHandler> handlerFactory) {
            this.name = name;
            this.parts = SLASH.split(name, -1);
            this.handlerFactory = handlerFactory;
        }

        private static String decode(String str) {
            try {
                return URLDecoder.decode(str, "UTF-8");
            } catch (UnsupportedEncodingException exc) {
                // UTF-8 is always supported
                throw new AssertionError(exc);
            }
        }

        public QueryHandler parse(String queryString) {
            int qpos = queryString.indexOf('?');
            String query = qpos == -1 ? queryString : queryString.substring(0, qpos);
            String[] qparts = SLASH.split(query, -1);
            if (qparts.length != parts.length) {
                return null;
            }
            StringBuilder path = new StringBuilder();
            String pathInfo = null;
            StringBuilder urlStart = new StringBuilder();
            for (int i = 0; i < parts.length; ++i) {
                if (parts[i].equals("*")) {
                    pathInfo = decode(qparts[i]);
                } else if (parts[i].equals(qparts[i])) {
                    path.append('/').append(parts[i]);
                } else {
                    return null;
                }
                if (i > 1) {
                    urlStart.append("../");
                }
            }

            ImmutableListMultimap.Builder<String, String> params = ImmutableListMultimap.builder();
            if (qpos != -1) {
                for (String item : AMPER.split(queryString.substring(qpos + 1))) {
                    int epos = item.indexOf('=');
                    if (epos != -1) {
                        params.put(decode(item.substring(0, epos)),
                                   decode(item.substring(epos + 1)));
                    }
                }
            }

            QueryHandler handler = handlerFactory.get();
            handler.setPath(path.substring(2));
            handler.setUrlStart(urlStart.toString());
            handler.setQuery(pathInfo);
            handler.setParams(params.build());
            return handler;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Snapshot snapshot;
    private final EngineThreadLocal engine = new EngineThreadLocal();
    private final ImmutableList<HandlerRoute> routes = makeHandlerRoutes();

    private ImmutableList<HandlerRoute> makeHandlerRoutes() {
        final boolean isOQLSupported = OQLEngine.isOQLSupported();
        ImmutableList.Builder<HandlerRoute> builder = ImmutableList.builder();

        if (isOQLSupported) {
            builder.add(new HandlerRoute("/oql/", () -> new OQLQuery(engine)),
                        new HandlerRoute("/oqlhelp/", OQLHelp::new));
        }
        builder.add(new HandlerRoute("/", () -> new AllClassesQuery(true, isOQLSupported)),
                    new HandlerRoute("/allClassesWithPlatform/", () -> new AllClassesQuery(false, isOQLSupported)),
                    new HandlerRoute("/showRoots/", AllRootsQuery::new),
                    new HandlerRoute("/showInstanceCounts/", () -> new InstancesCountQuery(true)),
                    new HandlerRoute("/showInstanceCounts/includePlatform/", () -> new InstancesCountQuery(false)),
                    new HandlerRoute("/instances/*", () -> new InstancesQuery(false, false)),
                    new HandlerRoute("/newInstances/*", () -> new InstancesQuery(false, true)),
                    new HandlerRoute("/allInstances/*", () -> new InstancesQuery(true, false)),
                    new HandlerRoute("/allNewInstances/*", () -> new InstancesQuery(true, true)),
                    new HandlerRoute("/object/*", ObjectQuery::new),
                    new HandlerRoute("/class/*", ClassQuery::new),
                    new HandlerRoute("/roots/*", () -> new RootsQuery(false)),
                    new HandlerRoute("/allRoots/*", () -> new RootsQuery(true)),
                    new HandlerRoute("/reachableFrom/*", ReachableQuery::new),
                    new HandlerRoute("/rootStack/*", RootStackQuery::new),
                    new HandlerRoute("/histo/*", HistogramQuery::new),
                    new HandlerRoute("/refsByType/*", RefsByTypeQuery::new),
                    new HandlerRoute("/finalizerSummary/", FinalizerSummaryQuery::new),
                    new HandlerRoute("/finalizerObjects/", FinalizerObjectsQuery::new));
        return builder.build();
    }

    public HttpReader (Socket s, Snapshot snapshot) {
        super(s);
        this.snapshot = snapshot;
    }

    protected void handleRequest(String query) throws IOException {
        if (snapshot == null) {
            outputError("The heap snapshot is still being read.");
            return;
        }
        QueryHandler handler = null;
        for (HandlerRoute route : routes) {
            handler = route.parse(query);
            if (handler != null) {
                break;
            }
        }

        if (handler != null) {
            handler.setOutput(out);
            handler.setSnapshot(snapshot);
            try {
                handler.run();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                outputError(ex.getMessage());
            }
        } else {
            outputError("Query '" + query + "' not implemented");
        }
    }

}
