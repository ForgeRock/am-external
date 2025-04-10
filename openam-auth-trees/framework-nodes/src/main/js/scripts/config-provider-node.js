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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

/**
 * The following script is a simplified template for understanding how to build
 * up a config Map object with custom values. The Config Provider Node will then
 * provide this config Map to the desired node type. It is important that the Map
 * you build here is named 'config'.
 *
 * Defined variables:
 *
 * nodeState - Node State (1)
 *           Always present, this represents the current values stored in the node state.
 *
 * idRepository - Profile Data (2)
 *           Always present, a repository to retrieve user information.
 *
 * secrets - Credentials and Secrets (3)
 *           Always present, an interface to access the Secrets API from a scripting context.
 *
 * requestHeaders (4) - Map (5)
 *           Always present, an object that provides methods for accessing headers in the login request.
 *
 * logger - Debug Logging (6)
 *          Always present, the debug logger instance.
 *
 * httpClient - HTTP Client (7)
 *          Always present, the HTTP client that can be used to make external HTTP requests.
 *
 * realm - String (primitive).
 *          Always present, the name of the realm the user is authenticating to.
 *
 * existingSession - Map<String, String> (5)
 *          Present if the request contains the session cookie, the user's session object. The returned map from
 *          SSOToken.getProperties() (8)
 *
 * requestParameters - Map (5)
 *          Always present, the object that contains the authentication request parameters.
 *
 *
 * Outputs:
 *
 * config - Map (5)
 *           Define and fill a Map object named 'config' with custom values, this will define the configuration for the
 *           associated node selected in the ConfigProviderNode.
 *
 * Reference:
 * (1) Node State - https://backstage.forgerock.com/docs/idcloud-am/latest/authentication-guide/scripting-api-node.html#scripting-api-node-nodeState
 * (2) Profile Data - https://backstage.forgerock.com/docs/am/7.1/authentication-guide/scripting-api-node.html#scripting-api-node-id-repo
 * (3) Credentials and Secrets - https://backstage.forgerock.com/docs/am/7.1/authentication-guide/scripting-api-node.html#scripting-api-authn-secrets
 * (4) Request Headers - https://backstage.forgerock.com/docs/am/7/authentication-guide/scripting-api-node.html#scripting-api-node-requestHeaders.
 * (5) Map - https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html
 * (6) Debug Logging - https://backstage.forgerock.com/docs/am/7/scripting-guide/scripting-api-global-logger.html#scripting-api-global-logger.
 * (7) HTTP Client - https://backstage.forgerock.com/docs/am/7/apidocs/org/forgerock/http/Client.html.
 * (8) SSOToken - https://backstage.forgerock.com/docs/am/7/apidocs/com/iplanet/sso/SSOToken.html.
 */

config = {
    "key0": {"subKey": "value0"},
    "key1": "value1"
};
