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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

//@Checkstyle:off LineLength
/**
 * The FIDO Alliance publishes authenticator metadata via the FIDO Alliance Metadata Service (MDS).
 * <p>
 * The Metadata Service supplies information for all known devices in a single BLOB. The BLOB is provided in the form
 * of a JSON Web Token (JWT) which is defined in the following specification:
 * </p>
 * <p>
 * <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC7519</a>
 * <a href="https://tools.ietf.org/html/rfc7515">RFC7515</a>
 * </p>
 * <p>
 * The classes in this package are intended to support the parsing and processing of the FIDO Alliance Metadata
 * Service BLOB for integration with WebAuthn implementations.
 * </p>
 * <p>
 * <a href="https://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#metadata-blob-object-processing-rules">
 * Metadata BLOB object processing rules</a>
 * <a href="https://www.w3.org/TR/webauthn-2/#sctn-registering-a-new-credential">
 *     WebAuthn Registering a New Credential</a>
 * </p>
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido;
