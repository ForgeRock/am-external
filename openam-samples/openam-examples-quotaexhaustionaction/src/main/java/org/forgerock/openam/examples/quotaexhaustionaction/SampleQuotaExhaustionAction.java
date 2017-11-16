/*
 * Copyright 2012 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.examples.quotaexhaustionaction;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.QuotaExhaustionAction;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;

/**
 * This is a sample {@link QuotaExhaustionAction} implementation, which just randomly kills the first session it finds.
 *
 * @author Peter Major
 */
public class SampleQuotaExhaustionAction implements QuotaExhaustionAction {

    private static Debug debug = SessionService.sessionDebug;

    /**
     * Check if the session quota for a given user has been exhausted and
     * perform necessary actions in such as case.
     *
     * @param is the to-be-actived InternalSession
     * @param existingSessions all existing sessions belonging to the same uuid (Map:sid-&gt;expiration_time)
     * @return true if the session activation request should be rejected, false otherwise
     */
    @Override
    public boolean action(InternalSession is, Map<String, Long> existingSessions) {
        //kills the first session it finds randomly :)
        for (Map.Entry<String, Long> entry : existingSessions.entrySet()) {
            try {
                //getting an actual Session instance based on the session id
                Session session = Session.getSession(new SessionID(entry.getKey()));
                //we use the session to destroy itself.
                session.destroySession(session);
                //we only want to destroy one session, remember?
                break;
            } catch (SessionException se) {
                if (debug.messageEnabled()) {
                    debug.message("Failed to destroy the next expiring session.", se);
                }
                //deny the session activation request in this case
                return true;
            }
        }
        return false;
    }
}
