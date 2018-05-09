/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

/**
 * Exception class to indicate when the sso request redirect failed.
 */
public class UnableToRedirectException extends Exception {

    /**
     * Creates a new UnableToRedirectException.
     * @param cause the exception that caused this exception
     */
    public UnableToRedirectException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new UnableToRedirectException.
     */
    public UnableToRedirectException() {
    }

    /**
     * Creates a new UnableToRedirectException.
     * @param message a message describing the cause of the exception
     */
    public UnableToRedirectException(String message) {
        super(message);
    }

    /**
     * Creates a new UnableToRedirectException.
     * @param message a message describing the cause of the exception
     * @param cause the exception that caused this exception
     */
    public UnableToRedirectException(String message, Throwable cause) {
        super(message, cause);
    }
}
