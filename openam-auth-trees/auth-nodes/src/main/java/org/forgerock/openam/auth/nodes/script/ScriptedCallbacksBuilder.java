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
 * Copyright 2023-2025 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.MetadataCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.auth.node.api.SuspendedTextOutputCallback;
import org.forgerock.openam.authentication.callbacks.BooleanAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.ConsentMappingCallback;
import org.forgerock.openam.authentication.callbacks.DeviceProfileCallback;
import org.forgerock.openam.authentication.callbacks.IdPCallback;
import org.forgerock.openam.authentication.callbacks.KbaCreateCallback;
import org.forgerock.openam.authentication.callbacks.NumberAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.SelectIdPCallback;
import org.forgerock.openam.authentication.callbacks.StringAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.TermsAndConditionsCallback;
import org.forgerock.openam.authentication.callbacks.ValidatedPasswordCallback;
import org.forgerock.openam.authentication.callbacks.ValidatedUsernameCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Constructor and collector class for ScriptedDecisionNode scripts to create and save a list of callbacks required by
 * the journey / authentication process.
 */
public class ScriptedCallbacksBuilder {

    private List<Callback> callbacks;

    /**
     * Default constructor.
     */
    public ScriptedCallbacksBuilder() {
        callbacks = new ArrayList<>();
    }

    /**
     * Returns the collected callbacks.
     * @return Array of callbacks.
     */
    public Callback[] getCallbacks() {
        return callbacks.toArray(new Callback[0]);
    }


