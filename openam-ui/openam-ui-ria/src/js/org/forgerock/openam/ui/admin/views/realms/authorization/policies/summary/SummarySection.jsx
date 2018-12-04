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

import { Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

import SectionMediaHeader
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/SectionMediaHeader";

const SummarySection = ({ children, icon, onClick, title }) => {
    return (
        <Panel className="am-masonry-item am-summary-section">
            <Panel.Heading><SectionMediaHeader icon={ icon } onClick={ onClick } title={ title } /></Panel.Heading>
            <Panel.Body>{ children }</Panel.Body>
        </Panel>
    );
};

SummarySection.propTypes = {
    children: PropTypes.node.isRequired,
    icon: PropTypes.string.isRequired,
    onClick: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired
};

export default SummarySection;
