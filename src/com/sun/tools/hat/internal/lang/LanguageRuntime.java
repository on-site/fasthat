/*
 * Copyright © 2011 On-Site.com.
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

package com.sun.tools.hat.internal.lang;

import com.sun.tools.hat.internal.model.Snapshot;

/**
 * Interface for language runtimes.
 *
 * <p>A language runtime has its own object system that sits on top of Java
 * objects. For example, JRuby has Java-side classes like {@code RubyObject}
 * and {@code RubyClass} for representing Ruby-side objects and classes,
 * respectively.
 *
 * <p>Language runtime instances may optionally also implement the
 * {@link SnapshotModelFactory} interface if class and object instances
 * are enumerable.
 *
 * @author Chris Jester-Young
 */
public interface LanguageRuntime {
    /**
     * Returns whether this language runtime supports the given
     * snapshot. For example, a language runtime for JRuby 1.6
     * would only return true here if the snapshot contained objects
     * from JRuby 1.6.
     *
     * @param snapshot the snapshot to check for support
     * @return whether the given snapshot is supported
     */
    boolean isSupported(Snapshot snapshot);

    /**
     * Returns a {@link ModelFactory} for the given snapshot, or null if
     * the snapshot is not {@linkplain #isSupported supported}.
     *
     * @param snapshot the snapshot to create a factory for
     * @return a factory for the given snapshot, or null
     */
    ModelFactory getFactory(Snapshot snapshot);
}
