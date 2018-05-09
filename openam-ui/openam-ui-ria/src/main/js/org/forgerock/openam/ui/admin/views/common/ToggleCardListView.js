/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "backbone",
    "handlebars-template-loader/runtime",
    "templates/admin/views/common/ToggleCardListTemplate"
], ($, Backbone, Handlebars, ToggleCardListTemplate) =>
    Backbone.View.extend({
        initialize (options) {
            this.options = options;
            this.options.activeView = this.options.activeView || 0;
        },

        getElementA () {
            return "#viewAContainer";
        },

        getElementB () {
            return "#viewBContainer";
        },

        getActiveView () {
            const index = this.$el.find(".tab-pane.active").index();
            return index > 0 ? index : 0;
        },

        render (callback) {
            const html = ToggleCardListTemplate(this.options.button);

            this.$el.html(html);
            this.$el.find(".tab-pane").eq(this.options.activeView).addClass("active");
            this.$el.find(".tab-toggles").eq(this.options.activeView).addClass("active");
            callback(this);
        }
    }, {
        DEFAULT_VIEW: 0
    })
);
