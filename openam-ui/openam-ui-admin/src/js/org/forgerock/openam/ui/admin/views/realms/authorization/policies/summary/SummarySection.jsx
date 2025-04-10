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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

import ActiveHeader
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/header/ActiveHeader";
import ReadOnlyHeader
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/header/ReadOnlyHeader";

const SummarySection = ({ children, icon, isReadOnly, onClick, title }) => {
    const header = isReadOnly
        ? <ReadOnlyHeader icon={ icon } title={ title } />
        : <ActiveHeader icon={ icon } onClick={ onClick } title={ title } />;

    return (
        <div className="am-masonry-item">
            <Panel className="am-summary-section">
                <Panel.Heading>{ header }</Panel.Heading>
                <Panel.Body>{ children }</Panel.Body>
            </Panel>
        </div>
    );
};

SummarySection.defaultProps = {
    isReadOnly: false
};

SummarySection.propTypes = {
    children: PropTypes.node.isRequired,
    icon: PropTypes.string.isRequired,
    isReadOnly: PropTypes.bool,
    onClick: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired
};

export default SummarySection;
