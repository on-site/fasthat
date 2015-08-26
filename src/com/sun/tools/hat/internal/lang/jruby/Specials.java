/*
 * Copyright © 2012 On-Site.com.
 * Copyright © 2015 Chris Jester-Young.
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

package com.sun.tools.hat.internal.lang.jruby;

import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.ScalarModel;
import com.sun.tools.hat.internal.lang.common.Singletons;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaObject;

/**
 * Scalar models for special values in JRuby.
 *
 * @author Chris Jester-Young
 */
public final class Specials {
    private static final Singletons.Key NIL = Singletons.Key.create("nil");
    private static final Singletons.Key FALSE = Singletons.Key.create("false");
    private static final Singletons.Key TRUE = Singletons.Key.create("true");
    private static final Singletons.Key NEVER = Singletons.Key.create("<never>");
    private static final Singletons.Key UNDEF = Singletons.Key.create("<undef>");

    /**
     * Disables instantiation for static class.
     */
    private Specials() {}

    public static ScalarModel makeNil(JRuby factory) {
        return NIL.apply(factory);
    }

    public static ScalarModel makeFalse(JRuby factory) {
        return FALSE.apply(factory);
    }

    public static ScalarModel makeTrue(JRuby factory) {
        return TRUE.apply(factory);
    }

    public static ScalarModel makeBoolean(JRuby factory, JavaObject value) {
        JavaInt flags = Models.getFieldThing(value, "flags", JavaInt.class);
        return flags == null ? null
                : (flags.value & 1) != 0 ? makeFalse(factory) : makeTrue(factory);
    }

    public static ScalarModel makeSpecial(JRuby factory, JavaObject value) {
        if (factory.isNever(value))
            return NEVER.apply(factory);
        if (factory.isUndef(value))
            return UNDEF.apply(factory);
        return null;
    }
}
