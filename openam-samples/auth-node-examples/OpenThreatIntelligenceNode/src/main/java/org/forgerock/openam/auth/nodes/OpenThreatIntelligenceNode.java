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
 * Copyright 2017 ForgeRock AS.
 */
/*
 * simon.moffatt@forgerock.com
 *
 * Checks for the presence of the named cookie in the authentication request.  Doesn't check cookie value, only presence
 */

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ResourceBundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;



@Node.Metadata(outcomeProvider = OpenThreatIntelligenceNode.OutcomeProvider.class,
        configClass = OpenThreatIntelligenceNode.Config.class)
public class OpenThreatIntelligenceNode implements Node {

    private final static String TRUE_OUTCOME_ID = "true";
    private final static String FALSE_OUTCOME_ID = "false";
    private final static String DEBUG_FILE = "OpenThreatIntelligenceNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    public interface Config {

    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public OpenThreatIntelligenceNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        debug.message("[" + DEBUG_FILE + "]: Started");

        //Pull out clientIP
	    String clientIP = context.request.clientIp;
        debug.message("[" + DEBUG_FILE + "]: client IP found as :" + clientIP);

        //Create sha256 hash of the IP....
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] clientIPHash = digest.digest(clientIP.getBytes(StandardCharsets.UTF_8));
        StringBuffer hexString = new StringBuffer();

        for (int i = 0; i < clientIPHash.length; i++) {
            String hex = Integer.toHexString(0xff & clientIPHash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        String clientIPAsAString = hexString.toString();

        debug.message("[" + DEBUG_FILE + "]: sha256 hash of client IP created as :" + clientIPAsAString);

        //Call helper function to see if IP hash is known
        return isIPMalicious(clientIPAsAString);


    }

    private Action isIPMalicious(String ipHash) {
        String json = "";

        try {

            URL url = new URL("https://api.cymon.io/v2/ioc/search/sha256/" + ipHash);
            debug.message("[" + DEBUG_FILE + "]: Sending request to OTT as " + url);

            //Build HTTP request
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.33 Safari/537.36");
            if (conn.getResponseCode() == 404) {
                debug.message("[" + DEBUG_FILE + "]: response 404 - no breaches found");
                return goTo(false).build();
            }
            if (conn.getResponseCode() != 200) {
                debug.message("[" + DEBUG_FILE + "]: HTTP failed, response code:" + conn.getResponseCode());
                throw new RuntimeException("[" + DEBUG_FILE + "]: HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                json = json + output;
            }

            conn.disconnect();

            debug.message("[" + DEBUG_FILE + "]: response from OTT: " + json);

            JsonValue otiResponseObj = JsonValueBuilder.toJsonValue(json);

            debug.message("[" + DEBUG_FILE + "]: response from OTT as JSON: " + otiResponseObj);

            JsonValue total = otiResponseObj.get("total");

            debug.message("[" + DEBUG_FILE + "]: total from OTT: " + total);


            //0 in total attribute means no matches so send to false/Non-Malicious
            if (total.asInteger().equals(0)) {

                debug.message("[" + DEBUG_FILE + "]: IP not from known malicious host");
                return goTo(false).build();

            } else {

                debug.message("[" + DEBUG_FILE + "]: IP from known malicious host");
                return goTo(true).build();
            }



        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return goTo(true).build();
    }


    private Action.ActionBuilder goTo(boolean outcome) {
        return Action.goTo(outcome ? TRUE_OUTCOME_ID : FALSE_OUTCOME_ID);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = OpenThreatIntelligenceNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString("true")),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString("false")));
        }
    }
}
