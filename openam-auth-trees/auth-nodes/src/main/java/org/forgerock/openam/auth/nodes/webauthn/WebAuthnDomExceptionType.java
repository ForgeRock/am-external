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
 * Copyright 2018-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn;

/**
 * The various types of DOM-based exceptions that can be thrown during WebAuthn negotiations.
 *
 * The links below are taken directly from the links given in the Web Authentication Spec.
 *
 * https://www.w3.org/TR/webauthn/
 */
public enum WebAuthnDomExceptionType {

    /** https://heycam.github.io/webidl/#notallowederror. */
    NotAllowedError,
    /** https://heycam.github.io/webidl/#securityerror. */
    SecurityError,
    /** https://heycam.github.io/webidl/#aborterror. */
    AbortError,
    /** https://heycam.github.io/webidl/#notsupportederror. */
    NotSupportedError,
    /** https://heycam.github.io/webidl/#invalidstateerror. */
    InvalidStateError,
    /** https://heycam.github.io/webidl/#unknownerror. */
    UnknownError,
    /** https://heycam.github.io/webidl/#networkerror. */
    NetworkError,
    /** https://heycam.github.io/webidl/#timeouterror. */
    TimeoutError,
    /** https://heycam.github.io/webidl/#encodingerror. */
    EncodingError,
    /** https://heycam.github.io/webidl/#constrainterror. */
    ConstraintError,
    /** https://heycam.github.io/webidl/#dataerror. */
    DataError

}
