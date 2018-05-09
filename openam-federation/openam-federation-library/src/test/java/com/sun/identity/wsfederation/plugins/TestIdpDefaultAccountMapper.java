/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
