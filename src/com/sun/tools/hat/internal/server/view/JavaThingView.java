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
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.server.QueryHandler;
import com.sun.tools.hat.internal.util.Misc;

import java.util.stream.StreamSupport;

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
            return StreamSupport.stream(toJavaClass().getInstances(false).spliterator(), false).filter(JavaHeapObject::isNew).count();
        }

        return null;
    }

    public Long getTotalInstanceSize() {
        if (isJavaClass()) {
            return toJavaClass().getTotalInstanceSize();
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
}
