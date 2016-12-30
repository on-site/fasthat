/*
 * Copyright © 2016 On-Site.com.
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
package com.sun.tools.hat.internal.server.view;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.lang.ClassModel;
import com.sun.tools.hat.internal.lang.CollectionModel;
import com.sun.tools.hat.internal.lang.HasSimpleForm;
import com.sun.tools.hat.internal.lang.MapModel;
import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.ModelVisitor;
import com.sun.tools.hat.internal.lang.ObjectModel;
import com.sun.tools.hat.internal.lang.ScalarModel;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaField;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaObjectArray;
import com.sun.tools.hat.internal.model.JavaStatic;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.JavaValueArray;
import com.sun.tools.hat.internal.model.StackTrace;
import com.sun.tools.hat.internal.server.QueryHandler;
import com.sun.tools.hat.internal.util.Misc;
import com.sun.tools.hat.internal.util.StreamIterable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * View model for {@link JavaThing}.
 *
 * @author Mike Virata-Stone
 */
public class JavaThingView extends ViewModel {
    private final JavaThing thing;
    private final boolean useSimpleForm;
    private final boolean showDetail;
    private final Integer limit;
    private Model model;
    private Integer instancesCountWithoutSubclasses;

    public JavaThingView(QueryHandler handler, JavaThing thing) {
        this(handler, thing, false, true, null);
    }

    public static JavaThingView detailed(QueryHandler handler, JavaThing thing) {
        return new JavaThingView(handler, thing, false, true, Integer.MAX_VALUE);
    }

    public static JavaThingView simple(QueryHandler handler, JavaThing thing) {
        return new JavaThingView(handler, thing, true, true, null);
    }

    public static JavaThingView summary(QueryHandler handler, JavaThing thing) {
        return new JavaThingView(handler, thing, false, false, null);
    }

    private JavaThingView(QueryHandler handler, JavaThing thing, boolean useSimpleForm, boolean showDetail, Integer limit) {
        super(handler);
        this.thing = thing;
        this.useSimpleForm = useSimpleForm;
        this.showDetail = showDetail;
        this.limit = limit;
    }

    public boolean isNull() {
        return thing == null;
    }

    public JavaThingView getSelf() {
        return this;
    }

    public boolean isShowingDetail() {
        return showDetail;
    }

    public boolean isJavaHeapObject() {
        return (thing instanceof JavaHeapObject);
    }

    public JavaHeapObject toJavaHeapObject() {
        return (JavaHeapObject) thing;
    }

    public boolean isJavaClass() {
        return (thing instanceof JavaClass);
    }

    public JavaClass toJavaClass() {
        return (JavaClass) thing;
    }

    public boolean isJavaObject() {
        return (thing instanceof JavaObject);
    }

    public JavaObject toJavaObject() {
        return (JavaObject) thing;
    }

    public boolean isJavaValueArray() {
        return (thing instanceof JavaValueArray);
    }

    public JavaValueArray toJavaValueArray() {
        return (JavaValueArray) thing;
    }

    public boolean isJavaObjectArray() {
        return (thing instanceof JavaObjectArray);
    }

    public JavaObjectArray toJavaObjectArray() {
        return (JavaObjectArray) thing;
    }

    public JavaThingView getClazz() {
        return new JavaThingView(handler, toJavaHeapObject().getClazz());
    }

    public String getPackageName() {
        if (isJavaClass()) {
            String name = toJavaClass().getName();
            int pos = name.lastIndexOf(".");

            if (name.startsWith("[")) {         // Only in ancient heap dumps
                return "<Arrays>";
            } else if (pos == -1) {
                return "<Default Package>";
            } else {
                return name.substring(0, pos);
            }
        }

        return null;
    }

    public String getName() {
        if (isJavaClass()) {
            return toJavaClass().getName();
        }

        return null;
    }

    public boolean isArrayClass() {
        if (isJavaClass()) {
            return getName().startsWith("[");
        }

        return false;
    }

    public Integer getInstancesCountWithoutSubclasses() {
        if (instancesCountWithoutSubclasses != null) {
            return instancesCountWithoutSubclasses;
        }

        if (isJavaClass()) {
            instancesCountWithoutSubclasses = toJavaClass().getInstancesCount(false);
            return instancesCountWithoutSubclasses;
        }

        return null;
    }

    public Long getInstancesWithoutSubclasses() {
        if (isJavaClass()) {
            try (Stream<JavaHeapObject> stream = toJavaClass().getInstances(false)) {
                return stream.filter(JavaHeapObject::isNew).count();
            }
        }

        return null;
    }

    public Long getTotalInstanceSize() {
        if (isJavaClass()) {
            return toJavaClass().getTotalInstanceSize();
        }

        return null;
    }

    public JavaThingView getSuperclass() {
        if (isJavaClass()) {
            return new JavaThingView(handler, toJavaClass().getSuperclass());
        }

        return null;
    }

    public JavaThingView getLoader() {
        if (isJavaClass()) {
            return new JavaThingView(handler, toJavaClass().getLoader());
        }

        return null;
    }

    public JavaThingView getSigners() {
        if (isJavaClass()) {
            return new JavaThingView(handler, toJavaClass().getSigners());
        }

        return null;
    }

    public JavaThingView getProtectionDomain() {
        if (isJavaClass()) {
            return new JavaThingView(handler, toJavaClass().getProtectionDomain());
        }

        return null;
    }

    public Iterable<JavaThingView> getSubclasses() {
        if (isJavaClass()) {
            List<JavaClass> subclasses = Arrays.asList(toJavaClass().getSubclasses());
            return Lists.transform(subclasses, clazz -> new JavaThingView(handler, clazz));
        }

        return null;
    }

