/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone",
    "org/forgerock/commons/ui/common/util/Base64",
    "org/forgerock/openam/ui/common/util/URLHelper",
    "org/forgerock/openam/ui/admin/utils/ModelUtils"
], (Backbone, Base64, URLHelper, ModelUtils) => {
    return Backbone.Model.extend({
        idAttribute: "_id",
        urlRoot: URLHelper.substitute("__api__/scripts"),
        defaults () {
            return {
                _id: null,
                name: "",
                script: "",
                language: "",
                context: ""
            };
        },

        validate (attrs) {
            if (attrs.name.trim() === "") {
                return "scriptErrorNoName";
            }

            if (attrs.language === "") {
                return "scriptErrorNoLanguage";
            }
        },

        parse (resp) {
            if (resp && resp.script) {
                resp.script = Base64.decodeUTF8(resp.script);
            }
            return resp;
        },

        sync (method, model, options) {
            options = options || {};
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };
            options.error = ModelUtils.errorHandler;

            method = method.toLowerCase();
            if (method === "create" || model.id === null) {
                options.url = `${this.urlRoot()}/?_action=create`;
            }

            if (method === "create" || method === "update") {
                model.set("script", Base64.encodeUTF8(model.get("script")));
            }

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});
