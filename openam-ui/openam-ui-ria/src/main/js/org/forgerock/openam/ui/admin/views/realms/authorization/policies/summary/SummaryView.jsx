/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Grid } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";

import ActionsSummary from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ActionsSummary";
import ConditionsSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ConditionsSummary";
import React from "react";
import ResourcesSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ResourcesSummary";
import AttributesSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/AttributesSummary";
import SummarySection from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/SummarySection";

const SummaryView = ({
    actionValues,
    condition,
    customAttributes,
    onClick,
    resources,
    staticAttributes,
    subject,
    userAttributes
}) => (
    <Grid bsClass="" className="am-masonry-grid-2">
        <SummarySection icon="file-o" onClick={ onClick } title={ t ("console.authorization.common.resources") }>
            <ResourcesSummary resources={ resources } />
        </SummarySection>

        <SummarySection icon="gavel" onClick={ onClick } title={ t ("console.authorization.common.actions") }>
            <ActionsSummary actionValues={ actionValues } onClick={ onClick } />
        </SummarySection>

        <SummarySection
            icon="mail-reply"
            onClick={ onClick }
            title={ t ("console.authorization.common.responseAttributes") }
        >
            <AttributesSummary
                customAttributes={ customAttributes }
                onClick={ onClick }
                staticAttributes={ staticAttributes }
                userAttributes={ userAttributes }
            />
        </SummarySection>

        <SummarySection icon="users" onClick={ onClick } title={ t ("console.authorization.common.subjects") }>
            <ConditionsSummary condition={ subject } conditionName={ t ("console.authorization.common.subjects") } />
        </SummarySection>

        <SummarySection
            icon="check-square-o"
            onClick={ onClick }
            title={ t ("console.authorization.common.environments") }
        >
            <ConditionsSummary
                condition={ condition }
                conditionName={ t ("console.authorization.common.environments") }
                onClick={ onClick }
            />
        </SummarySection>
    </Grid>
);

SummaryView.propTypes = {
    actionValues: PropTypes.object.isRequired, //eslint-disable-line react/forbid-prop-types
    condition: PropTypes.object.isRequired, //eslint-disable-line react/forbid-prop-types
    customAttributes: PropTypes.array.isRequired, //eslint-disable-line react/forbid-prop-types
    onClick: PropTypes.func.isRequired,
    resources: PropTypes.array.isRequired, //eslint-disable-line react/forbid-prop-types
    staticAttributes: PropTypes.array.isRequired, //eslint-disable-line react/forbid-prop-types
    subject: PropTypes.object.isRequired, //eslint-disable-line react/forbid-prop-types
    userAttributes: PropTypes.array.isRequired //eslint-disable-line react/forbid-prop-types
};

export default SummaryView;