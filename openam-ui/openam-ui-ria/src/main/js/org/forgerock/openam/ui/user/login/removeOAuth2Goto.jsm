/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
