/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/views/api/calculateHeight",
    "templates/admin/views/common/IframeViewTemplate"
], (_, AbstractView, Constants, calculateHeight, IframeViewTemplate) => {
    const ApiDocView = AbstractView.extend({
        template: IframeViewTemplate,
        render () {
            this.data.src = `${Constants.context}/json/docs/api`;
            this.data.className = "am-iframe-full-width";
            this.parentRender(() => {
                this.resize();
                window.addEventListener("resize", _.debounce(this.resize.bind(this), 100));
            });
        },
        resize () {
            const height = calculateHeight.default();
            this.$el.find("[data-iframe]").height(height);
        },
        remove () {
            window.removeEventListener("resize", this.resize);
        }
    });

    return new ApiDocView();
});
