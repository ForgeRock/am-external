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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.Objects;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.util.Reject;

import com.sun.identity.idm.IdType;

/**
 * Simple class that captures the username and identityType of an identity.
 * <p>Creation of an identified identity indicates the identity has been confirmed to exist.
 * <p>Note that there are many ways of identifying an identity through a journey and the presence of an
 * {@link IdentifiedIdentity} is not evidence that sufficient authentication has taken place.
 */
@Supported
public final class IdentifiedIdentity {

    private static final String USERNAME_FIELD = "name";
    private static final String IDENTITY_TYPE_FIELD = "type";

    private final String username;
    private final IdType identityType;

    /**
     * Create an instance of {@link IdentifiedIdentity} with the given username and identityType.
     * <p>Creation of an identified identity indicates the identity has been confirmed to exist.
     *
     * @param username     A non-null String denoting the username of the identified identity.
     * @param identityType A non-null String denoting the type of the identity.
     */
    @Supported
    public IdentifiedIdentity(String username, IdType identityType) {
        Reject.ifNull(username, "username must be non-null");
        Reject.ifNull(identityType, "identityType must be non-null");

        this.username = username;
        this.identityType = identityType;
    }

    /**
     * Retrieves the username for this {@link IdentifiedIdentity} instance.
     *
     * @return The username for this instance.
     */
    @Supported
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the identityType for this {@link IdentifiedIdentity} instance.
     *
     * @return The identityType for this instance.
     */
    @Supported
    public IdType getIdentityType() {
        return identityType;
    }

    /**
     * Returns the {@link JsonValue} representation of this {@link IdentifiedIdentity} instance.
     *
     * @return The {@link JsonValue} representation of this instance.
     */
    public JsonValue toJson() {
        return json(object(field(USERNAME_FIELD, username), field(IDENTITY_TYPE_FIELD, identityType.getName())));
    }

    /**
     * Returns a new {@link IdentifiedIdentity} instance based on the provided {@link JsonValue} value.
     *
     * @param json A {@link JsonValue} value representing an {@link IdentifiedIdentity}.
     * @return A new {@link IdentifiedIdentity} instance.
     */
    public static IdentifiedIdentity fromJson(JsonValue json) {
        Reject.unless(json.keys().containsAll(List.of(USERNAME_FIELD, IDENTITY_TYPE_FIELD)),
                "Provided json must contain a \"" + USERNAME_FIELD + "\" and \"" + IDENTITY_TYPE_FIELD + "\" field");
        String idTypeName = json.get(IDENTITY_TYPE_FIELD).asString();
        Reject.unless(IdType.values().stream()
                              .map(IdType::getName)
                              .anyMatch(name -> name.equals(idTypeName)),
                IDENTITY_TYPE_FIELD + " must be a known IdType");
        return new IdentifiedIdentity(json.get(USERNAME_FIELD).asString(), IdType.getType(idTypeName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IdentifiedIdentity that = (IdentifiedIdentity) o;
        return Objects.equals(username, that.username) && Objects.equals(identityType, that.identityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, identityType);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
