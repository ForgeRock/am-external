/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "backbone"
], (_, Backbone) =>
    Backbone.View.extend({
        initialize ({ template, data = {}, callback = _.noop }) {
            if (!template) {
                throw new Error("[TemplateBasedView] No \"template\" found.");
            }
            this.template = template;
            this.callback = callback;
            this.data = data;
        },
        render () {
            const html = this.template(this.data);

            this.$el.html(html);
            this.callback();

            return this;
        }
    })
);
