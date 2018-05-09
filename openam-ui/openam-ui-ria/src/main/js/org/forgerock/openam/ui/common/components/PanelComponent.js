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
 * Copyright 2016-2018 ForgeRock AS.
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
