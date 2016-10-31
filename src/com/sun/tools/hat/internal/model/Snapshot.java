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

package com.sun.tools.hat.internal.model;

import java.lang.ref.SoftReference;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.LanguageRuntime;
import com.sun.tools.hat.internal.oql.OQLEngine;
import com.sun.tools.hat.internal.parser.LoadProgress;
import com.sun.tools.hat.internal.parser.ReadBuffer;
import com.sun.tools.hat.internal.util.Misc;

/**
 *
 * @author      Bill Foote
 */

/**
 * Represents a snapshot of the Java objects in the VM at one instant.
 * This is the top-level "model" object read out of a single .hprof or .bod
 * file.
 */

public class Snapshot {

    public static long SMALL_ID_MASK = 0x0FFFFFFFFL;
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private static final JavaField[] EMPTY_FIELD_ARRAY = new JavaField[0];
    private static final JavaStatic[] EMPTY_STATIC_ARRAY = new JavaStatic[0];

    // all heap objects
    private final Map<Number, JavaHeapObject> heapObjects = new HashMap<>();

    private final Map<Number, JavaClass> fakeClasses = new HashMap<>();

    // all Roots in this Snapshot
    private final List<Root> roots = new ArrayList<>();

    // name-to-class map
    private final Map<String, JavaClass> classes = new TreeMap<>();

    // new objects relative to a baseline
    private final Set<JavaHeapObject> newObjects = new HashSet<>();

    // allocation site traces for all objects
    private final Map<JavaHeapObject, StackTrace> siteTraces = new HashMap<>();

    // object-to-Root map for all objects
    private final Map<JavaHeapObject, Root> rootsMap = new HashMap<>();

    // soft cache of finalizeable objects - lazily initialized
    private SoftReference<List<JavaHeapObject>> finalizablesCache;

    // represents null reference
    private JavaThing nullThing;

    // java.lang.ref.Reference class
    private JavaClass weakReferenceClass;
    // index of 'referent' field in java.lang.ref.Reference class
    private int referentFieldIndex;

    // java.lang.Class class
    private JavaClass javaLangClass;
    // java.lang.String class
    private JavaClass javaLangString;
    // java.lang.ClassLoader class
    private JavaClass javaLangClassLoader;

    // unknown "other" array class
    private volatile JavaClass otherArrayType;
    // Stuff to exclude from reachable query
    private ReachableExcludes reachableExcludes;
    // the underlying heap dump buffer
    private ReadBuffer readBuf;

    // True iff some heap objects have isNew set
    private boolean hasNewSet;
    private boolean unresolvedObjectsOK;

    // whether object array instances have new style class or
    // old style (element) class.
    private boolean newStyleArrayClass;

    // object id size in the heap dump
    private int identifierSize = 4;

    // minimum object size - accounts for object header in
    // most Java virtual machines - we assume 2 identifierSize
    // (which is true for Sun's hotspot JVM).
    private int minimumObjectSize;

    private volatile ImmutableList<ModelFactory> modelFactories;

    private final ThreadLocal<OQLEngine> oqlEngine = new ThreadLocal<OQLEngine>() {
        @Override
        protected OQLEngine initialValue() {
            return new OQLEngine(Snapshot.this);
        }
    };

    public Snapshot(ReadBuffer buf) {
        nullThing = new HackJavaValue("<null>", 0);
        readBuf = buf;
    }

    public OQLEngine getOqlEngine() {
        return oqlEngine.get();
    }

    public void setSiteTrace(JavaHeapObject obj, StackTrace trace) {
        if (trace != null && trace.getFrames().length != 0) {
            siteTraces.put(obj, trace);
        }
    }

    public StackTrace getSiteTrace(JavaHeapObject obj) {
        return siteTraces.get(obj);
    }

    public void setNewStyleArrayClass(boolean value) {
        newStyleArrayClass = value;
    }

    public boolean isNewStyleArrayClass() {
        return newStyleArrayClass;
    }

    public void setIdentifierSize(int size) {
        identifierSize = size;
        minimumObjectSize = 2 * size;
    }

    public int getIdentifierSize() {
        return identifierSize;
    }

    public int getMinimumObjectSize() {
        return minimumObjectSize;
    }

