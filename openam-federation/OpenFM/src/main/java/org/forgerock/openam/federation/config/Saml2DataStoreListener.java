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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.federation.config;

import javax.inject.Inject;

import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreServiceChangeNotifier;

import com.sun.identity.saml2.meta.SAML2MetaServiceListener;

/**
 * This listener clears out all the SAML2 caches whenever the external data store service has been updated (either in
 * the realm, or globally).
 */
public class Saml2DataStoreListener implements DataStoreServiceChangeNotifier {

    private final SAML2MetaServiceListener metaServiceListener;

    /**
     * Constructor.
     *
     * @param metaServiceListener The SAML2 metadata service listener to delegate to.
     */
    @Inject
    public Saml2DataStoreListener(SAML2MetaServiceListener metaServiceListener) {
        this.metaServiceListener = metaServiceListener;
    }

    @Override
    public void notifyGlobalChanges(DataStoreId dataStoreId, Type type) {
        metaServiceListener.clearCaches();
    }

    @Override
    public void notifyOrgChanges(String realm) {
        metaServiceListener.clearCaches();
    }
}
