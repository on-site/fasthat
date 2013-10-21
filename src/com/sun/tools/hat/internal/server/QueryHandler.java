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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.sun.tools.hat.internal.lang.AbstractScalarModel;
import com.sun.tools.hat.internal.lang.ClassModel;
import com.sun.tools.hat.internal.lang.CollectionModel;
import com.sun.tools.hat.internal.lang.MapModel;
import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.ModelVisitor;
import com.sun.tools.hat.internal.lang.ObjectModel;
import com.sun.tools.hat.internal.lang.ScalarModel;
import com.sun.tools.hat.internal.lang.SimpleScalarModel;
import com.sun.tools.hat.internal.model.*;
import com.sun.tools.hat.internal.util.Misc;
import java.io.StringWriter;

import java.net.URLEncoder;
import java.util.Collection;
import java.util.Formatter;
import java.util.Map;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author      Bill Foote
 */


abstract class QueryHandler implements Runnable {
    protected enum GetIdString implements Function<JavaClass, String> {
        INSTANCE;

        @Override
        public String apply(JavaClass clazz) {
            return clazz.getIdString();
        }
    }

    protected static class ClassResolver implements Function<String, JavaClass> {
        private final Snapshot snapshot;
        private final boolean valueRequired;

        public ClassResolver(Snapshot snapshot, boolean valueRequired) {
            this.snapshot = snapshot;
            this.valueRequired = valueRequired;
        }

        @Override
        public JavaClass apply(String name) {
            if (name == null && !valueRequired) {
                return null;
            }
            JavaClass result = snapshot.findClass(name);
            Preconditions.checkNotNull(result, "class not found: %s", name);
            return result;
        }
    }

    protected String path;
    protected String urlStart;
    protected String query;
    protected PrintWriter out;
    protected Snapshot snapshot;
    protected ImmutableListMultimap<String, String> params;

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

    void setSnapshot(Snapshot ss) {
        this.snapshot = ss;
    }

    void setParams(ImmutableListMultimap<String, String> params) {
        this.params = params;
    }

