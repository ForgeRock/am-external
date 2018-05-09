/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/openam/ui/user/uma/views/resource/BasePage",
    "org/forgerock/openam/ui/user/uma/services/UMAService"
], (_, BasePage, UMAService) => {
    var StarredPage = BasePage.extend({
        template: "user/uma/views/resource/StarredPageTemplate",
        render (args, callback) {
            var self = this;

            UMAService.labels.all().done((data) => {
                var starred = _.find(data.result, (label) => {
                    return label.name.toLowerCase() === "starred";
                });

                if (starred) {
                    self.renderGrid(self.createLabelCollection(starred._id), self.createColumns("starred"), callback);
                } else {
                    console.error("Unable to find \"starred\" label. " +
                                  "Label should have been created by UI on first load.");
                }
            });
        }
    });

    return StarredPage;
});
