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
 * Copyright 2015-2020 ForgeRock AS.
 */

import _ from "lodash";

import AbstractModel from "org/forgerock/commons/ui/common/main/AbstractModel";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import flattenValues from "org/forgerock/openam/ui/common/util/object/flattenValues";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";

const baseUrl = `${Constants.host}${Constants.context}/json`;
const UserModel = AbstractModel.extend({
    idAttribute: "id",
    defaults: {},

    sync (method, model, options) {
        if (method === "read") {
            // The only other supported operation is read
            return ServiceInvoker.restCall(_.extend(
                {
                    "url" : baseUrl + fetchUrl(`/users/${encodeURIComponent(this.id)}`),
                    "headers": { "Accept-API-Version": "protocol=1.0,resource=2.0" },
                    "type": "GET"
                },
                options
            )).then((response) => {
                model.clear();
                if (options.parse) {
                    model.set(model.parse(response, options));
                } else {
                    model.set(response);
                }
                return model.toJSON();
            });
        }
    },
    parse (response) {
        delete response.userPassword;

        /**
         * flattenValues due to the response having many values wrapped in arrays (makes for a simpler data
         * structure)
         */
        let user = flattenValues(response);

        /**
         * Re-apply defaults to attributes that were not present in the response. Duplicate of what Backbone
         * does when a model is first initialised. Fixes scenarios where a previous server response was an value
         * but the next was missing the attribute and value entirely, failing to clear the previous now invalid
         * value from the model
         */
        user = _.defaults({}, user, _.result(this, "defaults"));

        // When we parse response the first time, amadmin don't have uid
        //First try to resolve the user id from the user id attributes of the configured identity stores in the realm
        if (Configuration.globalData.userIdAttributes) {
            for (let i = 0; i < Configuration.globalData.userIdAttributes.length; i++) {
                var item = Configuration.globalData.userIdAttributes[i];
                if (user.hasOwnProperty(item)) {
                    user.id = user[item];
                    break;
                }
            }
        }
        //On failing to resolve user id from the user id attributes of the configured identity stores in the realm
        //fall back to the uid attribute or username attribute to resolve the user id
        if (!user.id) {
            user.id = user.uid || user.username;
        }
        if (!_.has(user, "roles")) {
            this.uiroles = [];
        } else if (_.isString(user.roles)) {
            this.uiroles = user.roles.split(",");
        } else {
            this.uiroles = user.roles;
        }

        if (user.id.toLowerCase() === "amadmin" && !_.includes(this.uiroles, "ui-amadmin")) {
            this.uiroles.push("ui-amadmin");
        }

        return user;
    },
    fetchById (id) {
        return this.set({ id }, { silent: true }).fetch().then(() => { return this; });
    }
});

export default new UserModel();
