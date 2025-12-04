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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn;

/**
 * The various types of DOM-based exceptions that can be thrown during WebAuthn negotiations.
 * <p>
 * The links below are taken directly from the links given in the Web Authentication Spec.
 * <p>
 * <a href="https://www.w3.org/TR/webauthn/"/>
 */
public enum WebAuthnDomExceptionType {

    /** <a href="https://heycam.github.io/webidl/#notallowederror"/>. */
    NotAllowedError,
    /** <a href="https://heycam.github.io/webidl/#securityerror"/>. */
    SecurityError,
    /** <a href="https://heycam.github.io/webidl/#aborterror"/>. */
    AbortError,
    /** <a href="https://heycam.github.io/webidl/#notsupportederror"/>. */
    NotSupportedError,
    /** <a href="https://heycam.github.io/webidl/#invalidstateerror"/>. */
    InvalidStateError,
    /** <a href="https://heycam.github.io/webidl/#unknownerror"/>. */
    UnknownError,
    /** <a href="https://heycam.github.io/webidl/#networkerror"/>. */
    NetworkError,
    /** <a href="https://heycam.github.io/webidl/#timeouterror"/>. */
    TimeoutError,
    /** <a href="https://heycam.github.io/webidl/#encodingerror"/>. */
    EncodingError,
    /** <a href="https://heycam.github.io/webidl/#constrainterror"/>. */
    ConstraintError,
    /** <a href="https://heycam.github.io/webidl/#dataerror"/>. */
    DataError,
    /** <a href="https://webidl.spec.whatwg.org/#hierarchyrequesterror"/>. */
    HierarchyRequestError,
    /** <a href="https://webidl.spec.whatwg.org/#wrongdocumenterror"/>. */
    WrongDocumentError,
    /** <a href="https://webidl.spec.whatwg.org/#invalidcharactererror"/>. */
    InvalidCharacterError,
    /** <a href="https://webidl.spec.whatwg.org/#nomodificationallowederror"/>. */
    NoModificationAllowedError,
    /** <a href="https://webidl.spec.whatwg.org/#notfounderror"/>. */
    NotFoundError,
    /** <a href="https://webidl.spec.whatwg.org/#notsupportederror"/>. */
    InUseAttributeError,
    /** <a href="https://webidl.spec.whatwg.org/#invalidstateerror"/>. */
    SyntaxError,
    /** <a href="https://webidl.spec.whatwg.org/#invalidmodificationerror"/>. */
    InvalidModificationError,
    /** <a href="https://webidl.spec.whatwg.org/#namespaceerror"/>. */
    NamespaceError,
    /** <a href="https://webidl.spec.whatwg.org/#invalidaccesserror"/>. */
    InvalidAccessError,
    /** <a href="https://webidl.spec.whatwg.org/#typemismatcherror"/>. */
    TypeMismatchError,
    /** <a href="https://webidl.spec.whatwg.org/#urlmismatcherror"/>. */
    URLMismatchError,
    /** <a href="https://webidl.spec.whatwg.org/#quotaexceedederror"/>. */
    QuotaExceededError,
    /** <a href="https://webidl.spec.whatwg.org/#timeouterror"/>. */
    InvalidNodeTypeError,
    /** <a href="https://webidl.spec.whatwg.org/#datacloneerror"/>. */
    DataCloneError,
    /** <a href="https://webidl.spec.whatwg.org/#notreadableerror"/>. */
    NotReadableError,
    /** <a href="https://webidl.spec.whatwg.org/#encodingerror"/>. */
    TransactionInactiveError,
    /** <a href="https://webidl.spec.whatwg.org/#readonlyerror"/>. */
    ReadOnlyError,
    /** <a href="https://webidl.spec.whatwg.org/#versionerror"/>. */
    VersionError,
    /** <a href="https://webidl.spec.whatwg.org/#operationerror"/>. */
    OperationError
}
