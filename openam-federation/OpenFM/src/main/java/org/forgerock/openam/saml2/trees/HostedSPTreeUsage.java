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

package org.forgerock.openam.saml2.trees;

import com.google.auto.service.AutoService;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import org.forgerock.am.trees.api.TreeUsage;
import org.forgerock.am.trees.api.TreeUsageException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.String.format;

/**
 * A {@link TreeUsage} implementation that reports the usages of a tree by hosted SP entities.
 */
@AutoService(TreeUsage.class)
public class HostedSPTreeUsage implements TreeUsage {

    private static final Logger logger = LoggerFactory.getLogger(HostedSPTreeUsage.class);

    /**
     * Reports the usages of a tree by the Hosted SP entities in a realm.
     * @param realm the realm
     * @param treeName the tree name
     * @return a {@link Response} object containing the result of the usage.
     * @throws TreeUsageException if an error occurs while retrieving the usages.
     */
    @Override
    public Response reportUsages(Realm realm, String treeName) throws TreeUsageException {
        SAML2MetaManager saml2MetaManager = SAML2Utils.getSAML2MetaManager();
        try {
            List<String> associatedHostedSPs = saml2MetaManager.getAllHostedEntities(realm.asPath()).stream()
                    .filter(entity -> isTreeAssociated(realm.asPath(), treeName, entity))
                    .sorted()
                    .toList();
            if (associatedHostedSPs.isEmpty()) {
                return new Response(false, "Tree is not in use by any SAML Hosted SP in the realm.");
            } else {
                logger.debug("Tree '{}' is in use by {} SAML hosted SPs '{}' in the realm '{}'", treeName,
                        associatedHostedSPs.size(), associatedHostedSPs, realm.asPath());
                return new Response(true, format("%d SAML Hosted SP(s)", associatedHostedSPs.size()),
                        associatedHostedSPs.subList(0, Math.min(associatedHostedSPs.size(), MAX_USAGES)));
            }
        } catch (SAML2MetaException e) {
            logger.error("Error while retrieving hosted entities. Unable to find tree usages for the tree '{}'.",
                    treeName, e);
            throw new TreeUsageException(format("Unable to find tree usages for the tree '%s'.", treeName));
        }
    }

    /**
     * Returns true if the tree is associated to the hosted SP.
     * @param realm the realm
     * @param treeName the tree name
     * @param entityID the hosted SP entity ID
     * @return true if the tree is associated to the hosted SP
     */
    private boolean isTreeAssociated(String realm, String treeName, String entityID) {
        String hostedSpTree = SAML2Utils.getConfiguredServiceForHostedSP(realm, entityID);
        return !StringUtils.isEmptyOrEmptySelection(hostedSpTree) && treeName.equals(hostedSpTree);
    }
}
