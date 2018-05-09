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
    "backbone-relational"
], ($, _, Backbone) => {
    return Backbone.RelationalModel.extend({
        parse (response) {
            if (_.isUrl(response.id)) {
                response = this.resolve(response.id);
            } else {
                response.name = response.id;
            }

            return response;
        },
        resolve (url) {
            var resolved = {
                id: url,
                name: url
            };

            // Synchronous!
            $.ajax({
                async: false,
                dataType: "json",
                success (data) {
                    resolved.name = data.name;
                    resolved["icon_uri"] = data.icon_uri;
                },
                url
            });

            return resolved;
        },
        sync (method, model, options) {
            options.beforeSend = function (xhr) {
                xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
            };

            return Backbone.Model.prototype.sync.call(this, method, model, options);
        }
    });
});
