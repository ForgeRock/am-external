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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";

export default AbstractView.extend({
    element: "#footer",
    template: "common/FooterTemplate",
    noBaseTemplate: true,

    /**
     * Retrieves the version number of the product
     * @returns {Promise} Promise representing the return version
     */
    getVersion () {
        throw new Error("#getVersion not implemented");
    },
    render () {
        const self = this;

        this.data = {};

        if (this.showVersion()) {
            this.getVersion().then((version) => {
                self.data.version = version;
            }).always(
                self.parentRender.bind(self)
            ).always(() => {
                $("body").addClass("footer-deep");
            });
        } else {
            self.parentRender();
            $("body").removeClass("footer-deep");
        }
    },
    /**
     * Determines if to show the version
     * @returns {boolean} Whether to show the version
     */
    showVersion () {
        return false;
    }
});
