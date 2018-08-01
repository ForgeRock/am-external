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

import org.forgerock.util.Reject;

/**
 * A simple model object to represent a Data Store Config
 */
public class DataStoreConfig {

    private final String id;
    private final String hostname;
    private final int port;
    private final String bindDN;
    private final String bindPassword;
    private final boolean useSsl;
    private final boolean useStartTLS;

    private DataStoreConfig(Builder builder) {
        this.id = builder.id;
        this.hostname = builder.hostname;
        this.port = builder.port;
        this.bindDN = builder.bindDN;
        this.bindPassword = builder.bindPassword;
        this.useSsl = builder.useSsl;
        this.useStartTLS = builder.useStartTLS;
    }

    String getId() {
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

    boolean isUseSsl() {
        return useSsl;
    }

    boolean isUseStartTLS() {
        return useStartTLS;
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for building the Data Store Config
     */
    public static final class Builder {

        private String id;
        private String hostname;
        private int port;
        private String bindDN;
        private String bindPassword;
        private boolean useSsl;
        private boolean useStartTLS;

        /**
         * Sets the ID of the Data Store Config.
         *
         * @param id The Data Store Config ID.
         * @return The Data Store Config Instance
         */
        public Builder withId(String id) {
            Reject.ifBlank(id);
            this.id = id;
            return this;
        }

        /**
         * Sets the host name of the Data Store.
         *
         * @param hostname The Data Store Host Name.
         * @return The Data Store Config Instance
         */
        public Builder withHostname(String hostname) {
            Reject.ifBlank(hostname);
            this.hostname = hostname;
            return this;
        }

        /**
         * Sets the Server Port for the Data Store.
         *
         * @param port The Data Store Server Port.
         * @return The Data Store Config Instance
         */
        public Builder withPort(int port) {
            Reject.ifNull(port);
            this.port = port;
            return this;
        }

        /**
         * Sets the Bind DN to connect to the Data Store.
         *
         * @param bindDN The Data Store BindDN.
         * @return The Data Store Config Instance
         */
        public Builder withBindDN(String bindDN) {
            Reject.ifBlank(bindDN);
            this.bindDN = bindDN;
            return this;
        }

        /**
         * Sets the Bind Password to connect to the Data Store.
         *
         * @param bindPassword The Data Store Bind Password.
         * @return The Data Store Config Instance
         */
        public Builder withBindPassword(String bindPassword) {
            Reject.ifBlank(bindPassword);
            this.bindPassword = bindPassword;
            return this;
        }

        /**
         * Sets the useSsl flag
         *
         * @param useSsl The useSsl flag.
         * @return The Data Store Config Instance
         */
        public Builder withUseSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        /**
         * Sets the useStartTLS flag.
         *
         * @param useStartTLS The useStartTLS flag.
         * @return The Data Store Config Instance
         */
        public Builder withUseStartTLS(boolean useStartTLS) {
            this.useStartTLS = useStartTLS;
            return this;
        }

        /**
         * Builds the Data Store Config.
         * @return The Data Store Config Instance
         */
        public DataStoreConfig build() {
            return new DataStoreConfig(this);
        }
    }
}
