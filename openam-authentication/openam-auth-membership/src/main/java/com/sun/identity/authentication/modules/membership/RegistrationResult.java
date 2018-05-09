/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.authentication.modules.membership;

/**
 * The enum wraps up all of the possible registration error states in the Membership module
 * 
 * @author steve
 */
public enum RegistrationResult {

    NO_USER_NAME_ERROR("noUserNameError"),

    NO_PASSWORD_ERROR("noPasswordError"),
    
    NO_CONFIRMATION_ERROR("noConfirmationError"),

    PASSWORD_MISMATCH_ERROR("PasswdMismatch"),

    USER_EXISTS_ERROR("userExistsError"),

    MISSING_REQ_FIELD_ERROR("missingReqFieldError"),

    USER_PASSWORD_SAME_ERROR("UPsame"),

    PASSWORD_TOO_SHORT("passwordTooShort"),
    
    NO_ERROR("noError"),
    
    PROFILE_ERROR("profileError");

    private final String name;

    private RegistrationResult(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }   
}
