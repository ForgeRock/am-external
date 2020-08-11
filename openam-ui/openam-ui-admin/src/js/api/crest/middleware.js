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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module api/crest/middleware
 */

import { CRESTError, RequestError, ParseError } from "@forgerock/crest-js";
import { t } from "i18next";

import Messages from "org/forgerock/commons/ui/common/components/Messages";
import redirectToUserLoginWithGoto from "org/forgerock/openam/ui/common/redirectToUser/loginWithGoto";

/**
 * CREST.js middleware to handle various types of CREST error.
 * @param {Promise} promise Promise from previous middleware.
 * @returns {Promise} Promise to pass to next middleware.
 */
const middleware = (promise) => {
    return promise.catch((error) => {
        if (error instanceof CRESTError) {
            if (error.status === 401) {
                redirectToUserLoginWithGoto();
            } else {
                Messages.addMessage({ message: error.message, type: Messages.TYPE_DANGER });
            }
        } else if (error instanceof RequestError) {
            Messages.addMessage({ message: t("config.messages.api.requestError"), type: Messages.TYPE_DANGER });
        } else if (error instanceof ParseError) {
            Messages.addMessage({ message: t("config.messages.api.parseError"), type: Messages.TYPE_DANGER });
        }

        throw error;
    });
};

export default middleware;
