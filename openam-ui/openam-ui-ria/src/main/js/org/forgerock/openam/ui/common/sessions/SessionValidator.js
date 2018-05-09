/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/common/sessions/SessionValidator
  */
define([
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/user/login/logout"
], (Router, logout) => {
    let delay;
    const ONE_SECOND_IN_MILLISECONDS = 1000;
    const SESSION_ALMOST_EXPIRED_BACKOFF_SECONDS = 1;

    function stop () {
        clearTimeout(delay);

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

                // Invoke generic logout module to ensure Configuration.loggedUser is nullified
                logout.default().then(() => {
                    Router.routeTo(Router.configuration.routes.sessionExpired, { trigger: true });
                });
            });
        }, seconds * ONE_SECOND_IN_MILLISECONDS);
    }

    return {
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
});

/**
 * Interface that strategies must adhere to
 * @callback Strategy
 * @memberOf module:org/forgerock/openam/ui/common/sessions/SessionValidator
 * @returns {Promise} when resolved, promise must pass a single argument with the seconds until the next check
 */
