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
 * Copyright 2023-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.authentication.callbacks.BooleanAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.ConsentMappingCallback;
import org.forgerock.openam.authentication.callbacks.DeviceProfileCallback;
import org.forgerock.openam.authentication.callbacks.IdPCallback;
import org.forgerock.openam.authentication.callbacks.KbaCreateCallback;
import org.forgerock.openam.authentication.callbacks.NumberAttributeInputCallback;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * List of Callbacks wrapper for scripting.
 * This class is responsible for hiding the java classes of different callback types and provides simple methods
 * for accessing data of the callbacks arriving into AM.
 */
public class ScriptedCallbacksWrapper {

    private final List<? extends Callback> callbacks;

    /**
     * Default constructor.
     *
     * @param callbacks The list of available callbacks
     */
    public ScriptedCallbacksWrapper(List<? extends Callback> callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Returns false if no callbacks arrived in AM.
     * @return boolean value of any callbacks stored.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean isEmpty() {
        return callbacks.isEmpty();
    }

    /**
     * Getter for ChoiceCallback type callbacks.
     *
     * @return List of the ChoiceCallbacks selected indexes.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<int[]> getChoiceCallbacks() {
        return getCallbackByType(ChoiceCallback.class).stream()
                .map(ChoiceCallback::getSelectedIndexes)
                .collect(Collectors.toList());
    }

    /**
     * Getter for NameCallback type callbacks.
     *
     * @return List of the NameCallbacks return values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getNameCallbacks() {
        return getCallbackByType(NameCallback.class).stream()
                .map(c -> String.valueOf(c.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Getter for PasswordCallback type callbacks.
     *
     * @return List of the PasswordCallbacks return values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getPasswordCallbacks() {
        return getCallbackByType(PasswordCallback.class).stream()
                .map(c -> String.valueOf(c.getPassword()))
                .collect(Collectors.toList());
    }

    /**
     * Getter for HiddenValueCallback type callbacks.
     *
     * @return Map of the HiddenValueCallbacks by id - value pairs.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public Map<String, String> getHiddenValueCallbacks() {
        return getCallbackByType(HiddenValueCallback.class).stream()
                .collect(Collectors.toMap(HiddenValueCallback::getId, HiddenValueCallback::getValue));
    }

    /**
     * Getter for TextInputCallback type callbacks.
     *
     * @return List of the TextInputCallbacks return values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getTextInputCallbacks() {
        return getCallbackByType(TextInputCallback.class).stream()
                .map(TextInputCallback::getText)
                .collect(Collectors.toList());
    }

    /**
     * Getter for NumberAttributeInputCallback type callbacks.
     *
     * @return List of the NumberAttributeCallbacks return values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getStringAttributeInputCallbacks() {
        return getCallbackByType(StringAttributeInputCallback.class).stream()
                .map(StringAttributeInputCallback::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Getter for NumberAttributeInputCallback type callbacks.
     *
     * @return List of the NumberAttributeCallbacks return values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Double> getNumberAttributeInputCallbacks() {
        return getCallbackByType(NumberAttributeInputCallback.class).stream()
                .map(NumberAttributeInputCallback::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Getter for BooleanAttributeInputCallback type callbacks.
     *
     * @return List of the BooleanAttributeInputCallbacks return values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Boolean> getBooleanAttributeInputCallbacks() {
        return getCallbackByType(BooleanAttributeInputCallback.class).stream()
                .map(BooleanAttributeInputCallback::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Getter for ConfirmationCallback type callbacks.
     *
     * @return List of the ConfirmationCallbacks selected values.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Integer> getConfirmationCallbacks() {
        return getCallbackByType(ConfirmationCallback.class).stream()
                .map(ConfirmationCallback::getSelectedIndex)
                .collect(Collectors.toList());
    }

    /**
     * Getter for LanguageCallback type callbacks.
     *
     * @return List of the LanguageCallbacks selected locales.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getLanguageCallbacks() {
        return getCallbackByType(LanguageCallback.class).stream()
                .map(c -> c.getLocale().toString())
                .collect(Collectors.toList());
    }

    /**
     * Getter for IdPCallback type callbacks.
     * Available values are: nodeName - String
     *                       provider - String
     *                       clientId - String
     *                       redirectUri - String
     *                       scope - List<String>
     *                       nonce - String
     *                       request - String
     *                       requestUri - String
     *                       acrValues - List<String>
     *                       token - String
     *                       tokenType - String
     *                       userInfo - String
     *                       requestNativeAppForUserInfo - boolean
     *
     * @return List of the IdPCallbacks data as a map.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, Object>> getIdpCallbacks() {
        return getCallbackByType(IdPCallback.class).stream()
                .map(c -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("nodeName", IdPCallback.getNodeName());
                    data.put("provider", c.getProvider());
                    data.put("clientId", c.getClientId());
                    data.put("redirectUri", c.getRedirectUri());
                    data.put("scope", c.getScope());
                    data.put("nonce", c.getNonce());
                    data.put("request", c.getRequest());
                    data.put("requestUri", c.getRequestUri());
                    data.put("acrValues", c.getAcrValues());
                    data.put("token", c.getToken());
                    data.put("tokenType", c.getTokenType());
                    data.put("userInfo", c.getUserInfo());
                    data.put("requestNativeAppForUserInfo", c.isRequestNativeAppForUserInfo());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Getter for ValidatePasswordCallback type callbacks.
     *
     * @return List of the ValidatePasswordCallbacks return values.
     * Available values are: value - the password as String
     *                       validateOnly - When true, this lets the UI validate input as the user types instead of
     *                                      validating the input once and continuing the journey to the next node.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, Object>> getValidatedPasswordCallbacks() {
        return getCallbackByType(ValidatedPasswordCallback.class).stream()
                .map(c -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("value", String.valueOf(c.getPassword()));
                    data.put("validateOnly", c.validateOnly());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Getter for ValidatedUsernameCallback type callbacks.
     *
     * @return List of the ValidatedUsernameCallbacks return values.
     * Available values are: value - the username as String
     *                       validateOnly - When true, this lets the UI validate input as the user types instead of
     *                                      validating the input once and continuing the journey to the next node.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, Object>> getValidatedUsernameCallbacks() {
        return getCallbackByType(ValidatedUsernameCallback.class).stream()
                .map(c -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("value", c.getUsername());
                    data.put("validateOnly", c.validateOnly());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Getter for HttpCallback type callbacks.
     *
     * @return List of the HttpCallbacks authorization token.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getHttpCallbacks() {
        return getCallbackByType(HttpCallback.class).stream()
                .map(HttpCallback::getAuthorization)
                .collect(Collectors.toList());
    }

    /**
     * Getter for X509CertificateCallback type callbacks.
     *
     * @return List of the X509CertificateCallback multiple data in Map format.
     * Avilable values are: certificate - X509Certificate
     *                      clientId - String
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, Object>> getX509CertificateCallbacks() {
        return getCallbackByType(X509CertificateCallback.class).stream()
                .map(c -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("certificate", c.getCertificate());
                    data.put("signature", Arrays.toString(c.getSignature()));
                    data.put("reqSignature", c.getReqSignature());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Getter for ConsentMappingCallback type callbacks.
     *
     * @return List of the ConsentMappingCallbacks boolean return value.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Boolean> getConsentMappingCallbacks() {
        return getCallbackByType(ConsentMappingCallback.class).stream()
                .map(ConsentMappingCallback::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Getter for DeviceProfileCallback type callbacks.
     *
     * @return List of the DeviceProfileCallbacks device information as Json value represented in a String.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<String> getDeviceProfileCallbacks() {
        return getCallbackByType(DeviceProfileCallback.class).stream()
                .map(DeviceProfileCallback::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Getter for KbaCreateCallback type callbacks.
     *
     * @return List of the KbaCreateCallback information return value in a map format.
     *  values are: "selectedQuestion"
     *              "selectedAnswer"
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, String>> getKbaCreateCallbacks() {
        return getCallbackByType(KbaCreateCallback.class).stream()
                .map(c -> {
                    Map<String, String> data = new HashMap<>();
                    data.put("selectedQuestion", c.getSelectedQuestion());
                    data.put("selectedAnswer", c.getSelectedAnswer());
                    return data;
                })
                .collect(Collectors.toList());
    }

    /**
     * Getter for SelectIdPCallback type callbacks.
     *
     * @return List of the SelectIdPCallback with choices of an enabled social identity provider or local
     * authentication Json represented as a Map.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Map<String, Object>> getSelectIdPCallbacks() {
        return getCallbackByType(SelectIdPCallback.class).stream()
                .map(c -> c.getProviders().asMap())
                .collect(Collectors.toList());
    }

    /**
     * Getter for TermsAndConditionsCallback type callbacks.
     *
     * @return List of the TermsAndConditionsCallback accepted boolean.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<Boolean> getTermsAndConditionsCallbacks() {
        return getCallbackByType(TermsAndConditionsCallback.class).stream()
                .map(TermsAndConditionsCallback::getAccept)
                .collect(Collectors.toList());
    }

    private <T> List<T> getCallbackByType(Class<T> klazz) {
        return callbacks.stream()
                .filter(klazz::isInstance)
                .map(klazz::cast)
                .collect(Collectors.toList());
    }
}