    protected static String encodeForURL(String s) {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Should never happen
            throw new AssertionError(ex);
        }
        return s;
    }

    protected void startHtml(String title) {
        out.print("<html><title>");
        print(title);
        out.println("</title>");
        out.println("<body bgcolor=\"#ffffff\"><center><h1>");
        print(title);
        out.println("</h1></center>");
    }

    protected void endHtml() {
        out.println("</body></html>");
    }

    protected void error(String msg) {
        println(msg);
    }

    protected void printAnchorStart() {
        out.print("<a href=\"");
        out.print(urlStart);
    }

    protected void printThingAnchorTag(long id) {
        printAnchorStart();
        out.print("object/");
        printHex(id);
        out.print("\">");
    }

    protected void printObject(JavaObject obj) {
        printThing(obj);
    }

    protected void printThing(JavaThing thing) {
        printImpl(thing, true, true, null);
    }

    protected void printDetailed(JavaThing thing) {
        printImpl(thing, true, true, Integer.MAX_VALUE);
    }

    protected void printSimple(JavaThing thing) {
        printImpl(thing, false, true, null);
    }

    protected void printSummary(JavaThing thing) {
        printImpl(thing, true, false, null);
    }

    private void printImpl(JavaThing thing, boolean useNonScalarModel,
            boolean showDetail, Integer limit) {
        if (thing == null) {
            out.print("null");
            return;
        }

        if (thing instanceof JavaHeapObject) {
            JavaHeapObject ho = (JavaHeapObject) thing;
            long id = ho.getId();
            if (id != -1L) {
                printThingAnchorTag(id);
                if (ho.isNew())
                    out.print("<strong>");
            }
            Model model = getModelFor(thing, useNonScalarModel);
            printSummary(model, thing);
            if (id != -1) {
                if (ho.isNew())
                    out.print("[new]</strong>");
                out.print("</a>");
                if (showDetail) {
                    printDetail(model, ho.getSize(), limit);
                }
            }
        } else {
            print(thing.toString());
        }
    }

    protected void printRoot(Root root) {
        StackTrace st = root.getStackTrace();
        boolean traceAvailable = (st != null) && (st.getFrames().length != 0);
        if (traceAvailable) {
            printAnchorStart();
            out.print("rootStack/");
            printHex(root.getIndex());
            out.print("\">");
        }
        print(root.getDescription());
        if (traceAvailable) {
            out.print("</a>");
        }
    }

    protected void printClass(JavaClass clazz) {
        if (clazz == null) {
            out.println("null");
            return;
        }
        printAnchorStart();
        out.print("class/");
        print(encodeForURL(clazz));
        out.print("\">");
        print(clazz.toString());
        out.println("</a>");
    }

    protected static String encodeForURL(JavaClass clazz) {
        if (clazz.getId() == -1) {
            return encodeForURL(clazz.getName());
        } else {
            return clazz.getIdString();
        }
    }

    protected void printField(JavaField field) {
        print(field.getName() + " (" + field.getSignature() + ")");
    }

    protected void printStatic(JavaStatic member) {
        JavaField f = member.getField();
        printField(f);
        out.print(" : ");
        if (f.hasId()) {
            JavaThing t = member.getValue();
            printThing(t);
        } else {
            print(member.getValue().toString());
        }
    }

    protected void printStackTrace(StackTrace trace) {
        StackFrame[] frames = trace.getFrames();
        for (StackFrame f : frames) {
            String clazz = f.getClassName();
            out.print("<font color=purple>");
            print(clazz);
            out.print("</font>");
            print("." + f.getMethodName() + "(" + f.getMethodSignature() + ")");
            out.print(" <bold>:</bold> ");
            print(f.getSourceFileName() + " line " + f.getLineNumber());
            out.println("<br>");
        }
    }

    protected void printException(Throwable t) {
        println(t.getMessage());
        out.println("<pre>");
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        print(sw.toString());
        out.println("</pre>");
    }

    protected void printHex(long addr) {
        if (snapshot.getIdentifierSize() == 4) {
            out.print(Misc.toHex((int)addr));
        } else {
            out.print(Misc.toHex(addr));
        }
    }

    protected long parseHex(String value) {
        return Misc.parseHex(value);
    }

    protected void print(String str) {
        out.print(Misc.encodeHtml(str));
    }

    private Model getModelFor(JavaThing thing, boolean useNonScalarModel) {
        for (ModelFactory factory : snapshot.getModelFactories()) {
            Model model = factory.newModel(thing);
            if (model != null && useNonScalarModel) {
                return model;
            }
            if (model instanceof ScalarModel) {
                final ScalarModel scalar = (ScalarModel) model;
                // Ensures visit() visits with ScalarModel, not whatever
                return scalar instanceof AbstractScalarModel ? scalar
                        : new SimpleScalarModel(scalar.getFactory(), scalar.toString());
            }
        }
        return null;
    }

    private void printSummary(Model model, final JavaThing thing) {
        if (model != null) {
            model.visit(new ModelVisitor() {
                @Override
                public void visit(ScalarModel model) {
                    print(model.toString());
                }

                @Override
                public void visit(CollectionModel model) {
                    print(thing.toString());
                }

                @Override
                public void visit(MapModel model) {
                    print(thing.toString());
                }

                @Override
                public void visit(ObjectModel model) {
                    print(model.getClassModel().getName());
                }

                @Override
                public void visit(ClassModel model) {
                    print(model.getName());
                }
            });
        } else {
            print(thing.toString());
        }
    }

    private void printDetail(Model model, int size, final Integer limit) {
        if (model != null) {
            model.visit(new ModelVisitor() {
                private final int lim = Objects.firstNonNull(limit, 10);

                @Override
                public void visit(ScalarModel model) {
                }

                @Override
                public void visit(CollectionModel model) {
                    out.print(" [");
                    Collection<JavaThing> collection = model.getCollection();
                    boolean first = true;
                    for (JavaThing thing : Iterables.limit(collection, lim)) {
                        if (first)
                            first = false;
                        else
                            out.print(", ");
                        printSimple(thing);
                    }
                    if (collection.size() > lim) {
                        out.printf(", &hellip;%d more", collection.size() - lim);
                    }
                    out.print("]");
                }

                @Override
                public void visit(MapModel model) {
                    out.print(" {");
                    Map<JavaThing, JavaThing> map = model.getMap();
                    boolean first = true;
                    for (Map.Entry<JavaThing, JavaThing> entry
                            : Iterables.limit(map.entrySet(), lim)) {
                        if (first)
                            first = false;
                        else
                            out.print(", ");
                        printSimple(entry.getKey());
                        out.print(" &rArr; ");
                        printSimple(entry.getValue());
                    }
                    if (map.size() > lim) {
                        out.printf(", &hellip;%d more", map.size() - lim);
                    }
                    out.print("}");
                }

                @Override
                public void visit(ObjectModel model) {
                    out.print(" {");
                    Map<String, JavaThing> map = model.getProperties();
                    boolean first = true;
                    for (Map.Entry<String, JavaThing> entry : map.entrySet()) {
                        if (first)
                            first = false;
                        else
                            out.print(", ");
                        String key = entry.getKey();
                        JavaThing value = entry.getValue();
                        print(key);
                        out.print(": ");
                        if ("@attributes".equals(key))
                            printDetailed(value);
                        else
                            printSimple(value);
                    }
                    out.print("}");
                }

                @Override
                public void visit(ClassModel model) {
                    out.print(" (");
                    Collection<ClassModel> supers = model.getSuperclasses();
                    boolean first = true;
                    for (ClassModel cls : supers) {
                        if (first)
                            first = false;
                        else
                            out.print(", ");
                        printSummary(cls.getClassObject());
                    }
                    out.print(")");
                }
            });
        } else {
            out.print(" (" + size + " bytes)");
        }
    }

    /**
     * Returns a link to <code>/<var>path</var>/<var>pathInfo</var></code>
     * with the given label and parameters.
     *
     * @param path the static portion of the link target (should only
     *             contain trusted text)
     * @param pathInfo the non-static portion of the link target (will be
     *                 URL-encoded)
     * @param label the link text to use
     * @param params any {@code GET} parameters to append to the link target
     * @return an HTML {@code <a>} tag formatted as described
     */
    protected static String formatLink(String path, String pathInfo,
            String label, Multimap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("resource")
        Formatter fmt = new Formatter(sb);
        fmt.format("<a href='/%s/%s?", path,
                encodeForURL(Strings.nullToEmpty(pathInfo)));
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entries()) {
                fmt.format("%s=%s&", encodeForURL(entry.getKey()),
                        encodeForURL(entry.getValue()));
            }
        }
        sb.setLength(sb.length() - 1);
        fmt.format("'>%s</a>", Misc.encodeHtml(label));
        return sb.toString();
    }

    /**
     * Returns a link to <code>/<var>path</var>/<var>pathInfo</var></code>
     * that can be used to construct a referrer chain. See also the related
     * {@link #printBreadcrumbs} function.
     *
     * <p>For queries that support referrer chains, there is a primary
     * class that the query works on, followed by a referrer chain that
     * further filters instances. The primary class is specified as
     * {@code clazz}.
     *
     * <p>The referrer chain is always written with parameter name
     * {@code referrer}, so the query handler should use that name to get
     * the referrer chain. Additionally, if the primary class is omitted,
     * then the referrer chain is irrelevant and will not be printed.
     *
     * @param path the static portion of the link target (should only
     *             contain trusted text)
     * @param pathInfo the non-static portion of the link target (will be
     *                 URL-encoded); ignored if {@code name} is omitted
     * @param label the link text to use
     * @param name the parameter name for referring to the primary class;
     *             if omitted, place the class reference in {@code pathInfo}
     * @param clazz the primary class in use
     * @param referrers the referrer chain in use
     * @param tail an optional element to append to the referrer chain
     * @param params any further parameters to be prepended
     * @return an HTML {@code <a>} tag formatted as described
     */
    protected static String formatLink(String path, String pathInfo,
            String label, String name, JavaClass clazz,
            Collection<JavaClass> referrers, JavaClass tail,
            Multimap<String, String> params) {
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
        if (params != null) {
            builder.putAll(params);
        }
        if (clazz != null) {
            if (name != null) {
                builder.put(name, clazz.getIdString());
            } else {
                pathInfo = clazz.getIdString();
            }
            if (referrers != null) {
                builder.putAll("referrer", Collections2.transform(referrers,
                        GetIdString.INSTANCE));
            }
            if (tail != null) {
                builder.put("referrer", tail.getIdString());
            }
        }
        return formatLink(path, pathInfo, label, builder.build());
    }

    /**
     * Prints out breadcrumbs for accessing previous elements in the
     * referrer chain.
     *
     * <p>For queries that support referrer chains, there is a primary
     * class that the query works on, followed by a referrer chain that
     * further filters instances. The primary class is specified as
     * {@code clazz}.
     *
     * <p>The referrer chain is always written with parameter name
     * {@code referrer}, so the query handler should use that name to get
     * the referrer chain. Additionally, if the primary class is omitted,
     * then the referrer chain is irrelevant and will not be printed.
     *
     * @param path the static portion of the link target (see {@link #formatLink})
     * @param pathInfo the non-static portion of the link target
     *                 (see {@link #formatLink}); ignored if {@code name}
     *                 is omitted
     * @param name the parameter name for referring to the primary class;
     *             if omitted, place the class reference in {@code pathInfo}
     * @param clazz the primary class in use
     * @param referrers the referrer chain in use
     * @param params any further parameters to be prepended
     */
    protected void printBreadcrumbs(String path, String pathInfo,
            String name, JavaClass clazz, Iterable<JavaClass> referrers,
            Multimap<String, String> params) {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        if (params != null) {
            builder.putAll(params);
        }
        if (clazz != null) {
            out.print("<p align='center'>");
            if (name != null) {
                builder.put(name, clazz.getIdString());
            } else {
                pathInfo = clazz.getIdString();
            }
            out.print(formatLink(path, pathInfo, clazz.getName(), builder.build()));
            for (JavaClass referrer : referrers) {
                out.print(" &rarr; ");
                builder.put("referrer", referrer.getIdString());
                out.print(formatLink(path, pathInfo, referrer.getName(), builder.build()));
            }
            out.println("</p>");
        }
    }

    protected void println(String str) {
        out.println(Misc.encodeHtml(str));
    }
}
