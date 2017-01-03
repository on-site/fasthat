/*
 * Copyright © 2011, 2012 On-Site.com.
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

import com.google.common.base.Charsets;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.common.SimpleScalarModel;
import com.sun.tools.hat.internal.model.JavaInt;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaValueArray;

import java.nio.ByteBuffer;

public class JRubyString extends SimpleScalarModel {
    private JRubyString(JRuby factory, ByteBuffer value) {
        super(factory, () -> "\"" + Charsets.UTF_8.decode(value) + "\"");
    }

    public static JRubyString make(JRuby factory, JavaObject obj) {
        JavaObject value = Models.getFieldObject(obj, "value");
        if (value != null) {
            JavaValueArray bytes = Models.safeCast(value.getField("bytes"), JavaValueArray.class);
            JavaInt begin = Models.safeCast(value.getField("begin"), JavaInt.class);
            JavaInt realSize = Models.safeCast(value.getField("realSize"), JavaInt.class);
            if (bytes != null && begin != null && realSize != null) {
                return new JRubyString(factory, ByteBuffer.wrap((byte[]) bytes.getElements(),
                        begin.value, realSize.value));
            }
        }
        return null;
    }
}
