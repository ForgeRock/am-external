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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.base64UrlDecode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class with shared code for the client scripts used in webauthn.
 */
@Singleton
public final class ClientScriptUtilities {

    private static final int DEVICE_NAME_INDEX = 3;
    private static final int ENCODED_RESPONSE_LENGTH = 4;
    private static final int CLIENT_DATA_INDEX = 0;
    private static final int AUTHENTICATOR_DATA_INDEX = 1;
    private static final int CREDENTIAL_ID_INDEX = 3;
    private static final int SIGNATURE_INDEX = 2;
    private static final int USER_HANDLE_INDEX = 4;
    private final Logger logger = LoggerFactory.getLogger(ClientScriptUtilities.class);
    private static final String NUMBER_ENCODING_DELIMITER = ",";
    private static final String WAITING_MESSAGE_KEY = "waiting";
    private static final String SPINNER_SCRIPT = "org/forgerock/openam/auth/nodes/webauthn/webauthn-spinner.js";
    private static final String BUNDLE = ClientScriptUtilities.class.getName();

    /**
     * Delimits various sections in client's responses.
     */
    static final String RESPONSE_DELIMITER = "::";

    /**
     * Gets a JavaScript script as a String.
     *
     * @param scriptFileName the filename of the script.
     * @return the script as an executable string.
     * @throws NodeProcessException if the file doesn't exist.
     */
    public String getScriptAsString(String scriptFileName) throws NodeProcessException {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
        String script;
        try {
            script = IOUtils.toString(resourceStream, "UTF-8");
        } catch (IOException e) {
            logger.error("Failed to get the script, fatal error!", e);
            throw new NodeProcessException(e);
        }
        return script;
    }

    /**
     * Gets the generic spinner script as a localized String.
     *
     * @param locales the locale used for translation.
     * @return the spinner script as an executable String.
     * @throws NodeProcessException if the file doesn't exist.
     */
    String getSpinnerScript(PreferredLocales locales) throws NodeProcessException {
        ResourceBundle bundle = locales
                .getBundleInPreferredLocale(BUNDLE, AbstractDecisionNode.OutcomeProvider.class.getClassLoader());
        String spinnerScript = getScriptAsString(SPINNER_SCRIPT);
        return String.format(spinnerScript, bundle.getString(WAITING_MESSAGE_KEY));
    }

    /**
     * Get the public key credential parameters as a JavaScript String.
     *
     * @param coseAlgorithms the algorithms allowed.
     * @return the public key credential params for the browser API call.
     */
    String getPubKeyCredParams(Set<CoseAlgorithm> coseAlgorithms) {
        JsonValue jsonValue = getPubKeyCredParamsAsJson(coseAlgorithms);
        return jsonValue.toString();
    }

    JsonValue getPubKeyCredParamsAsJson(Set<CoseAlgorithm> coseAlgorithms) {
        List<Object> array = array();
        for (CoseAlgorithm coseAlgorithm : coseAlgorithms) {
            array.add(object(field("type", "public-key"), field("alg", coseAlgorithm.getCoseNumber())));
        }
        return json(array);
    }


    /**
     * Parses the response from the client authentication script.
     *
     * @param encodedResponse       the response as an encoded String.
     * @param useSuppliedUserHandle whether to parse the user handle as the user identifier
     * @return the response as a rich data object.
     */
    ClientAuthenticationScriptResponse parseClientAuthenticationResponse(String encodedResponse,
            boolean useSuppliedUserHandle) {
        String[] resultsArray = encodedResponse.split(RESPONSE_DELIMITER);
        return new ClientAuthenticationScriptResponse(resultsArray[CLIENT_DATA_INDEX],
                getBytesFromNumberEncoding(resultsArray[AUTHENTICATOR_DATA_INDEX]),
                resultsArray[CREDENTIAL_ID_INDEX],
                getBytesFromNumberEncoding(resultsArray[SIGNATURE_INDEX]),
                useSuppliedUserHandle ? new String(base64UrlDecode(resultsArray[USER_HANDLE_INDEX])) : null);
    }

    /**
     * Parses the response from the client registration script.
     *
     * @param encodedResponse the response as an encoded String.
     * @return the response as a rich data object.
     */
    ClientRegistrationScriptResponse parseClientRegistrationResponse(String encodedResponse) {
        String[] resultsArray = encodedResponse.split(RESPONSE_DELIMITER);
        String deviceName = null;
        //The device name is optional
        if (resultsArray.length == ENCODED_RESPONSE_LENGTH) {
            deviceName = resultsArray[DEVICE_NAME_INDEX];
        }
        ClientRegistrationScriptResponse response = new ClientRegistrationScriptResponse(resultsArray[0],
                getBytesFromNumberEncoding(resultsArray[1]), resultsArray[2], deviceName);
        return response;
    }

    private byte[] getBytesFromNumberEncoding(String data) {
        String[] numbers = data.split(NUMBER_ENCODING_DELIMITER);
        byte[] results = new byte[numbers.length];

        for (int i = 0; i < numbers.length; i++) {
            results[i] = Byte.parseByte(numbers[i]);
        }

        return results;
    }

    JsonValue getDevicesAsJson(List<WebAuthnDeviceSettings> devices) {
        List<JsonValue> result = new ArrayList<>();

        for (WebAuthnDeviceSettings device : devices) {
            result.add(getDeviceAsJson(device));
        }
        return json(result);
    }


    String getDevicesAsJavaScript(List<WebAuthnDeviceSettings> devices) {
        StringBuilder sb = new StringBuilder();

        for (WebAuthnDeviceSettings device : devices) {
            sb.append(getDeviceAsJavaScript(device)).append(",");
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String getDeviceAsJavaScript(WebAuthnDeviceSettings authenticatorEntry) {
        String credentialId = authenticatorEntry.getCredentialId();
        String decodedId = Arrays.toString(base64UrlDecode(credentialId));

        JsonValue js = json(object(
                field("type", "public-key"),
                field("id", "{ID_REPLACE}")));

        return js.toString().replace("\"{ID_REPLACE}\"", "new Int8Array(" + decodedId + ").buffer");
    }

    private JsonValue getDeviceAsJson(WebAuthnDeviceSettings authenticatorEntry) {
        byte[] bytes = base64UrlDecode(authenticatorEntry.getCredentialId());
        Byte[] result = ArrayUtils.toObject(bytes);

        return json(object(
                field("type", "public-key"),
                field("id", array((Object[]) result))));

    }
}
