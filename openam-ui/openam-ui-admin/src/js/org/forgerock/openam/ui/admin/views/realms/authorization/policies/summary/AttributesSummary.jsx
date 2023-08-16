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

import _ from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Fragment } from "react";

import AttributesSummaryItem
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/AttributesSummaryItem";
import CallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/emptySection/CallToAction";
import NoItemAvailable
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/emptySection/NoItemAvailable";

const AttributesSummary = ({ customAttributes, isReadOnly, onClick, staticAttributes, userAttributes }) => {
    let content;
    if (_.isEmpty([...userAttributes, ...staticAttributes, ...customAttributes])) {
        content = isReadOnly
            ? <NoItemAvailable sectionName={ t ("console.authorization.common.responseAttributes") } />
            : (
                <CallToAction
                    onClick={ onClick }
                    sectionName={ t ("console.authorization.common.responseAttributes") }
                />
            );
    } else {
        content = (
            <Fragment>
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
            </Fragment>
        );
    }

    return content;
};

AttributesSummary.propTypes = {
    customAttributes: PropTypes.arrayOf(PropTypes.object).isRequired,
    isReadOnly: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired,
    staticAttributes: PropTypes.arrayOf(PropTypes.object).isRequired,
    userAttributes: PropTypes.arrayOf(PropTypes.object).isRequired
};

export default AttributesSummary;