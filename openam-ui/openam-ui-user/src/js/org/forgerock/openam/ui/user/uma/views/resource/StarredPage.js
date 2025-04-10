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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";

import BasePage from "org/forgerock/openam/ui/user/uma/views/resource/BasePage";
import UMAService from "org/forgerock/openam/ui/user/uma/services/UMAService";
const StarredPage = BasePage.extend({
    template: "user/uma/views/resource/StarredPageTemplate",
    render (args, callback) {
        const self = this;

        UMAService.labels.all().then((data) => {
            const starred = _.find(data.result, (label) => {
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

export default StarredPage;
