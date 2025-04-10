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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
