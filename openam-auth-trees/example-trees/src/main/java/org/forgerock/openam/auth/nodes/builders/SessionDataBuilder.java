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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.SessionDataNode;

/**
 * A {@link SessionDataNode} Builder.
 */
public class SessionDataBuilder extends AbstractNodeBuilder implements SessionDataNode.Config {
    private static final String DEFAULT_DISPLAY_NAME = "Get Session Data";

    private String sessionDataKey;
    private String sharedStateKey;

    /**
     * The constructor.
     */
    public SessionDataBuilder() {
        super(DEFAULT_DISPLAY_NAME, SessionDataNode.class);
    }

    /**
     * Sets the sessionDataKey.
     *
     * @param sessionDataKey the session data key
     * @return the key
     */
    public SessionDataBuilder sessionDataKey(String sessionDataKey) {
        this.sessionDataKey = sessionDataKey;
        return this;
    }

    /**
     * Sets the sharedStateKey.
     *
     * @param sharedStateKey the shared state key
     * @return the key
     */
    public SessionDataBuilder sharedStateKey(String sharedStateKey) {
        this.sharedStateKey = sharedStateKey;
        return this;
    }

    @Override
    public String sessionDataKey() {
        return sessionDataKey;
    }

    @Override
    public String sharedStateKey() {
        return sharedStateKey;
    }
}
