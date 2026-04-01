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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.service;

import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.RESPONSE_KEY_PARAM;
import static org.forgerock.openam.scripting.domain.DecisionNodeApplicationConstants.SAML_OBJECT_KEY_PARAM;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import org.forgerock.http.protocol.Request;
import org.forgerock.openam.auth.node.api.ConditionalTreeHook;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.profile.SPCache;

/**
 * Deletes samlApplication object from CTS after successful authentication, if samlObjectKey or responseKey are present
 * in the request.
 * Deletion only occurs on success, for failure/exception scenarios the samlApplication object is left in CTS as it will
 * need to be present in any subsequent tree executions.
 */
@AutoService(ConditionalTreeHook.class)
public class DeleteSamlApplicationTreeHook implements ConditionalTreeHook {

    private static final Logger logger = LoggerFactory.getLogger(DeleteSamlApplicationTreeHook.class);

    private final Request request;

    @Inject
    DeleteSamlApplicationTreeHook(@Assisted @Nullable Request request) {
        this.request = request;
    }

    @Override
    public boolean shouldExecute() {
        return isNotBlank(resolveKey());
    }

    @Override
    public void accept() {
        String keyToDelete = resolveKey();
        if (isBlank(keyToDelete)) {
            return;
        }

        try {
            logger.debug("Deleting samlApplication object with key {}", keyToDelete);
            SAML2FailoverUtils.deleteSAML2Token(keyToDelete);
            SPCache.samlDataHash.remove(keyToDelete);
        } catch (SAML2TokenRepositoryException e) {
            logger.warn("Failed to delete samlApplication object for key {}", keyToDelete, e);
        }
    }

    private String resolveKey() {
        if (request == null || request.getUri() == null || request.getQueryParams() == null) {
            logger.debug("Could not resolve samlApplication keys due to null request data");
            return null;
        }
        String samlObjectKey = request.getQueryParams().getFirst(SAML_OBJECT_KEY_PARAM);
        String responseKey = request.getQueryParams().getFirst(RESPONSE_KEY_PARAM);
        return isNotBlank(samlObjectKey) ? samlObjectKey : responseKey;
    }

}
