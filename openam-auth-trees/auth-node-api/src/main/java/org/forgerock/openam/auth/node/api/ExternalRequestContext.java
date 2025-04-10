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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;

/**
 * A representation of the external HTTP request in the current tree authentication context.
 *
 */
@SupportedAll
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
     * The authId. This is typically available after the first call to the authentication.
     */
    public final String authId;
    /**
     * The parameters of the request.
     */
    public final Map<String, List<String>> parameters;
    /**
     * The URL of the server.
     */
    public final String serverUrl;
    /**
     * The {@link HttpServletRequest} of the current authentication context.
     */
    public final HttpServletRequest servletRequest;
    /**
     * The {@link HttpServletResponse} of the current authentication context.
     */
    public final HttpServletResponse servletResponse;

    private ExternalRequestContext(com.google.common.collect.ListMultimap<String, String> headers,
            Map<String, String> cookies, PreferredLocales locales, String clientIp, String hostName, String ssoTokenId,
            Map<String, String[]> parameters, String serverUrl, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, String authId) {
        this.headers = new WrappedListMultimap<>(Multimaps.unmodifiableListMultimap(checkNotNull(headers)));
        this.cookies = Collections.unmodifiableMap(cookies);
        this.locales = checkNotNull(locales);
        this.clientIp = checkNotNull(clientIp);
        this.hostName = checkNotNull(hostName);
        this.ssoTokenId = ssoTokenId;
        this.parameters = new ConsumableMap<>(parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ImmutableList.copyOf(e.getValue()))));
        this.serverUrl = serverUrl;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.authId = authId;
    }

    /**
     * A builder for {@link ExternalRequestContext} instances.
     */
    public static class Builder {

        private com.google.common.collect.ListMultimap<String, String> headers = ImmutableListMultimap.of();
        private Map<String, String> cookies = ImmutableMap.of();
        private PreferredLocales locales = new PreferredLocales();
        private String clientIp = "unknown";
        private String hostName = "localhost";
        private String ssoTokenId = null;
        private Map<String, String[]> parameters = ImmutableMap.of();
        private String serverUrl = null;
        private HttpServletRequest servletRequest;
        private HttpServletResponse servletResponse;
        private String authId = null;

        /**
         * Sets the HTTP headers for the request.
         *
         * @param headers The HTTP headers.
         * @return This builder instance.
         */
        public Builder headers(com.google.common.collect.ListMultimap<String, String> headers) {
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
         * Sets the authId of the request.
         *
         * @param authId the authId.
         * @return this builder.
         */
        public Builder authId(String authId) {
            this.authId = authId;
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
         * Sets the serverUrl of the request.
         *
         * @param serverUrl the serverUrl.
         * @return this builder.
         */
        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        /**
         * Sets the {@link HttpServletRequest}.
         *
         * @param request the {@link HttpServletRequest}
         * @return this builder.
         */
        public Builder servletRequest(HttpServletRequest request) {
            this.servletRequest = request;
            return this;
        }

        /**
         * Sets the {@link HttpServletResponse}.
         *
         * @param response the {@link HttpServletResponse}
         * @return this builder.
         */
        public Builder servletResponse(HttpServletResponse response) {
            this.servletResponse = response;
            return this;
        }

        /**
         * Creates a new {@link ExternalRequestContext} instance.
         *
         * @return A new instance of {@link ExternalRequestContext}.
         */
        public ExternalRequestContext build() {
            return new ExternalRequestContext(headers, cookies, locales, clientIp, hostName, ssoTokenId, parameters,
                    serverUrl, servletRequest, servletResponse, authId);
        }
    }

    /**
     * Extension of HashMap that prevents additional loading of entries after instantiation.
     * Remove operations are allowed to "consume" entries from the map, similar to a pop operation in a stack/queue.
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    private static class ConsumableMap<K, V> extends HashMap<K, V> {
        ConsumableMap(Map<? extends K, ? extends V> m) {
            super(m);
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }
    }
}
