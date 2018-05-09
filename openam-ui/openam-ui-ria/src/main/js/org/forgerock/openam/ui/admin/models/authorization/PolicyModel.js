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
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/utils/ModelUtils"
], (_, Backbone, URLHelper, ModelUtils) => {
    return Backbone.Model.extend({
        idAttribute: "name",
        urlRoot: URLHelper.substitute("__api__/policies"),

        defaults () {
            return {
                name: null,
                description: "",
                resources: [],
                actionValues: {}
            };
        },

        validate (attrs) {
            if (attrs.name.trim() === "") {
                return "errorNoName";
            }

            // entities that are stored in LDAP can't start with '#'. http://www.jguru.com/faq/view.jsp?EID=113588
            if (attrs.name.indexOf("#") === 0) {
                return "errorCantStartWithHash";
            }

            if (attrs.resources.length === 0) {
                return "policyErrorNoResources";
            }
        },

        sync (method, model, options) {
            options = options || {};
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=2.0");
            };
            options.error = ModelUtils.errorHandler;

            if (model.id === null) {
                method = "create";
                options.url = `${this.urlRoot()}/?_action=create`;
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});