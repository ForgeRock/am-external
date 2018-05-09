/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.DataStoreDecisionNode;

/**
 * A DataStoreNode builder.
 */
public class DataStoreBuilder extends AbstractNodeBuilder implements DataStoreDecisionNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Data Store Decision";

    /**
     * A DataStoreBuilder constructor.
     */
    public DataStoreBuilder() {
        super(DEFAULT_DISPLAY_NAME, DataStoreDecisionNode.class);
    }
}
