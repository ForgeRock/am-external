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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import redirectToUserLoginWithGoto from "org/forgerock/openam/ui/common/redirectToUser/loginWithGoto";

/**
  * @module org/forgerock/openam/ui/common/sessions/SessionValidator
  */

const ONE_SECOND_IN_MILLISECONDS = 1000;
const SESSION_ALMOST_EXPIRED_BACKOFF_SECONDS = 1;
let delay;

/**
 * Interface that strategies must adhere to
 * @callback Strategy
 * @returns {Promise} when resolved, promise must pass a single argument with the seconds until the next check
 */

function stop () {
    window.clearTimeout(delay);

    delay = null;
}

function validate (strategy, seconds) {
    delay = setTimeout(() => {
        strategy().then((seconds) => {
            /**
             * If we're within the window of 0 seconds left on the session but still monumentality valid,
             * backoff the next schedule by a predetermined number of seconds. Avoids an immediate schedule.
             */
            const adjustedSeconds = seconds > 0 ? seconds : SESSION_ALMOST_EXPIRED_BACKOFF_SECONDS;

            validate(strategy, adjustedSeconds);
        }, () => {
            stop();

            redirectToUserLoginWithGoto();
        });
    }, seconds * ONE_SECOND_IN_MILLISECONDS);
}

export default {
    /**
     * Starts the periodic session validation progress using the specified strategy.
     * @param {org/forgerock/openam/ui/common/sessions/SessionValidator~Strategy} strategy Strategy to use to
     * perform validation
     */
    start (strategy) {
        if (delay) { throw new Error("Validator has already been started"); }

        validate(strategy, 0);
    },
    /**
     * Stops the periodic session validation progress.
     */
    stop
};
