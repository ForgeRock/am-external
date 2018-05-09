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
 * Copyright 2012-2017 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openam.examples.quotaexhaustionaction;

import static org.forgerock.openam.session.SessionConstants.SESSION_DEBUG;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.QuotaExhaustionAction;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.session.clientsdk.SessionCache;

import javax.inject.Inject;
import java.util.Map;

/**
 * This is a sample {@link QuotaExhaustionAction} implementation,
 * which randomly kills the first session it finds.
 */
public class SampleQuotaExhaustionAction implements QuotaExhaustionAction {

    private static Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));

    private final SessionCache sessionCache;
    private final SessionService sessionService;

    public SampleQuotaExhaustionAction() {
        this.sessionCache = InjectorHolder.getInstance(SessionCache.class);
        this.sessionService = InjectorHolder.getInstance(SessionService.class);
    }

    @Inject
    public SampleQuotaExhaustionAction(SessionCache sessionCache, SessionService sessionService) {
        this.sessionCache = sessionCache;
        this.sessionService = sessionService;
    }

    /**
     * Check if the session quota for a given user has been exhausted and
     * if so perform the necessary actions. This implementation randomly
     * destroys the first session it finds.
     *
     * @param is               The InternalSession to be activated.
     * @param existingSessions All existing sessions that belong to the same
     *                         uuid (Map:sid-&gt;expiration_time).
     * @return true If the session activation request should be rejected,
     *              otherwise false.
     */
    @Override
    public boolean action(
            Session is,
            Map<String, Long> existingSessions, long excessSessionCount) {
        for (Map.Entry<String, Long> entry : existingSessions.entrySet()) {
            try {
                // Get a Session from the cache based on the session ID, and destroy it.
                SessionID sessionId = new SessionID(entry.getKey());
                Session session = sessionCache.getSession(sessionId);
                sessionService.destroySession(session, sessionId);
                // Only destroy the first session.
                break;
            } catch (SessionException se) {
                if (debug.messageEnabled()) {
                    debug.message("Failed to destroy existing session.", se);
                }
                // In this case, deny the session activation request.
                return true;
            }
        }
        return false;
    }
}