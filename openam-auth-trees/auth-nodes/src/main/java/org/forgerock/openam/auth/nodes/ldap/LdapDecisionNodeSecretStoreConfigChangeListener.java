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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.ldap;

import static org.forgerock.openam.shared.secrets.Labels.LDAP_DECISION_NODE_MTLS_CERT;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.secrets.config.SecretStoreConfigChangeListener;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;

/**
 * Config change listener for the LDAP decision node secret store configuration.
 * <p>When a change is made to a secret store mapping which relates to a label used by an LDAP decision node for mTLS
 * connections that LDAP connection pool will be refreshed.
 */
public class LdapDecisionNodeSecretStoreConfigChangeListener implements SecretStoreConfigChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(LdapDecisionNodeSecretStoreConfigChangeListener.class);

    private static final Pattern MTLS_LDAP_DECISION_NODE_PATTERN = Pattern.compile(
            LDAP_DECISION_NODE_MTLS_CERT.replace(".", "\\.").replace("%s", "(.*)"));

    @Override
    public void secretStoreHasChanged(SecretStoreWithMappings secretStore, String orgName, int type) {
        if (type == ServiceListener.ADDED || orgName == null) {
            // no notifications required on secret store creation
            return;
        }
        if (secretStore != null) {
            try {
                secretStore.mappings().get(QueryFilter.alwaysTrue())
                        .forEach(mapping -> secretStoreMappingHasChanged(mapping, orgName, type));
            } catch (SMSException | SSOException e) {
                logger.error("Unable to get secret mappings for {}", secretStore.id(), e);
            }
        } else if (type == ServiceListener.REMOVED) {
            secretStoreMappingHasChanged(null, orgName, type);
        }
    }

    @Override
    public void secretStoreMappingHasChanged(PurposeMapping mapping, String orgName, int type) {
        if (mapping == null && type != ServiceListener.REMOVED) {
            logger.error("No mapping provided but type is not delete");
            return;
        }
        if (orgName == null) {
            return;
        }
        String secretId = mapping != null ? mapping.secretId() : null;
        if (secretId != null) {
            Matcher ldapDecisionNodeMtlsMatcher = MTLS_LDAP_DECISION_NODE_PATTERN.matcher(secretId);
            if (!ldapDecisionNodeMtlsMatcher.matches()) {
                return;
            }
        }
        if (secretId != null) {
            logger.info("Refreshing connection for secret ID {} as secret mapping has changed", secretId);
        } else {
            logger.info("Refreshing connections as secret mapping has been deleted");
        }
        LDAPAuthUtils.closeConnectionPoolsBySecretLabel(secretId, orgName);
    }
}
