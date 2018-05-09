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
        idAttribute: "uuid",
        urlRoot: URLHelper.substitute("__api__/resourcetypes"),

        defaults () {
            return {
                uuid: null,
                description: "",
                actions: {},
                patterns: []
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

            if (_.isEmpty(attrs.patterns)) {
                return "resTypeErrorNoPatterns";
            }

            if (_.isEmpty(attrs.actions)) {
                return "resTypeErrorNoActions";
            }
        },

        sync (method, model, options) {
            options = options || {};
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };
            options.error = ModelUtils.errorHandler;

            if (method.toLowerCase() === "create" || model.id === null) {
                options.url = `${this.urlRoot()}/?_action=create`;
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});
