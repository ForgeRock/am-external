/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone",
    "handlebars-template-loader/runtime",
    "lodash"
], (Backbone, Handlebars, _) => Backbone.View.extend({
    initialize (options) {
        if (!_.isString(options.partial)) {
            throw new TypeError("[PartialBasedView] \"partial\" argument is not a String.");
        }

        this.options = options;
    },
    render () {
        const html = Handlebars.partials[this.options.partial](this.options.data);
        this.$el.html(html);
        return this;
    }
}));
