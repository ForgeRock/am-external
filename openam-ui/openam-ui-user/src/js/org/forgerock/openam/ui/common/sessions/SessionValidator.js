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
 * Copyright 2015-2019 ForgeRock AS.
 */

import removeLocalUserData from "org/forgerock/openam/ui/user/login/removeLocalUserData";
import Router from "org/forgerock/commons/ui/common/main/Router";

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
    delay = window.setTimeout(() => {
        strategy().then((seconds) => {
            /**
             * If we're within the window of 0 seconds left on the session but still monumentality valid,
             * backoff the next schedule by a predetermined number of seconds. Avoids an immediate schedule.
             */
            const adjustedSeconds = seconds > 0 ? seconds : SESSION_ALMOST_EXPIRED_BACKOFF_SECONDS;

            validate(strategy, adjustedSeconds);
        }, () => {
            stop();
            removeLocalUserData();
            Router.routeTo(Router.configuration.routes.sessionExpired, { trigger: true });
        });
    }, seconds * ONE_SECOND_IN_MILLISECONDS);
}

export default {
    /**
     * Starts the periodic session validation progress using the specified strategy.
     * @param {org/forgerock/openam/ui/common/sessions/SessionValidator~Strategy} strategy Strategy to use to
     * perform validation
     * @param seconds The number of seconds delay before triggering the strategy for the first time
     */
    start (strategy, seconds) {
        if (delay) {
            stop();
        }
        validate(strategy, seconds);
    },
    /**
     * Stops the periodic session validation progress.
     */
    stop
};