    public void addHeapObject(long id, JavaHeapObject ho) {
        heapObjects.put(makeId(id), ho);
    }

    public void addRoot(Root r) {
        r.setIndex(roots.size());
        roots.add(r);
    }

    public void addClass(long id, JavaClass c) {
        addHeapObject(id, c);
        putInClassesMap(c);
    }

    JavaClass addFakeInstanceClass(long classID, int instSize) {
        // Create a fake class name based on ID.
        String name = "unknown-class<@" + Misc.toHex(classID) + ">";

        // Create fake fields convering the given instance size.
        // Create as many as int type fields and for the left over
        // size create byte type fields.
        int numInts = instSize / 4;
        int numBytes = instSize % 4;
        JavaField[] fields = new JavaField[numInts + numBytes];
        int i;
        for (i = 0; i < numInts; i++) {
            fields[i] = new JavaField("unknown-field-" + i, "I");
        }
        for (i = 0; i < numBytes; i++) {
            fields[i + numInts] = new JavaField("unknown-field-" +
                                                i + numInts, "B");
        }

        // Create fake instance class
        JavaClass c = new JavaClass(name, 0, 0, 0, 0, fields,
                                 EMPTY_STATIC_ARRAY, instSize);
        // Add the class
        addFakeClass(makeId(classID), c);
        return c;
    }


    /**
     * @return true iff it's possible that some JavaThing instances might
     *          isNew set
     *
     * @see JavaHeapObject#isNew()
     */
    public boolean getHasNewSet() {
        return hasNewSet;
    }

    /**
     * Called after reading complete, to initialize the structure
     */
    public void resolve(LoadProgress loadProgress, boolean calculateRefs, boolean preCacheHistograms) {
        System.out.println("Resolving " + heapObjects.size() + " objects...");

        // First, resolve the classes.  All classes must be resolved before
        // we try any objects, because the objects use classes in their
        // resolution.
        javaLangClass = findClass("java.lang.Class");
        if (javaLangClass == null) {
            System.err.println("WARNING:  hprof file does not include java.lang.Class!");
            javaLangClass = new JavaClass("java.lang.Class", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClass);
        }
        javaLangString = findClass("java.lang.String");
        if (javaLangString == null) {
            System.err.println("WARNING:  hprof file does not include java.lang.String!");
            javaLangString = new JavaClass("java.lang.String", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangString);
        }
        javaLangClassLoader = findClass("java.lang.ClassLoader");
        if (javaLangClassLoader == null) {
            System.err.println("WARNING:  hprof file does not include java.lang.ClassLoader!");
            javaLangClassLoader = new JavaClass("java.lang.ClassLoader", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClassLoader);
        }

        LoadProgress.TickedProgress progress = loadProgress.startTickedProgress("Resolving objects", heapObjects.size() * 2);

        for (JavaHeapObject t : heapObjects.values()) {
            if (t instanceof JavaClass) {
                t.resolve(this);
            }

            progress.tick();
        }

        // Now, resolve everything else.
        for (JavaHeapObject t : heapObjects.values()) {
            if (!(t instanceof JavaClass)) {
                t.resolve(this);
            }

            progress.tick();
        }

        loadProgress.end();
        heapObjects.putAll(fakeClasses);
        fakeClasses.clear();

        weakReferenceClass = findClass("java.lang.ref.Reference");
        if (weakReferenceClass == null)  {      // JDK 1.1.x
            weakReferenceClass = findClass("sun.misc.Ref");
            referentFieldIndex = 0;
        } else {
            JavaField[] fields = weakReferenceClass.getFieldsForInstance();
            for (int i = 0; i < fields.length; i++) {
                if ("referent".equals(fields[i].getName())) {
                    referentFieldIndex = i;
                    break;
                }
            }
        }

        if (calculateRefs) {
            calculateReferencesToObjects(loadProgress);
        }
        progress = loadProgress.startTickedProgress("Eliminating duplicate references", heapObjects.size());
        for (JavaHeapObject t : heapObjects.values()) {
            t.setupReferers();
            progress.tick();
        }
        loadProgress.end();

        if (preCacheHistograms) {
            preCacheHistograms(loadProgress);
        }
    }

