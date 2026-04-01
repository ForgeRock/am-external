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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.securid;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.AttemptResponseCode;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.AuthenticationMethod;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.AuthenticationMethodVersion;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.Challenge;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.ChallengeMethods;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.CredentialValidationResult;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.MethodResponseCode;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.NameValuePair;
import org.forgerock.openam.auth.nodes.securid.SecurIdResponse.SecurIdContext;

class SecurIdResponseBuilder {
    private String messageId;
    private String authnAttemptId;
    private AttemptResponseCode attemptResponseCode;
    private List<CredentialValidationResult> credentialValidationResults = null;
    private final List<Challenge> challenges = new ArrayList<>();

    static SecurIdResponseBuilder securIdResponse() {
        return new SecurIdResponseBuilder();
    }

    SecurIdResponseBuilder messageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    SecurIdResponseBuilder authnAttemptId(String authnAttemptId) {
        this.authnAttemptId = authnAttemptId;
        return this;
    }

    SecurIdResponseBuilder attemptResponseCode(AttemptResponseCode attemptResponseCode) {
        this.attemptResponseCode = attemptResponseCode;
        return this;
    }

    SecurIdResponseBuilder withCredentialValidationResult(CredentialValidationResult credentialValidationResult) {
        if (credentialValidationResults == null) {
            credentialValidationResults = new ArrayList<>();
        }
        credentialValidationResults.add(credentialValidationResult);
        return this;
    }

    SecurIdResponseBuilder withAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        challenges.add(new Challenge(List.of(authenticationMethod)));
        return this;
    }

    SecurIdResponse build() {
        return new SecurIdResponse(
                new SecurIdContext(messageId, authnAttemptId),
                credentialValidationResults,
                attemptResponseCode,
                null,
                new ChallengeMethods(challenges));
    }

    static class CredentialValidationResultBuilder {
        private MethodResponseCode methodResponseCode;
        private final List<NameValuePair> nameValuePairs = new ArrayList<>();

        static CredentialValidationResultBuilder credentialValidationResult() {
            return new CredentialValidationResultBuilder();
        }

        CredentialValidationResultBuilder methodResponseCode(MethodResponseCode methodResponseCode) {
            this.methodResponseCode = methodResponseCode;
            return this;
        }

        CredentialValidationResultBuilder withQrCodeUrl(String qrCodeUrl) {
            nameValuePairs.add(new NameValuePair("QRCODE", qrCodeUrl));
            return this;
        }

        CredentialValidationResult build() {
            return new CredentialValidationResult(null, methodResponseCode, null, nameValuePairs);
        }
    }

    static class AuthenticationMethodBuilder {
        private String methodId;
        private int priority;
        private String referenceId;
        List<NameValuePair> methodAttributes = new ArrayList<>();

        static AuthenticationMethodBuilder authenticationMethod() {
            return new AuthenticationMethodBuilder();
        }

        AuthenticationMethodBuilder methodId(String methodId) {
            this.methodId = methodId;
            return this;
        }

        AuthenticationMethodBuilder priority(int priority) {
            this.priority = priority;
            return this;
        }

        AuthenticationMethodBuilder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        AuthenticationMethodBuilder withMethodAttribute(String name, String value) {
            methodAttributes.add(new NameValuePair(name, value));
            return this;
        }

        AuthenticationMethod build() {
            return new AuthenticationMethod(methodId, null, priority, List.of(
                    new AuthenticationMethodVersion(null, methodAttributes, null, referenceId)));
        }
    }
}
