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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.doc.jwt.bearer;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.util.SignatureUtil;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.encode.Base64url;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.HashMap;

/**
 * Use a JWT as a bearer token to get an OAuth 2.0 access token.
 */
public final class Main {

    private static String clientId = "jwt-bearer-client";
    private static String password = "password";
    private static String serverUrl = null;

    /**
     * Use a JWT as a bearer token to get an OAuth 2.0 access token.
     *
     * @param args Command line arguments: OpenAM-Url
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println(getUsage());
            System.exit(-1);
        }

        serverUrl = args[0];

        String idToken = getIdToken(getEncodedHeaders(), getEncodedClaims(serverUrl));
        System.out.println("POSTing the following as a JWT bearer token:\n" + idToken);
        System.out.println();

        try {
            post(idToken);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static String getUsage() {
        String certificate = "-----BEGIN CERTIFICATE-----\n"
                + "MIIDETCCAfmgAwIBAgIEU8SXLjANBgkqhkiG9w0BAQsFADA5MRswGQYDVQQKExJvcGVuYW0uZXhh\n"
                + "bXBsZS5jb20xGjAYBgNVBAMTEWp3dC1iZWFyZXItY2xpZW50MB4XDTE0MTAyNzExNTY1NloXDTI0\n"
                + "MTAyNDExNTY1NlowOTEbMBkGA1UEChMSb3BlbmFtLmV4YW1wbGUuY29tMRowGAYDVQQDExFqd3Qt\n"
                + "YmVhcmVyLWNsaWVudDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAID4ZZ/DIGEBr4QC\n"
                + "2uz0GYFOCUlAPanxX21aYHSvELsWyMa7DJlD+mnjaF8cPRRMkhYZFXDJo/AVcjyblyT3ntqL+2Js\n"
                + "3D7TmS6BSjkxZWsJHyhJIYEoUwwloc0kizgSm15MwBMcbnksQVN5VWiOe4y4JMbi30t6k38lM62K\n"
                + "KtaSPP6jvnW1LTmL9uiqLWz54AM6hU3NlCI3J6Rfh8waBIPAEjmHZNquOl2uGgWumzubYDFJbomL\n"
                + "SQqO58RuKVaSVMwDbmENtMYWXIKQL2xTt5XAbwEQEgJ/zskwpA2aQt1HE6de3UymOhONhRiu4rk3\n"
                + "AIEnEVbxrvy4Ik+wXg7LZVsCAwEAAaMhMB8wHQYDVR0OBBYEFIuI7ejuZTg5tJsh1XyRopGOMBcs\n"
                + "MA0GCSqGSIb3DQEBCwUAA4IBAQBM/+/tYYVIS6LvPl3mfE8V7x+VPXqj/uK6UecAbfmRTrPk1ph+\n"
                + "jjI6nmLX9ncomYALWL/JFiSXcVsZt3/412fOqjakFVS0PmK1vEPxDlav1drnVA33icy1wORRRu5/\n"
                + "qA6mwDYPAZSbm5cDVvCR7Lt6VqJ+D0V8GABFxUw9IaX6ajTqkWhldY77usvNeTD0Xc4R7OqSBrnA\n"
                + "SNCaUlJogWyzhbFlmE9Ne28j4RVpbz/EZn0oc/cHTJ6Lryzsivf4uDO1m3M3kM/MUyXc1Zv3rqBj\n"
                + "TeGSgcqEAd6XlGXY1+M/yIeouUTi0F1bk1rNlqJvd57Xb4CEq17tVbGBm0hkECM8\n"
                + "-----END CERTIFICATE-----";

        return "Before trying this client, "
                + "configure a top-level realm OAuth 2.0 client profile\n"
                + "with client_id: " + clientId + ", "
                + "client_secret: " + password + ",\n"
                + "and Client JWT Bearer Public Key:\n\n" + certificate
                + "\n\n"
                + "Then to use this client, pass it the OpenAM Server URL\n"
                + "such as http://openam.example.com:8080/openam";
    }

    private static String getEncodedHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("alg", "RS256");
        return Base64url.encode(new JsonValue(headers).toString().getBytes());
    }

    private static String getEncodedClaims(String aud) {
        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put("iss", clientId);
        claims.put("sub", clientId);
        claims.put("aud", aud);
        claims.put("exp", Long.toString(System.currentTimeMillis() / 1000) + 600);
        return Base64url.encode(new JsonValue(claims).toString().getBytes());
    }

    private static String getIdToken(String header, String claims) {
        SignatureUtil signatureUtil = SignatureUtil.getInstance();
        RSASigningHandler rsaSigningHandler = new RSASigningHandler(getPrivateKey(), signatureUtil);
        String signature = Base64url.encode(rsaSigningHandler.sign(JwsAlgorithm.RS256, header + "." + claims));
        return header + "." + claims + "." + signature;
    }

    private static PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Main.class.getResourceAsStream("/keystore.jks"), "changeit".toCharArray());
            privateKey = (PrivateKey) keystore.getKey("self-signed", "changeit".toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (privateKey == null) {
            System.err.println("Failed to retrieve private key from keystore.jks.");
            System.exit(-1);
        }

        return privateKey;
    }

    private static void post(String idToken) throws Exception {
        URL token = new URL(serverUrl + "/oauth2/access_token");
        String grantType = URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8");
        String data = "grant_type=" + grantType + "&assertion=" + idToken;

        HttpURLConnection connection = (HttpURLConnection) token.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Authorization",
                "Basic " + Base64.encode((clientId + ":" + password).getBytes()));
        connection.setDoOutput(true);

        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.writeBytes(data);
        dataOutputStream.flush();
        dataOutputStream.close();

        int responseCode = connection.getResponseCode();
        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        if (responseCode == 200) {
            input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = input.readLine()) != null) {
            response.append(line);
        }
        input.close();

        System.out.println("Response code: " + responseCode);
        System.out.println(response.toString());
    }

    private Main() {
        // Prevent instantiation.
    }
}
