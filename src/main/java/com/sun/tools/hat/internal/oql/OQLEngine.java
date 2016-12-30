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

package com.sun.tools.hat.internal.oql;

import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.Snapshot;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * This is Object Query Language Interpreter
 *
 */
public class OQLEngine {
    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine jse = manager.getEngineByName("nashorn");
        oqlSupported = jse != null;
    }

    // check OQL is supported or not before creating OQLEngine
    public static boolean isOQLSupported() {
        return oqlSupported;
    }

    public OQLEngine(Snapshot snapshot) {
        if (!isOQLSupported()) {
            throw new UnsupportedOperationException("OQL not supported");
        }
        init(snapshot);
    }

    public synchronized void executeQuery(String query, ObjectVisitor visitor) throws OQLException {
        try (Stream<Object> stream = executeQuery(query)) {
            Iterator<Object> iterator = stream.iterator();
            while (iterator.hasNext()) {
                if (visitor.visit(iterator.next())) {
                    return;
                }
            }
        }
    }

    /**
       Query is of the form

          select &lt;java script code to select&gt;
          [ from [instanceof] &lt;class name&gt; [&lt;identifier&gt;]
            [ where &lt;java script boolean expression&gt; ]
          ]
    */
    public synchronized Stream<Object> executeQuery(String query) throws OQLException {
        debugPrint("query : " + query);
        StringTokenizer st = new StringTokenizer(query);
        if (st.hasMoreTokens()) {
            String first = st.nextToken();
            if (! first.equals("select") ) {
                // Query does not start with 'select' keyword.
                // Just treat it as plain JavaScript and eval it.
                try {
                    return Stream.of(evalScript(query));
                } catch (Exception e) {
                    throw new OQLException(e);
                }
            }
        } else {
            throw new OQLException("query syntax error: no 'select' clause");
        }

        String selectExpr = "";
        boolean seenFrom = false;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("from")) {
                seenFrom = true;
                break;
            }
            selectExpr += " " + tok;
        }

        if (selectExpr.equals("")) {
            throw new OQLException("query syntax error: 'select' expression can not be empty");
        }

        String className = null;
        boolean isInstanceOf = false;
        String whereExpr =  null;
        String identifier = null;

        if (seenFrom) {
            if (st.hasMoreTokens()) {
                String tmp = st.nextToken();
                if (tmp.equals("instanceof")) {
                    isInstanceOf = true;
                    if (! st.hasMoreTokens()) {
                        throw new OQLException("no class name after 'instanceof'");
                    }
                    className = st.nextToken();
                } else {
                    className = tmp;
                }
            } else {
                throw new OQLException("query syntax error: class name must follow 'from'");
            }

            if (st.hasMoreTokens()) {
                identifier = st.nextToken();
                if (identifier.equals("where")) {
                    throw new OQLException("query syntax error: identifier should follow class name");
                }
                if (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    if (! tmp.equals("where")) {
                        throw new OQLException("query syntax error: 'where' clause expected after 'from' clause");
                    }

                    whereExpr = "";
                    while (st.hasMoreTokens()) {
                        whereExpr += " " + st.nextToken();
                    }
                    if (whereExpr.equals("")) {
                        throw new OQLException("query syntax error: 'where' clause cannot have empty expression");
                    }
                }
            } else {
                throw new OQLException("query syntax error: identifier should follow class name");
            }
        }

        return executeQuery(new OQLQuery(selectExpr, isInstanceOf, className, identifier, whereExpr));
    }

    private Stream<Object> executeQuery(OQLQuery q) throws OQLException {
        JavaClass clazz = null;
        if (q.className != null) {
            clazz = snapshot.findClass(q.className);
            if (clazz == null) {
                throw new OQLException(q.className + " is not found!");
            }
        }

        String selectCode = String.format("function __select__(%s) { return %s; }",
                Objects.toString(q.identifier, ""), q.selectExpr.replace('\n', ' '));
        debugPrint(selectCode);
        String whereCode = null;
        if (q.whereExpr != null) {
            whereCode = String.format("function __where__(%s) { return %s; }",
                    q.identifier, q.whereExpr.replace('\n', ' '));
        }
        debugPrint(whereCode);

        // compile select expression and where condition
        try {
            evalScript(selectCode);
            if (whereCode != null) {
                evalScript(whereCode);
            }

            if (clazz != null) {
                boolean whereIsNull = whereCode == null;

                return clazz.getInstances(q.isInstanceOf).filter(obj -> {
                    try {
                        Object[] args = { wrapJavaObject(obj) };
                        boolean b = whereIsNull;
                        if (!b) {
                            Object res = call("__where__", args);
                            if (res instanceof Boolean) {
                                b = ((Boolean)res).booleanValue();
                            } else if (res instanceof Number) {
                                b = ((Number)res).intValue() != 0;
                            } else {
                                b = (res != null);
                            }
                        }

                        return b;
                    } catch (NoSuchMethodException | ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }).map(obj -> {
                    try {
                        return call("__select__", wrapJavaObject(obj));
                    } catch (NoSuchMethodException | ScriptException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                // simple "select <expr>" query
                return Stream.of(call("__select__"));
            }
        } catch (Exception e) {
            throw new OQLException(e);
        }
    }

    public Object evalScript(String script) throws ScriptException {
        return engine.eval(script);
    }

    public Object wrapJavaObject(JavaHeapObject obj) throws ScriptException, NoSuchMethodException {
        return call("wrapJavaObject", obj);
    }

    public Object toHtml(Object obj) throws ScriptException, NoSuchMethodException {
        return call("toHtml", obj);
    }

    public Object call(String func, Object... args) throws ScriptException, NoSuchMethodException {
        return ((Invocable) engine).invokeFunction(func, args);
    }

    private static void debugPrint(String msg) {
        if (debug) System.err.println(msg);
    }

    private void init(Snapshot snapshot) throws RuntimeException {
        this.snapshot = snapshot;
        ScriptEngineManager manager = new ScriptEngineManager();
        try {
            engine = manager.getEngineByName("nashorn");
            engine.eval(new InputStreamReader(getInitStream()));
            engine.put("heap", call("wrapHeapSnapshot", snapshot));
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private InputStream getInitStream() {
        return getClass().getResourceAsStream("/com/sun/tools/hat/resources/hat.js");
    }

    private ScriptEngine engine;
    private Snapshot snapshot;
    private static boolean debug = false;
    private static final boolean oqlSupported;
}