    /**
     * Construct and save to the list of callbacks a TextOutputCallback with a message type and message
     * to be displayed.
     *
     * @param messageType the message type ({@code INFORMATION},
     *                  {@code WARNING} or {@code ERROR}).
     *
     * @param message the message to be displayed.
     *
     * @exception IllegalArgumentException if {@code messageType}
     *                  is not either {@code INFORMATION},
     *                  {@code WARNING} or {@code ERROR},
     *                  if {@code message} is null,
     *                  or if {@code message} has a length of 0.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void textOutputCallback(int messageType, String message) {
        callbacks.add(new TextOutputCallback(messageType, message));
    }

    /**
     * Construct and save to the list of callbacks a SuspendedTextOutputCallback with a message type and message
     * to be displayed.
     *
     * @param messageType the message type ({@code INFORMATION},
     *                  {@code WARNING} or {@code ERROR}).
     * @param message the message to be displayed.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void suspendedTextOutputCallback(int messageType, String message) {
        callbacks.add(new SuspendedTextOutputCallback(messageType, message));
    }

    /**
     * Create and save to the list of callbacks a new HiddenValueCallback with the id and initial value as specified.
     *
     * @param id The id for the HiddenValueCallback when it is rendered as an HTML element.
     * @param value The initial value for the HiddenValueCallback when it is rendered as an HTML element.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void hiddenValueCallback(String id, String value) {
        callbacks.add(new HiddenValueCallback(id, value));
    }

    /**
     * Construct and save to the list of callbacks a {@code ChoiceCallback} with a prompt,
     * a list of choices, a default choice, and a boolean specifying
     * whether or not multiple selections from the list of choices are allowed.
     *
     *
     * @param prompt the prompt used to describe the list of choices.
     *
     * @param choices the list of choices.
     *
     * @param defaultChoice the choice to be used as the default choice
     *                  when the list of choices are displayed.  This value
     *                  is represented as an index into the
     *                  {@code choices} array.
     *
     * @param multipleSelectionsAllowed boolean specifying whether or
     *                  not multiple selections can be made from the
     *                  list of choices.
     *
     * @exception IllegalArgumentException if {@code prompt} is null,
     *                  if {@code prompt} has a length of 0,
     *                  if {@code choices} is null,
     *                  if {@code choices} has a length of 0,
     *                  if any element from {@code choices} is null,
     *                  if any element from {@code choices}
     *                  has a length of 0 or if {@code defaultChoice}
     *                  does not fall within the array boundaries of
     *                  {@code choices}.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void choiceCallback(String prompt, String[] choices, int defaultChoice, boolean multipleSelectionsAllowed) {
        ChoiceCallback choiceCallback = new ChoiceCallback(prompt, choices, defaultChoice, multipleSelectionsAllowed);
        choiceCallback.setSelectedIndex(defaultChoice);
        callbacks.add(choiceCallback);
    }

    /**
     * Construct and save to the list of callbacks a {@code NameCallback} with a prompt
     * and default name.
     *
     * @param prompt the prompt used to request the information.
     *
     * @exception IllegalArgumentException if {@code prompt} is null,
     *                  if {@code prompt} has a length of 0,
     *                  if {@code defaultName} is null,
     *                  or if {@code defaultName} has a length of 0.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void nameCallback(String prompt) {
        callbacks.add(new NameCallback(prompt));
    }

    /**
     * Construct and save to the list of callbacks a {@code NameCallback} with a prompt
     * and default name.
     *
     * @param prompt the prompt used to request the information.
     *
     * @param defaultName the name to be used as the default name displayed
     *                  with the prompt.
     *
     * @exception IllegalArgumentException if {@code prompt} is null,
     *                  if {@code prompt} has a length of 0,
     *                  if {@code defaultName} is null,
     *                  or if {@code defaultName} has a length of 0.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void nameCallback(String prompt, String defaultName) {
        callbacks.add(new NameCallback(prompt, defaultName));
    }

    /**
     * Construct and save to the list of callbacks a {@code PasswordCallback} with a prompt
     * and a boolean specifying whether the password should be displayed
     * as it is being typed.
     *
     * @param prompt the prompt used to request the password.
     *
     * @param echoOn true if the password should be displayed
     *                  as it is being typed.
     *
     * @exception IllegalArgumentException if {@code prompt} is null or
     *                  if {@code prompt} has a length of 0.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void passwordCallback(String prompt, boolean echoOn) {
        callbacks.add(new PasswordCallback(prompt, echoOn));
    }

    /**
     * Construct and save to the list of callbacks a {@code TextInputCallback} with a prompt
     * and default input value.
     *
     * @param prompt the prompt used to request the information.
     *
     * @exception IllegalArgumentException if {@code prompt} is null,
     *                  if {@code prompt} has a length of 0,
     *                  if {@code defaultText} is null
     *                  or if {@code defaultText} has a length of 0.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void textInputCallback(String prompt) {
        callbacks.add(new TextInputCallback(prompt));
    }

    /**
     * Construct and save to the list of callbacks a {@code TextInputCallback} with a prompt
     * and default input value.
     *
     * @param prompt the prompt used to request the information.
     *
     * @param defaultText the text to be used as the default text displayed
     *                  with the prompt.
     *
     * @exception IllegalArgumentException if {@code prompt} is null,
     *                  if {@code prompt} has a length of 0,
     *                  if {@code defaultText} is null
     *                  or if {@code defaultText} has a length of 0.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void textInputCallback(String prompt, String defaultText) {
        callbacks.add(new TextInputCallback(prompt, defaultText));
    }

    /**
     * Construct and save to the list of callbacks it as an INFORMATION TextOutputCallback.
     *
     * @param message The script which will be inserted into the page receiving this callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void scriptTextOutputCallback(String message) {
        callbacks.add(new ScriptTextOutputCallback(message));
    }

    /**
     * Constructs and saves to the list of callbacks a <code>RedirectCallback</code> object with
     * redirect URL,redirect data,redirect method,status parameter
     * and redirect back URL Cookie name.
     *
     * @param redirectUrl URL to be redirected to.
     * @param redirectData the data to be redirected to redirect URL.
     * @param method Method used for redirection, either "GET" or "POST".
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void redirectCallback(String redirectUrl, Map redirectData, String method) {
        callbacks.add(new RedirectCallback(redirectUrl, redirectData, method));
    }

    /**
     * Constructs and saves to the list of callbacks a <code>RedirectCallback</code> object with
     * redirect URL,redirect data,redirect method,status parameter
     * and redirect back URL Cookie name.
     *
     * @param redirectUrl URL to be redirected to.
     * @param redirectData the data to be redirected to redirect URL.
     * @param method Method used for redirection, either "GET" or "POST".
     * @param statusParameter statusParameter to be checked from
     * HttpServletRequest object at the result of redirection.
     * @param redirectBackUrlCookie redirectBackUrlCookie name to be set as the
     * OpenAM server URL when redirecting to external web site.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void redirectCallback(String redirectUrl, Map redirectData, String method, String statusParameter,
                                 String redirectBackUrlCookie) {
        callbacks.add(new RedirectCallback(redirectUrl, redirectData, method, statusParameter, redirectBackUrlCookie));
    }

    /**
     * Constructs and saves to the list of callbacks a <code>RedirectCallback</code> object with
     * redirect URL,redirect data,redirect method,status parameter
     * and redirect back URL Cookie name.
     *
     * @param redirectUrl URL to be redirected to.
     * @param redirectData the data to be redirected to redirect URL.
     * @param method Method used for redirection, either "GET" or "POST".
     * @param setTrackingCookie set to true if a tracking cookie should be set.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void redirectCallback(String redirectUrl, Map redirectData, String method, boolean setTrackingCookie) {
        RedirectCallback redirectCallback = new RedirectCallback(redirectUrl, redirectData, method);
        redirectCallback.setTrackingCookie(setTrackingCookie);
        callbacks.add(redirectCallback);
    }

    /**
     * Constructs and saves to the list of callbacks a <code>RedirectCallback</code> object with
     * redirect URL,redirect data,redirect method,status parameter
     * and redirect back URL Cookie name.
     *
     * @param redirectUrl URL to be redirected to.
     * @param redirectData the data to be redirected to redirect URL.
     * @param method Method used for redirection, either "GET" or "POST".
     * @param statusParameter statusParameter to be checked from
     * HttpServletRequest object at the result of redirection.
     * @param redirectBackUrlCookie redirectBackUrlCookie name to be set as the
     * OpenAM server URL when redirecting to external web site.
     * @param setTrackingCookie set to true if a tracking cookie should be set.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void redirectCallback(String redirectUrl, Map redirectData, String method, String statusParameter,
            String redirectBackUrlCookie, boolean setTrackingCookie) {
        RedirectCallback redirectCallback = new RedirectCallback(redirectUrl, redirectData, method, statusParameter,
                redirectBackUrlCookie);
        redirectCallback.setTrackingCookie(setTrackingCookie);
        callbacks.add(redirectCallback);
    }

    /**
     * Create and save into the list of callbacks the {@link MetadataCallback} with the provided metadata.
     * @param outputValue The output value in json format.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void metadataCallback(Object outputValue) {
        callbacks.add(new MetadataCallback(JsonValue.json(outputValue)));
    }

    /**
     * Construct and save into the list of callbacks a {@link StringAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void stringAttributeInputCallback(String name, String prompt, String value, Boolean required) {
        callbacks.add(new StringAttributeInputCallback(name, prompt, value, required));
    }

    /**
     * Construct and save into the list of callbacks a {@link StringAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void stringAttributeInputCallback(String name, String prompt, String value, Boolean required,
                                             List<String> failedPolicies) {
        StringAttributeInputCallback attrInpCallback = new StringAttributeInputCallback(name, prompt, value, required);
        failedPolicies.forEach(attrInpCallback::addFailedPolicy);
        callbacks.add(attrInpCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link StringAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param policies validation policies that apply to this attribute in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void stringAttributeInputCallback(String name, String prompt, String value, Boolean required,
                                             Object policies, Boolean validateOnly) {
        callbacks.add(new StringAttributeInputCallback(name, prompt, value, required, JsonValue.json(policies),
                validateOnly));
    }

    /**
     * Construct and save into the list of callbacks a {@link StringAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param policies validation policies that apply to this attribute in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void stringAttributeInputCallback(String name, String prompt, String value, Boolean required,
                                             Object policies, Boolean validateOnly, List<String> failedPolicies) {
        StringAttributeInputCallback attrInpCallback = new StringAttributeInputCallback(name, prompt, value, required,
                JsonValue.json(policies), validateOnly);
        failedPolicies.forEach(attrInpCallback::addFailedPolicy);
        callbacks.add(attrInpCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link NumberAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void numberAttributeInputCallback(String name, String prompt, Double value, Boolean required) {
        callbacks.add(new NumberAttributeInputCallback(name, prompt, value, required));
    }

    /**
     * Construct and save into the list of callbacks a {@link NumberAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void numberAttributeInputCallback(String name, String prompt, Double value, Boolean required,
                                             List<String> failedPolicies) {
        NumberAttributeInputCallback attrInpCallback = new NumberAttributeInputCallback(name, prompt, value, required);
        failedPolicies.forEach(attrInpCallback::addFailedPolicy);
        callbacks.add(attrInpCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link NumberAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param policies validation policies that apply to this attribute in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void numberAttributeInputCallback(String name, String prompt, Double value, Boolean required,
                                             Object policies, Boolean validateOnly) {
        callbacks.add(new NumberAttributeInputCallback(name, prompt, value, required, JsonValue.json(policies),
                validateOnly));
    }

    /**
     * Construct and save into the list of callbacks a {@link NumberAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param policies validation policies that apply to this attribute in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void numberAttributeInputCallback(String name, String prompt, Double value, Boolean required,
                                             Object policies, Boolean validateOnly, List<String> failedPolicies) {
        NumberAttributeInputCallback attrInpCallback = new NumberAttributeInputCallback(name, prompt, value, required,
                JsonValue.json(policies), validateOnly);
        failedPolicies.forEach(attrInpCallback::addFailedPolicy);
        callbacks.add(attrInpCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link BooleanAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void booleanAttributeInputCallback(String name, String prompt, Boolean value, Boolean required) {
        callbacks.add(new BooleanAttributeInputCallback(name, prompt, value, required));
    }

    /**
     * Construct and save into the list of callbacks a {@link BooleanAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void booleanAttributeInputCallback(String name, String prompt, Boolean value, Boolean required,
                                              List<String> failedPolicies) {
        BooleanAttributeInputCallback attrInpCallback = new BooleanAttributeInputCallback(name, prompt, value,
                required);
        failedPolicies.forEach(attrInpCallback::addFailedPolicy);
        callbacks.add(attrInpCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link BooleanAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param policies validation policies that apply to this attribute in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void booleanAttributeInputCallback(String name, String prompt, Boolean value, Boolean required,
                                              Object policies, Boolean validateOnly) {
        callbacks.add(new BooleanAttributeInputCallback(name, prompt, value, required, JsonValue.json(policies),
                validateOnly));
    }

    /**
     * Construct and save into the list of callbacks a {@link BooleanAttributeInputCallback}.
     *
     * @param name the displayable name of the attribute
     * @param prompt the displayable prompt for this attribute
     * @param value the current value of the attribute, if any
     * @param required whether the attribute is required
     * @param policies validation policies that apply to this attribute in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void booleanAttributeInputCallback(String name, String prompt, Boolean value, Boolean required,
                                              Object policies, Boolean validateOnly, List<String> failedPolicies) {
        BooleanAttributeInputCallback attrInpCallback = new BooleanAttributeInputCallback(name, prompt, value,
                required, JsonValue.json(policies), validateOnly);
        failedPolicies.forEach(attrInpCallback::addFailedPolicy);
        callbacks.add(attrInpCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link ConfirmationCallback}.
     *
     * @param messageType the message type (INFORMATION, WARNING or ERROR).
     * @param optionType the option type (YES_NO_OPTION, YES_NO_CANCEL_OPTION or OK_CANCEL_OPTION).
     * @param defaultOption the default option from the provided optionType (YES, NO, CANCEL or OK).
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void confirmationCallback(int messageType, int optionType, int defaultOption) {
        callbacks.add(new ConfirmationCallback(messageType, optionType, defaultOption));
    }

    /**
     * Construct and save into the list of callbacks a {@link ConfirmationCallback}.
     *
     * @param messageType the message type (INFORMATION, WARNING or ERROR).
     * @param options the list of confirmation options.
     * @param defaultOption the default option from the provided optionType (YES, NO, CANCEL or OK).
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void confirmationCallback(int messageType, String[] options, int defaultOption) {
        callbacks.add(new ConfirmationCallback(messageType, options, defaultOption));
    }

    /**
     * Construct and save into the list of callbacks a {@link ConfirmationCallback}.
     *
     * @param prompt the prompt used to describe the list of options.
     * @param messageType the message type (INFORMATION, WARNING or ERROR).
     * @param optionType the option type (YES_NO_OPTION, YES_NO_CANCEL_OPTION or OK_CANCEL_OPTION).
     * @param defaultOption the default option from the provided optionType (YES, NO, CANCEL or OK).
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void confirmationCallback(String prompt, int messageType, int optionType, int defaultOption) {
        callbacks.add(new ConfirmationCallback(prompt, messageType, optionType, defaultOption));
    }

    /**
     * Construct and save into the list of callbacks a {@link ConfirmationCallback}.
     *
     * @param prompt the prompt used to describe the list of options.
     * @param messageType the message type (INFORMATION, WARNING or ERROR).
     * @param options the list of confirmation options.
     * @param defaultOption the default option from the provided optionType (YES, NO, CANCEL or OK).
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void confirmationCallback(String prompt, int messageType, String[] options, int defaultOption) {
        callbacks.add(new ConfirmationCallback(prompt, messageType, options, defaultOption));
    }

    /**
     * Construct and save into the list of callbacks a {@link LanguageCallback}.
     *
     * @param language the language for building a {@link Locale} object.
     * @param country the country for building a {@link Locale} object.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void languageCallback(String language, String country) {
        LanguageCallback languageCallback = new LanguageCallback();
        languageCallback.setLocale(new Locale(language, country));
        callbacks.add(languageCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link IdPCallback}.
     *
     * @param provider The provider name.
     * @param clientId The provider clientId.
     * @param redirectUri The provider redirectURI.
     * @param scope The requested scope.
     * @param nonce Nonce for the Social Login request.
     * @param request The Request Object parameter.
     * @param requestUri The Request URI parameter.
     * @param acrValues The Requested Authentication Context Class Reference values.
     * @param requestNativeAppForUserInfo Indicates if the Native SDK app should be requested to send user info.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void idPCallback(String provider, String clientId, String redirectUri, List<String> scope, String nonce,
                String request, String requestUri, List<String> acrValues, boolean requestNativeAppForUserInfo) {
        callbacks.add(new IdPCallback(provider, clientId, redirectUri, scope, nonce, request, requestUri, acrValues,
                requestNativeAppForUserInfo));
    }

    /**
     * Construct and save into the list of callbacks a {@link IdPCallback}.
     *
     * @param provider The provider name.
     * @param clientId The provider clientId.
     * @param redirectUri The provider redirectURI.
     * @param scope The requested scope.
     * @param nonce Nonce for the Social Login request.
     * @param request The Request Object parameter.
     * @param requestUri The Request URI parameter.
     * @param acrValues The Requested Authentication Context Class Reference values.
     * @param requestNativeAppForUserInfo Indicates if the Native SDK app should be requested to send user info.
     * @param token The token that used to identity the user, it can be a JWT id_token, access token or authorization
     *              code.
     * @param tokenType The token type for the provided token. Can be one of id_token, access_token or
     *                  authorization_code
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void idPCallback(String provider, String clientId, String redirectUri, List<String> scope, String nonce,
        String request, String requestUri, List<String> acrValues, boolean requestNativeAppForUserInfo, String token,
        String tokenType) {
        IdPCallback idPCallback = new IdPCallback(provider, clientId, redirectUri, scope, nonce, request,
                requestUri, acrValues, requestNativeAppForUserInfo);
        idPCallback.setToken(token);
        idPCallback.setTokenType(tokenType);
        callbacks.add(idPCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link PollingWaitCallback}.
     *
     * @param waitTime the wait time for this PollingWaitCallback
     * @param message the message which should be displayed to the user.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void pollingWaitCallback(String waitTime, String message) {
        callbacks.add(new PollingWaitCallback(waitTime, message));
    }

    /**
     * Construct and save into the list of callbacks a {@link ValidatedPasswordCallback}.
     *
     * @param prompt the prompt used to request the password.
     * @param echoOn true if the password should be displayed as it is being typed.
     * @param policies a list of validation policy ids to be applied to this callback in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void validatedPasswordCallback(String prompt, boolean echoOn, Object policies, Boolean validateOnly) {
        callbacks.add(new ValidatedPasswordCallback(prompt, echoOn, JsonValue.json(policies), validateOnly));
    }

    /**
     * Construct and save into the list of callbacks a {@link ValidatedPasswordCallback}.
     *
     * @param prompt the prompt used to request the password.
     * @param echoOn true if the password should be displayed as it is being typed.
     * @param policies a list of validation policy ids to be applied to this callback in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void validatedPasswordCallback(String prompt, boolean echoOn, Object policies, Boolean validateOnly,
                                          List<String> failedPolicies) {
        ValidatedPasswordCallback validatedPasswordCallback = new ValidatedPasswordCallback(prompt, echoOn,
                JsonValue.json(policies), validateOnly);
        failedPolicies.forEach(validatedPasswordCallback::addFailedPolicy);
        callbacks.add(validatedPasswordCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link ValidatedUsernameCallback}.
     *
     * @param prompt the prompt used to request the name
     * @param policies the IDM policies which will be used to validate the username in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
    */
    @Supported(scriptingApi = true, javaApi = false)
    public void validatedUsernameCallback(String prompt, Object policies, Boolean validateOnly) {
        callbacks.add(new ValidatedUsernameCallback(prompt, JsonValue.json(policies), validateOnly));
    }

