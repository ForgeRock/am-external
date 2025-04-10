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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import getAllPaths from "./getAllPaths";

import data from "../__test__/SampleData";

describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
    () => {
        const { one, two, three, four, five, six, seven, eight, nine, success } = data.tree.nodes;
        const paths = [
            [{ one }, { two }, { five }, { four }, { six }],
            [{ one }, { two }, { four }, { five }],
            [{ one }, { two }, { four }, { six }],
            [{ one }, { two }, { five }, { four }],
            [{ one }, { two }, { six }],
            [{ one }, { two }, { seven }],
            [{ one }, { two }, { eight }],
            [{ one }, { two }, { nine }],
            [{ one }, { three }, { success }]
        ];

        it("returns all paths for the tree", () => {
            expect(
                getAllPaths(data.tree.entryNodeId, data.tree.nodes)
            ).to.deep.equal(paths);
        });
    });
