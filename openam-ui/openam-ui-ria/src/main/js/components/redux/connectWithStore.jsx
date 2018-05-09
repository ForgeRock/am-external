/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { connect } from "react-redux";
import React from "react";

import store from "store/index";

const getDisplayName = (WrappedComponent) => {
    return WrappedComponent.displayName || WrappedComponent.name || "Component";
};

/**
 * A HoC (higher-order component) that wraps another component to provide `this.props.store`.
 * Pass in your component and it will return the wrapped component.
 * @param {ReactComponent} WrappedComponent Component to wrap
 * @returns {ReactComponent} Wrapped component
 * @example
 * import connectWithStore from "components/redux/connectWithStore"
 * import store from "store/index";
 *
 * class MyReactComponent extends Component { ... }
 *
 * const mapStateToProps = (state) => { ... }
 * const mapDispatchToProps = (dispatch) => { ... )
 *
 * export default connectWithStore(MyReactComponent, mapStateToProps, mapDispatchToProps)
 * @see https://github.com/reactjs/react-redux/blob/master/docs/api.md#connectmapstatetoprops-mapdispatchtoprops-mergeprops-options
 */
const connectWithStore = (WrappedComponent, ...args) => {
    const ConnectedWrappedComponent = connect(...args)(WrappedComponent);

    const component = (props) => <ConnectedWrappedComponent { ...props } store={ store } />;
    component.displayName = `connectWithStore(${getDisplayName(WrappedComponent)})`;
    component.WrappedComponent = WrappedComponent;

    return component;
};

export default connectWithStore;
