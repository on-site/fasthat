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
package com.sun.tools.hat.internal.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Convenience class to turn a Stream into an Iterable. Please note that this
 * Iterable will only be able to create a single Iterator.
 *
 * @author Mike Virata-Stone
 */
public class StreamIterable<T> implements Iterable<T> {
    private final Stream<T> stream;
    private boolean closed = false;

    public StreamIterable(Stream<T> stream) {
        this.stream = stream;
    }

    @Override
    public Iterator<T> iterator() {
        return new StreamIterator();
    }

    private class StreamIterator implements Iterator<T> {
        private final Iterator<T> iterator = stream.iterator();

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }

            boolean result = iterator.hasNext();

            if (!result) {
                stream.close();
                closed = true;
            }

            return result;
        }

        @Override
        public T next() {
            if (closed) {
                throw new NoSuchElementException("The stream has been closed!");
            }

            return iterator.next();
        }
    }
}
