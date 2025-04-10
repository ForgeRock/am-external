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

package org.forgerock.openam.saml2.trees;

import static java.lang.String.format;

import java.util.List;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.am.trees.api.TreeUsage;
import org.forgerock.am.trees.api.TreeUsageException;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * A {@link TreeUsage} implementation that reports the usages of a tree by remote SP entities.
 */
@AutoService(TreeUsage.class)
public class RemoteSPTreeUsage implements TreeUsage {

    private static final Logger logger = LoggerFactory.getLogger(RemoteSPTreeUsage.class);

    /**
     * Reports the usages of a tree by the Remote SP entities in a realm.
     * @param realm the realm
     * @param treeName the tree name
     * @return a {@link Response} object containing the result of the usage.
     */
    @Override
    public Response reportUsages(Realm realm, String treeName) throws TreeUsageException {
        SAML2MetaManager saml2MetaManager = SAML2Utils.getSAML2MetaManager();
        try {
            List<String> associatedRemoteSPs = saml2MetaManager.getAllRemoteEntities(realm.asPath()).stream()
                    .filter(entity -> isTreeAssociated(realm.asPath(), treeName, entity))
                    .sorted()
                    .toList();
            if (associatedRemoteSPs.isEmpty()) {
                return new Response(false, "Tree is not in use by any SAML Remote SP in the realm.");
            } else {
                logger.warn("Tree '{}' is in use by {} SAML remote SPs '{}' in the realm '{}'", treeName,
                        associatedRemoteSPs.size(), associatedRemoteSPs, realm.asPath());
                return new Response(true, format("%d SAML Remote SP(s)", associatedRemoteSPs.size()),
                        associatedRemoteSPs.subList(0, Math.min(associatedRemoteSPs.size(), MAX_USAGES)));
            }
        } catch (SAML2MetaException e) {
            logger.error("Error while retrieving remote entities. Unable to find tree usages for the tree '{}'.",
                    treeName, e);
            throw new TreeUsageException(format("Unable to find tree usages for the tree '%s'.", treeName));
        }
    }

    /**
     * Returns true if the tree is associated to the remote SP.
     * @param realm the realm
     * @param treeName the tree name
     * @param entityID the remote SP entity ID
     * @return true if the tree is associated to the remote SP
     */
    private boolean isTreeAssociated(String realm, String treeName, String entityID) {
        String remoteSpTree = SAML2Utils.getConfiguredServiceForSP(realm, entityID);
        return !StringUtils.isEmptyOrEmptySelection(remoteSpTree) && treeName.equals(remoteSpTree);
    }
}
