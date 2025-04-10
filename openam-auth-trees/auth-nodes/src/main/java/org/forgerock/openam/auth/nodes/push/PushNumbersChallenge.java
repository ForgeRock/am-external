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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Provides a set of numbers as challenge for Push notification.
 */
public class PushNumbersChallenge {

    private static final int SIZE = 3;
    private static final int START = 10;
    private static final int END = 100;

    private int[] choices;
    private int answer;

    /**
     * Default constructor.
     */
    public PushNumbersChallenge() {
        this.getNextChallenge();
    }

    /**
     * Create three random int values from 10 to 100 to be used as a challenge.
     * @return numbers challenge as String.
     */
    public String getNextChallenge() {
        this.choices = uniqueRandomElements();
        Arrays.sort(this.choices);

        this.answer = pickRandomAnswer(this.choices);

        return intArrayToString(this.choices);
    }

    /**
     * Retrieve the challenge numbers as int array.
     * @return choices as int array.
     */
    public int[] getChoices() {
        return this.choices;
    }

    /**
     * Retrieve the challenge answer.
     * @return answer to the numbers challenge.
     */
    public int getAnswer() {
        return this.answer;
    }

    /**
     * Creates an array with 3 unique random numbers between 10 (inclusive) to 100 (exclusive).
     * @return int array with 3 unique numbers.
     */
    private int[] uniqueRandomElements() {
        int[] array = new int[SIZE];
        List<Integer> numbers = IntStream.range(START, END).boxed().collect(Collectors.toList());
        Collections.shuffle(numbers);
        int pos = (new Random()).nextInt(numbers.size() - SIZE);
        for (int i = 0; i < SIZE; i++) {
            array[i] = numbers.get(i + pos);
        }
        return array;
    }

    private int pickRandomAnswer(int[] ints) {
        return ints[(new Random()).nextInt(ints.length)];
    }

    private String intArrayToString(int[] ints) {
        if (ints == null || ints.length == 0) {
            return null;
        }

        return Arrays.stream(ints)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
    }

}
