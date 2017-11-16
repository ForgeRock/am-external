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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.extensions.quotaexhaustionaction;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.QuotaExhaustionAction;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import org.forgerock.openam.session.SessionCache;

/**
 * This is a sample {@link QuotaExhaustionAction} implementation, which just randomly kills the first session it finds.
 */
public class SampleQuotaExhaustionAction implements QuotaExhaustionAction {

    private static final Debug DEBUG = Debug.getInstance("amSession");

    /**
     * Check if the session quota for a given user has been exhausted and perform necessary actions in such as case.
     *
     * @param is The to-be-actived InternalSession.
     * @param existingSessions All existing sessions belonging to the same uuid (Map:sid-&gt;expiration_time).
     * @return <code>true</code> if the session activation request should be rejected, <code>false</code> otherwise.
     */
    @Override
    public boolean action(InternalSession is, Map<String, Long> existingSessions) {
        //kills the first session it finds randomly :)
        for (Map.Entry<String, Long> entry : existingSessions.entrySet()) {
            try {
                //getting an actual Session instance based on the session id
                Session session = SessionCache.getInstance().getSession(new SessionID(entry.getKey()));
                //we use the session to destroy itself.
                session.destroySession(session);
                //we only want to destroy one session, remember?
                break;
            } catch (SessionException se) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Failed to destroy the next expiring session.", se);
                }
                //deny the session activation request in this case
                return true;
            }
        }
        return false;
    }
}
