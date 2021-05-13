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
 * Copyright 2018-2019 ForgeRock AS.
 */

import PropTypes from "prop-types";
import React from "react";

import EmphasizedText from "components/EmphasizedText";

const EmphasizedTextCell = ({ cell, match }) => (
    <EmphasizedText match={ match }>{ cell }</EmphasizedText>
);

EmphasizedTextCell.propTypes = {
    cell: PropTypes.oneOfType([
        PropTypes.array,
        PropTypes.object,
        PropTypes.string
    ]),
    match: PropTypes.string
};

export default EmphasizedTextCell;
