/*
 * Copyright (c) 2016 On-Site.com.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */
package com.sun.tools.hat.internal.server;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Test case for {@link Server}.
 *
 * @author Mike Virata-Stone
 */
public class ServerTest {
    @Test
    public void nullDumpIsAllowed() {
        assertTrue(server().allowedDumpPath(null));
    }

    @Test
    public void fileWithinDumpPathIsAllowed() {
        assertTrue(server().allowedDumpPath("/etc/heaps/someheap.hprof"));
    }

    @Test
    public void fileNestedWithinDumpPathIsAllowed() {
        assertTrue(server().allowedDumpPath("/etc/heaps/nested/someheap.hprof"));
    }

    @Test
    public void fileOutsideDumpPathIsDisallowed() {
        assertFalse(server().allowedDumpPath("/etc/someheap.hprof"));
    }

    @Test
    public void fileOutsideDumpPathViaRelativePathIsDisallowed() {
        assertFalse(server().allowedDumpPath("/etc/heaps/../outsideheap.hprof"));
    }

    @Test
    public void theDirectoryItselfIsDisallowed() {
        assertFalse(server().allowedDumpPath("/etc/heaps"));
    }

    @Test
    public void specialFilesAreDisallowed() {
        assertFalse(server().allowedDumpPath("/etc/heaps/."));
        assertFalse(server().allowedDumpPath("/etc/heaps/.."));
    }

    @Test
    public void aFileStartingWithTheHeapsDirectoryIsDisallowed() {
        assertFalse(server().allowedDumpPath("/etc/heapsFile.hprof"));
    }

    private Server server() {
        Server server = new Server();
        server.setHeapsDir("/etc/heaps");
        return server;
    }
}
