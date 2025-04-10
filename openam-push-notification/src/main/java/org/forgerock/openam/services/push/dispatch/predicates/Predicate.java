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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.services.push.dispatch.predicates;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.openam.services.push.PushMessageResource;

/**
 * <p>An interface for a basic, stand-alone predicate which can be evaluated given some
 * {@link JsonValue} input and serialized for storage.</p>
 *
 * <p>{@link Predicate}s are evaluated by the {@link PushMessageResource} once a response has been received by an
 * appropriate endpoint but before that response is delivered back to the processing module.</p>
 *
 */
@SupportedAll
public interface Predicate {

    /**
     * Execute the predicate against the given Json content and return the predicate's success/failure.
     *
     * @param content against which the predicate can be performed.
     * @return whether the predicate passed or not.
     */
    boolean perform(JsonValue content);

    /**
     * Returns a jsonified representation of this object to be used when tranmitting across cluster.
     *
     * @return A jsonified representation of this class instance.
     */
    String jsonify();

}
