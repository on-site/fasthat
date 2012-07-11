/*
 * Copyright (c) 2012 On-Site.com.
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

package com.sun.tools.hat.internal.lang.common;

import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;
import com.sun.tools.hat.internal.lang.Model;
import com.sun.tools.hat.internal.lang.ModelFactory;
import com.sun.tools.hat.internal.lang.SimpleScalarModel;

/**
 * Static class for dealing with singleton language-specific objects
 * (e.g., nil, false, true, etc.). Currently these objects must all be
 * scalars.
 *
 * @author Chris K. Jester-Young
 */
public final class Singletons {
    public static class Key implements Function<ModelFactory, Model> {
        private final String value;

        public Key(String value) {
            this.value = value;
            FACTORY_MAP.put(this, value);
        }

        @Override
        public Model apply(ModelFactory factory) {
            return CACHE.getUnchecked(factory).getUnchecked(this);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final ConcurrentMap<Key, String> FACTORY_MAP
            = new MapMaker().weakKeys().makeMap();

    private static final LoadingCache<ModelFactory, LoadingCache<Key, Model>> CACHE
            = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<ModelFactory, LoadingCache<Key, Model>>() {
        @Override
        public LoadingCache<Key, Model> load(final ModelFactory factory) {
            return CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Key, Model>() {
                @Override
                public Model load(Key key) {
                    return new SimpleScalarModel(factory, FACTORY_MAP.get(key));
                }
            });
        }
    });

    /**
     * Disables instantiation for static class.
     */
    private Singletons() {}
}
