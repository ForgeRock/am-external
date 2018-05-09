/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backbone",
    "backbone-relational",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/user/uma/models/UMAPolicy",
    "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermissionScope"
], ($, _, Backbone, BackboneRelational, Configuration, Constants, fetchUrl, UMAPolicy, UMAPolicyPermissionScope) => {
    return Backbone.RelationalModel.extend({
        // Promise version of fetch
        fetch () {
            var d = $.Deferred();
            Backbone.RelationalModel.prototype.fetch.call(this, {
                success (model) {
                    d.resolve(model);
                },
                error (model, response) {
                    d.reject(response);
                }
            });
            return d.promise();
        },
        idAttribute: "_id",
        parse (response) {
            // Hardwiring the id across to the UMAPolicy object as the server doesn't provide it
            if (!response.policy) {
                response.policy = {};
                response.policy.permissions = [];
            }
            response.policy.policyId = response._id;

            response.scopes = _.map(response.scopes, (scope) => {
                return { id: scope };
            });

            return response;
        },
        relations: [{
            type: Backbone.HasOne,
            key: "policy",
            relatedModel: UMAPolicy,
            parse: true
        }, {
            type: Backbone.HasMany,
            key: "scopes",
            relatedModel: UMAPolicyPermissionScope,
            includeInJSON: Backbone.Model.prototype.idAttribute,
            parse: true
        }],
        toggleStarred (starredLabelId) {
            var isStarred = _.contains(this.get("labels"), starredLabelId);

            if (isStarred) {
                this.set("labels", _.reject(this.get("labels"), (label) => {
                    return label === starredLabelId;
                }));
            } else {
                this.get("labels").push(starredLabelId);
            }
        },
        urlRoot: `${Constants.host}${Constants.context}/json${
            fetchUrl.default(`/users/${Configuration.loggedUser.get("username")}/oauth2/resources/sets`)}`
    });
});