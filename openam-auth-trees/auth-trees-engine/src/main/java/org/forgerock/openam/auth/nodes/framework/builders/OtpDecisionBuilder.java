/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.OneTimePasswordCollectorDecisionNode;

/**
 * A OneTimePasswordDecisionNode builder.
 */
public class OtpDecisionBuilder extends AbstractNodeBuilder implements OneTimePasswordCollectorDecisionNode.Config {

    /**
     * A OtpDecisionBuilder constructor.
     */
    public OtpDecisionBuilder() {
        super("OTP Collector Decision", OneTimePasswordCollectorDecisionNode.class);
    }
}
