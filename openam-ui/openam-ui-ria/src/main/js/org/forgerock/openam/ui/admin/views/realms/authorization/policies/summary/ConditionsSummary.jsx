/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import PropTypes from "prop-types";
import React from "react";

import EmptySectionCallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/EmptySectionCallToAction";

const ConditionsSummary = ({ condition, conditionName, onClick }) => {
    return _.isEmpty(condition)
        ? <EmptySectionCallToAction onClick={ onClick } sectionName={ conditionName } />
        : <pre className="am-text-preformatted">{ JSON.stringify(condition, null, 2) }</pre>;
};

ConditionsSummary.propTypes = {
    condition: PropTypes.objectOf(PropTypes.any),
    conditionName: PropTypes.string.isRequired,
    onClick: PropTypes.func.isRequired
};

export default ConditionsSummary;
