/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * Maximum Idle Time vs Session Time Left Strategy.
  * <p/>
  * Supports both stateful and stateless sessions.
  * <p/>
  * This strategy utilises the fact that all calls to the server's <code>session</code> end-point will respond with
  * <code>401</code> if the current session has expired, while the response payload will help to decide when to
  * next perform a validation if the session is still valid.
  * <p/>
  * Subsequent calls to <code>getTimeLeft</code> are used to check if the idle timeout has been reached. The number of
  * seconds until the idle timeout is used to determine the next best time to check the session.
  *
  * @module org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy
  */
define([
    "org/forgerock/openam/ui/user/services/SessionService"
], (SessionService) => {
    return function () {
        return SessionService.getTimeLeft();
    };
});
