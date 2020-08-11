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
 * Copyright 2013-2019 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap.helpers;

import com.sun.identity.idm.IdRepoErrorCode;
import org.forgerock.opendj.ldap.messages.Result;

import java.nio.charset.Charset;

/**
 * Handles AD and ADAM specific aspects of Data Store. More specifically handles the way the unicodePwd attribute needs
 * to be generated.
 */
public class ADAMHelper extends DirectoryHelper {

    /**
     * Encloses the password with double quotes first, then returns the UTF-16LE bytes representing that value.
     *
     * @param password The password in string format.
     * @return The encoded password, or null if encoding is not applicable.
     */
    @Override
    public byte[] encodePassword(String password) {
        return password == null ? null : ("\"" + password + "\"").getBytes(Charset.forName("UTF-16LE"));
    }

    /**
     * Returns the Password Policy error code from the result passed.
     *
     * @param result Result object from the DJLDAPv3Repo Operation.
     * @return IdRepoErrorCode based on the result. Returns null if AD Error Code doesn't have a match.
     */
    @Override
    public String getPasswordPolicyErrorCode(Result result) {
        String AD_CONSTRAINT_ERROR_CODE = "00000005:";
        String AD_PASSWORD_POLICY_ERROR_CODE = "0000052D:";
        String AD_PASSWORD_EXPIRED = "data 532";
        String S4_PASSWORD_EXPIRED = "NT_STATUS_PASSWORD_EXPIRED";
        String AD_ACCOUNT_DISABLED = "data 533";
        String S4_ACCOUNT_DISABLED = "NT_STATUS_ACCOUNT_DISABLED";
        String AD_ACCOUNT_EXPIRED = "data 701";
        String S4_ACCOUNT_EXPIRED = "NT_STATUS_ACCOUNT_EXPIRED";
        String AD_PASSWORD_RESET = "data 773";
        String S4_PASSWORD_RESET = "NT_STATUS_PASSWORD_MUST_CHANGE";
        String AD_ACCOUNT_LOCKED = "data 775";
        String S4_ACCOUNT_LOCKED = "NT_STATUS_ACCOUNT_LOCKED_OUT";

        String diagMessage = result.getDiagnosticMessageAsString();
        if (diagMessage.startsWith(AD_PASSWORD_POLICY_ERROR_CODE)) {
            return IdRepoErrorCode.INSUFFICIENT_PASSWORD_QUALITY;
        } else if (diagMessage.startsWith(AD_CONSTRAINT_ERROR_CODE)) {
            return IdRepoErrorCode.PASSWORD_MOD_NOT_ALLOWED;
        } else if (diagMessage.contains(AD_PASSWORD_EXPIRED) || diagMessage.contains(S4_PASSWORD_EXPIRED)) {
            return IdRepoErrorCode.PASSWORD_EXPIRED;
        } else if (diagMessage.contains(AD_ACCOUNT_DISABLED) || diagMessage.contains(S4_ACCOUNT_DISABLED) ||
                diagMessage.contains(AD_ACCOUNT_EXPIRED) || diagMessage.contains(S4_ACCOUNT_EXPIRED) ||
                diagMessage.contains(AD_ACCOUNT_LOCKED) || diagMessage.contains(S4_ACCOUNT_LOCKED)) {
            return IdRepoErrorCode.ACCOUNT_LOCKED;
        } else if (diagMessage.contains(AD_PASSWORD_RESET) || diagMessage.contains(S4_PASSWORD_RESET)) {
            return IdRepoErrorCode.CHANGE_AFTER_RESET;
        }
        return null;
    }
}
