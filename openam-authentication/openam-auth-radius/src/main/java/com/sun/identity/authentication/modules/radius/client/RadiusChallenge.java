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
package com.sun.identity.authentication.modules.radius.client;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.util.encode.Base64;

/**
 * Concrete implementation of Challenge interface for use in RADIUS authentication nodes.
 * This class preserves the original octets to avoid re-encoding issues.
 */
public class RadiusChallenge implements Challenge {

    private final byte[] octets;
    private final String replyMessage;
    private final String response;
    private StateAttribute stateAttribute;

    /**
     * Constructs a RadiusChallenge from a StateAttribute to preserve original octets.
     *
     * @param stateAttribute the StateAttribute from the RADIUS server
     * @param replyMessage the challenge message to present to the user
     * @param response the challenge response from the user
     */
    public RadiusChallenge(StateAttribute stateAttribute, String replyMessage, String response) {
        this.octets = stateAttribute != null ? stateAttribute.getOctets() : null;
        this.replyMessage = replyMessage;
        this.response = response;
        this.stateAttribute = stateAttribute;
    }

    /**
     * Constructs a RadiusChallenge from a {@link JsonValue}.
     *
     * @param jsonValue The {@link JsonValue} to construct the {@link RadiusChallenge} from.
     */
    public RadiusChallenge(JsonValue jsonValue) {
        JsonValue octetsValue = jsonValue.get("octets");
        this.octets = Base64.decode(octetsValue.asString());
        this.replyMessage = jsonValue.get("replyMessage").asString();
        this.response = jsonValue.get("response").asString();
    }

    /**
     * Returns the StateAttribute, creating it from preserved octets.
     * This avoids re-encoding by using the original RADIUS octets.
     *
     * @return the StateAttribute containing the original state
     */
    @Override
    public StateAttribute getStateAttribute() {
        if (stateAttribute == null && octets != null) {
            stateAttribute = new StateAttribute(octets);
        }
        return stateAttribute;
    }

    /**
     * Returns the state string extracted from octets.
     *
     * @return the opaque state string from the RADIUS server
     */
    @Override
    public String getState() {
        if (stateAttribute == null) {
            StateAttribute temp = new StateAttribute(octets);
            return temp.getState();
        }
        return stateAttribute.getState();
    }

    /**
     * Returns the reply message to be presented to the user.
     *
     * @return the challenge message
     */
    @Override
    public String getReplyMessage() {
        return replyMessage;
    }

    /**
     * Returns the challenge response.
     *
     * @return the challenge response
     */
    public String getResponse() {
        return response;
    }

    /**
     * Converts the {@link RadiusChallenge} to a {@link JsonValue}.
     * Stores octets as Base64 encoded string to preserve binary data.
     *
     * @return The {@link JsonValue} representation of the {@link RadiusChallenge}.
     */
    public JsonValue toJsonValue() {
        return json(object(
                field("octets", octets != null ? Base64.encode(octets) : null),
                field("replyMessage", replyMessage),
                field("response", response)));
    }
}
