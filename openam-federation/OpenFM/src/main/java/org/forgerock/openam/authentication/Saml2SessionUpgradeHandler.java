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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.impl.FMSessionNotification;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.session.SessionUpgradeHandler;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.LoggerFactory;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPSession;

/**
 * This {@link SessionUpgradeHandler} implementation should ensure that during a session upgrade the SAML authentication
 * related cache mappings are updated, so that SLO will correctly trigger logout on all remote entities. This should
 * also prevent the original session's destroy event to trigger session synchronization with remote entities.
 * The logic implemented here was mainly based on {@link com.sun.identity.saml2.profile.IDPSessionListener}.
 */
public class Saml2SessionUpgradeHandler implements SessionUpgradeHandler {

    private static final org.slf4j.Logger debug = LoggerFactory.getLogger(Saml2SessionUpgradeHandler.class);
    private final SSOTokenManager ssoTokenManager;
    private final FMSessionNotification sessionNotification;

    public Saml2SessionUpgradeHandler() {
        ssoTokenManager = InjectorHolder.getInstance(SSOTokenManager.class);
        sessionNotification = InjectorHolder.getInstance(FMSessionNotification.class);
    }

    @Override
    public void handleSessionUpgrade(Session oldSession, Session newSession) {
        String sessionIndex = null;
        try {
            sessionIndex = oldSession.getProperty(SAML2Constants.IDP_SESSION_INDEX);
            debug.debug("Session upgrade, sessionIndex from old session: {}", sessionIndex);
        } catch (SessionException ex) {
            Logger.getLogger(Saml2SessionUpgradeHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (StringUtils.isNotEmpty(sessionIndex)) {
            final String oldSessionID = oldSession.getID().toString();
            final String newSessionID = newSession.getID().toString();
            final SSOToken oldSSOToken;
            final SSOToken newSSOToken;
            try {
                oldSSOToken = ssoTokenManager.createSSOToken(oldSessionID);
                newSSOToken = ssoTokenManager.createSSOToken(newSessionID);
            } catch (SSOException ssoe) {
                debug.warn("Unable to create an SSOToken for the session ID due to " + ssoe.toString());
                return;
            }

            try {
                // Any deletion of the old session should not result in deletion of the associated
                // IDPSession. This step is also done here (early) to avoid IDPSessionListener seeing this
                // too late (via psearch notification which may be after IDPSessionListener has run).
                debug.debug("Session upgrade, marking old session (id {}) sessionIndex to not be removed",
                        oldSessionID);
                oldSSOToken.setProperty(SAML2Constants.DO_NOT_REMOVE_SAML2_IDPSESSION, Boolean.TRUE.toString());
                // Update the cached session in FMSessionNotification directly to avoid a race condition where
                // the session notification has not occurred before IDPSessionListener.sessionInvalidated is called.
                sessionNotification.setDoNotRemoveSessionIndex(oldSessionID, Boolean.TRUE.toString());
            } catch (SSOException ssoe) {
                debug.error("Failed to set IDP Session Index for old session", ssoe);
            }

            IDPSession idpSession = IDPSSOUtil.retrieveCachedIdPSession(sessionIndex);

            if (idpSession != null) {
                idpSession.setSession(newSSOToken);
                IDPCache.idpSessionsByIndices.put(sessionIndex, idpSession);
                try {
                    SessionProvider sessionProvider = SessionManager.getProvider();
                    IDPSSOUtil.saveIdPSessionToTokenRepository(sessionIndex, sessionProvider, idpSession, newSSOToken);
                } catch (com.sun.identity.plugin.session.SessionException e) {
                    debug.warn("Failed to save idpSession to token store for {}", sessionIndex, e);
                }
                IDPCache.idpSessionsBySessionID.put(newSessionID, idpSession);
            }

            IDPCache.idpSessionsBySessionID.remove(oldSessionID);
            final String partner = IDPCache.spSessionPartnerBySessionID.remove(oldSessionID);
            if (partner != null) {
                IDPCache.spSessionPartnerBySessionID.put(newSessionID, partner);
            }
        }
    }
}
