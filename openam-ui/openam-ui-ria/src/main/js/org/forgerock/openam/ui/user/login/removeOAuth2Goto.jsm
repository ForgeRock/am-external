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
 * Copyright 2017 ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/user/login/removeOAuth2Goto
  * @param {Object} params Takes an object of key value pairs
  * During authentication between a client and an IDP a goto is issued with a path to the consent page and params which
  * are only relevant to that specific authentication request. Futher authentication requests should be issued new
  * parameters, and so we need to remove the any old goto's containing the consent page path as the parameters will no
  * longer be valid.
  * @returns {Object} Returns the key value pairs minus the goto if that contained the oauth2/authorize in its path.
  */
const removeOAuth2Goto = (params) => {
    const consentPagePath = "oauth2/authorize?";
    if (params.goto && decodeURIComponent(params.goto).indexOf(consentPagePath) > 0) {
        delete params.goto;
    }
    return params;
};

export default removeOAuth2Goto;
