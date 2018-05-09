/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.OneTimePasswordGeneratorNode;

/**
 * A OneTimePasswordGeneratorNode builder.
 */
public class HotpGeneratorBuilder extends AbstractNodeBuilder implements OneTimePasswordGeneratorNode.Config {

    /**
     * A HotpGeneratorBuilder constructor.
     */
    public HotpGeneratorBuilder() {
        super("HOTP Generator", OneTimePasswordGeneratorNode.class);
    }
}
