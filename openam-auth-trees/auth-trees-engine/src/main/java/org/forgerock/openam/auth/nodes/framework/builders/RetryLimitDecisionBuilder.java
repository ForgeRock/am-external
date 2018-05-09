/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.RetryLimitDecisionNode;

/**
 * A RetryLimitDecisionNode builder.
 */
public class RetryLimitDecisionBuilder extends AbstractNodeBuilder implements RetryLimitDecisionNode.Config {

    /** The Retry outcome. **/
    public static final String RETRY_OUTCOME = "Retry";
    /** The Reject outcome. **/
    public static final String REJECT_OUTCOME = "Reject";

    /**
     * A RetryLimitDecisionBuilder constructor.
     */
    public RetryLimitDecisionBuilder() {
        super("Retry Limit Decision", RetryLimitDecisionNode.class);
    }
}