    private void calculateReferencesToObjects(LoadProgress loadProgress) {
        LoadProgress.TickedProgress progress = loadProgress.startTickedProgress("Chasing references", heapObjects.size() + roots.size());
        for (final JavaHeapObject t : heapObjects.values()) {
            // call addReferenceFrom(t) on all objects t references:
            t.visitReferencedObjects(other -> other.addReferenceFrom(t));
            progress.tick();
        }
        for (Root r : roots) {
            r.resolve(this);
            JavaHeapObject t = findThing(r.getId());
            if (t != null) {
                t.addReferenceFromRoot(r);
            }
            progress.tick();
        }
        loadProgress.end();
    }

    private void preCacheHistograms(LoadProgress loadProgress) {
        LoadProgress.TickedProgress progress = loadProgress.startTickedProgress("Pre-caching histograms", classes.size());

        classes.values().parallelStream().forEach(clazz -> {
            clazz.cacheTotalInstanceSize();
            progress.tick();
        });

        progress.end();
    }

    public void markNewRelativeTo(Snapshot baseline) {
        hasNewSet = true;
        for (JavaHeapObject t : heapObjects.values()) {
            boolean isNew;
            long thingID = t.getId();
            if (thingID == 0L || thingID == -1L) {
                isNew = false;
            } else {
                JavaThing other = baseline.findThing(t.getId());
                if (other == null) {
                    isNew = true;
                } else {
                    isNew = !t.isSameTypeAs(other);
                }
            }
            t.setNew(isNew);
        }
    }

    public Collection<JavaHeapObject> getThings() {
        return heapObjects.values();
    }


    public JavaHeapObject findThing(long id) {
        Number idObj = makeId(id);
        JavaHeapObject jho = heapObjects.get(idObj);
        return jho != null? jho : fakeClasses.get(idObj);
    }

    public JavaHeapObject findThing(String id) {
        return findThing(Misc.parseHex(id));
    }

    public JavaClass findClass(String name) {
        if (name.startsWith("0x")) {
            return (JavaClass) findThing(name);
        } else {
            return classes.get(name);
        }
    }

    /**
     * Return an Iterator of all of the classes in this snapshot.
     **/
    public Collection<JavaClass> getClasses() {
        // note that because classes is a TreeMap
        // classes are already sorted by name
        return Collections.unmodifiableCollection(classes.values());
    }

    public JavaClass[] getClassesArray() {
        return classes.values().toArray(new JavaClass[classes.size()]);
    }

    public synchronized Collection<JavaHeapObject> getFinalizerObjects() {
        if (finalizablesCache != null) {
            List<JavaHeapObject> obj = finalizablesCache.get();
            if (obj != null) {
                return obj;
            }
        }

        JavaClass clazz = findClass("java.lang.ref.Finalizer");
        JavaObject queue = (JavaObject) clazz.getStaticField("queue");
        JavaThing tmp = queue.getField("head");
        List<JavaHeapObject> finalizables = new ArrayList<>();
        if (tmp != getNullThing()) {
            JavaObject head = (JavaObject) tmp;
            while (true) {
                JavaHeapObject referent = (JavaHeapObject) head.getField("referent");
                JavaThing next = head.getField("next");
                if (next == getNullThing() || next.equals(head)) {
                    break;
                }
                head = (JavaObject) next;
                finalizables.add(referent);
            }
        }
        finalizablesCache = new SoftReference<>(finalizables);
        return finalizables;
    }

    public Collection<Root> getRoots() {
        return roots;
    }

    public Root getRootAt(int i) {
        return roots.get(i);
    }

