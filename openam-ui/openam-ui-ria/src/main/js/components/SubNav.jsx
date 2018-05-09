/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Col, Grid, Row } from "react-bootstrap";
import PropTypes from "prop-types";
import React from "react";

import Breadcrumb from "components/Breadcrumb";

/**
 * A Sub navigation component.
 * @module components/SubNav
 * @param {ReactNode[]} children Children to add within this component
 * @param {Boolean} fluid If set to true, the subnav will render full-width
 * @returns {ReactElement} Renderable React element
 */
const SubNav = ({ children, fluid }) => {
    return (
        <div className="subnav-container">
            <Grid fluid={ fluid }>
                <Row>
                    <nav className="navbar navbar-default">
                        <Col className="navbar-header" lg={ 2 } md={ 3 } sm={ 3 } xs={ 4 } >
                            <div className="ellipsis">
                                <strong>
                                    { children }
                                </strong>
                            </div>
                        </Col>
                        <Col className="ellipsis" lg={ 10 } md={ 9 } sm={ 9 } xs={ 8 } >
                            <Breadcrumb />
                        </Col>
                    </nav>
                </Row>
            </Grid>
        </div>
    );
};

SubNav.propTypes = {
    children: PropTypes.node,
    fluid: PropTypes.boolean
};

export default SubNav;
