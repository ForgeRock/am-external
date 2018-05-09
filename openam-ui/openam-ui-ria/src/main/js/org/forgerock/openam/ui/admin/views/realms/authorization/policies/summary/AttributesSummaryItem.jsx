/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import { Badge } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

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
