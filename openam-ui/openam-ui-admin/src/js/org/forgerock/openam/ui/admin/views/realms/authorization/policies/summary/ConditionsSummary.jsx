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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import _ from "lodash";
import PropTypes from "prop-types";
import React from "react";

import CallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/emptySection/CallToAction";
import NoItemAvailable
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/emptySection/NoItemAvailable";

const ConditionsSummary = ({ condition, conditionName, isReadOnly, onClick }) => {
    let content;
    if (_.isEmpty(condition)) {
        content = isReadOnly
            ? <NoItemAvailable sectionName={ conditionName } />
            : <CallToAction onClick={ onClick } sectionName={ conditionName } />;
    } else {
        content = <pre className="am-text-preformatted">{ JSON.stringify(condition, null, 2) }</pre>;
    }

    return content;
};

ConditionsSummary.propTypes = {
    condition: PropTypes.objectOf(PropTypes.any),
    conditionName: PropTypes.string.isRequired,
    isReadOnly: PropTypes.bool.isRequired,
    onClick: PropTypes.func
};

export default ConditionsSummary;
