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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import DatePicker from "react-datepicker";
import moment from "moment";
import PropTypes from "prop-types";
import React, { Component } from "react";

const TAB_KEY_CODE = 9;
const ENTER_KEY_CODE = 13;

class DateTimeInput extends Component {
    constructor (props) {
        super(props);

        const initialDateTime = moment(this.props.dateTime, moment.ISO_8601);
        const utcOffset = initialDateTime.utcOffset();
        const dateTime = initialDateTime.utc();

        this.state = {
            dateTime: dateTime.isValid() ? dateTime : undefined,
            utcOffset
        };
    }

    handleOnChange = (dateTime) => {
        this.setState({
            dateTime: dateTime.isValid() ? dateTime : undefined
        });
    };

    handleOnChangeRaw = (event) => {
        const dateTime = moment.utc(event.target.value, this.props.format);
        this.setState({
            dateTime: dateTime.isValid() ? dateTime : undefined
        });
    };

    handleFormControlBlur = (event) => {
        const dateTime = moment.utc(event.target.value, this.props.format);
        this.setState({
            dateTime: dateTime.isValid() ? dateTime : undefined
        });
        this.props.onUpdate(dateTime.toISOString());
    };

    handleDatePickerBlur = () => {
        this.props.onUpdate(this.state.dateTime.toISOString());
    };

    handleOnKeyDown = (event) => {
        if (event.keyCode === ENTER_KEY_CODE || event.keyCode === TAB_KEY_CODE) {
            this.handleFormControlBlur(event);
        }
    };

    render () {
        return (
            <DatePicker
                autoFocus={ this.props.autoFocus } // eslint-disable-line jsx-a11y/no-autofocus
                className={ "form-control" }
                dateFormat={ this.props.format }
                id={ this.props.id }
                onBlur={ this.handleFormControlBlur }
                onChange={ this.handleOnChange }
                onChangeRaw={ this.handleOnChangeRaw }
                onClickOutside={ this.handleDatePickerBlur }
                onKeyDown={ this.handleOnKeyDown }
                selected={ this.state.dateTime }
                showTimeSelect
                timeCaption="time (UTC)"
                timeFormat="HH:mm"
                timeIntervals={ 60 }
                utcOffset={ this.state.utcOffset }
            />
        );
    }
}

DateTimeInput.defaultProps = {
    autoFocus: false,
    format: "DD-MMM YYYY HH:mm"
};

DateTimeInput.propTypes = {
    autoFocus: PropTypes.bool,
    dateTime: PropTypes.string,
    format: PropTypes.string,
    id: PropTypes.string,
    onUpdate: PropTypes.func.isRequired
};

export default DateTimeInput;
