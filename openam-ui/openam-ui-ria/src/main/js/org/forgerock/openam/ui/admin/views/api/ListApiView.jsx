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

import { debounce, isEmpty, map } from "lodash";
import { getPathsTree } from "org/forgerock/openam/ui/admin/services/global/ApiService";
import { FormControl, Grid, Panel, Row, Col } from "react-bootstrap";
import { t } from "i18next";
import calculateHeight from "./calculateHeight";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import filterTree from "./filterTree";
import React, { Component } from "react";
import Router from "org/forgerock/commons/ui/common/main/Router";
import Tree from "components/Tree";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const getIframeSource = (apiPath) => `${Constants.context}/api/?url=${Constants.context}/json${
    map(apiPath.split("/"), (item) => encodeURIComponent(item)).join("/")}?_api`;

class ListApiView extends Component {
    constructor (props) {
        super(props);
        this.handlePathSelect = this.handlePathSelect.bind(this);
        this.handleSearchFilter = this.handleSearchFilter.bind(this);
        this.resize = this.resize.bind(this);

        const apiPath = props.router.params[0];
        const iframeSource = apiPath ? getIframeSource(`/${apiPath}`) : "";

        this.state = {
            collapsed: true,
            filteredTree: [],
            filter: "",
            initialTree: [],
            iframeSource,
            containerHeight: calculateHeight()
        };
    }

    componentDidMount () {
        getPathsTree().then((response) => {
            /*
             * Some objects are just containers for other objects, and so do not have corresponding APIs and do not
             * contain the path property (which is used to add links to these corresponding APIs).
             * Because of this we add the objectPath to all the items and use this 'always present' property to
             * ascertain an object's path in the tree.
             */
            const addObjectPaths = (children, parentPath = "") => map(children, (item) => {
                item.objectPath = `${parentPath}${item.id}`;
                if (item.children) {
                    item.children = addObjectPaths(item.children, item.objectPath);
                }
                return item;
            });

            if (response.length && this.state.iframeSource === "") {
                this.routeToPath(response[0].path);
            }

            const dataWithObjectPath = addObjectPaths(response);
            this.setState({
                initialTree: dataWithObjectPath,
                filteredTree: dataWithObjectPath
            });
        });
        window.addEventListener("resize", debounce(this.resize, 100));
    }

    componentWillUnmount () {
        window.removeEventListener("resize", this.resize);
    }

    handleSearchFilter (e) {
        const filter = e.target.value;
        this.setState({
            collapsed: isEmpty(filter),
            filter,
            filteredTree: isEmpty(filter) ? this.state.initialTree : filterTree(this.state.initialTree, filter)
        });
    }

    handlePathSelect (apiPath) {
        this.routeToPath(apiPath);
    }

    resize () {
        this.setState({ containerHeight: calculateHeight() });
    }

    routeToPath (apiPath) {
        Router.routeTo(Router.configuration.routes.apiExplorer, {
            args: [apiPath.slice(1)],
            trigger: true
        });
    }

    render () {
        const realm = this.props.router.params[0];
        const activePaths = map(realm.split("/"), (value, index, path) => {
            return `/${path.slice(0, path.length - index).join("/")}`;
        });

        const header = (
            <div className="input-group">
                <FormControl
                    onChange={ this.handleSearchFilter }
                    placeholder={ t("common.form.search") }
                    type="text"
                />
                <span className="input-group-addon">
                    <i className="fa fa-search" />
                </span>
            </div>
        );

        return (
            <Grid fluid="true">
                <Row>
                    <Col md={ 3 }>
                        <Panel
                            className="am-iframe-overflow-scroll row"
                            header={ header }
                            style={ { height: `${this.state.containerHeight}px` } }
                        >
                            <nav className="sidenav">
                                <Tree
                                    activePaths={ activePaths }
                                    collapsed={ this.state.collapsed }
                                    data={ this.state.filteredTree }
                                    filter={ this.state.filter }
                                    onNodeSelect={ this.handlePathSelect }
                                />
                            </nav>
                        </Panel>
                    </Col>

                    <Col md={ 9 }>
                        <iframe
                            className="am-iframe-overflow-scroll am-iframe-full-width"
                            src={ this.state.iframeSource }
                            style={ { height: `${this.state.containerHeight}px` } }
                            title={ t("console.api.explorer.iFrameTitle") }
                        />
                    </Col>
                </Row>
            </Grid>
        );
    }
}

ListApiView.propTypes = {
    router: withRouterPropType
};

export default withRouter(ListApiView);
