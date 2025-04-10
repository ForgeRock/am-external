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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.IDPAccountMapperUtils;

import org.forgerock.openam.annotations.Supported;

/**
 * This class exposes methods that are only intended to be used by NameID Mapper script types.
 */
@Supported(scriptingApi = true, javaApi = false)
public class NameIDScriptHelper {

    @Supported(scriptingApi = true, javaApi = false)
    public static final String NAMEID_FORMAT_TRANSIENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
    @Supported(scriptingApi = true, javaApi = false)
    public static final String NAMEID_FORMAT_PERSISTENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
    @Supported(scriptingApi = true, javaApi = false)
    public static final String NAMEID_FORMAT_UNSPECIFIED = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
    @Supported(scriptingApi = true, javaApi = false)
    public static final String NAMEID_FORMAT_EMAIL = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";

    private final Object session;
    private final String hostEntityID;
    private final String remoteEntityID;
    private final String realm;
    private final String nameIDFormat;

    private final IDPAccountMapperUtils idpAccountMapperUtils;
    private final IDPAccountMapper idpAccountMapper;

    public NameIDScriptHelper(Object session, String hostEntityID, String remoteEntityID, String realm,
            String nameIDFormat) throws SAML2Exception {
        this.session = session;
        this.hostEntityID = hostEntityID;
        this.remoteEntityID = remoteEntityID;
        this.realm = realm;
        this.nameIDFormat = nameIDFormat;
        this.idpAccountMapper = SAML2Utils.getIDPAccountMapper(realm, hostEntityID);
        this.idpAccountMapperUtils =  new IDPAccountMapperUtils();
    }

    /**
     * Returns a Name Identifier.
     *
     * @return a pseudo-random name identifier, or null if an exception was thrown during generation.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String createNameIdentifier() {
        return SAML2Utils.createNameIdentifier();
    }

    /**
     * Delegate to the configured java plugin to retrieve NameID value.
     *
     * @return the nameID value returned by the configured java plugin
     * @throws SAML2Exception if there is an exception thrown by the underlying java plugin implementation
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getNameIDValue() throws SAML2Exception {
        NameID nameID = idpAccountMapper.getNameID(session, hostEntityID, remoteEntityID, realm, nameIDFormat);
        if (nameID == null) {
            throw new SAML2Exception("Could not retrieve NameID from java plugin");
        } else {
            return nameID.getValue();
        }
    }

    /**
     * Delegate to the configured java plugin to determine whether the provided NameID-Format should be persisted to the
     * user data store
     *
     * @return <code>true</code> if the provided NameID-Format should be persisted in the user data store, otherwise
     * <code>false</code>
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean shouldPersistNameIDFormat() {
        return idpAccountMapper.shouldPersistNameIDFormat(realm, hostEntityID, remoteEntityID, nameIDFormat);
    }

    /**
     * Returns existing NameID associated with session, or null if none is associated.
     *
     * @return an associated nameId value, or null.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getNameIDFromSession() {
        return idpAccountMapperUtils.getNameIdFromSession(session, remoteEntityID);
    }
}
