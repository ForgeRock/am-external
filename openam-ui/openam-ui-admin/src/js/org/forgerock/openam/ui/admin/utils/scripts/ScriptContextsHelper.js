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
 * Copyright 2024-2026 Ping Identity Corporation.
 */

/**
 * Dual-version script types and their associated contexts.
 */
const DUAL_VERSION_SCRIPT_TYPES = {
    "AUTHENTICATION_TREE_DECISION_NODE": {
        "legacyContext": "AUTHENTICATION_TREE_DECISION_NODE",
        "nextGenContext": "SCRIPTED_DECISION_NODE"
    },
    "DEVICE_MATCH_NODE": {
        "legacyContext": "AUTHENTICATION_TREE_DECISION_NODE",
        "nextGenContext": "DEVICE_MATCH_NODE"
    },
    "POLICY_CONDITION": {
        "legacyContext": "POLICY_CONDITION",
        "nextGenContext": "POLICY_CONDITION_NEXT_GEN"
    },
    "CONFIG_PROVIDER_NODE": {
        "legacyContext": "CONFIG_PROVIDER_NODE",
        "nextGenContext": "CONFIG_PROVIDER_NODE_NEXT_GEN"
    },
    "OAUTH2_ACCESS_TOKEN_MODIFICATION": {
        "legacyContext": "OAUTH2_ACCESS_TOKEN_MODIFICATION",
        "nextGenContext": "OAUTH2_ACCESS_TOKEN_MODIFICATION_NEXT_GEN"
    },
    "OIDC_CLAIMS": {
        "legacyContext": "OIDC_CLAIMS",
        "nextGenContext": "OIDC_CLAIMS_NEXT_GEN"
    },
    "OAUTH2_SCRIPTED_JWT_ISSUER": {
        "legacyContext": "OAUTH2_SCRIPTED_JWT_ISSUER",
        "nextGenContext": "OAUTH2_SCRIPTED_JWT_ISSUER_NEXT_GEN"
    },
    "OAUTH2_VALIDATE_SCOPE": {
        "legacyContext": "OAUTH2_VALIDATE_SCOPE",
        "nextGenContext": "OAUTH2_VALIDATE_SCOPE_NEXT_GEN"
    },
    "OAUTH2_AUTHORIZE_ENDPOINT_DATA_PROVIDER": {
        "legacyContext": "OAUTH2_AUTHORIZE_ENDPOINT_DATA_PROVIDER",
        "nextGenContext": "OAUTH2_AUTHORIZE_ENDPOINT_DATA_PROVIDER_NEXT_GEN"
    },
    "OAUTH2_EVALUATE_SCOPE": {
        "legacyContext": "OAUTH2_EVALUATE_SCOPE",
        "nextGenContext": "OAUTH2_EVALUATE_SCOPE_NEXT_GEN"
    },
    "OAUTH2_MAY_ACT": {
        "legacyContext": "OAUTH2_MAY_ACT",
        "nextGenContext": "OAUTH2_MAY_ACT_NEXT_GEN"
    },
    "SAML2_SP_ADAPTER": {
        "legacyContext": "SAML2_SP_ADAPTER",
        "nextGenContext": "SAML2_SP_ADAPTER_NEXTGEN"
    },
    "SAML2_IDP_ADAPTER": {
        "legacyContext": "SAML2_IDP_ADAPTER",
        "nextGenContext": "SAML2_IDP_ADAPTER_NEXTGEN"
    },
    "SAML2_IDP_ATTRIBUTE_MAPPER": {
        "legacyContext": "SAML2_IDP_ATTRIBUTE_MAPPER",
        "nextGenContext": "SAML2_IDP_ATTRIBUTE_MAPPER_NEXT_GEN"
    },
    "SOCIAL_IDP_PROFILE_TRANSFORMATION": {
        "legacyContext": "SOCIAL_IDP_PROFILE_TRANSFORMATION",
        "nextGenContext": "SOCIAL_IDP_PROFILE_TRANSFORMATION_NEXT_GEN"
    },
    "SOCIAL_PROVIDER_HANDLER_NODE": {
        "legacyContext": "SOCIAL_IDP_PROFILE_TRANSFORMATION",
        "nextGenContext": "SOCIAL_PROVIDER_HANDLER_NODE"
    },
    "OIDC_NODE": {
        "legacyContext": "SOCIAL_IDP_PROFILE_TRANSFORMATION",
        "nextGenContext": "OIDC_NODE"
    }
};

/**
 * Check if a script type is a hidden subtype. Hidden subtypes are script types that are not directly
 * exposed to the user to avoid duplication in the UI.
 */
function isHiddenSubType (scriptType, context) {
    return (scriptType.legacyContext === context || scriptType.nextGenContext === context) &&
        !DUAL_VERSION_SCRIPT_TYPES[context];
}

/**
 * Collapse a list of script contexts into a list of script types.
 * @param {Array} contexts - List of script contexts.
 * @returns {Array} - List of script types.
 */
export const collapseContexts = (contexts) => {
    return contexts
        .filter((context) => {
            return Object.values(DUAL_VERSION_SCRIPT_TYPES).every((scriptType) => {
                return !isHiddenSubType(scriptType, context._id);
            });
        });
};

/**
 * Lookup a script type based on a script context.
 * @param {string} context - The script context.
 * @returns {object|null} - The associated script type object or null if not found.
 */
export const lookupContext = (context) => {
    for (const value of Object.values(DUAL_VERSION_SCRIPT_TYPES)) {
        if (value.legacyContext === context || value.nextGenContext === context) {
            return value;
        }
    }
};