    public Iterable<JavaFieldView> getFields() {
        if (isJavaClass()) {
            return new StreamIterable<>(Arrays.stream(toJavaClass().getFields())
                    .sorted(Ordering.natural().onResultOf(JavaField::getName))
                    .map(field -> new JavaFieldView(handler, field)));
        }

        return null;
    }

    public Iterable<JavaFieldView.WithValue> getFieldsWithValues() {
        if (isJavaObject()) {
            JavaField[] fields = getClazz().toJavaClass().getFieldsForInstance();
            JavaThing[] values = toJavaObject().getFields();
            ImmutableList.Builder<JavaFieldView.WithValue> builder = ImmutableList.builder();

            for (int i = 0; i < fields.length; ++i) {
                builder.add(new JavaFieldView(handler, fields[i]).withValue(new JavaThingView(handler, values[i])));
            }

            return new StreamIterable<>(builder.build().stream()
                    .sorted(Ordering.natural().onResultOf(fieldWithValue -> fieldWithValue.getField().getName())));
        }

        return null;
    }

    public String valueString() {
        if (isJavaValueArray()) {
            return toJavaValueArray().valueString(true);
        }

        return null;
    }

    public Integer getLength() {
        if (isJavaObjectArray()) {
            return toJavaObjectArray().getLength();
        }

        return null;
    }

    public Iterable<ArrayElementView> getElements() {
        if (isJavaObjectArray()) {
            JavaThing[] elements = toJavaObjectArray().getElements();
            ImmutableList.Builder<ArrayElementView> builder = ImmutableList.builder();

            for (int i = 0; i < elements.length; ++i) {
                builder.add(new ArrayElementView(handler, i, new JavaThingView(handler, elements[i])));
            }

            return builder.build();
        }

        return null;
    }

    public Iterable<JavaStaticView> getStatics() {
        if (isJavaClass()) {
            List<JavaStatic> statics = Arrays.asList(toJavaClass().getStatics());
            return Lists.transform(statics, s -> new JavaStaticView(handler, s));
        }

        return null;
    }

    public String getInstancesCountWithoutSubclassesLabel() {
        if (getInstancesCountWithoutSubclasses() == null) {
            return null;
        } else {
            return Misc.pluralize(getInstancesCountWithoutSubclasses(), "instance");
        }
    }

    public long getId() {
        return toJavaHeapObject().getId();
    }

    public String getUrlEncodedId() {
        if (isValidId()) {
            return getIdString();
        } else if (isJavaClass()) {
            return Misc.encodeForURL(getName());
        } else {
            throw new UnsupportedOperationException("Url encoded id not implemented for type: " + thing.getClass());
        }
    }

    public String getIdString() {
        return toJavaHeapObject().getIdString();
    }

    public String getHexId() {
        return Misc.toHex(getId());
    }

    public boolean isValidId() {
        return toJavaHeapObject().getId() != -1L;
    }

    public boolean isNew() {
        return toJavaHeapObject().isNew();
    }

    public int getSize() {
        return toJavaHeapObject().getSize();
    }

    public Iterable<RefererView> getReferers() {
        return new StreamIterable<>(toJavaHeapObject().getReferers().stream()
                .map(ref -> new RefererView(handler, ref, thing)));
    }

    public StackTraceView getAllocatedStackTrace() {
        StackTrace trace = toJavaHeapObject().getAllocatedFrom();

        if (trace != null && trace.getFrames().length > 0) {
            return new StackTraceView(handler, trace);
        }

        return null;
    }

    public Model getModel() {
        if (handler.isRawMode()) {
            return null;
        }

        if (model != null) {
            return model;
        }

        for (ModelFactory factory : handler.getSnapshot().getModelFactories()) {
            model = factory.newModel(thing);
            if (model != null) {
                if (useSimpleForm) {
                    return model instanceof HasSimpleForm
                            ? ((HasSimpleForm) model).getSimpleFormModel() : null;
                }
                return model;
            }
        }

        return model;
    }

    public String getModelSummary() {
        if (getModel() != null) {
            String[] result = new String[1];

            getModel().visit(new ModelVisitor() {
                @Override
                public void visit(ScalarModel model) {
                    result[0] = model.toString();
                }

                @Override
                public void visit(CollectionModel model) {
                    result[0] = thing.toString();
                }

                @Override
                public void visit(MapModel model) {
                    result[0] = thing.toString();
                }

                @Override
                public void visit(ObjectModel model) {
                    result[0] = model.getClassModel().getName();
                }

                @Override
                public void visit(ClassModel model) {
                    result[0] = model.getName();
                }
            });

            return result[0];
        } else {
            return thing.toString();
        }
    }

    public CollectionModelView getCollectionModel() {
        if (getModel() != null && getModel() instanceof CollectionModel) {
            return new CollectionModelView(handler, (CollectionModel) getModel(), limit);
        }

        return null;
    }

    public MapModelView getMapModel() {
        if (getModel() != null && getModel() instanceof MapModel) {
            return new MapModelView(handler, (MapModel) getModel(), limit);
        }

        return null;
    }

    public ObjectModelView getObjectModel() {
        if (getModel() != null && getModel() instanceof ObjectModel) {
            return new ObjectModelView(handler, (ObjectModel) getModel());
        }

        return null;
    }

    public ClassModelView getClassModel() {
        if (getModel() != null && getModel() instanceof ClassModel) {
            return new ClassModelView(handler, (ClassModel) getModel());
        }

        return null;
    }

    @Override
    public String toString() {
        return thing.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(thing);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JavaThingView)) {
            return false;
        }

        if (other == null) {
            return false;
        }

        return Objects.equals(thing, ((JavaThingView) other).thing);
    }
}
