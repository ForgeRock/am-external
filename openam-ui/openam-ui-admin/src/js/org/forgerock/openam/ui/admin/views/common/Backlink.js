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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";

import BackLinkTemplate from "templates/admin/views/common/BackLink";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

function getBaseURI (allFragments, fragmentIndex) {
    return _.take(allFragments, fragmentIndex + 1).join("/");
}

/**
 * @module org/forgerock/openam/ui/admin/views/common/Backlink
 */
export default Backbone.View.extend({
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
            "title": $.t(`console.navigation.${pageFragment}.title`, { defaultValue }),
            "hash": `#${getBaseURI(allFragments, fragmentIndex)}`
        };

        this.$el.html(BackLinkTemplate(data));
    }
});
