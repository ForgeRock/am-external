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
 * Copyright 2011-2018 ForgeRock AS.
 */

import Constants from "org/forgerock/openam/ui/common/util/Constants";

export default {
    "badRequest": {
        status: "400",
        message: "badRequestError"
    },
    "unauthenticated": {
        status: "401",
        event: Constants.EVENT_UNAUTHENTICATED
    },
    "forbidden": {
        status: "403",
        event: Constants.EVENT_UNAUTHORIZED,
        message: "forbiddenError"
    },
    "notFound": {
        status: "404",
        message: "notFoundError"
    },
    "conflict": {
        status: "409",
        message: "conflictError"
    },
    "serverError": {
        status: "503",
        event: Constants.EVENT_SERVICE_UNAVAILABLE
    },
    "internalServerError": {
        status: "500",
        message: "internalError"
    },
    "incorrectRevision": {
        status: "412",
        message: "incorrectRevisionError"
    }
};
