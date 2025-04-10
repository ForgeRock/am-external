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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { isEqual, merge, noop, setWith } from "lodash";
import PropTypes from "prop-types";
import React, { forwardRef, PureComponent } from "react";

/**
 * Form Higher-Order Components (HOC) to programmatically focus upon a specific attribute.
 */
const focusableAttributes = (WrappedComponent) => {
    class FocusableAttributes extends PureComponent {
        static propTypes = {
            focusPath: PropTypes.arrayOf(PropTypes.string),
            forwardedRef: PropTypes.func,
            onFocusComplete: PropTypes.func,
            uiSchema: PropTypes.objectOf(PropTypes.any)
        };

        static defaultProps = {
            onFocusComplete: noop
        };

        constructor (props) {
            super(props);

            this.state = {
                uiSchema: this.createUiSchema(props.focusPath.slice(0, -1))
            };
        }

        componentDidUpdate (prevProps) {
            if (!isEqual(this.props.focusPath, prevProps.focusPath) && this.props.focusPath.length) {
                /**
                 * Use of #setState within a conditional statement allowed.
                 * @see https://reactjs.org/docs/react-component.html#componentdidupdate
                 **/
                this.setState({ uiSchema: this.createUiSchema(this.props.focusPath.slice(0, -1)) }); // eslint-disable-line react/no-did-update-set-state
            }
        }

        /**
         * Given a JSON Schema path to focus upon, creates a UI Schema with the appropriate `expanded` UI options.
         * @param {string[]} path JSON Schema path to focus upon. Path MUST NOT include the last element indicating the attribute to focus upon.
         * @returns {object} UI Schema.
         */
        createUiSchema = (path) => {
            return path.length
                ? setWith({}, path.join("."),
                    { "ui:options": { expanded: this.handleExpandedComplete } },
                    () => ({ "ui:options": { expanded: true } })
                )
                : {};
        };

        /**
         * Checks to see if an element with the given id is of type input. If it is, the browser sets it's focus on that element,
         * otherwise it will focus on the first child of type input within the element.
         * @param {string} id Element ID.
         */
        focusInput = (id) => {
            const elementWithId = document.getElementById(id);

            if (elementWithId) {
                if (elementWithId.tagName === "INPUT") {
                    elementWithId.focus();
                } else {
                    const childInput = elementWithId.querySelector("input");
                    if (childInput) {
                        childInput.focus();
                    }
                }
            }
        };

        handleExpandedComplete = () => {
            this.setState({ uiSchema: {} });
            this.focusInput(`root_${this.props.focusPath.join("_")}`);
            this.props.onFocusComplete();
        };

        render () {
            const { forwardedRef, uiSchema, ...restProps } = this.props;

            return (
                <WrappedComponent
                    { ...restProps }
                    ref={ forwardedRef }
                    uiSchema={ merge({}, this.state.uiSchema, uiSchema) }
                />
            );
        }
    }

    /* eslint-disable prefer-arrow-callback, react/display-name, react/no-multi-comp */
    return forwardRef(function focusableAttributes (props, ref) {
        return <FocusableAttributes { ...props } forwardedRef={ ref } />;
    });
    /* eslint-enable */
};

export default focusableAttributes;
