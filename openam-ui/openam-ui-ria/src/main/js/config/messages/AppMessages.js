/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
], () => {
    return {
        /**
         * Common Messages.
         */
        "duplicateRealm": {
            msg: "config.messages.AppMessages.duplicateRealm",
            type: "error"
        },
        "deleteFail": {
            msg: "config.messages.AppMessages.deleteFail",
            type: "error"
        },
        "changesSaved": {
            msg: "config.messages.AppMessages.changesSaved",
            type: "info"
        },
        "invalidItem": {
            msg: "config.messages.AppMessages.invalidItem",
            type: "error"
        },
        "duplicateItem": {
            msg: "config.messages.AppMessages.duplicateItem",
            type: "error"
        },
        "errorNoName": {
            msg: "config.messages.AdminMessages.policies.error.noName",
            type: "error"
        },
        "errorNoId": {
            msg: "config.messages.AdminMessages.policies.error.noId",
            type: "error"
        },
        "errorCantStartWithHash": {
            msg: "config.messages.AdminMessages.policies.error.cantStartWithHash",
            type: "error"
        },

        /**
         * UMA Messages.
         */
        "policyCreatedSuccess": {
            msg: "uma.share.messages.success",
            type: "info"
        },
        "policyCreatedFail": {
            msg: "uma.share.messages.fail",
            type: "error"
        },
        "unshareAllResourcesSuccess": {
            msg: "uma.resources.myresources.unshareAllResources.messages.success",
            type: "info"
        },
        "unshareAllResourcesFail": {
            msg: "uma.resources.myresources.unshareAllResources.messages.fail",
            type: "error"
        },
        "revokeAllPoliciesSuccess": {
            msg: "uma.resources.show.revokeAllPoliciesSuccess",
            type: "info"
        },
        "revokeAllPoliciesFail": {
            msg: "uma.resources.show.revokeAllPoliciesFail",
            type: "error"
        },
        "revokePolicySuccess": {
            msg: "uma.resources.show.revokePolicySuccess",
            type: "info"
        },
        "revokePolicyFail": {
            msg: "uma.resources.show.revokePolicyFail",
            type: "error"
        },
        "deleteLabelSuccess": {
            msg: "uma.resources.myLabels.deleteLabel.messages.success",
            type: "info"
        },
        "deleteLabelFail": {
            msg: "uma.resources.myLabels.deleteLabel.messages.fail",
            type: "error"
        },

        /**
         * Scripts messages.
         */
        "scriptErrorNoName": {
            msg: "config.messages.AdminMessages.scripts.error.noName",
            type: "error"
        },
        "scriptErrorNoLanguage": {
            msg: "config.messages.AdminMessages.scripts.error.noLanguage",
            type: "error"
        },

        /**
         * Policies messages.
         */
        "applicationErrorNoResourceTypes": {
            msg: "config.messages.AdminMessages.policies.error.noResourceTypes",
            type: "error"
        },
        "policyErrorNoResources": {
            msg: "config.messages.AdminMessages.policies.error.noResources",
            type: "error"
        },
        "resTypeErrorNoPatterns": {
            msg: "config.messages.AdminMessages.policies.error.noPatterns",
            type: "error"
        },
        "resTypeErrorNoActions": {
            msg: "config.messages.AdminMessages.policies.error.noActions",
            type: "error"
        },

        "invalidationSuccessful": {
            msg: "console.sessions.invalidationSuccessful",
            type: "info"
        }
    };
});
