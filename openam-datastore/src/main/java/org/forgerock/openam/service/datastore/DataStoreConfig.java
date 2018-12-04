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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.service.datastore;

import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.util.Reject;

/**
 * A simple model object to represent a data store.
 *
 * @since 6.5.0
 */
final class DataStoreConfig {

    private final DataStoreId id;
    private final String hostname;
    private final int port;
    private final String bindDN;
    private final String bindPassword;
    private final int minimumConnectionPool;
    private final int maximumConnectionPool;
    private final boolean useSsl;
    private final boolean useStartTLS;

    private DataStoreConfig(Builder builder) {
        this.id = builder.id;
        this.hostname = builder.hostname;
        this.port = builder.port;
        this.bindDN = builder.bindDN;
        this.bindPassword = builder.bindPassword;
        this.minimumConnectionPool = builder.minimumConnectionPool;
        this.maximumConnectionPool = builder.maximumConnectionPool;
        this.useSsl = builder.useSsl;
        this.useStartTLS = builder.useStartTLS;
    }

    DataStoreId getId() {
        return id;
    }

    String getHostname() {
        return hostname;
    }

    int getPort() {
        return port;
    }

    String getBindDN() {
        return bindDN;
    }

    String getBindPassword() {
        return bindPassword;
    }

    int getMinimumConnectionPool() {
        return minimumConnectionPool;
    }

    int getMaximumConnectionPool() {
        return maximumConnectionPool;
    }

    boolean isUseSsl() {
        return useSsl;
    }

    boolean isUseStartTLS() {
        return useStartTLS;
    }

    static Builder builder(DataStoreId id) {
        return new Builder(id);
    }

    /**
     * Builder for building the Data Store Config
     */
    static final class Builder {

        private final DataStoreId id;
        private String hostname;
        private int port;
        private String bindDN;
        private String bindPassword;
        private int minimumConnectionPool;
        private int maximumConnectionPool;
        private boolean useSsl;
        private boolean useStartTLS;

        Builder(DataStoreId id) {
            this.id = id;
        }

        Builder withHostname(String hostname) {
            Reject.ifBlank(hostname);
            this.hostname = hostname;
            return this;
        }

        Builder withPort(int port) {
            Reject.ifTrue(port < 0);
            this.port = port;
            return this;
        }

        Builder withBindDN(String bindDN) {
            Reject.ifBlank(bindDN);
            this.bindDN = bindDN;
            return this;
        }

        Builder withBindPassword(String bindPassword) {
            Reject.ifBlank(bindPassword);
            this.bindPassword = bindPassword;
            return this;
        }

        Builder withMinimumConnectionPool(int minimumConnectionPool) {
            Reject.ifTrue(minimumConnectionPool < 0);
            this.minimumConnectionPool = minimumConnectionPool;
            return this;
        }

        Builder withMaximumConnectionPool(int maximumConnectionPool) {
            Reject.ifTrue(maximumConnectionPool < 0);
            this.maximumConnectionPool = maximumConnectionPool;
            return this;
        }

        Builder withUseSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        Builder withUseStartTLS(boolean useStartTLS) {
            this.useStartTLS = useStartTLS;
            return this;
        }

        DataStoreConfig build() {
            Reject.ifTrue(minimumConnectionPool > maximumConnectionPool,
                    "Minimal pool size must be less or equal to the maximum pool size");
            return new DataStoreConfig(this);
        }
    }

}
