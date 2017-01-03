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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.sun.tools.hat.internal.annotations.ViewGetter;
import com.sun.tools.hat.internal.server.QueryHandler;
import com.sun.tools.hat.internal.util.Misc;

import java.util.Collection;

/**
 * View model for a link to another page.
 *
 * @author Mike Virata-Stone
 */
public class Link extends ViewModel {
    private static final Joiner PARAMS_JOINER = Joiner.on("&");

    private final String path;
    private final String pathInfo;
    private final String label;
    private final String name;
    private final JavaThingView clazz;
    private final ReferrerSet referrers;
    private final JavaThingView tail;
    private final Multimap<String, String> params;

    public Link(QueryHandler handler, String pathInfo, String label, JavaThingView referee, ReferrerSet referrers) {
        this(handler, pathInfo, label, referee, referrers, null);
    }

    public Link(QueryHandler handler, String pathInfo, String label, JavaThingView referee, ReferrerSet referrers, JavaThingView tail) {
        this(handler, handler.getPath(), pathInfo, label, "referee", referee, referrers, tail, null);
    }

    public Link(QueryHandler handler, String path, String pathInfo, String label, String name, JavaThingView clazz, ReferrerSet referrers, JavaThingView tail, Multimap<String, String> params) {
        super(handler);
        this.path = path;
        this.pathInfo = pathInfo;
        this.label = label;
        this.name = name;
        this.clazz = clazz;
        this.referrers = referrers;
        this.tail = tail;
        this.params = params;
    }

    @ViewGetter
    public String getPath() {
        return String.format("/%s/%s", path, Misc.encodeForURL(Strings.nullToEmpty(getPathInfo())));
    }

    private String getPathInfo() {
        if (clazz != null && !clazz.isNull() && name == null) {
            return clazz.getIdString();
        } else {
            return pathInfo;
        }
    }

    protected Multimap<String, String> getParams() {
        return params;
    }

    public Multimap<String, String> buildParams() {
        ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();

        if (getParams() != null) {
            builder.putAll(getParams());
        }

        if (clazz != null && !clazz.isNull()) {
            if (name != null) {
                builder.put(name, clazz.getIdString());
            }

            if (referrers != null) {
                builder.putAll("referrer", Collections2.transform(referrers.getReferrers(), JavaThingView::getIdString));
            }

            if (tail != null && !tail.isNull()) {
                builder.putAll("referrer", tail.getIdString());
            }
        }

        return builder.build();
    }

    @ViewGetter
    public String getQueryString() {
        Collection<String> paramStrings = Collections2.transform(buildParams().entries(),
                 entry -> String.format("%s=%s", Misc.encodeForURL(entry.getKey()), Misc.encodeForURL(entry.getValue())));
        return PARAMS_JOINER.join(paramStrings);
    }

    @ViewGetter
    public String getLabel() {
        return label;
    }
}
