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
 * Copyright 2021-2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import java.util.List;

import org.forgerock.openam.annotations.Supported;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.IDPAttributeMapper;
import com.sun.identity.saml2.plugins.scripted.wrappers.ScriptedSaml2Attribute;
import com.sun.identity.saml2.profile.IDPSSOUtil;


/**
 * NextGen version of the {@link IdpAttributeMapperScriptHelper}.  This class differs by storing the session token and
 * not requiring it to be passed in from the script.  Method calls are otherwise delegated to the legacy helper class.
 */
public class IdpAttributeMapperScriptNextGenHelper {

    private final ObjectMapper jackson = new ObjectMapper();

    private final SSOToken ssoToken;
    private final String realm;
    private final String remoteEntityId;
    private final String hostedEntityId;

    /**
     * Constructor.
     *
     * @param ssoToken       The SSO token of the current user.
     * @param realm          The realm for the current session.
     * @param remoteEntityId Entity ID of the hosted IDP.
     * @param hostedEntityId Entity ID of the remote SP.
     */
    public IdpAttributeMapperScriptNextGenHelper(
            SSOToken ssoToken,
            String realm,
            String remoteEntityId,
            String hostedEntityId
    ) {
        this.ssoToken = ssoToken;
        this.realm = realm;
        this.remoteEntityId = remoteEntityId;
        this.hostedEntityId = hostedEntityId;
    }


    /**
     * Using the configured Java {@link IDPAttributeMapper}, get the attributes for the current session.  This avoids
     * the script having to do a lot of work itself to get the attributes and simplifies the scripts.
     * <p>
     * The attributes are converted using Jackson to a list of maps, which is then usable in the script as a native JSON
     * object
     *
     * @return List of attributes as maps.  This is usable in the script as a native JSON object.
     * @throws SAML2Exception If there is an error getting the attributes.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Object> getStandardAttributes() throws SAML2Exception {
        List<ScriptedSaml2Attribute> list = IDPSSOUtil
                .getIDPAttributeMapper(realm, hostedEntityId)
                .getAttributes(ssoToken, hostedEntityId, remoteEntityId, realm)
                .stream()
                .map(ScriptedSaml2Attribute::new)
                .toList();

        return jackson.convertValue(list, new TypeReference<>() {
        });
    }

}
