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

import { Grid } from "react-bootstrap";
import { t } from "i18next";
import ActionsSummary from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ActionsSummary";
import ConditionsSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ConditionsSummary";
import React, { PropTypes } from "react";
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
