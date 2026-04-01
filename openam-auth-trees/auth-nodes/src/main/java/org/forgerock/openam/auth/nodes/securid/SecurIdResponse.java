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

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.Objects;

import org.forgerock.openam.auth.node.api.NodeProcessException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from a SecurID Initialize or Verify request.
 *
 * @param context
 * @param credentialValidationResults
 * @param attemptResponseCode
 * @param attemptReasonCode
 * @param challengeMethods
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SecurIdResponse(SecurIdContext context,
                       List<CredentialValidationResult> credentialValidationResults,
                       AttemptResponseCode attemptResponseCode,
                       String attemptReasonCode,
                       ChallengeMethods challengeMethods) {

    /** Message associated with the response code. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SecurIdContext(String messageId, String authnAttemptId) {
    }

    /**
     * Credential Validation Result.
     *
     * @param methodId
     * @param methodResponseCode
     * @param methodReasonCode
     * @param authnAttributes
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CredentialValidationResult(String methodId,
                                      MethodResponseCode methodResponseCode,
                                      String methodReasonCode,
                                      List<NameValuePair> authnAttributes) {
    }

    /** Possible method response codes. */
    enum MethodResponseCode {
        SUCCESS,
        FAIL,
        IN_PROCESS
    }

    /** Possible attempt response codes. */
    enum AttemptResponseCode {
        SUCCESS,
        CHALLENGE
    }

    /** Available challenge methods for MFA. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChallengeMethods(List<Challenge> challenges) {
    }

    /**
     * Challenge.
     *
     * @param requiredMethods
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Challenge(List<AuthenticationMethod> requiredMethods) {
    }

    /** Authentication Methods.
     *
     * @param methodId
     * @param displayName
     * @param priority
     * @param versions
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthenticationMethod(String methodId,
                                String displayName,
                                Integer priority,
                                List<AuthenticationMethodVersion> versions) {
        /** The priority, or 0 if not provided. */
        public Integer priority() {
            return priority != null ? priority : 0;
        }
    }

    /**
     * Authentication Method Version.
     *
     * @param versionId
     * @param methodAttributes
     * @param valueRequired
     * @param referenceId
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthenticationMethodVersion(String versionId,
                                       List<NameValuePair> methodAttributes,
                                       Boolean valueRequired,
                                       String referenceId) {
    }

    /**
     * Name-Value pair.
     *
     * @param name
     * @param value
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record NameValuePair(String name, String value) {
    }

    /**
     * Get the reference ID corresponding to the given method ID.
     * @param methodId the method ID of the method to get the reference ID for
     * @return the reference ID
     * @throws NodeProcessException if no reference ID is found
     */
    String getReferenceId(String methodId) throws NodeProcessException {
        return challengeMethods.challenges.stream()
                .map(challenge -> challenge.requiredMethods().get(0))
                .filter(method -> method.methodId().equalsIgnoreCase(methodId))
                .map(method -> method.versions().get(0).referenceId())
                .findFirst()
                .orElseThrow(() -> new NodeProcessException("No reference ID found for method: " + methodId));
    }

    /**
     * Get the available authentication methods sorted by priority.
     * @return the available methods
     */
    List<SecurIdMethod> getAvailableMethods() {
        return challengeMethods.challenges.stream()
                .map(challenge -> challenge.requiredMethods.get(0))
                .filter(method -> isApplicable(method.versions.get(0).methodAttributes))
                .sorted(comparing(AuthenticationMethod::priority))
                .map(method -> SecurIdMethod.fromMethodId(method.methodId))
                .distinct()
                .filter(Objects::nonNull)
                .toList();
    }

    private static boolean isApplicable(List<NameValuePair> methodAttributes) {
        return methodAttributes == null || methodAttributes.isEmpty()
                || !methodAttributes.get(0).name.equalsIgnoreCase("METHOD_NOT_APPLICABLE");
    }
}
