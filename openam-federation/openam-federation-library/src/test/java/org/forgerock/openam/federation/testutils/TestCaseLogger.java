/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
