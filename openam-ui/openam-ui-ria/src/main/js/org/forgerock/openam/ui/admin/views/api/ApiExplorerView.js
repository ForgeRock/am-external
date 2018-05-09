/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "templates/admin/views/common/IframeViewTemplate"
], (AbstractView, Constants, IframeViewTemplate) => {
    const ApiExplorerView = AbstractView.extend({
        template: IframeViewTemplate,
        render () {
            this.data.src = `${Constants.context}/api`;
            this.parentRender();
        }
    });

    return new ApiExplorerView();
});
