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

import com.google.common.collect.Iterables;
import com.sun.tools.hat.internal.lang.ClassModel;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.server.QueryHandler;

/**
 * View model for {@link ClassModel}.
 *
 * @author Mike Virata-Stone
 */
public class ClassModelView extends ViewModel {
    private final ClassModel model;

    public ClassModelView(QueryHandler handler, ClassModel model) {
        super(handler);
        this.model = model;
    }

    public Iterable<JavaThingView> getSupers() {
        Iterable<JavaThing> result = Iterables.transform(model.getSuperclasses(), ClassModel::getClassObject);
        return Iterables.transform(result, this::newJavaThingView);
    }

    private JavaThingView newJavaThingView(JavaThing thing) {
        return JavaThingView.summary(handler, thing);
    }
}
