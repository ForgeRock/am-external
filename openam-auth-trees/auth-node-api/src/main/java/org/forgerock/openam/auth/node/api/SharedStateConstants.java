/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

/**
 * This class represents all the constants that can be used as keys for storing values in the tree's shared state.
 *
 * @supported.all.api
 */
public final class SharedStateConstants {

    /**
     * Private constructor.
     */
    private SharedStateConstants() {
    }

    /** The Realm. */
    public static final String REALM = "realm";

    /** The current authentication level achieved so far processing the tree. */
    public static final String AUTH_LEVEL = "authLevel";

    /** The desirable authentication level to achieve at successful authentication. */
    public static final String TARGET_AUTH_LEVEL = "targetAuthLevel";

    /** The nodeId of the node being processed in the tree. */
    public static final String CURRENT_NODE_ID = "currentNodeId";

    /** The username. */
    public static final String USERNAME = "username";

    /** The user's password. */
    public static final String PASSWORD = "password";

    /** The One Time Password. */
    public static final String ONE_TIME_PASSWORD = "oneTimePassword";

    /** The time at which the one time password was created. */
    public static final String ONE_TIME_PASSWORD_TIMESTAMP = "oneTimePasswordTimestamp";

    /** The countdown of number of retries remaining. */
    public static final String RETRIES_REMAINING = "retriesRemaining";

    /** The user's email address. */
    public static final String EMAIL_ADDRESS = "emailAddress";

    /** The post authentication success URL. */
    public static final String SUCCESS_URL = "successUrl";

    /** The post authentication failure URL. */
    public static final String FAILURE_URL = "failureUrl";

    /** The goto URL parameter key. **/
    public static final String USER_GOTO_PARAM_KEY = "userGotoParam";

    /** The gotoOnFail URL parameter key. **/
    public static final String USER_GOTO_ON_FAIL_PARAM_KEY = "userGotoOnFailParam";

}
