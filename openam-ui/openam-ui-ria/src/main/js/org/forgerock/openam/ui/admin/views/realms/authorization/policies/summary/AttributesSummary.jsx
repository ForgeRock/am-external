/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import AttributesSummaryItem
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/AttributesSummaryItem";
import EmptySectionCallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/EmptySectionCallToAction";

const AttributesSummary = ({ customAttributes, onClick, staticAttributes, userAttributes }) => {
    if (_.isEmpty([...userAttributes, ...staticAttributes, ...customAttributes])) {
        return (
            <EmptySectionCallToAction
                onClick={ onClick }
                sectionName={ t ("console.authorization.common.responseAttributes") }
            />
        );
    }

    return (
        <div>
            <AttributesSummaryItem
                attributes={ userAttributes }
                title={ t("console.authorization.common.subjectAttributes") }
            />
            <AttributesSummaryItem
                attributes={ staticAttributes }
                title={ t("console.authorization.common.staticAttributes") }
            />
            <AttributesSummaryItem
                attributes={ customAttributes }
                title={ t("console.authorization.common.customAttributes") }
            />
        </div>
    );
};

AttributesSummary.propTypes = {
    customAttributes: PropTypes.array.isRequired, //eslint-disable-line react/forbid-prop-types
    onClick: PropTypes.func.isRequired,
    staticAttributes: PropTypes.array.isRequired, //eslint-disable-line react/forbid-prop-types
    userAttributes: PropTypes.array.isRequired //eslint-disable-line react/forbid-prop-types
};

export default AttributesSummary;
