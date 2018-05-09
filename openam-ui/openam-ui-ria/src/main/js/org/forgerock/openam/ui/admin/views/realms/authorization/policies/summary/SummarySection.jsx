/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Panel } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

import SectionMediaHeader
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/SectionMediaHeader";

const SummarySection = ({ children, icon, onClick, title }) => {
    return (
        <Panel
            className="am-masonry-item am-summary-section"
            header={ <SectionMediaHeader icon={ icon } onClick={ onClick } title={ title } /> }
        >
            { children }
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
