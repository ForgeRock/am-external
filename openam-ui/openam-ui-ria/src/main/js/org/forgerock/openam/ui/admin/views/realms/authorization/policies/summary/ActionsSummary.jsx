/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
    actionValues: PropTypes.objectOf(PropTypes.string),
    onClick: PropTypes.func.isRequired
};

export default ActionsSummary;
