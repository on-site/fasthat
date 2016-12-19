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

import com.sun.tools.hat.internal.model.JavaField;
import com.sun.tools.hat.internal.server.QueryHandler;

/**
 * View model for {@link JavaField}.
 *
 * @author Mike Virata-Stone
 */
public class JavaFieldView extends ViewModel {
    private final JavaField field;

    public JavaFieldView(QueryHandler handler, JavaField field) {
        super(handler);
        this.field = field;
    }

    public boolean hasId() {
        return field.hasId();
    }

    public String getName() {
        return field.getName();
    }

    public String getSignature() {
        return field.getSignature();
    }

    public WithValue withValue(JavaThingView value) {
        return new WithValue(handler, this, value);
    }

    /**
     * A field paired with its value.
     */
    public static class WithValue extends ViewModel {
        private final JavaFieldView field;
        private final JavaThingView value;

        public WithValue(QueryHandler handler, JavaFieldView field, JavaThingView value) {
            super(handler);
            this.field = field;
            this.value = value;
        }

        public JavaFieldView getField() {
            return field;
        }

        public JavaThingView getValue() {
            return value;
        }
    }
}
