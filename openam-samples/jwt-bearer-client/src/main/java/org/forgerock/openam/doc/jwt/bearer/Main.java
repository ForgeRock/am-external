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
 * Copyright 2014-2020 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openam.doc.jwt.bearer;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.Date;

/**
 * Use a JWT as a bearer token to get an OAuth 2.0 access token.
 */
public final class Main {

    private static String clientId = "jwt-bearer-client";
    private static String tokenEndpoint = null;
    private static String serverUrl="http://openam.example.com:8080/openam";
    private static String authnEndpoint=null;
    private static String user="demo";
    private static String pass="Ch4ng31t";
    private static String sessionToken;
    private static String mode="pubkey";
    private static String keystorePath="/keystore.jks";
    private static String keystorePass="changeit";
    private static String keystoreAlias="self-signed";
    private static String keyPass="changeit";
    private static String keyId="SylLC6Njt1KGQktD9Mt+0zceQSU=";
    private static String jws=null;
    private static boolean shouldAuthn=false;
    private static boolean curlOut=false;


    /**
     * Use a JWT as a bearer token to get an OAuth 2.0 access token.
     *
     * @param args
     *            Command line arguments: OpenAM-serverUrl [[-mode jwks_uri] [-user user] [-password password] [-keystore path_to_keystore] [-alias keystore alias] [-keypass keystore password] [-kid kid_fromjwk]]
     *            If there is no mode parameter then assume the original non-jwks uri mode
     */
    public static void main(String[] args) {
        int i=0;
        String arg,argvalue;
        if (args.length == 0) {
            System.err.println(getUsage());
            System.exit(-1);
        }

        serverUrl = args[0];

        tokenEndpoint = serverUrl.replaceAll("/$", "") + "/oauth2/access_token";
        authnEndpoint = serverUrl.replaceAll("/$","") +"/json/authenticate";

        i=1;
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
            if (i<args.length)
            {
                argvalue=args[i++];
            }
            else {
                System.out.println ("Argument specified without value:" + arg+"; ignoring.");break;
            }

            switch(arg)
            {
                case "-mode":
                     mode=argvalue;
                break;
                case "-authn":shouldAuthn=(argvalue.toLowerCase().equals("true"));
                    break;
                case "-user":user=argvalue;
                break;
                case "-password":pass=argvalue;
                break;
                case "-keystore":keystorePath=argvalue;
                    break;
                case "-keystorepass":keystorePass=argvalue;
                    break;
                case "-kid":keyId=argvalue;
                    break;
                case "-alias":keystoreAlias=argvalue;
                    break;
                case "-keypass":keyPass=argvalue;
                    break;
                case "-clientname":clientId=argvalue;
                    break;
                case "-curlout":curlOut=(argvalue.toLowerCase().equals("true"));
                    break;

                default:System.out.println("Unknown argument: "+arg+";ignoring");
                    break;

            }


        }
        switch (mode)
        {
            case "pubkey":
                jws = getJws(tokenEndpoint);
                System.out.println("\nPOSTing the following as a JWT bearer token:\n"
                        + jws);
                System.out.println();

                break;
            case "jwks_uri":
                shouldAuthn=true; //this may be scope dependent
                jws = getJwsForJwk(tokenEndpoint);
                System.out.println("\nPOSTing the following as a JWT bearer token including kid value:\n"
                        + jws);
                System.out.println();

                break;
            default:
                System.out.println("Unknown mode: "+ mode);
                System.err.println(getUsage());
                System.exit(-1);
        }
        try {
            if(shouldAuthn) {
                sessionToken = authenticate(user, pass);
                post(jws, sessionToken);
            } else
            {
                post(jws);
            }
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

        return "Usage: OpenAM-serverUrl [-mode pubkey|jwks_uri] [-authn true|false] [-clientname clientname] [-user user] [-password password] [-keystore path_to_keystore] [-keystorepass keystorepass] [-alias keystore alias] [-keypass keypassword] [-kid kid_fromjwk] [-curlout true|false]\n\n" +
                "Default mode is pubkey with no authentication. \n Before trying this client, "
                + "configure a top-level realm OAuth 2.0 client profile\n"
                + "with client_id: " + clientId + ", "
                + "For pubkey mode, configure Client JWT Bearer Public Key Certificate:\n\n" + certificate + " or that generated from your custom keystore and also use the X509 option for the client.\n\n"
                + "Then to use this client, pass it the OpenAM Server URL\n"
                + "such as "+ serverUrl + "n\n"
                +"For jwks_uri mode, you can use the keystore from OpenAM confighome/openam/keystore.jks. If you are replacing the current keystore then copy it into the src/resources folder" +
                " Also remember that this will take a different alias (default test). Configure the url "+serverUrl+"/oauth2/connect/jwk_uri as the jwks_uri and also specify that you are using the jwks uri and RS256 in the oauth2 provider and client. openid should be in the scope. the private key should be used" +
                "example for jwks: http://openam.example.com:18080/openam -mode jwks_uri  -keystore /jwks/keystore.jks -keystorepass changeit -alias test\n" +
                        "-curlout true results in outputting the equivalent curl command.";
    }

