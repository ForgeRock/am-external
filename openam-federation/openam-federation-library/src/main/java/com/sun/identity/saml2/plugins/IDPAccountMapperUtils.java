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
package com.sun.identity.saml2.plugins;


import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.profile.IDPSession;
import com.sun.identity.saml2.profile.NameIDandSPpair;

import java.util.List;

/**
 * A helper class with common functions used in IDP Account mapping.
 */
public class IDPAccountMapperUtils {

    /**
     * Returns existing NameID value associated with this session, or null if none is associated.
     *
     * @param session the session object.
     * @param remoteEntityID the remote entity id.
     * @return an associated nameId value, or null.
     */
    public String getNameIdFromSession(Object session, String remoteEntityID) {
        String nameIDValue = null;
        String sessionIndex = IDPSSOUtil.getSessionIndex(session);
        if (sessionIndex != null) {
            IDPSession idpSession = IDPSSOUtil.retrieveCachedIdPSession(sessionIndex);
            if (idpSession != null) {
                List<NameIDandSPpair> list = idpSession.getNameIDandSPpairs();
                if (list != null) {
                    for (NameIDandSPpair pair : list) {
                        if (pair.getSPEntityID().equals(remoteEntityID)) {
                            nameIDValue = pair.getNameID().getValue();
                            break;
                        }
                    }
                }
            }
        }
        return nameIDValue;
    }
}
