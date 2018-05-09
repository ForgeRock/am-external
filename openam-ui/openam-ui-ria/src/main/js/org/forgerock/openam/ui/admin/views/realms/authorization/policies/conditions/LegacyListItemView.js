/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView"
], ($, _, AbstractView) => {
    return AbstractView.extend({
        data: {},
        mode: "append",
        render (itemData, callback, element, itemID) {
            this.setElement(element);
            this.data.itemID = itemID;
            this.data.itemData = itemData;

            var self = this;

            self.setElement(`#legacy_${itemID}`);
            self.delegateEvents();

            self.$el.data("itemData", itemData);
            if (callback) {
                callback();
            }
        }
    });
});
