/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oauth2;


public class NoEmailSentException extends Exception {
    // private Exception nestedException;

//    public NoEmailSentException(
//              Exception nestedException,
//              String exceptionMessage) {
//        this(exceptionMessage);
//        this.nestedException = nestedException;
//    }

    public NoEmailSentException(String message) {
        super(message);
    }

    public NoEmailSentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoEmailSentException(Throwable cause) {
        super(cause);
    }
}
