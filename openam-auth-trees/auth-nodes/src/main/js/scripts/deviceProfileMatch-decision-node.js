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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/** ******************************************************************
 *
 * The following script is a simplified template for understanding
 * the basics of device matching. _This is not functionally complete._
 * For a functionally complete script as well as a development toolkit,
 * visit https://github.com/ForgeRock/forgerock-device-match-script.
 *
 * Global node variables accessible within this scope:
 * 1. `sharedState` provides access to incoming request
 * 2. `deviceProfilesDao` provides access to stored profiles
 * 3. `outcome` variable maps to auth tree node outcomes; values are
 *    'true', 'false', or 'unknownDevice' (notice _all_ are strings).
 * ******************************************************************/

/**
 * Get the incoming request's device profile.
 * Returns serialized JSON (type string); parsing this will result a
 * native JS object.
 */
var incomingJson = sharedState.get('forgeRock.device.profile').toString();
var incoming = JSON.parse(incomingJson);

/**
 * Get the incoming user's username and realm.
 * Notice the use of `.asString()`.
 */
var username = sharedState.get("username").asString();
var realm = sharedState.get("realm").asString();

/**
 * Get the user's stored profiles for appropriate realm.
 * Returns a _special_ object with methods for profile data
 */
var storedProfiles = deviceProfilesDao.getDeviceProfiles(username, realm);

// Default to `outcome` of 'unknownDevice'
outcome = 'unknownDevice';

if (storedProfiles) {
    var i = 0;
    // NOTE: `.size()` method returns the number of stored profiles
    var len = storedProfiles.size();

    for (i; i < len; i++) {
        /**
         * Get the stored profile.
         * Returns serialized JSON (type string); parsing this will result
         * a native JS object.
         */
        var storedJson = storedProfiles.get(i);
        var stored = JSON.parse(storedJson);

        /**
         * Find a stored profile with the same identifier.
         */
        if (incoming.identifier === stored.identifier) {

            /**
             * Now that you've found the appropriate profile, you will perform
             * the logic here to match the values of the `incoming` profile
             * with that of the `stored` profile.
             *
             * The result of the matching logic is assigned to `outcome`. Since
             * we have profiles of the same identifier, the value (type string)
             * should now be either 'true' or 'false' (properties matched or not).
             *
             * For more information about this topic, visit this Github repo:
             * https://github.com/ForgeRock/forgerock-device-match-script
             */
            outcome = 'false';
        }
    }
}
