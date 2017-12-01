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
/**
 * jon.knight@forgerock.com
 *
 * A node that returns true if the user's email address is recorded as breached by the HaveIBeenPwned website (http://haveibeenpwned.com)
 * or false if no breach has been recorded
 */


package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.selfservice.core.crypto.CryptoService;
import org.forgerock.selfservice.core.crypto.JsonCryptoException;

import javax.inject.Inject;
import javax.security.auth.callback.NameCallback;
import java.util.Optional;
import java.util.Set;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;


/**
* A KBA node. Asks a random question from self-service KBA questions in user profile.
*
*/
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
       configClass = KBANode.Config.class)
public class KBANode extends AbstractDecisionNode {

   /**
    * Configuration for the node.
    */
   public interface Config {
   }

   private final Config config;
   private final CoreWrapper coreWrapper;
   private final static String DEBUG_FILE = "KBANode";
   protected Debug debug = Debug.getInstance(DEBUG_FILE);


   /**
    * Guice constructor.
    *
    * @param config The service config for the node.
    * @throws NodeProcessException If there is an error reading the configuration.
    */
   @Inject
   public KBANode(@Assisted Config config, CoreWrapper coreWrapper)
           throws NodeProcessException {
       this.config = config;
       this.coreWrapper = coreWrapper;
   }


   @Override
   public Action process(TreeContext context) throws NodeProcessException {

       // If a callback exists then this is a response from a user
       if (context.hasCallbacks()) {
           CryptoService cryptoService = new CryptoService();
           // Retrieve question from shared state
           JsonValue contents = JsonValueBuilder.toJsonValue(context.sharedState.get("kbaInfo").asString());

           // Get user's answer and compare with hashed value
           Optional<String> answer = context.getCallback(NameCallback.class).map(NameCallback::getName);
           if (answer.isPresent()) {
               try {
                   if (cryptoService.matches(answer.get().toLowerCase(), contents.get("answer")))
                       return goTo(true).build();
               } catch (JsonCryptoException e) {
                   e.printStackTrace();
               }
           }
           return goTo(false).build();

       } else {
           // Retrieve users KBA answers from repo and store a random one in shared state
           AMIdentity userIdentity = coreWrapper.getIdentity(context.sharedState.get(USERNAME).asString(),context.sharedState.get(REALM).asString());
           debug.message("[" + DEBUG_FILE + "]: Looking for profile attribute kbaInfo");
           String kbaInfo = "";
           try {
               Set<String> idAttrs = userIdentity.getAttribute("kbaInfo");
               if (idAttrs == null || idAttrs.isEmpty()) {
                   debug.error("[" + DEBUG_FILE + "]: " + "Unable to find kbaInfo attribute");
               } else {
                   int index = (int) (Math.random() * idAttrs.size());
                   int i = 0;
                   for (String str: idAttrs) {
                       if (i == index) kbaInfo = str;
                       i++;
                   }
               }
           } catch (IdRepoException e) {
               debug.error("[" + DEBUG_FILE + "]: " + " Error retrieving profile atttribute '{}' ", e);
           } catch (SSOException e) {
               debug.error("[" + DEBUG_FILE + "]: " + "Node exception", e);
           }

           // Store selects KBA in shared state for next iteration to validate
           debug.message("[" + DEBUG_FILE + "]: Random select kba = " + kbaInfo);
           JsonValue newSharedState = context.sharedState.copy();
           newSharedState.put("kbaInfo", kbaInfo);
           return send(new NameCallback(getQuestion(kbaInfo))).replaceSharedState(newSharedState).build();
       }
   }

   // Gets question prompt string from KBA. Custom questions are stored in user profile, stock questions are stored here.
   public String getQuestion(String kbaInfo) {
       JsonValue contents = JsonValueBuilder.toJsonValue(kbaInfo);
       if (contents.isDefined("customQuestion")) return contents.get("customQuestion").asString();
       switch (Integer.parseInt(contents.get("questionId").asString())) {
           case 1:
               return("What is the name of your favourite restaurant?");
           case 2:
               return("What was the model of your first car?");
           case 3:
                return("What was the name of your childhood pet?");
           case 4:
               return("What is your mother's maiden name?");
       }
        return "unknown";
   }

}

