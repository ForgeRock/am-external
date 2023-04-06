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
 * Copyright 2019-2022 ForgeRock AS.
 */
package org.forgerock.am.saml2.impl;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.am.saml2.profile.Saml2SsoResult;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.utils.StringUtils;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.plugins.SPAttributeMapper;
import com.sun.identity.saml2.profile.SPACSUtils;

/**
 * Utility methods for working with SAML2 SSO responses.
 *
 * @since AM 7.0.0
 */
public class Saml2SsoResponseUtils {

    private final SAML2MetaManager metaManager;

    /**
     * Constructor.
     *
     * @param metaManager The SAML2 metadata manager.
     */
    @Inject
    public Saml2SsoResponseUtils(SAML2MetaManager metaManager) {
        this.metaManager = metaManager;
    }

    /**
     * Reads the SAML2 response data from the SAML SFO.
     *
     * @param storageKey The storage key used for the lookup.
     * @return The SAML2 response data. May be null, if the response data could not be found.
     * @throws SAML2TokenRepositoryException If there was an issue while retrieving the object from SAML SFO.
     */
    public Saml2ResponseData readSaml2ResponseData(String storageKey) throws SAML2TokenRepositoryException {
        Saml2ResponseData data = null;
        if (!StringUtils.isBlank(storageKey)) {
            data = (Saml2ResponseData) SAML2FailoverUtils.retrieveSAML2Token(storageKey);
        }
        return data;
    }

    /**
     * Removes the data associated with the specified key from the SAML Failover store.
     *
     * @param storageKey The storage key used for the lookup.
     * @throws SAML2TokenRepositoryException If there was an issue in deleting the object from the store.
     */
    public void removeSaml2ResponseData(String storageKey) throws SAML2TokenRepositoryException {
        if (StringUtils.isNotEmpty(storageKey)) {
            SAML2FailoverUtils.deleteSAML2Token(storageKey);
        }
    }

    /**
     * Processes the received SAML2 response, without triggering local authentication.
     *
     * @param realm The realm the service provider belongs to.
     * @param spEntityId The service provider's entity ID.
     * @param idpEntityId The identity provider's entity ID.
     * @param subject The Assertion's subject.
     * @param assertion The SAML2 assertion.
     * @param storageKey The storage key used for cache operations.
     * @return The SAML2 SSO authentication result.
     * @throws SAML2Exception If there was an error while processing the SAML assertion.
     */
    public Saml2SsoResult getSsoResultWithoutLocalLogin(Realm realm, String spEntityId, String idpEntityId,
            Subject subject, Assertion assertion, String storageKey) throws SAML2Exception {
        return SPACSUtils.getSsoResultWithoutLocalLogin(subject, assertion, realm.asPath(), spEntityId, metaManager,
                idpEntityId, storageKey);
    }

    /**
     * Maps the SAML2 attributes from the assertion to local attribute values.
     *
     * @param realm The realm the service provider belongs to.
     * @param spEntityId The service provider's entity ID.
     * @param idpEntityId The identity provider's entity ID.
     * @param ssoResult The SAML2 SSO authentication's result.
     * @param assertion The SAML2 assertion.
     * @return The mapped SAML2 attributes.
     * @throws SAML2Exception If there was an issue while mapping the attributes.
     */
    public Map<String, Set<String>> mapSamlAttributes(Realm realm, String spEntityId, String idpEntityId,
            Saml2SsoResult ssoResult, Assertion assertion)
            throws SAML2Exception {
        SPSSOConfigElement spConfig = metaManager.getSPSSOConfig(realm.asPath(), spEntityId);
        boolean wantAssertionEncrypted = SAML2Utils.getBooleanAttributeValueFromSSOConfig(realm.asPath(), spEntityId,
                SP_ROLE, SAML2Constants.WANT_ASSERTION_ENCRYPTED);
        List<Attribute> attrs = SPACSUtils.getSAMLAttributes(assertion,
                SPACSUtils.getNeedAttributeEncrypted(wantAssertionEncrypted, spConfig), ssoResult.getDecryptionKeys());
        SPAttributeMapper attributeMapper = SAML2Utils.getSPAttributeMapper(realm.asPath(), spEntityId);
        return attributeMapper.getAttributes(attrs, ssoResult.getUniversalId(), spEntityId, idpEntityId,
                realm.asPath());
    }

    /**
     * Sets up a persistent link between the federated account and the local user.
     *
     * @param nameIdInfo The NameID received by the local service provider.
     * @param universalId The universal Id of the user.
     * @throws SAML2Exception If there was an issue while persisting the account link.
     */
    public void linkAccounts(String nameIdInfo, String universalId) throws SAML2Exception {
        AccountUtils.setAccountFederation(NameIDInfo.parse(nameIdInfo), universalId);

    }
}
