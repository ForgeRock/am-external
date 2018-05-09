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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openam.federation.testutils;

import java.util.Map;
import java.util.logging.Level;

import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.Logger;

/**
 * A no-op logger used for Federation test cases.
 */
public class TestCaseLogger implements Logger {

    @Override
    public void init(String componentName) throws LogException {
    }

    @Override
    public void access(Level level, String messageID, String[] data, Object session) throws LogException {
    }

    @Override
    public void access(Level level, String msgid, String[] data, Object session, Map props) throws LogException {
    }

    @Override
    public void error(Level level, String messageId, String[] data, Object session) throws LogException {
    }

    @Override
    public void error(Level level, String msgid, String[] data, Object session, Map props) throws LogException {
    }

    @Override
    public boolean isLogEnabled() {
        return false;
    }

    @Override
    public boolean isAccessLoggable(Level level) {
        return false;
    }

    @Override
    public boolean isErrorLoggable(Level level) {
        return false;
    }
}
