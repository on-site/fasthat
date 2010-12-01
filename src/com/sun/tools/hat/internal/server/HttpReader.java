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
import java.util.regex.Pattern;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.Closeables;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.oql.OQLEngine;

public class HttpReader implements Runnable {
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
            String pathInfo = null;
            StringBuilder urlStart = new StringBuilder();
            for (int i = 0; i < parts.length; ++i) {
                if (parts[i].equals("*")) {
                    pathInfo = decode(qparts[i]);
                } else if (!parts[i].equals(qparts[i])) {
                    return null;
                }
                if (i > 1) {
                    urlStart.append("../");
                }
            }

            ImmutableMultimap.Builder<String, String> params = ImmutableMultimap.builder();
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

    private final Socket socket;
    private PrintWriter out;
    private final Snapshot snapshot;
    private final EngineThreadLocal engine = new EngineThreadLocal();
    private final ImmutableList<HandlerRoute> routes = makeHandlerRoutes();

    private ImmutableList<HandlerRoute> makeHandlerRoutes() {
        final boolean isOQLSupported = OQLEngine.isOQLSupported();
        ImmutableList.Builder<HandlerRoute> builder = ImmutableList.builder();

        if (isOQLSupported) {
            builder.add(new HandlerRoute("/oql/", new Supplier<QueryHandler>() {
                public QueryHandler get() {return new OQLQuery(engine);}
            }), new HandlerRoute("/oqlhelp/", new Supplier<QueryHandler>() {
                public QueryHandler get() {return new OQLHelp();}
            }));
        }
        builder.add(new HandlerRoute("/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new AllClassesQuery(true, isOQLSupported);}
        }), new HandlerRoute("/allClassesWithPlatform/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new AllClassesQuery(false, isOQLSupported);}
        }), new HandlerRoute("/showRoots/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new AllRootsQuery();}
        }), new HandlerRoute("/showInstanceCounts/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new InstancesCountQuery(true);}
        }), new HandlerRoute("/showInstanceCounts/includePlatform/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new InstancesCountQuery(false);}
        }), new HandlerRoute("/instances/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new InstancesQuery(false, false);}
        }), new HandlerRoute("/newInstances/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new InstancesQuery(false, true);}
        }), new HandlerRoute("/allInstances/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new InstancesQuery(true, false);}
        }), new HandlerRoute("/allNewInstances/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new InstancesQuery(true, true);}
        }), new HandlerRoute("/object/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new ObjectQuery();}
        }), new HandlerRoute("/class/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new ClassQuery();}
        }), new HandlerRoute("/roots/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new RootsQuery(false);}
        }), new HandlerRoute("/allRoots/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new RootsQuery(true);}
        }), new HandlerRoute("/reachableFrom/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new ReachableQuery();}
        }), new HandlerRoute("/rootStack/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new RootStackQuery();}
        }), new HandlerRoute("/histo/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new HistogramQuery();}
        }), new HandlerRoute("/refsByType/*", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new RefsByTypeQuery();}
        }), new HandlerRoute("/finalizerSummary/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new FinalizerSummaryQuery();}
        }), new HandlerRoute("/finalizerObjects/", new Supplier<QueryHandler>() {
            public QueryHandler get() {return new FinalizerObjectsQuery();}
        }));

        return builder.build();
    }

    public HttpReader (Socket s, Snapshot snapshot) {
        this.socket = s;
        this.snapshot = snapshot;
    }

    public void run() {
        InputStream in = null;
        try {
            in = new BufferedInputStream(socket.getInputStream());
            out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(
                                socket.getOutputStream())));
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
                handler.run();
            } else {
                outputError("Query '" + query + "' not implemented");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            Closeables.closeQuietly(out);
            Closeables.closeQuietly(in);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void outputError(String msg) {
        out.println();
        out.println("<html><body bgcolor=\"#ffffff\">");
        out.println(msg);
        out.println("</body></html>");
    }

}
