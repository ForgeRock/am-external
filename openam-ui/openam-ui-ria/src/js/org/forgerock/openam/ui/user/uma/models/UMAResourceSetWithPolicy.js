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
 * Copyright 2015-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import UMAPolicy from "org/forgerock/openam/ui/user/uma/models/UMAPolicy";
import UMAPolicyPermissionScope from "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermissionScope";

export default Backbone.RelationalModel.extend({
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
        var isStarred = _.includes(this.get("labels"), starredLabelId);

        if (isStarred) {
            this.set("labels", _.reject(this.get("labels"), (label) => {
                return label === starredLabelId;
            }));
        } else {
            this.get("labels").push(starredLabelId);
        }
    },
    urlRoot: `${Constants.host}${Constants.context}/json${
        fetchUrl(`/users/${Configuration.loggedUser.get("username")}/oauth2/resources/sets`)}`
});