    public ImmutableList<ReferenceChain>
    rootsetReferencesTo(JavaHeapObject target, boolean includeWeak) {
        Queue<ReferenceChain> fifo = new ArrayDeque<>();
            // Must be a fifo to go breadth-first
        Set<JavaHeapObject> visited = new HashSet<>();
        // Objects are added here right after being added to fifo.
        ImmutableList.Builder<ReferenceChain> result = ImmutableList.builder();
        visited.add(target);
        fifo.add(new ReferenceChain(target, null));

        while (!fifo.isEmpty()) {
            ReferenceChain chain = fifo.remove();
            JavaHeapObject curr = chain.getObj();
            if (curr.getRoot() != null) {
                result.add(chain);
                // Even though curr is in the rootset, we want to explore its
                // referers, because they might be more interesting.
            }
            for (JavaHeapObject t : curr.getReferers()) {
                if (t != null && !visited.contains(t)) {
                    if (includeWeak || !t.refersOnlyWeaklyTo(this, curr)) {
                        visited.add(t);
                        fifo.add(new ReferenceChain(t, chain));
                    }
                }
            }
        }
        return result.build();
    }

    public boolean getUnresolvedObjectsOK() {
        return unresolvedObjectsOK;
    }

    public void setUnresolvedObjectsOK(boolean v) {
        unresolvedObjectsOK = v;
    }

    public JavaClass getWeakReferenceClass() {
        return weakReferenceClass;
    }

    public int getReferentFieldIndex() {
        return referentFieldIndex;
    }

    public JavaThing getNullThing() {
        return nullThing;
    }

    public void setReachableExcludes(ReachableExcludes e) {
        reachableExcludes = e;
    }

    public ReachableExcludes getReachableExcludes() {
        return reachableExcludes;
    }

    // package privates
    void addReferenceFromRoot(Root r, JavaHeapObject obj) {
        Root root = rootsMap.get(obj);
        if (root == null) {
            rootsMap.put(obj, r);
        } else {
            rootsMap.put(obj, root.mostInteresting(r));
        }
    }

    Root getRoot(JavaHeapObject obj) {
        return rootsMap.get(obj);
    }

    JavaClass getJavaLangClass() {
        return javaLangClass;
    }

    JavaClass getJavaLangString() {
        return javaLangString;
    }

    JavaClass getJavaLangClassLoader() {
        return javaLangClassLoader;
    }

    JavaClass getOtherArrayType() {
        if (otherArrayType == null) {
            synchronized(this) {
                if (otherArrayType == null) {
                    addFakeClass(new JavaClass("[<other>", 0, 0, 0, 0,
                                     EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY,
                                     0));
                    otherArrayType = findClass("[<other>");
                }
            }
        }
        return otherArrayType;
    }

    JavaClass getArrayClass(String elementSignature) {
        JavaClass clazz;
        synchronized(classes) {
            clazz = findClass("[" + elementSignature);
            if (clazz == null) {
                clazz = new JavaClass("[" + elementSignature, 0, 0, 0, 0,
                                   EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
                addFakeClass(clazz);
                // This is needed because the JDK only creates Class structures
                // for array element types, not the arrays themselves.  For
                // analysis, though, we need to pretend that there's a
                // JavaClass for the array type, too.
            }
        }
        return clazz;
    }

    ReadBuffer getReadBuffer() {
        return readBuf;
    }

    void setNew(JavaHeapObject obj, boolean isNew) {
        if (isNew) {
            newObjects.add(obj);
        }
    }

    boolean isNew(JavaHeapObject obj) {
        return newObjects.contains(obj);
    }

    // Internals only below this point
    private Number makeId(long id) {
        if (identifierSize == 4) {
            return (int)id;
        } else {
            return id;
        }
    }

    private void putInClassesMap(JavaClass c) {
        String name = c.getName();
        if (classes.containsKey(name)) {
            // more than one class can have the same name
            // if so, create a unique name by appending
            // - and id string to it.
            name += "-" + c.getIdString();
        }
        classes.put(name, c);
    }

    private void addFakeClass(JavaClass c) {
        putInClassesMap(c);
        c.resolve(this);
    }

    private void addFakeClass(Number id, JavaClass c) {
        fakeClasses.put(id, c);
        addFakeClass(c);
    }

    public ImmutableList<ModelFactory> getModelFactories() {
        return modelFactories;
    }

    public void setUpModelFactories(LanguageRuntime... runtimes) {
        ImmutableList.Builder<ModelFactory> builder = ImmutableList.builder();
        for (LanguageRuntime runtime : runtimes) {
            if (runtime.isSupported(this)) {
                builder.add(runtime.getFactory(this));
            }
        }
        modelFactories = builder.build();
    }
}
