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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.EMERGENCY_TOKENCODE;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.SECURID;
import static org.forgerock.openam.auth.nodes.securid.SecurIdMethod.TOKEN;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.AuthenticationMethodBuilder.authenticationMethod;
import static org.forgerock.openam.auth.nodes.securid.SecurIdResponseBuilder.securIdResponse;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.junit.jupiter.api.Test;

class SecurIdResponseTest {
    @Test
    void getsReferenceIdCorrespondingToProvidedMethod() throws Exception {
        var securIdResponse = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId("method1").referenceId("refId1").build())
                .withAuthenticationMethod(authenticationMethod().methodId("method2").referenceId("refId2").build())
                .build();

        assertThat(securIdResponse.getReferenceId("method1")).isEqualTo("refId1");
        assertThat(securIdResponse.getReferenceId("method2")).isEqualTo("refId2");
    }

    @Test
    void throwsNodeProcessExceptionIfCorrespondingReferenceIdNotFound() {
        var securIdResponse = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId("method1").referenceId("refId1").build())
                .build();

        assertThatThrownBy(() -> securIdResponse.getReferenceId("method2")).isInstanceOf(NodeProcessException.class);
    }

    @Test
    void returnsAvailableAuthenticationMethodsSortedByAscendingPriority() {
        var securIdResponse = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId("SECURID").priority(1).build())
                .withAuthenticationMethod(authenticationMethod().methodId("TOKEN").priority(3).build())
                .withAuthenticationMethod(authenticationMethod().methodId("EMERGENCY_TOKENCODE").priority(2).build())
                .build();

        assertThat(securIdResponse.getAvailableMethods()).containsExactly(SECURID, EMERGENCY_TOKENCODE, TOKEN);
    }

    @Test
    void omitsNonApplicableMethods() {
        var securIdResponse = securIdResponse()
                .withAuthenticationMethod(authenticationMethod().methodId("SECURID").priority(1).build())
                .withAuthenticationMethod(authenticationMethod().methodId("TOKEN").priority(3).build())
                .withAuthenticationMethod(authenticationMethod()
                        .methodId("EMERGENCY_TOKENCODE")
                        .withMethodAttribute("METHOD_NOT_APPLICABLE", "DEVICE_NOT_REGISTERED")
                        .priority(2).build())
                .build();

        assertThat(securIdResponse.getAvailableMethods()).containsExactly(SECURID, TOKEN);
    }
}
