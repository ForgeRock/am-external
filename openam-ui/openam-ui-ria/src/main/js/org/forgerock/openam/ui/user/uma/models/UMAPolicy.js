/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "backbone",
    "backbone-relational",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermission"
], (_, Backbone, BackboneRelational, Configuration, Constants, fetchUrl, UMAPolicyPermission) => {
    return Backbone.RelationalModel.extend({
        idAttribute: "policyId",
        toBeCreated: true,
        relations: [{
            type: Backbone.HasMany,
            key: "permissions",
            relatedModel: UMAPolicyPermission
        }],
        parse (response) {
            if (!_.isEmpty(response.permissions)) {
                this.toBeCreated = false;
            }

            return response;
        },
        sync (method, model, options) {
            options = options || {};
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };

            if (method.toLowerCase() === "update" && model.toBeCreated === true) {
                model.toBeCreated = false;
                options.headers = {};
                options.headers["If-None-Match"] = "*";
            }

            if (!model.get("permissions").length) {
                model.toBeCreated = true;
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        },
        urlRoot: `${Constants.host}${Constants.context}/json${
            fetchUrl.default(`/users/${Configuration.loggedUser.get("username")}/uma/policies`)}`
    });
});
