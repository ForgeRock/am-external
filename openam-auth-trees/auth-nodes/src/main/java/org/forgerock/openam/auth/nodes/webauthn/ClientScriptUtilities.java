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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.base64Decode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
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

    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private static final String NUMBER_ENCODING_DELIMITER = ",";
    private static final String WAITING_MESSAGE_KEY = "waiting";
    private static final String SPINNER_SCRIPT = "org/forgerock/openam/auth/nodes/webauthn/webauthn-spinner.js";
    private static final String BUNDLE = ClientScriptUtilities.class.getName().replace(".", "/");

    /** Delimits various sections in client's responses. */
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
     * @param coseAlgorithms the algorithms allowed.
     * @return the public key credential params for the browser API call.
     */
    String getPubKeyCredParams(Set<CoseAlgorithm> coseAlgorithms) {
        String entryTemplate = "{\n"
                + "            type: \"public-key\",\n"
                + "            alg: %1$s\n"
                + "        }\n";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (CoseAlgorithm coseAlgorithm : coseAlgorithms) {
            if (!first) {
                sb.append(",");
            }
            sb.append(String.format(entryTemplate, coseAlgorithm.getCoseNumber()));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Parses the response from the client authentication script.
     * @param encodedResponse the response as an encoded String.
     * @return the response as a rich data object.
     */
    ClientScriptResponse parseClientAuthenticationResponse(String encodedResponse) {
        ClientScriptResponse response = new ClientScriptResponse();
        String[] resultsArray = encodedResponse.split(RESPONSE_DELIMITER);
        response.setClientData(resultsArray[0]);
        response.setCredentialId(resultsArray[3]);
        response.setAuthenticatorData(getBytesFromNumberEncoding(resultsArray[1]));
        response.setSignature(getBytesFromNumberEncoding(resultsArray[2]));
        return response;
    }

    /**
     * Parses the response from the client registration script.
     * @param encodedResponse the response as an encoded String.
     * @return the reponse as a rich data object.
     */
    ClientScriptResponse parseClientRegistrationResponse(String encodedResponse) {
        ClientScriptResponse response = new ClientScriptResponse();
        String[] resultsArray = encodedResponse.split(RESPONSE_DELIMITER);
        response.setClientData(resultsArray[0]);
        response.setAttestationData(getBytesFromNumberEncoding(resultsArray[1]));
        response.setCredentialId(resultsArray[2]);
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
        String template = "{\n"
                + "     type: \"public-key\",\n"
                + "     id: new Int8Array(%1$s).buffer\n"
                + " }";
        String decodedId = Arrays.toString(base64Decode(credentialId));
        return String.format(template, decodedId);
    }
}
