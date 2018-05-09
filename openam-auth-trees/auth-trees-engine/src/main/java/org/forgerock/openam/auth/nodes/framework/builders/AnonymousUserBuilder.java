/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.AnonymousUserNode;

/**
 * AnonymousUserNode builder.
 */
public class AnonymousUserBuilder extends AbstractNodeBuilder implements AnonymousUserNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Map to Anonymous User";

    /**
     * A AnonymousUserBuilder constructor.
     */
    public AnonymousUserBuilder() {
        super(DEFAULT_DISPLAY_NAME, AnonymousUserNode.class);
    }
}