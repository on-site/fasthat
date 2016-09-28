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

import com.google.common.collect.ImmutableListMultimap;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

class HandlerRoute {
    private static final Pattern SLASH = Pattern.compile("/");
    private static final Pattern AMPER = Pattern.compile("[&;]");

    private final String name;
    private final HttpHandler.Method method;
    private final String[] parts;
    private final Supplier<QueryHandler> handlerFactory;

    public HandlerRoute(String name, Supplier<QueryHandler> handlerFactory) {
        this(name, HttpHandler.Method.GET, handlerFactory);
    }

    public HandlerRoute(String name, HttpHandler.Method method, Supplier<QueryHandler> handlerFactory) {
        this.name = name;
        this.method = method;
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

    public static QueryHandler requestHandler(List<HandlerRoute> routes, HttpHandler.Method method, String query) {
        for (HandlerRoute route : routes) {
            if (method != route.method) {
                continue;
            }

            QueryHandler handler = route.parse(query);
            if (handler != null) {
                return handler;
            }
        }

        return null;
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
