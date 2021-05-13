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
 * Copyright 2018-2019 ForgeRock AS.
 */

package org.forgerock.openam.service.datastore;

import java.util.Set;

import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;

/**
 * A simple model object to represent a data store.
 *
 * @since 6.5.0
 */
final class DataStoreConfig {

    private final DataStoreId id;
    private final Set<String> serverUrls;
    private final String bindDN;
    private final String bindPassword;
    private final int minimumConnectionPool;
    private final int maximumConnectionPool;
    private final boolean useSsl;
    private final boolean useStartTLS;
    private final boolean affinityEnabled;

    private DataStoreConfig(Builder builder) {
        this.id = builder.id;
        this.serverUrls = builder.serverUrls;
        this.bindDN = builder.bindDN;
        this.bindPassword = builder.bindPassword;
        this.minimumConnectionPool = builder.minimumConnectionPool;
        this.maximumConnectionPool = builder.maximumConnectionPool;
        this.useSsl = builder.useSsl;
        this.useStartTLS = builder.useStartTLS;
        this.affinityEnabled = builder.affinityEnabled;
    }

    DataStoreId getId() {
        return id;
    }

    Set<String> getServerUrls() {
        return serverUrls;
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

    boolean isAffinityEnabled() {
        return affinityEnabled;
    }

    static Builder builder(DataStoreId id) {
        return new Builder(id);
    }

    /**
     * Builder for building the Data Store Config
     */
    static final class Builder {

        private final DataStoreId id;
        private String bindDN;
        private String bindPassword;
        private int minimumConnectionPool;
        private int maximumConnectionPool;
        private boolean useSsl;
        private boolean useStartTLS;
        private boolean affinityEnabled;
        private Set<String> serverUrls;

        Builder(DataStoreId id) {
            this.id = id;
        }

        Builder withServerUrls(Set<String> serverUrls) {
            Reject.ifTrue(CollectionUtils.isEmpty(serverUrls));
            this.serverUrls = serverUrls;
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

        Builder withAffinityEnabled(boolean affinityEnabled) {
            this.affinityEnabled = affinityEnabled;
            return this;
        }

        DataStoreConfig build() {
            Reject.ifTrue(minimumConnectionPool > maximumConnectionPool,
                    "Minimal pool size must be less or equal to the maximum pool size");
            return new DataStoreConfig(this);
        }
    }

}
