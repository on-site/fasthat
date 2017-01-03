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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheResolver;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.sun.tools.hat.internal.annotations.ViewGetter;
import com.sun.tools.hat.internal.util.Misc;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import java.util.function.Function;

abstract class MustacheQueryHandler extends QueryHandler {
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory(MustacheQueryHandler::getReader);
    private static final LoadingCache<Class<?>, Mustache> MUSTACHES = CacheBuilder.newBuilder().build(CacheLoader.from(MustacheQueryHandler::load));

    private static Reader getReader(String resourceName) {
        InputStream stream = MustacheQueryHandler.class.getResourceAsStream("/com/sun/tools/hat/resources/" + resourceName + ".mustache");
        Preconditions.checkState(stream != null, "Invalid resource: /com/sun/tools/hat/resources/%s.mustache", resourceName);
        return new InputStreamReader(stream);
    }

    private static Mustache load(Class<?> queryClass) {
        String name = queryClass.getSimpleName();

        try (Reader reader = MustacheQueryHandler.getReader(name)) {
            return MUSTACHE_FACTORY.compile(reader, name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Functions for templates to use
    @ViewGetter
    public static final Function<String, String> urlEncode = Misc::encodeForURL;

    @ViewGetter
    public void flush() {
        out.flush();
    }

    @Override
    public void run() {
        try {
            MUSTACHES.get(getClass()).execute(out, this);
        } catch (Exception e) {
            printException(e);
        }
    }

    private void printException(Throwable t) {
        out.println(Misc.encodeHtml(t.getMessage()));
        out.println("<pre>");
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        out.print(Misc.encodeHtml(sw.toString()));
        out.println("</pre>");
    }
}
