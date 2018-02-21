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
 * Copyright 2017 ForgeRock AS.
 */

import React, { PropTypes } from "react";
import { Col, Grid, Row } from "react-bootstrap";

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
