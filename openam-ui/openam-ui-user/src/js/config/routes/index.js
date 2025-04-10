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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import BaseRoutes from "./BaseRoutes";
import MiscRoutes from "./anonymous/MiscRoutes";
import SelfServiceRoutes from "./anonymous/SelfServiceRoutes";
import AuthenticationRoutes from "./user/AuthenticationRoutes";
import UserRoutes from "./user/UserRoutes";
import UMARoutes from "./user/UMARoutes";

/**
 * Formatting routes:
 *
 * Separate routes should be separated with forward slashes "/". If a parent is not clickable then it's parts should be
 * hyphenated. Camel case should be used in cases where a link contains more than one word.
 * Take the following navigational tree, where all links are clickable except authentication and modules:
 *
 * authentication
 *      - chains
 *      - modules
 *            - foo
 *            - fooBar
 *      - policySets
 *      - resourceTypes
 *            - foo
 *            - fooBar
 *                  - fooBar
 *
 * The base routes will be:
 *    authentication-chains
 *    authentication-modules-foo
 *    authentication-modules-fooBar
 *    authentication-policySets
 *    authentication-resourceTypes
 *    authentication-resourceTypes/foo
 *    authentication-resourceTypes/fooBar
 *    authentication-resourceTypes/fooBar/fooBar
 */
export default {
    ...BaseRoutes,
    ...MiscRoutes,
    ...SelfServiceRoutes,
    ...AuthenticationRoutes,
    ...UserRoutes,
    ...UMARoutes
};
