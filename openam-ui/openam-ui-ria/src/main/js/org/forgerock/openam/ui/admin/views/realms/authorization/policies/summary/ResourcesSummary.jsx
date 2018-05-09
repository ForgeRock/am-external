/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Badge } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

const ResourcesSummary = ({ resources }) => (
    <span className="am-badge-group">
        { resources.map((resource) => <Badge key={ resource }>{ resource }</Badge>) }
    </span>
);

ResourcesSummary.propTypes = {
    resources: PropTypes.arrayOf(PropTypes.string).isRequired
};

export default ResourcesSummary;
