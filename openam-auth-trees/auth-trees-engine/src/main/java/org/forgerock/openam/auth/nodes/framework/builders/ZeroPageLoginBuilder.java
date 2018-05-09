/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.ZeroPageLoginNode;

/**
 * A ZeroPageLoginNode builder.
 */
public class ZeroPageLoginBuilder extends AbstractNodeBuilder implements ZeroPageLoginNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Zero Page Login Collector";

    /**
     * A ZeroPageLoginBuilder constructor.
     */
    public ZeroPageLoginBuilder() {
        super(DEFAULT_DISPLAY_NAME, ZeroPageLoginNode.class);
    }
}
