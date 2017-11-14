/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static org.forgerock.guava.common.collect.Multimaps.unmodifiableListMultimap;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.Collections;
import java.util.Map;

import org.forgerock.guava.common.collect.ImmutableListMultimap;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.util.i18n.PreferredLocales;

/**
 * A representation of the external HTTP request in the current tree authentication context.
 *
 * @supported.all.api
 */
public final class ExternalRequestContext {

    /**
     * The HTTP headers from the current authenticate HTTP request.
     */
    public final ListMultimap<String, String> headers;
    /**
     * The cookie name-value pairs for the current authenticate HTTP request.
     */
    public final Map<String, String> cookies;
    /**
     * The preferred locales for the request.
     */
    public final PreferredLocales locales;
    /**
     * The IP address associated with the current HTTP request.
     */
    public final String clientIp;

    private ExternalRequestContext(ListMultimap<String, String> headers, Map<String, String> cookies,
            PreferredLocales locales, String clientIp) {
        this.headers = unmodifiableListMultimap(checkNotNull(headers));
        this.cookies = Collections.unmodifiableMap(cookies);
        this.locales = checkNotNull(locales);
        this.clientIp = checkNotNull(clientIp);
    }

    /**
     * A builder for {@link ExternalRequestContext} instances.
     */
    public static class Builder {

        private ListMultimap<String, String> headers = ImmutableListMultimap.of();
        private Map<String, String> cookies = ImmutableMap.of();
        private PreferredLocales locales = new PreferredLocales();
        private String clientIp = "unknown";

        /**
         * Sets the HTTP headers for the request.
         *
         * @param headers The HTTP headers.
         * @return This builder instance.
         */
        public Builder headers(ListMultimap<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the cookies for the request.
         *
         * @param cookies The cookies.
         * @return This builder instance.
         */
        public Builder cookies(Map<String, String> cookies) {
            this.cookies = cookies;
            return this;
        }

        /**
         * Sets the preferred locales for the request.
         *
         * @param locales The preferred locales.
         * @return This builder instance.
         */
        public Builder locales(PreferredLocales locales) {
            this.locales = locales;
            return this;
        }

        /**
         * Sets the client IP address for the request.
         *
         * @param clientIp The client's IP address.
         * @return This builder instance.
         */
        public Builder clientIp(String clientIp) {
            this.clientIp = checkNotNull(clientIp);
            return this;
        }

        /**
         * Creates a new {@link ExternalRequestContext} instance.
         *
         * @return A new instance of {@link ExternalRequestContext}.
         */
        public ExternalRequestContext build() {
            return new ExternalRequestContext(headers, cookies, locales, clientIp);
        }
    }
}
