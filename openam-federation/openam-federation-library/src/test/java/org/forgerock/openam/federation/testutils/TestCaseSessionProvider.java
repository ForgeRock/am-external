/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.federation.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.utils.Time;
import org.forgerock.openam.utils.TimeDuration;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionProvider;

/**
 * A simplified SessionProvider implementation for an in-memory only session store for a single user session.
 */
public class TestCaseSessionProvider implements SessionProvider {

    private static Object session;
    private static String sessionId;
    private static String userId;
    private static Map<String, List<String>> properties;

    /**
     * Set/reset the properties for this instance.
     *
     * @param newSession The new session.
     * @param newSessionId The new sessionId for this session.
     * @param newUserId The new UserID for this session.
     * @param newProperties The new set of properties to be used with the session.
     */
    public static void setState(Object newSession, String newSessionId, String newUserId,
            Map<String, List<String>> newProperties) {
        session = newSession;
        sessionId = newSessionId;
        userId = newUserId;
        properties = newProperties;
    }

    @Override
    public Object createSession(Map info,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                StringBuffer targetApplication) throws SessionException {
        return session;
    }

    @Override
    public Object getSession(String sessionID) throws SessionException {
        return session;
    }

    @Override
    public Object getSession(HttpServletRequest request) throws SessionException {
        return session;
    }

    @Override
    public void invalidateSession(Object session, HttpServletRequest request, HttpServletResponse response) throws SessionException {

    }

    @Override
    public boolean isValid(Object session) throws SessionException {
        return true;
    }

    @Override
    public String getSessionID(Object session) {
        return sessionId;
    }

    @Override
    public String getPrincipalName(Object session) throws SessionException {
        return userId;
    }

    @Override
    public void setProperty(Object session, String name, String[] values) throws UnsupportedOperationException, SessionException {
        properties.put(name, new ArrayList<>(Arrays.asList(values)));
    }

    @Override
    public String[] getProperty(Object session, String name) throws UnsupportedOperationException, SessionException {
        return properties.get(name).toArray(new String[0]);
    }

    @Override
    public void addListener(Object session, SessionListener listener) throws UnsupportedOperationException, SessionException {

    }

    @Override
    public void setLoadBalancerCookie(HttpServletRequest request, HttpServletResponse response) {

    }

    @Override
    public long getTimeLeft(Object session) throws SessionException {
        return 300;
    }
}