    /**
     * Construct and save into the list of callbacks a {@link ValidatedUsernameCallback}.
     *
     * @param prompt the prompt used to request the name
     * @param policies the IDM policies which will be used to validate the username in json format
     * @param validateOnly set to true iff successful validation should not result in advancing the tree
     * @param failedPolicies optional parameter to add one or more failed policies to the callback
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void validatedUsernameCallback(String prompt, Object policies, Boolean validateOnly,
                                          List<String> failedPolicies) {
        ValidatedUsernameCallback validatedUsernameCallback = new ValidatedUsernameCallback(prompt,
                JsonValue.json(policies), validateOnly);
        failedPolicies.forEach(validatedUsernameCallback::addFailedPolicy);
        callbacks.add(validatedUsernameCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link HttpCallback}.
     *
     * @param authorizationHeader Header name for the authorization string.
     * @param negotiationHeader  Negotiation header string.
     * @param errorCode Error code set in the header for negotiation.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void httpCallback(String authorizationHeader, String negotiationHeader, String errorCode) {
        callbacks.add(new HttpCallback(authorizationHeader, negotiationHeader, errorCode));
    }

    /**
     * Construct and save into the list of callbacks a {@link HttpCallback}.
     *
     * @param authRHeader Header name for the authorization string.
     * @param negoName Negotiation name in the negotiation header.
     * @param negoValue Negotiation value in the negotiation header.
     * @param errorCode Error code set in the header for negotiation.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void httpCallback(String authRHeader, String negoName, String negoValue, int errorCode) {
        callbacks.add(new HttpCallback(authRHeader, negoName, negoValue, errorCode));
    }

    /**
     * Construct and save into the list of callbacks a {@link X509CertificateCallback}.
     *
     * @param prompt the prompt used to request the X.509 Certificate
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void x509CertificateCallback(String prompt) {
        callbacks.add(new X509CertificateCallback(prompt));
    }

    /**
     * Construct and save into the list of callbacks a {@link X509CertificateCallback}.
     *
     * @param prompt the prompt used to request the X.509 Certificate
     * @param certificate the X.509 Certificate
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void x509CertificateCallback(String prompt, X509Certificate certificate) {
        callbacks.add(new X509CertificateCallback(prompt, certificate));
    }

    /**
     * Construct and save into the list of callbacks a {@link X509CertificateCallback}.
     *
     * @param prompt the prompt used to request the X.509 Certificate
     * @param certificate the X.509 Certificate
     * @param requestSignature set the request signature flag
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void x509CertificateCallback(String prompt, X509Certificate certificate, boolean requestSignature) {
        X509CertificateCallback x509CertificateCallback = new X509CertificateCallback(prompt, certificate);
        x509CertificateCallback.setReqSignature(requestSignature);
        callbacks.add(x509CertificateCallback);
    }

    /**
     * Construct and save into the list of callbacks a {@link ConsentMappingCallback}.
     *
     * @param config configuration of the callback in json format
     * @param message the message for the privacy and consent notice
     * @param isRequired whether all fields require consent
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void consentMappingCallback(Object config, String message, Boolean isRequired) {
        callbacks.add(new ConsentMappingCallback(JsonValue.json(config), message, isRequired));
    }

    /**
     * Construct and save into the list of callbacks a {@link ConsentMappingCallback}.
     *
     * @param name the name of the mapping
     * @param displayName the display name of the mapping
     * @param icon the icon spec for the mapping
     * @param accessLevel the access level description for the mapping
     * @param titles titles of the attributes shared by the mapping
     * @param message the message for the privacy and consent notice
     * @param isRequired whether all fields require consent
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void consentMappingCallback(String name, String displayName, String icon, String accessLevel,
                                       List<String> titles, String message, Boolean isRequired) {
        callbacks.add(new ConsentMappingCallback(name, displayName, icon, accessLevel, titles, message, isRequired));
    }

    /**
     * Construct and save into the list of callbacks a {@link DeviceProfileCallback}.
     *
     * @param metadata A boolean indicating whether to collect device metadata.
     * @param location A boolean indicating whether to collect the device location.
     * @param message A string containing optional text to display while collecting device information.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void deviceProfileCallback(Boolean metadata, Boolean location, String message) {
        callbacks.add(new DeviceProfileCallback(metadata, location, message));
    }

    /**
     * Construct and save into the list of callbacks a {@link KbaCreateCallback}.
     *
     * @param prompt A prompt to display to the user explaining what is expected of them
     * @param predefinedQuestions A list of localized, predefined questions the user can choose from
     * @param allowUserDefinedQuestions Whether to allow user defined questions.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void kbaCreateCallback(String prompt, List<String> predefinedQuestions, boolean allowUserDefinedQuestions) {
        callbacks.add(new KbaCreateCallback(prompt, predefinedQuestions, allowUserDefinedQuestions));
    }

    /**
     * Construct and save into the list of callbacks a {@link SelectIdPCallback}.
     *
     * @param providers Providers in a Json formatted object in json format
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void selectIdPCallback(Object providers) {
        callbacks.add(new SelectIdPCallback(JsonValue.json(providers)));
    }

    /**
     * Construct and save into the list of callbacks a {@link TermsAndConditionsCallback}.
     *
     * @param version the version of the Terms & Conditions
     * @param terms the Terms &amp; Conditions
     * @param createDate the date the Terms &amp; Conditions were created
     */
    @Supported(scriptingApi = true, javaApi = false)
    public void termsAndConditionsCallback(String version, String terms, String createDate) {
        callbacks.add(new TermsAndConditionsCallback(version, terms, createDate));
    }

}