    private static String getJws(String tokenEndpoint) {
        Date exp = new Date(System.currentTimeMillis() + 1000 * 60 * 5);

        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        return jwtBuilderFactory
                .jws(new SigningManager().newRsaSigningHandler(getPrivateKey()))
                .headers()
                .alg(JwsAlgorithm.RS256)
                .done()
                // TODO OPENAM-17093
                .claims(jwtBuilderFactory.claims().iss(clientId).sub(clientId)
                        .aud(Collections.singletonList(tokenEndpoint)).exp(exp)
                        .build()).build();
    }
    private static String getJwsForJwk(String tokenEndpoint) {
        Date exp = new Date(System.currentTimeMillis() + 1000 * 60 * 5);

        JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
        return jwtBuilderFactory
                .jws(new SigningManager().newRsaSigningHandler(getPrivateKey()))
                .headers()
                .alg(JwsAlgorithm.RS256)
                .kid(keyId) //get this from the jwk
                .done()
                // TODO OPENAM-17093
                .claims(jwtBuilderFactory.claims().iss(clientId).sub(clientId)
                        .aud(Collections.singletonList(tokenEndpoint)).exp(exp)
                        .build()).build();
    }

    private static PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Main.class.getResourceAsStream(keystorePath),
                    keystorePass.toCharArray());
            privateKey = (PrivateKey) keystore.getKey(keystoreAlias,
                    keyPass.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (privateKey == null) {
            System.err
                    .println("Failed to retrieve private key from "+keystorePath);
            System.exit(-1);
        }

        return privateKey;
    }

    private static String authenticate(String username,String password) throws Exception{

        System.out.println("Authenticating as user:"+username+";endpoint:"+authnEndpoint);
        URL authn= new URL(authnEndpoint);
        HttpURLConnection connection = (HttpURLConnection) authn
                .openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/json");
        connection.setRequestProperty("X-OpenAM-Username",username);
        connection.setRequestProperty("X-OpenAM-Password",password);
        connection.setDoOutput(true);


      /*  $ curl \
        --request POST \
        --header "Content-Type: application/json" \
        --header "X-OpenAM-Username: demo" \
        --header "X-OpenAM-Password: Ch4ng31t" \
        https://openam.example.com:8443/openam/json/authenticate */

        DataOutputStream dataOutputStream = new DataOutputStream(
                connection.getOutputStream());
        dataOutputStream.writeBytes("{}");
        dataOutputStream.flush();
        dataOutputStream.close();

        int responseCode = connection.getResponseCode();

        BufferedReader input;
        if (responseCode == 200) {
            input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
        } else {
            input = new BufferedReader(new InputStreamReader(
                    connection.getErrorStream()));
        }

        //Need to parse json response
        StringBuilder response = new StringBuilder();
        if (input != null) {
            String line;
            while ((line = input.readLine()) != null) {
                response.append(line);
            }
            input.close();
        } else {
            response.append("No input stream from reader.");
        }
        String tokenID=parseTokenId(response.toString());

        System.out.println("Response code: " + responseCode);
        return tokenID;

    }

    private static String parseTokenId(String publishResponse) throws Exception {
        java.util.Map responseContent;
        try {
            com.fasterxml.jackson.core.JsonParser parser =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .getFactory()
                            .createParser(publishResponse);
            responseContent = parser.readValueAs(java.util.Map.class);
        } catch (IOException e) {
            throw new Exception("Could not map the response from the PublishService to a json object. The response: "
                    + publishResponse + "; The exception: " + e);
        }
        return responseContent.get("tokenId").toString();

    }
    private static void post(String idToken) throws Exception {
        URL token = new URL(tokenEndpoint);

        HttpURLConnection connection = (HttpURLConnection) token
                .openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setDoOutput(true);




        // This client is of type confidential, so authentication is required
        // according to http://tools.ietf.org/html/rfc6749#section-3.2.1
        String clientAssertionType = URLEncoder.encode(
                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "UTF-8");
        String grantType = URLEncoder.encode(
                "urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8");
        String data = "client_assertion_type=" + clientAssertionType
                + "&client_assertion=" + idToken + "&grant_type=" + grantType
                + "&assertion=" + idToken;

        DataOutputStream dataOutputStream = new DataOutputStream(
                connection.getOutputStream());
       if(curlOut) {
           System.out.print("curl --request POST -H 'Content-Type=application/x-www-form-urlencoded' ");

           System.out.print("-d '" + data + "\' ");
           System.out.println("'" + tokenEndpoint + "'");
       }
        dataOutputStream.writeBytes(data);
        dataOutputStream.flush();
        dataOutputStream.close();

        int responseCode = connection.getResponseCode();

        BufferedReader input;
        if (responseCode == 200) {
            input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
        } else {
            input = new BufferedReader(new InputStreamReader(
                    connection.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        if (input != null) {
            String line;
            while ((line = input.readLine()) != null) {
                response.append(line);
            }
            input.close();
        } else {
            response.append("No input stream from reader.");
        }

        System.out.println("Response code: " + responseCode);
        System.out.println(response.toString());
    }

    private static void post(String idToken,String sessionToken) throws Exception {
        URL token = new URL(tokenEndpoint);

        HttpURLConnection connection = (HttpURLConnection) token
                .openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setRequestProperty("Cookie","iPlanetDirectoryPro="+sessionToken);

        connection.setDoOutput(true);



        // This client is of type confidential, so authentication is required
        // according to http://tools.ietf.org/html/rfc6749#section-3.2.1
        String clientAssertionType = URLEncoder.encode(
                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "UTF-8");
        String grantType = URLEncoder.encode(
                "urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8");
        String data = "client_assertion_type=" + clientAssertionType
                + "&client_assertion=" + idToken + "&grant_type=" + grantType
                + "&assertion=" + idToken;

        DataOutputStream dataOutputStream = new DataOutputStream(
                connection.getOutputStream());
        if (curlOut) {
            System.out.print("curl --request POST -H 'Content-Type=application/x-www-form-urlencoded' ");
            System.out.print("-H 'Cookie:iPlanetDirectoryPro=" + sessionToken + "' ");
            System.out.print("-d '" + data + "\' ");
            System.out.print("'" + tokenEndpoint + "'");
        }
        dataOutputStream.writeBytes(data);
        dataOutputStream.flush();
        dataOutputStream.close();

        int responseCode = connection.getResponseCode();

        BufferedReader input;
        if (responseCode == 200) {
            input = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
        } else {
            input = new BufferedReader(new InputStreamReader(
                    connection.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        if (input != null) {
            String line;
            while ((line = input.readLine()) != null) {
                response.append(line);
            }
            input.close();
        } else {
            response.append("No input stream from reader.");
        }

        System.out.println("Response code: " + responseCode);
        System.out.println(response.toString());
    }

    private Main() {
        // Prevent instantiation.
    }
}
