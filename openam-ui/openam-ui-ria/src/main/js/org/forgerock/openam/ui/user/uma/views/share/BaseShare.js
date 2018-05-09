/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/uma/views/share/CommonShare",
    "templates/common/DefaultBaseTemplate"
], (AbstractView, CommonShare, DefaultBaseTemplate) => {
    var BaseShare = AbstractView.extend({
        template: "user/uma/views/share/BaseShare",
        baseTemplate: DefaultBaseTemplate,
        render (args, callback) {
            var self = this;
            self.shareView = new CommonShare();
            self.shareView.element = "#commonShare";
            self.shareView.noBaseTemplate = true;
            self.parentRender(() => {
                self.data.resourceSet = {};
                self.shareView.render(args, callback);
            }, callback);
        }

    });

    return new BaseShare();
});
