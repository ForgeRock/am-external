/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/common/Backlink
 */
define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "templates/admin/views/common/BackLink"
], ($, _, Backbone, URIUtils, BackLinkTemplate) => {
    function getBaseURI (allFragments, fragmentIndex) {
        return _.take(allFragments, fragmentIndex + 1).join("/");
    }

    return Backbone.View.extend({
        el:"#backlink",
        /**
         * Renders a back link navigation element.
         * @param {number} [fragmentIndex=1] Fragment index indicates which url fragment used for back link
         */
        render (fragmentIndex = 1) {
            const allFragments = URIUtils.getCurrentFragment().split("/");
            const pageFragment = allFragments[fragmentIndex];
            const defaultValue = _.startCase(`${pageFragment}`);
            const data = {
                "title": $.t(`console.common.navigation.${pageFragment}`, { defaultValue }),
                "hash": `#${getBaseURI(allFragments, fragmentIndex)}`
            };

            this.$el.html(BackLinkTemplate(data));
        }
    });
});
