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
 * Copyright 2017 ForgeRock AS.
 */
package com.sun.identity.wsfederation.plugins;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.wsfederation.common.WSFederationException;

/**
 * Used in test cases that want to simply return a fixed nameID value.
 */
public class TestIdpDefaultAccountMapper implements IDPAccountMapper {

    private static NameIdentifier nameIdentifier;

    /**
     * Set nameID to a fixed value for a test run.
     * @param newNameIdentifier The fixed value to use during a test run.
     */
    public static void setNameIdentifier(NameIdentifier newNameIdentifier) {
        nameIdentifier = newNameIdentifier;
    }

    @Override
    public NameIdentifier getNameID(Object session, String realm, String hostEntityID, String remoteEntityID)
            throws WSFederationException {
        return nameIdentifier;
    }
}
