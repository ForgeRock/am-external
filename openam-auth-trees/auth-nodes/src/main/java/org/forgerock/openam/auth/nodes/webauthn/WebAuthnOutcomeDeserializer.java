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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved.
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.nodes.webauthn.ClientScriptUtilities.RESPONSE_DELIMITER;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Deserializes the outcome of a WebAuthn operation.
 * <p>
 * The outcome can be in one of two formats:
 * <ul>
 *     <li>Legacy format: A :: separated string, e.g. "unsupported", "ERROR::DataError:message".</li>
 *     <li>JSON format: A JSON object with the following keys:
 *     <ul>
 *         <li>legacyData: The original :: separated format used by the javascript returned by AM and the Ping SDK.</li>
 *         <li>authenticatorAttachment: The authenticator attachment type (e.g. "platform", "cross-platform").</li>
 *         <li>error: An optional error that occurred during the operation.</li>
 *     </ul>
 * </ul>
 * <p>
 * In the future, the intention is to migrate to the JSON format. This class should maintain backwards compatibility
 * with the legacy format for the foreseeable future, however may take on more of the deserialization responsibilities
 * as the migration progresses (e.g. decoding CBOR data, etc).
 */
public class WebAuthnOutcomeDeserializer {
    private static final String UNSUPPORTED = "unsupported";
    /** Used to indicate we are communicating a DOM Exception back to the server. */
    private static final String ERROR_MESSAGE = "ERROR";
    private static final String LEGACY_DATA_KEY = "legacyData";
    private static final String ERROR_KEY = "error";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Deserialize the outcome of a WebAuthn operation.
     *
     * @param rawOutcome The raw outcome.
     * @return The deserialized outcome.
     */
    public WebAuthnOutcome deserialize(String rawOutcome) {
        try {
            JsonValue jsonValue = new JsonValue(OBJECT_MAPPER.readValue(rawOutcome, Map.class));
            if (jsonValue.isDefined(LEGACY_DATA_KEY) || jsonValue.isDefined(ERROR_KEY)) {
                return deserializeJson(jsonValue);
            }
        } catch (IOException | JsonValueException e) {
            // ignore exception and continue to decode as legacy instead
        }
        return deserializeLegacy(rawOutcome);
    }

    private WebAuthnOutcome deserializeLegacy(String rawOutcome) {
        if (rawOutcome.equals(UNSUPPORTED)) {
            return WebAuthnOutcome.unsupported();
        }
        if (rawOutcome.startsWith(ERROR_MESSAGE)) {
            String errorDescription = rawOutcome.substring((ERROR_MESSAGE + RESPONSE_DELIMITER).length());
            return WebAuthnOutcome.error(WebAuthnDomException.parse(errorDescription));
        }
        return new WebAuthnOutcome(rawOutcome, Optional.empty());
    }

    private WebAuthnOutcome deserializeJson(JsonValue jsonValue) {
        if (jsonValue.isDefined("error")) {
            return WebAuthnOutcome.error(WebAuthnDomException.parse(jsonValue.get("error").asString()));
        }
        return new WebAuthnOutcome(jsonValue.get("legacyData").asString(),
                Optional.ofNullable(jsonValue.get("authenticatorAttachment").asString()));
    }
}
