/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "templates/common/components/PanelComponentTemplate"
], (Backbone, UIUtils, PanelComponentTemplate) => {
    const PanelComponent = Backbone.View.extend({
        template: PanelComponentTemplate,
        initialize ({ createBody, createFooter }) {
            this.createBody = createBody;
            this.createFooter = createFooter;
        },

        getBody () {
            return this.panelBody;
        },

        render () {
            const html = this.template();

            this.$el.html(html);
            this.panelBody = this.createBody();
            this.$el.find("[data-panel-body]").append(this.panelBody.render().$el);
            this.$el.find("[data-panel-footer]").append(this.createFooter().render().$el);

            return this;
        }
    });

    return PanelComponent;
});
