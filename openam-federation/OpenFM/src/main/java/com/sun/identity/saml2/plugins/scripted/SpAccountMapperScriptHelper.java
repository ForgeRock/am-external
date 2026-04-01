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
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;

import java.security.PrivateKey;
import java.util.Map;
import java.util.Set;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.AccountMapperPluginHelper;

/**
 * Provides helper functions for SP Account Mapper Script Implementations.
 */
@Supported(scriptingApi = true, javaApi = false)
public class SpAccountMapperScriptHelper {

    private final static ObjectMapper mapper = new ObjectMapper();

    private final AccountMapperPluginHelper accountMapperPluginHelper;
    private final Assertion assertion;
    private final String hostedEntityID;
    private final String realm;
    private Set<PrivateKey> decryptionKeys;

    /**
     * Construct a new instance of {@link SpAccountMapperScriptHelper}.
     */
    public SpAccountMapperScriptHelper(Assertion assertion, String hostedEntityID, String realm) {
        this.accountMapperPluginHelper = new AccountMapperPluginHelper();
        this.assertion = assertion;
        this.hostedEntityID = hostedEntityID;
        this.realm = realm;
    }

    /**
     * Returns the decryption keys.
     *
     * @return Set of decryption keys.
     */
    private Set<PrivateKey> getDecryptionKeys() throws SAML2Exception {
        if (decryptionKeys != null) {
            return decryptionKeys;
        }
        decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveValidDecryptionCredentials(realm, hostedEntityID, SP);
        return decryptionKeys;
    }

    /**
     * Returns the {@link NameID} from the assertion as a map.
     *
     * @return Map representation of the {@link NameID} from the assertion.
     * @throws SAML2Exception if there is an error decrypting the NameID.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public Map<String, Object> getNameID() throws SAML2Exception {
        NameID nameID = accountMapperPluginHelper.getNameID(assertion, getDecryptionKeys());
        return mapper.convertValue(nameID, new TypeReference<>() {
        });
    }

    /**
     * Returns whether the nameID format is transient or not.
     *
     * @param nameIDFormat Format of the nameID.
     * @return Whether the nameID format is transient or not.
     * reason.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean isTransientNameId(String nameIDFormat) {
        return SAML2Constants.NAMEID_TRANSIENT_FORMAT.equals(nameIDFormat);
    }

    /**
     * Returns the transient user ID.
     *
     * @return Transient user ID for the realm and entity ID.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getTransientUserForSP() throws SAML2Exception {
        return accountMapperPluginHelper.getAttribute(realm, hostedEntityID, SAML2Constants.TRANSIENT_FED_USER,
                SP_ROLE);
    }

    /**
     * Checks if the NameID should be used as the SP user ID.
     *
     * @return <code>true</code> if the NameID should be used as the SP user ID, <code>false</code> otherwise.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean useNameIDAsSPUserID() throws SAML2Exception {
        return accountMapperPluginHelper.useNameIDAsSPUserID(realm, hostedEntityID, SP_ROLE);
    }

    /**
     * Checks if auto-federation is enabled for the entity.
     *
     * @return <code>true</code> if auto-federation is enabled, <code>false</code> otherwise.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean isAutoFedEnabled() throws SAML2Exception {
        return accountMapperPluginHelper.isAutoFedEnabled(realm, hostedEntityID, SP_ROLE);
    }

    /**
     * Validates the user ID.
     *
     * @param userId User ID to validate.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void validateUserId(String userId) throws SAML2Exception {
        accountMapperPluginHelper.validateUserId(userId);
    }

    /**
     * Returns the auto-federated user ID.
     *
     * @param defaultValue Default value to return if no auto-federated user is found.
     * @return Auto-federated user ID for the given realm and entity ID.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String getAutoFedUser(String defaultValue) throws SAML2Exception {
        return accountMapperPluginHelper.getAutoFedUser(realm, hostedEntityID, assertion, defaultValue, getDecryptionKeys(),
                SP_ROLE);
    }
}
