/**********************************************************************************************************************
 * garbagecat                                                                                                         *
 *                                                                                                                    *
 * Copyright (c) 2008-2016 Red Hat, Inc.                                                                              *
 *                                                                                                                    * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse *
 * Public License v1.0 which accompanies this distribution, and is available at                                       *
 * http://www.eclipse.org/legal/epl-v10.html.                                                                         *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Red Hat, Inc. - initial API and implementation                                                                  *
 *********************************************************************************************************************/
package org.eclipselabs.garbagecat.util.jdk;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Math utility methods and constants for OpenJDK and Sun JDK.
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class JdkMath {

    /**
     * Make default constructor private so the class cannot be instantiated.
     */
    private JdkMath() {

    }

    /**
     * Convert seconds to milliseconds.
     * 
     * For example: Convert 0.0225213 to 23.
     * 
     * @param secs
     *            Seconds as a whole number or decimal.
     * @return Milliseconds rounded to a whole number.
     */
    public static BigDecimal convertSecsToMillis(String secs) {
        // BigDecimal does not accept decimal commas, only decimal periods
        BigDecimal duration = new BigDecimal(secs.replace(",", "."));
        duration = duration.movePointRight(3);
        // Round down to avoid TimeWarpExceptions when events are spaced close together
        duration = duration.setScale(0, RoundingMode.DOWN);
        return duration;
    }

    /**
     * Convert milliseconds to seconds.
     * 
     * For example: Convert 123456 123.456.
     * 
     * @param millis
     *            Milliseconds as a whole number.
     * @return Seconds rounded to 3 decimal places.
     */
    public static BigDecimal convertMillisToSecs(long millis) {
        BigDecimal duration = new BigDecimal(millis);
        duration = duration.movePointLeft(3);
        duration = duration.setScale(3, RoundingMode.HALF_EVEN);
        return duration;
    }

    /**
     * Convert seconds to microseconds.
     * 
     * For example: Convert 0.0225213 to 23521
     * 
     * @param secs
     *            Seconds as a whole number or decimal.
     * @return Microseconds rounded to a whole number.
     */
    public static BigDecimal convertSecsToMicros(String secs) {
        // BigDecimal does not accept decimal commas, only decimal periods
        BigDecimal duration = new BigDecimal(secs.replace(",", "."));
        duration = duration.movePointRight(6);
        // Round down to avoid TimeWarpExceptions when events are spaced close together
        duration = duration.setScale(0, RoundingMode.DOWN);
        return duration;
    }

    /**
     * Convert microseconds to milliseconds.
     * 
     * For example: Convert 987654321 987.654.
     * 
     * @param micros
     *            Microseconds as a whole number.
     * @return Milliseconds rounded to 3 decimal places.
     */
    public static BigDecimal convertMicrosToMillis(long micros) {
        BigDecimal duration = new BigDecimal(micros);
        duration = duration.movePointLeft(3);
        duration = duration.setScale(3, RoundingMode.HALF_EVEN);
        return duration;
    }

    /**
     * Add together an array of durations and convert seconds to milliseconds.
     * 
     * Useful for the CMS Remark phase, where a single event can have multiple phases and durations.
     * 
     * For example: 0.0226730 + 0.0624566 + 0.0857010 = .1708306 seconds = 171 milliseconds
     * 
     * @param durations
     *            <code>Array</code> of seconds as whole numbers and/or decimals.
     * @return Total time rounded to a whole number.
     */
    public static int totalDuration(String[] durations) {
        BigDecimal duration = new BigDecimal("0");
        for (int i = 0; i < durations.length; i++) {
            // BigDecimal does not accept decimal commas, only decimal periods
            duration = duration.add(new BigDecimal(durations[i].replace(",", ".")));
        }
        return convertSecsToMillis(duration.toPlainString()).intValue();
    }

    /**
     * Calculate the throughput between two garbage collection (GC) points. Throughput is the percent of time not spent
     * doing GC.
     * 
     * @param currentDuration
     *            Current collection time spent doing GC (milliseconds) beginning at currentTimestamp.
     * @param currentTimestamp
     *            Current collection timestamp (milliseconds after JVM startup).
     * @param priorDuration
     *            Prior collection time spent doing GC (milliseconds) beginning at priorTimestamp. 0 for the first
     *            collection.
     * @param priorTimestamp
     *            Prior collection timestamp (milliseconds after JVM startup). 0 for the first collection.
     * @return Throughput as a percent. 0 means all time was spent doing GC. 100 means no time was spent doing GC.
     */
    public static int calcThroughput(final int currentDuration, final long currentTimestamp, final int priorDuration,
            final long priorTimestamp) {
        long timeTotal = currentTimestamp + Long.valueOf(currentDuration) - priorTimestamp;
        long timeNotGc = timeTotal - Long.valueOf(currentDuration) - Long.valueOf(priorDuration);
        BigDecimal throughput = new BigDecimal(timeNotGc);
        throughput = throughput.divide(new BigDecimal(timeTotal), 2, RoundingMode.HALF_EVEN);
        throughput = throughput.movePointRight(2);
        return throughput.intValue();
    }

    /**
     * Calculate size in kilobytes.
     * 
     * @param size
     *            Size block value.
     * @param units
     *            Size block units.
     * @return The size in Kilobytes.
     */
    public static int calcKilobytes(final int size, final char units) {
        int kilobytes = size;
        switch (units) {
        case 'M':
            kilobytes = kilobytes * 1024;
            break;
        case 'G':
            kilobytes = kilobytes * 1024 * 1024;
            break;
        }
        return kilobytes;
    }

    /**
     * Convert SIZE_G1_DETAILS to kilobytes.
     * 
     * @param size
     *            The size (e.g. '128.0').
     * @param units
     *            The units (e.g. 'G').
     * @return The size in Kilobytes.
     */
    public static int convertSizeG1DetailsToKilobytes(final String size, final char units) {

        BigDecimal kilobytes = new BigDecimal(size);
        BigDecimal kilo = new BigDecimal("1024");

        switch (units) {

        case 'B':
            kilobytes = kilobytes.divide(new BigDecimal("1024"));
            break;
        case 'K':
            break;
        case 'M':
            kilobytes = kilobytes.multiply(kilo);
            break;
        case 'G':
            kilobytes = (kilobytes.multiply(kilo)).multiply(kilo);
            break;
        default:
            throw new AssertionError("Unexpected units value: " + units);

        }
        kilobytes = kilobytes.setScale(0, RoundingMode.HALF_EVEN);
        return kilobytes.intValue();
    }
}
