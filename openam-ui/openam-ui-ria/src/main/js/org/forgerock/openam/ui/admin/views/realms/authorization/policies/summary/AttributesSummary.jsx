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
import { t } from "i18next";
import EmptySectionCallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/EmptySectionCallToAction";
import React, { PropTypes } from "react";
import AttributesSummaryItem
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/AttributesSummaryItem";

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
