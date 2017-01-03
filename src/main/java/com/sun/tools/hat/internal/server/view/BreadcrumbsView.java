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

import com.sun.tools.hat.internal.annotations.ViewGetter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sun.tools.hat.internal.server.QueryHandler;

import java.util.Iterator;

/**
 * View model for the breadcrumbs links at the top of various pages.
 *
 * @author Mike Virata-Stone
 */
public class BreadcrumbsView extends ViewModel {
    private final String path;
    private final String pathInfo;
    private final String name;
    private final JavaThingView javaClass;
    private final ReferrerSet referrers;
    private final Multimap<String, String> params;

    public BreadcrumbsView(QueryHandler handler, String path, JavaThingView javaClass, ReferrerSet referrers) {
        this(handler, path, null, null, javaClass, referrers, null);
    }

    public BreadcrumbsView(QueryHandler handler, String path, String pathInfo, String name, JavaThingView javaClass, ReferrerSet referrers) {
        this(handler, path, pathInfo, name, javaClass, referrers, null);
    }

    private BreadcrumbsView(QueryHandler handler, String path, String pathInfo, String name, JavaThingView javaClass, ReferrerSet referrers, Multimap<String, String> params) {
        super(handler);
        this.path = path;
        this.name = name;
        this.javaClass = javaClass;
        this.referrers = referrers;
        this.params = params;

        if (!javaClass.isNull() && name == null) {
            this.pathInfo = javaClass.getIdString();
        } else {
            this.pathInfo = pathInfo;
        }
    }

    @ViewGetter
    public JavaThingView getJavaClass() {
        return javaClass;
    }

    @ViewGetter
    public boolean hasReferrers() {
        return !referrers.getReferrers().isEmpty();
    }

    @ViewGetter
    public Breadcrumb getBaseCrumb() {
        return new Breadcrumb(javaClass, getParamsBuilder().build());
    }

    @ViewGetter
    public BreadcrumbIterable getCrumbs() {
        return new BreadcrumbIterable();
    }

    private ImmutableMultimap.Builder<String, String> getParamsBuilder() {
        ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();

        if (params != null) {
            builder.putAll(params);
        }

        if (!javaClass.isNull() && name != null) {
            builder.put(name, javaClass.getIdString());
        }

        return builder;
    }

    public class BreadcrumbIterable implements Iterable<Breadcrumb> {
        @Override
        public BreadcrumbIterator iterator() {
            return new BreadcrumbIterator();
        }
    }

    public class BreadcrumbIterator implements Iterator<Breadcrumb> {
        private final Iterator<JavaThingView> iterator = referrers.getReferrers().iterator();
        private final ImmutableMultimap.Builder<String, String> paramsBuilder = getParamsBuilder();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Breadcrumb next() {
            JavaThingView referrer = iterator.next();
            paramsBuilder.put("referrer", referrer.getIdString());
            return new Breadcrumb(referrer, paramsBuilder.build());
        }
    }

    public class Breadcrumb extends Link {
        private Breadcrumb(JavaThingView thing, ImmutableMultimap<String, String> params) {
            super(BreadcrumbsView.this.handler, path, pathInfo, thing.getName(), null, null, null, null, params);
        }

        @Override
        public Multimap<String, String> buildParams() {
            return getParams();
        }
    }
}
