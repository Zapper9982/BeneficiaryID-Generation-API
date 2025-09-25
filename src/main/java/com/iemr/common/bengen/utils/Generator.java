/*
* AMRIT - Accessible Medical Records via Integrated Technologies
* Integrated EHR (Electronic Health Records) Solution
*
* Copyright (C) "Piramal Swasthya Management and Research Institute"
*
* This file is part of AMRIT.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/.
*/
package com.iemr.common.bengen.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* 
*
* @author Sunil.K.Sundaram
*/
public class Generator {

    private static final Logger log = LoggerFactory.getLogger(Generator.class);
    private static final BigInteger TEN = BigInteger.TEN;
    private static final BigInteger TEN_POW_10 = TEN.pow(10);

    public BigInteger generateBeneficiaryId() {
        BigInteger bid1 = generateFirst();
        BigInteger bid2 = generateNumN(10);

        if (log.isDebugEnabled()) {
            log.debug("bid1: {} length: {}", bid1, getDigitCount(bid1));
            log.debug("bid2: {} length: {}", bid2, getDigitCount(bid2));
        }

        BigInteger bid = bid1.add(bid2).multiply(TEN);
        String checksum = Verhoeff.generateVerhoeff(bid.toString());

        if (log.isDebugEnabled()) {
            log.debug("bid: {} length: {} chsum: {}", bid, getDigitCount(bid), checksum);
        }

        bid = bid.add(new BigInteger(checksum));
        if (log.isDebugEnabled()) {
            log.debug("BENEFICIARY ID: {}", bid);
        }

        return bid;
    }

    public BigInteger generateFirst() {
        int digit = getRandomInRange(2, 9);
        return BigInteger.valueOf(digit).multiply(TEN_POW_10);
    }

    protected BigInteger generateNumN(int n) {
        int[] source = new int[n];
        int[] target = new int[n];

        for (int i = 0; i < n; i++) {
            source[i] = getRandomDigit();
        }

        for (int i = 0, j = n - 1; i < n; i++, j--) {
            int num = (j == 0) ? getRandomDigit() : getRandomDigit() % j;
            target[j] = source[i];
            source[i] = num;
        }

        StringBuilder sb = new StringBuilder(n);
        for (int value : target) {
            sb.append(value);
        }

        return new BigInteger(sb.toString());
    }

    public int getDigitCount(BigInteger number) {
        double factor = Math.log10(2);
        int digits = (int) (factor * number.bitLength() + 1);
        return (TEN.pow(digits - 1).compareTo(number) > 0) ? digits - 1 : digits;
    }

    private int getRandomDigit() {
        SecureRandom secureRandom = new SecureRandom();
        return secureRandom.nextInt(10);

    }

    private int getRandomInRange(int min, int max) {
    	SecureRandom sr = new SecureRandom();
    	if (min > max) {
    	    throw new IllegalArgumentException("min must be <= max");
    	}
    	if (max == Integer.MAX_VALUE) {
    	    return sr.nextInt(max - min) + min;
    	}
    	return sr.nextInt(min, max + 1); // safe here
    }

    // Optional: only if you need debugging arrays
    public void displayArrays(int[] arr1, int[] arr2) {
        if (!log.isDebugEnabled()) return;

        log.debug("myarr  : {}", intArrayToString(arr1));
        log.debug("myarr2 : {}", intArrayToString(arr2));
    }

    private String intArrayToString(int[] array) {
        StringBuilder sb = new StringBuilder(array.length);
        for (int value : array) {
            sb.append(value);
        }
        return sb.toString();
    }
}
