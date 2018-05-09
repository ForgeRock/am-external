/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView"
], ($, AbstractView) => {
    var ShareCounter = AbstractView.extend({

        template: "user/uma/views/share/ShareCounter",
        element: "#shareCounter",

        render (count, callback) {
            this.data.shareCount = count;
            this.data.shareInfo = this.getShareInfo(count);
            this.data.shareIcon = this.getShareIcon(count);

            this.parentRender(() => {
                if (callback) { callback(); }
            });
        },

        getShareInfo (count) {
            var options = count ? { count } : { context: "none" };
            return $.t("uma.share.info", options);
        },

        getShareIcon (count) {
            var shareIcon = "fa fa-lock";
            if (count === 1) {
                shareIcon = "fa fa-user";
            } else if (count > 1) {
                shareIcon = "fa fa-users";
            }
            return shareIcon;
        }
    });

    return new ShareCounter();
});
