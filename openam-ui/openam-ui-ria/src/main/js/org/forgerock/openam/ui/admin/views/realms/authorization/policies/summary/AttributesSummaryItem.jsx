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
 * Copyright 2016-2017 ForgeRock AS.
 */

import _ from "lodash";
import { Badge } from "react-bootstrap";
import React, { PropTypes } from "react";

const AttributesSummaryItem = ({ attributes, title }) => {
    const getPropertyValue = (item) => {
        return _.isEmpty(item.propertyValues) ? "" : `: ${item.propertyValues.join(", ")}`;
    };

    return _.isEmpty(attributes) ? null : (
        <div className="am-summary-sub-section row">
            <h5>{ title }</h5>
            <span className="am-badge-group">
                { attributes.map((item) =>
                    <Badge key={ item.propertyName }>{ item.propertyName }{ getPropertyValue(item) }</Badge>
                ) }
            </span>
        </div>);
};

AttributesSummaryItem.propTypes = {
    attributes: PropTypes.arrayOf({
        propertyName: PropTypes.string,
        propertyValues: PropTypes.arrayOf(PropTypes.string)
    }).isRequired,
    title: PropTypes.string.isRequired
};

export default AttributesSummaryItem;
