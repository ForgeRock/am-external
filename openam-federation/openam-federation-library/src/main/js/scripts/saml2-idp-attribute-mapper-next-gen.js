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
 * Copyright 2026 Ping Identity Corporation.
 */

/*
 * This script returns a list of SAML Attribute objects for the IDP framework to insert into the generated Assertion.
 *
 * Next-gen bindings available in this script:
 *
 * session - ScriptedSession
 *      The user's session object.
 *
 * hostedEntityId - String
 *      The entity ID of the hosted Identity Provider.
 *
 * remoteEntityId - String
 *      The entity ID of the remote Service Provider.
 *
 * realm - String
 *      The name of the realm the user is authenticating to.
 *
 * idpAttributeMapperScriptHelper - IdpAttributeMapperScriptHelper
 *      Helper object providing utility methods for IDP attribute mapping operations.
 *
 * The last line must be a JSON list containing attributes with the structure:
 * 
 * [{
 *      "name:" "...",
 *      "nameFormat": "...",
 *      "values": ["..."]
 * },...]
 * 
 */

attributes = idpAttributeMapperScriptHelper.getStandardAttributes()

// modify the attrs object as needed

attributes  // this must be the last line of the script