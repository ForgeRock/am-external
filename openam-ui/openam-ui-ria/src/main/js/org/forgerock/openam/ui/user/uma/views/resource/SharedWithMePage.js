/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/user/uma/views/resource/BasePage"
], (BasePage) => {
    var SharedWithMePage = BasePage.extend({
        template: "user/uma/views/resource/SharedWithMePageTemplate",
        render (args, callback) {
            this.renderGrid(this.createSetCollection(true), this.createColumns("sharedwithme"), callback);
        }
    });

    return SharedWithMePage;
});
