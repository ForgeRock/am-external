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
 * Copyright 2018 ForgeRock AS.
 */

import { random } from "faker";

/**
 * Creates a CREST resource payload.
 * @module support/crest/createPayload
 * @param {string} [id=faker.random.uuid] ID for the resource.
 * @param {string} [typeId=faker.random.uuid] Type ID for the resource.
 * @returns {Object} A CREST resource payload.
 */
const createPayload = (id = random.uuid(), typeId = random.uuid()) => ({
    _id: id,
    _type: {
        _id: typeId
    },
    attribute: random.word()
});

export default createPayload;
