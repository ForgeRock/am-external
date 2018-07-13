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
import EmptySectionCallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/EmptySectionCallToAction";
import React, { PropTypes } from "react";

const ConditionsSummary = ({ condition, conditionName, onClick }) => {
    return _.isEmpty(condition)
        ? <EmptySectionCallToAction onClick={ onClick } sectionName={ conditionName } />
        : <pre className="am-text-preformatted">{ JSON.stringify(condition, null, 2) }</pre>;
};

ConditionsSummary.propTypes = {
    condition: React.PropTypes.objectOf(PropTypes.any),
    conditionName: PropTypes.string.isRequired,
    onClick: PropTypes.func.isRequired
};

export default ConditionsSummary;
