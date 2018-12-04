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
 * Copyright 2016-2018 ForgeRock AS.
 */

import _ from "lodash";
import { Badge } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import EmptySectionCallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/EmptySectionCallToAction";

const ActionsSummary = ({ actionValues, onClick }) => {
    if (_.isEmpty(actionValues)) {
        return (
            <EmptySectionCallToAction onClick={ onClick } sectionName={ t("console.authorization.common.actions") } />
        );
    }

    const actions = _(actionValues)
        .mapValues((value, key) => (
            <Badge key={ key }>{ key.toUpperCase() }: {
                value ? t("console.authorization.common.allowed") : t("console.authorization.common.denied")
            }
            </Badge>
        ))
        .values()
        .value();

    return <span className="am-badge-group">{ actions }</span>;
};

ActionsSummary.propTypes = {
    actionValues: PropTypes.objectOf(PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.bool
    ])),
    onClick: PropTypes.func.isRequired
};

export default ActionsSummary;
