/*
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
 * Copyright 2017-2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.node.api;

import static org.forgerock.guava.common.collect.Multimaps.unmodifiableListMultimap;
import static org.forgerock.util.Reject.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.guava.common.collect.ImmutableListMultimap;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.util.i18n.PreferredLocales;

import jdk.nashorn.internal.ir.annotations.Immutable;


/**
 * A representation of the external HTTP request in the current tree authentication context.
 *
 * @supported.all.api
 */
@Immutable
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
    /**
     * The host name associated with the current HTTP request.
     */
    public final String hostName;
    /**
     * The SSO token ID. This is available if user already has a session, usually during upgrade.
     */
    public final String ssoTokenId;
    /**
     * The parameters of the request.
     */
    public final Map<String, List<String>> parameters;

    private ExternalRequestContext(ListMultimap<String, String> headers, Map<String, String> cookies,
            PreferredLocales locales, String clientIp, String hostName, String ssoTokenId,
            Map<String, String[]> parameters) {
        this.headers = unmodifiableListMultimap(checkNotNull(headers));
        this.cookies = Collections.unmodifiableMap(cookies);
        this.locales = checkNotNull(locales);
        this.clientIp = checkNotNull(clientIp);
        this.hostName = checkNotNull(hostName);
        this.ssoTokenId = ssoTokenId;
        this.parameters = Collections.unmodifiableMap(parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ImmutableList.copyOf(e.getValue()))));
    }

    /**
     * A builder for {@link ExternalRequestContext} instances.
     */
    public static class Builder {

        private ListMultimap<String, String> headers = ImmutableListMultimap.of();
        private Map<String, String> cookies = ImmutableMap.of();
        private PreferredLocales locales = new PreferredLocales();
        private String clientIp = "unknown";
        private String hostName = "localhost";
        private String ssoTokenId = null;
        private Map<String, String[]> parameters = ImmutableMap.of();

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
         * Sets the hostname for the request.
         *
         * @param hostName the host name.
         * @return This builder instance.
         */
        public Builder hostName(String hostName) {
            this.hostName = checkNotNull(hostName);
            return this;
        }

        /**
         * Sets the ssoTokenId of the request.
         *
         * @param ssoTokenId the ssoTokenId.
         * @return this builder.
         */
        public Builder ssoTokenId(String ssoTokenId) {
            this.ssoTokenId = ssoTokenId;
            return this;
        }

        /**
         * Sets the parameterMap of the request.
         * @param parameters the parameters
         * @return this builder
         */
        public Builder parameters(Map<String, String[]> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Creates a new {@link ExternalRequestContext} instance.
         *
         * @return A new instance of {@link ExternalRequestContext}.
         */
        public ExternalRequestContext build() {
            return new ExternalRequestContext(headers, cookies, locales, clientIp, hostName, ssoTokenId, parameters);
        }
    }
}
