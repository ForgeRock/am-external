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
 * Copyright 2016-2019 ForgeRock AS.
 */
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import ActionsSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ActionsSummary";
import AttributesSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/AttributesSummary";
import ConditionsSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ConditionsSummary";
import ResourcesSummary
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/ResourcesSummary";
import SummarySection
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/SummarySection";

const SummaryView = ({ actionValues, condition, customAttributes, resources, staticAttributes,
    subject, userAttributes, ...restProps
}) => (
    <div className="am-masonry-grid-2">
        <SummarySection
            icon="file-o"
            title={ t("console.authorization.common.resources") }
            { ...restProps }
        >
            <ResourcesSummary resources={ resources } />
        </SummarySection>

        <SummarySection
            icon="gavel"
            title={ t("console.authorization.common.actions") }
            { ...restProps }
        >
            <ActionsSummary actionValues={ actionValues } { ...restProps } />
        </SummarySection>

        <SummarySection
            icon="mail-reply"
            title={ t("console.authorization.common.responseAttributes") }
            { ...restProps }
        >
            <AttributesSummary
                customAttributes={ customAttributes }
                staticAttributes={ staticAttributes }
                userAttributes={ userAttributes }
                { ...restProps }
            />
        </SummarySection>

        <SummarySection
            icon="users"
            title={ t("console.authorization.common.subjects") }
            { ...restProps }
        >
            <ConditionsSummary
                condition={ subject }
                conditionName={ t("console.authorization.common.subjects") }
                { ...restProps }
            />
        </SummarySection>

        <SummarySection
            icon="check-square-o"
            title={ t("console.authorization.common.environments") }
            { ...restProps }
        >
            <ConditionsSummary
                condition={ condition }
                conditionName={ t("console.authorization.common.environments") }
                { ...restProps }
            />
        </SummarySection>
    </div>
);

SummaryView.propTypes = {
    actionValues: PropTypes.objectOf(PropTypes.any).isRequired,
    condition: PropTypes.shape(PropTypes.object),
    customAttributes: PropTypes.arrayOf(PropTypes.any).isRequired,
    isReadOnly: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
    resources: PropTypes.arrayOf(PropTypes.any).isRequired,
    staticAttributes: PropTypes.arrayOf(PropTypes.any).isRequired,
    subject: PropTypes.objectOf(PropTypes.any).isRequired,
    userAttributes: PropTypes.arrayOf(PropTypes.any).isRequired
};

export default SummaryView;
