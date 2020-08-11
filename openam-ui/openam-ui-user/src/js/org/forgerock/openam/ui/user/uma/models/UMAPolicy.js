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
 * Copyright 2015-2019 ForgeRock AS.
 */

import _ from "lodash";

import Backbone from "backbone";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import UMAPolicyPermission from "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermission";

export default Backbone.RelationalModel.extend({
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
        fetchUrl(`/users/${Configuration.loggedUser.get("username")}/uma/policies`)}`
});
