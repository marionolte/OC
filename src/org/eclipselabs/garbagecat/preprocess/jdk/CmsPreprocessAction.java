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
package org.eclipselabs.garbagecat.preprocess.jdk;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipselabs.garbagecat.preprocess.PreprocessAction;
import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;

/**
 * <p>
 * CMS preprocessing.
 * </p>
 *
 * <p>
 * Fix issues with CMS logging.
 * </p>
 *
 * <h3>Example Logging</h3>
 * 
 * <p>
 * 1) {@link org.eclipselabs.garbagecat.domain.jdk.ParNewEvent} combined with
 * {@link org.eclipselabs.garbagecat.domain.jdk.CmsConcurrentEvent}:
 * </p>
 *
 * <pre>
 * 46674.719: [GC (Allocation Failure)46674.719: [ParNew46674.749: [CMS-concurrent-abortable-preclean: 1.427/2.228 secs] [Times: user=1.56 sys=0.01, real=2.23 secs]
 * : 153599K-&gt;17023K(153600K), 0.0383370 secs] 229326K-&gt;114168K(494976K), 0.0384820 secs] [Times: user=0.15 sys=0.01, real=0.04 secs]
 * </pre>
 *
 * <p>
 * Preprocessed:
 * </p>
 *
 * <pre>
 * 46674.719: [GC (Allocation Failure)46674.719: [ParNew: 153599K-&gt;17023K(153600K), 0.0383370 secs] 229326K-&gt;114168K(494976K), 0.0384820 secs] [Times: user=0.15 sys=0.01, real=0.04 secs]
 * 46674.749: [CMS-concurrent-abortable-preclean: 1.427/2.228 secs] [Times: user=1.56 sys=0.01, real=2.23 secs]
 * </pre>
 * 
 * <p>
 * 2) {@link org.eclipselabs.garbagecat.domain.jdk.ParNewEvent} combined with
 * {@link org.eclipselabs.garbagecat.domain.jdk.CmsConcurrentEvent} without trigger:
 * </p>
 *
 * <pre>
 * 10.963: [GC10.963: [ParNew10.977: [CMS-concurrent-abortable-preclean: 0.088/0.197 secs] [Times: user=0.33 sys=0.05, real=0.20 secs]
 * : 115327K-&gt;12800K(115328K), 0.0155930 secs] 349452K-&gt;251716K(404548K), 0.0156840 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]
 * </pre>
 *
 * <p>
 * Preprocessed:
 * </p>
 *
 * <pre>
 * 10.963: [GC10.963: [ParNew: 115327K-&gt;12800K(115328K), 0.0155930 secs] 349452K-&gt;251716K(404548K), 0.0156840 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]
 * 10.977: [CMS-concurrent-abortable-preclean: 0.088/0.197 secs] [Times: user=0.33 sys=0.05, real=0.20 secs]
 * </pre>
 * 
 * <p>
 * 3) {@link org.eclipselabs.garbagecat.domain.jdk.CmsSerialOldEvent} with concurrent mode failure trigger across 2
 * lines:
 * </p>
 * 
 * <pre>
 * 44.684: [Full GC44.684: [CMS44.877: [CMS-concurrent-mark: 1.508/2.428 secs] [Times: user=3.44 sys=0.49, real=2.42 secs]
 *  (concurrent mode failure): 1218548K-&gt;413373K(1465840K), 1.3656970 secs] 1229657K-&gt;413373K(1581168K), [CMS Perm : 83805K-&gt;80520K(83968K)], 1.3659420 secs] [Times: user=1.33 sys=0.01, real=1.37 secs]
 * </pre>
 *
 * <p>
 * Preprocessed:
 * </p>
 *
 * <pre>
 * 44.684: [Full GC44.684: [CMS (concurrent mode failure): 1218548K-&gt;413373K(1465840K), 1.3656970 secs] 1229657K-&gt;413373K(1581168K), [CMS Perm : 83805K-&gt;80520K(83968K)], 1.3659420 secs] [Times: user=1.33 sys=0.01, real=1.37 secs]
 * 44.877: [CMS-concurrent-mark: 1.508/2.428 secs] [Times: user=3.44 sys=0.49, real=2.42 secs]
 * </pre>
 * 
 * <p>
 * 4) {@link org.eclipselabs.garbagecat.domain.jdk.ParNewEvent} combined with
 * {@link org.eclipselabs.garbagecat.domain.jdk.CmsConcurrentEvent} with trigger and space after trigger:
 * </p>
 *
 * <pre>
 * 45.574: [GC (Allocation Failure) 45.574: [ParNew45.670: [CMS-concurrent-abortable-preclean: 3.276/4.979 secs] [Times: user=7.75 sys=0.28, real=4.98 secs]
 * : 619008K-&gt;36352K(619008K), 0.2165661 secs] 854952K-&gt;363754K(4157952K), 0.2168066 secs] [Times: user=0.30 sys=0.00, real=0.22 secs]
 * </pre>
 *
 * <p>
 * Preprocessed:
 * </p>
 *
 * <pre>
 * 45.574: [GC (Allocation Failure) 45.574: [ParNew: 619008K-&gt;36352K(619008K), 0.2165661 secs] 854952K-&gt;363754K(4157952K), 0.2168066 secs] [Times: user=0.30 sys=0.00, real=0.22 secs]
 * 45.670: [CMS-concurrent-abortable-preclean: 3.276/4.979 secs] [Times: user=7.75 sys=0.28, real=4.98 secs]
 * </pre>
 * 
 * <p>
 * 5) JDK 8 {@link org.eclipselabs.garbagecat.domain.jdk.CmsSerialOldEvent} with concurrent mode failure trigger
 * combined with {@link org.eclipselabs.garbagecat.domain.jdk.CmsConcurrentEvent} across 2 lines:
 * </p>
 * 
 * <pre>
 * 706.707: [Full GC (Allocation Failure) 706.708: [CMS709.137: [CMS-concurrent-mark: 3.381/5.028 secs] [Times: user=23.92 sys=3.02, real=5.03 secs]
 *  (concurrent mode failure): 2655937K-&gt;2373842K(2658304K), 11.6746550 secs] 3973407K-&gt;2373842K(4040704K), [Metaspace: 72496K-&gt;72496K(1118208K)] icms_dc=77 , 11.6770830 secs] [Times: user=14.05 sys=0.02,
 * </pre>
 *
 * <p>
 * Preprocessed:
 * </p>
 *
 * <pre>
 * 706.707: [Full GC (Allocation Failure) 706.708: [CMS (concurrent mode failure): 2655937K-&gt;2373842K(2658304K), 11.6746550 secs] 3973407K-&gt;2373842K(4040704K), [Metaspace: 72496K-&gt;72496K(1118208K)] icms_dc=77 , 11.6770830 secs] [Times: user=14.05 sys=0.02, real=11.68 secs]
 * 709.137: [CMS-concurrent-mark: 3.381/5.028 secs] [Times: user=23.92 sys=3.02, real=5.03 secs]
 * </pre>
 * 
 * <p>
 * 6) JDK 8 {@link org.eclipselabs.garbagecat.domain.jdk.ParNewConcurrentModeFailureEvent} combined with
 * {@link org.eclipselabs.garbagecat.domain.jdk.CmsConcurrentEvent} across 2 lines:
 * </p>
 * 
 * <pre>
 * 719.519: [GC (Allocation Failure) 719.521: [ParNew: 1382400K-&gt;1382400K(1382400K), 0.0000470 secs]719.521: [CMS722.601: [CMS-concurrent-mark: 3.567/3.633 secs] [Times: user=10.91 sys=0.69, real=3.63 secs]
 *  (concurrent mode failure): 2542828K-&gt;2658278K(2658304K), 12.3447910 secs] 3925228K-&gt;2702358K(4040704K), [Metaspace: 72175K-&gt;72175K(1118208K)] icms_dc=100 , 12.3480570 secs] [Times: user=15.38 sys=0.02, real=12.35 secs]
 * </pre>
 *
 * <p>
 * Preprocessed:
 * </p>
 *
 * <pre>
 * 719.519: [GC (Allocation Failure) 719.521: [ParNew: 1382400K-&gt;1382400K(1382400K), 0.0000470 secs] (concurrent mode failure): 2542828K-&gt;2658278K(2658304K), 12.3447910 secs] 3925228K-&gt;2702358K(4040704K), [Metaspace: 72175K-&gt;72175K(1118208K)] icms_dc=100 , 12.3480570 secs] [Times: user=15.38 sys=0.02, real=12.35 secs]
 * 719.521: [CMS722.601: [CMS-concurrent-mark: 3.567/3.633 secs] [Times: user=10.91 sys=0.69, real=3.63 secs]
 * </pre>
 * 
 * <p>
 * 7) {@link org.eclipselabs.garbagecat.domain.jdk.CmsSerialOldEvent} with
 * {@link org.eclipselabs.garbagecat.domain.jdk.ClassUnloadingEvent}:
 * </p>
 * 
 * <pre>
 * 830048.804: [Full GC 830048.804: [CMS[Unloading class sun.reflect.GeneratedConstructorAccessor73]
 * [Unloading class sun.reflect.GeneratedConstructorAccessor70]
 * : 1572185K-&gt;1070163K(1572864K), 6.8812400 secs] 2489689K-&gt;1070163K(2490368K), [CMS Perm : 46357K-&gt;46348K(77352K)], 6.8821630 secs] [Times: user=6.87 sys=0.00, real=6.88 secs]
 * </pre>
 * 
 * <p>
 * Preprocessed:
 * </p>
 * 
 * <pre>
 * 830048.804: [Full GC 830048.804: [CMS: 1572185K-&gt;1070163K(1572864K), 6.8812400 secs] 2489689K-&gt;1070163K(2490368K), [CMS Perm : 46357K-&gt;46348K(77352K)], 6.8821630 secs] [Times: user=6.87 sys=0.00, real=6.88 secs]
 * </pre>
 *
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 *
 */
/**
 * @author mmillson
 *
 */
public class CmsPreprocessAction implements PreprocessAction {

    /**
     * Regular expression for retained beginning PAR_NEW mixed with CMS_CONCURRENT collection.
     * 
     * 3576157.596: [GC 3576157.596: [CMS-concurrent-abortable-preclean: 0.997/1.723 secs] [Times: user=3.20 sys=0.03,
     * real=1.73 secs]
     */
    private static final String REGEX_RETAIN_BEGINNING_PARNEW_CONCURRENT = "^((" + JdkRegEx.DATESTAMP + ": )?"
            + JdkRegEx.TIMESTAMP + ": \\[GC( \\(" + JdkRegEx.TRIGGER_ALLOCATION_FAILURE + "\\))?( )?(("
            + JdkRegEx.DATESTAMP + ": )?" + JdkRegEx.TIMESTAMP + ": \\[ParNew)?( \\("
            + JdkRegEx.TRIGGER_PROMOTION_FAILED + "\\))?(: " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\("
            + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\](" + JdkRegEx.DATESTAMP + ": )?" + JdkRegEx.TIMESTAMP
            + ": \\[CMS)?)(( CMS: abort preclean due to time )?(" + JdkRegEx.DATESTAMP + ": )?" + JdkRegEx.TIMESTAMP
            + ": \\[CMS-concurrent-(abortable-preclean|mark|sweep|preclean): " + JdkRegEx.DURATION_FRACTION + "\\]"
            + JdkRegEx.TIMES_BLOCK + "?)[ ]*$";

    /**
     * Regular expression for retained beginning PAR_NEW mixed with FLS_STATISTICS.
     * 
     * 1.118: [GC Before GC:
     */
    private static final String REGEX_RETAIN_BEGINNING_PARNEW_FLS_STATISTICS = "^((" + JdkRegEx.DATESTAMP + ": )?"
            + JdkRegEx.TIMESTAMP + ": \\[GC )(Before GC:)$";

    /**
     * Regular expression for beginning CMS_SERIAL_OLD collection.
     */
    private static final String REGEX_RETAIN_BEGINNING_SERIAL = "^(" + JdkRegEx.TIMESTAMP + ": \\[Full GC "
            + JdkRegEx.TIMESTAMP + ": \\[Class Histogram:)[ ]*$";

    /**
     * Regular expression for beginning PAR_NEW collection.
     * 
     * 29.839: [GC 36.226: [ParNew
     * 
     * 182314.858: [GC 182314.859: [ParNew (promotion failed)
     */
    private static final String REGEX_RETAIN_BEGINNING_PARNEW = "^(" + JdkRegEx.TIMESTAMP + ": \\[GC( )?"
            + JdkRegEx.TIMESTAMP + ": \\[ParNew( \\((" + JdkRegEx.TRIGGER_PROMOTION_FAILED + ")\\))?)[ ]*$";

    /**
     * Regular expression for retained beginning CMS_SERIAL_OLD mixed with CMS_CONCURRENT collection.
     */
    private static final String REGEX_RETAIN_BEGINNING_SERIAL_CONCURRENT = "^((" + JdkRegEx.DATESTAMP + ": )?"
            + JdkRegEx.TIMESTAMP + ": \\[Full GC( )?(\\((" + JdkRegEx.TRIGGER_ALLOCATION_FAILURE + "|"
            + JdkRegEx.TRIGGER_JVM_TI_FORCED_GAREBAGE_COLLECTION + "|" + JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD + "|"
            + JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC + ")\\)[ ]{0,1})?(" + JdkRegEx.DATESTAMP + ": )?"
            + JdkRegEx.TIMESTAMP + ": \\[CMS)((" + JdkRegEx.DATESTAMP + ": )?" + JdkRegEx.TIMESTAMP
            + ": \\[CMS-concurrent-(mark|abortable-preclean|preclean|sweep): " + JdkRegEx.DURATION_FRACTION + "\\]"
            + JdkRegEx.TIMES_BLOCK + "?)[ ]*$";

    /**
     * Regular expression for retained beginning CMS_SERIAL_OLD bailing out collection.
     */
    private static final String REGEX_RETAIN_BEGINNING_SERIAL_BAILING = "^(" + JdkRegEx.TIMESTAMP + ": \\[Full GC "
            + JdkRegEx.TIMESTAMP + ": \\[CMSbailing out to foreground collection)[ ]*$";

    /**
     * Regular expression for retained CMS_SERIAL_OLD with -XX:+UseGCOverheadLimit at end.
     * 
     * 3743.645: [Full GC [PSYoungGen: 419840K->415020K(839680K)] [PSOldGen: 5008922K->5008922K(5033984K)]
     * 5428762K->5423942K(5873664K) [PSPermGen: 193275K->193275K(262144K)] GC time would exceed GCTimeLimit of 98%
     * 
     */
    private static final String REGEX_RETAIN_BEGINNING_SERIAL_GC_TIME_LIMIT_EXCEEDED = "^(" + JdkRegEx.TIMESTAMP
            + ": \\[Full GC \\[PSYoungGen: " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE
            + "\\)\\] \\[(PS|Par)OldGen: " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\)\\] "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\) \\[PSPermGen: " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE
            + "\\)\\])(      |\t)GC time (would exceed|is exceeding) GCTimeLimit of 98%$";

    /**
     * Regular expression for retained beginning PrintHeapAtGC collection.
     */
    private static final String REGEX_RETAIN_BEGINNING_PRINT_HEAP_AT_GC = "^(" + JdkRegEx.TIMESTAMP
            + ": \\[(Full )?GC )\\{Heap before gc invocations=\\d{1,10}:[ ]*$";

    /**
     * Regular expression for retained beginning PAR_NEW bailing out collection.
     */
    private static final String REGEX_RETAIN_BEGINNING_PARNEW_BAILING = "^(" + JdkRegEx.TIMESTAMP + ": \\[GC "
            + JdkRegEx.TIMESTAMP + ": \\[ParNew( \\(" + JdkRegEx.TRIGGER_PROMOTION_FAILED + "\\))?: " + JdkRegEx.SIZE
            + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]" + JdkRegEx.TIMESTAMP
            + ": \\[CMS(Java HotSpot\\(TM\\) Server VM warning: )?bailing out to foreground collection)[ ]*$";

    /**
     * Regular expression for retained beginning concurrent mode failure.
     */
    private static final String REGEX_RETAIN_MIDDLE_CONCURRENT_MODE_FAILURE = "^( \\(concurrent mode failure\\): "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]"
            + JdkRegEx.TIMESTAMP + ": \\[Class Histogram(:)?)[ ]*$";

    /**
     * Middle line when logging is split over 3 lines (e.g. bailing).
     * 
     * 233307.273: [CMS-concurrent-mark: 16.547/16.547 secs]
     */
    private static final String REGEX_RETAIN_MIDDLE_CONCURRENT = "^(" + JdkRegEx.TIMESTAMP
            + ": \\[CMS-concurrent-mark: " + JdkRegEx.DURATION_FRACTION + "\\]" + JdkRegEx.TIMES_BLOCK + "?)[ ]*$";

    /**
     * Middle line when mixed serial and concurrent logging (e.g. PrintHeapAtGC).
     * 
     * 28282.075: [CMS28284.687: [CMS-concurrent-preclean: 3.706/3.706 secs]
     * 
     * : 917504K->917504K(917504K), 5.5887120 secs]877375.047: [CMS877378.691: [CMS-concurrent-mark: 5.714/11.380 secs]
     * [Times: user=14.72 sys=4.81, real=11.38 secs]
     */
    private static final String REGEX_RETAIN_MIDDLE_SERIAL_CONCURRENT_MIXED = "^((: " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\])?" + JdkRegEx.TIMESTAMP
            + ": \\[CMS)(" + JdkRegEx.TIMESTAMP + ": \\[CMS-concurrent-(abortable-preclean|preclean|mark|sweep): "
            + JdkRegEx.DURATION_FRACTION + "\\]" + JdkRegEx.TIMES_BLOCK + "?)[ ]*$";

    /**
     * Middle line when mixed PAR_NEW and concurrent logging (e.g. PrintHeapAtGC).
     */
    private static final String REGEX_RETAIN_MIDDLE_PARNEW_CONCURRENT_MIXED = "^(" + JdkRegEx.TIMESTAMP
            + ": \\[ParNew( \\(" + JdkRegEx.TRIGGER_PROMOTION_FAILED + "\\))?: " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE
            + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]" + JdkRegEx.TIMESTAMP + ": \\[CMS)("
            + JdkRegEx.TIMESTAMP + ": \\[CMS-concurrent-(abortable-preclean|preclean|mark): "
            + JdkRegEx.DURATION_FRACTION + "\\])[ ]*$";

    /**
     * Middle line PAR_NEW with FLS_STATISTICS
     * 
     * 1.118: [ParNew: 377487K->8426K(5505024K), 0.0535260 secs] 377487K->8426K(43253760K)After GC:
     */
    private static final String REGEX_RETAIN_MIDDLE_PAR_NEW_FLS_STATISTICS = "^(" + JdkRegEx.TIMESTAMP + ": \\[ParNew: "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\] "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\))(After GC:)$";

    /**
     * Middle line with PrintHeapAtGC.
     */
    private static final String REGEX_RETAIN_MIDDLE_PRINT_HEAP_AT_GC = "^((" + JdkRegEx.TIMESTAMP + ": \\[CMS)?( \\("
            + JdkRegEx.TRIGGER_CONCURRENT_MODE_FAILURE + "\\))?: " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\("
            + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\] " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\("
            + JdkRegEx.SIZE + "\\)(, \\[CMS Perm : " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE
            + "\\)])?)Heap after gc invocations=\\d{1,10}:[ ]*$";

    /**
     * Middle line with PrintClassHistogram
     */
    private static final String REGEX_RETAIN_MIDDLE_PRINT_CLASS_HISTOGRAM = "^((" + JdkRegEx.TIMESTAMP + ": \\[CMS)?: "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]"
            + JdkRegEx.TIMESTAMP + ": \\[Class Histogram(:)?)[ ]*$";

    /**
     * Regular expression for retained end.
     * 
     * (concurrent mode failure): 1125100K->1156809K(1310720K), 36.8003032 secs] 1791073K->1156809K(2018560K),
     * 38.3378201 secs]
     * 
     * (concurrent mode interrupted): 861863K->904027K(1797568K), 42.9053262 secs] 1045947K->904027K(2047232K), [CMS
     * Perm : 252246K->252202K(262144K)], 42.9070278 secs] [Times: user=43.11 sys=0.18, real=42.91 secs]
     * 
     * (concurrent mode failure) (concurrent mode failure)[YG occupancy: 33620K (153344K)]85217.919: [Rescan (parallel)
     * , 0.0116680 secs]85217.931: [weak refs processing, 0.0167100 secs]85217.948: [class unloading, 0.0571300
     * secs]85218.005: [scrub symbol & string tables, 0.0291210 secs]: 423728K->423633K(4023936K), 0.5165330 secs]
     * 457349K->457254K(4177280K), [CMS Perm : 260428K->260406K(262144K)], 0.5167600 secs] [Times: user=0.55 sys=0.01,
     * real=0.52 secs]
     * 
     * : 36825K->4352K(39424K), 0.0224830 secs] 44983K->14441K(126848K), 0.0225800 secs]
     * 
     * 3576157.596: [ParNew: 147599K->17024K(153344K), 0.0795160 secs] 2371401K->2244459K(6274432K), 0.0810030 secs]
     * [Times: user=0.44 sys=0.00, real=0.08 secs]
     * 
     */
    private static final String REGEX_RETAIN_END = "^((" + JdkRegEx.TIMESTAMP + ": \\[ParNew)?( \\(("
            + JdkRegEx.TRIGGER_CONCURRENT_MODE_FAILURE + "|" + JdkRegEx.TRIGGER_CONCURRENT_MODE_INTERRUPTED
            + ")\\))?( \\(" + JdkRegEx.TRIGGER_CONCURRENT_MODE_FAILURE + "\\)\\[YG occupancy: " + JdkRegEx.SIZE + " \\("
            + JdkRegEx.SIZE + "\\)\\]" + JdkRegEx.TIMESTAMP + ": \\[Rescan \\(parallel\\) , " + JdkRegEx.DURATION
            + "\\]" + JdkRegEx.TIMESTAMP + ": \\[weak refs processing, " + JdkRegEx.DURATION + "\\]"
            + JdkRegEx.TIMESTAMP + ": \\[class unloading, " + JdkRegEx.DURATION + "\\]" + JdkRegEx.TIMESTAMP
            + ": \\[scrub symbol & string tables, " + JdkRegEx.DURATION + "\\])?(: " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\])? " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\)(, \\[(CMS Perm |Metaspace): " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\)\\])?" + JdkRegEx.ICMS_DC_BLOCK + "?, " + JdkRegEx.DURATION
            + "\\]" + JdkRegEx.TIMES_BLOCK + "?)[ ]*$";

    /**
     * Regular expression for retained duration. This can come in the middle or at the end of a logging event split over
     * multiple lines. Check the TOKEN to see if in the middle of preprocessing an event that spans multiple lines.
     */
    private static final String REGEX_RETAIN_DURATION = "(, " + JdkRegEx.DURATION + "\\]" + JdkRegEx.TIMES_BLOCK
            + "?)[ ]*";

    /**
     * Regular expression for PAR_NEW with extraneous prefix.
     */
    private static final String REGEX_RETAIN_PAR_NEW = "^(" + JdkRegEx.TIMESTAMP + ": \\[ParNew" + JdkRegEx.TIMESTAMP
            + ": \\[ParNew)((" + JdkRegEx.DATESTAMP + ": )?" + JdkRegEx.TIMESTAMP + ": \\[GC \\("
            + JdkRegEx.TRIGGER_ALLOCATION_FAILURE + "\\) " + JdkRegEx.TIMESTAMP + ": \\[ParNew: " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\] " + JdkRegEx.SIZE + "->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]" + JdkRegEx.TIMES_BLOCK
            + "?)[ ]*$";

    /**
     * Log entry in the entangle log list used to indicate the current high level preprocessor (e.g. CMS, G1). This
     * context is necessary to detangle multi-line events where logging patterns are shared among preprocessors.
     */
    public static final String TOKEN = "CMS_PREPROCESS_ACTION_TOKEN";

    /**
     * The log entry for the event. Can be used for debugging purposes.
     */
    private String logEntry;

    /**
     * Create event from log entry.
     *
     * @param priorLogEntry
     *            The prior log line.
     * @param logEntry
     *            The log line.
     * @param nextLogEntry
     *            The next log line.
     * @param entangledLogLines
     *            Log lines to be output out of order.
     * @param context
     *            Information to make preprocessing decisions.
     */
    public CmsPreprocessAction(String priorLogEntry, String logEntry, String nextLogEntry,
            List<String> entangledLogLines, Set<String> context) {

        // Beginning logging
        if (logEntry.matches(REGEX_RETAIN_BEGINNING_PARNEW_CONCURRENT)) {
            // Par_NEW mixed with CMS_CONCURRENT
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_PARNEW_CONCURRENT);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                entangledLogLines.add(matcher.group(49));
            }
            // Output beginning of PAR_NEW line
            this.logEntry = matcher.group(1);
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_PARNEW_FLS_STATISTICS)) {
            // Par_NEW mixed with FLS_STATISTICS
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_PARNEW_FLS_STATISTICS);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                // Output beginning of PAR_NEW line
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_SERIAL_CONCURRENT)) {
            // CMS_SERIAL_OLD mixed with CMS_CONCURRENT
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_SERIAL_CONCURRENT);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                entangledLogLines.add(matcher.group(29));
            }
            // Output beginning of CMS_SERIAL_OLD line
            this.logEntry = matcher.group(1);
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);

        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_SERIAL)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_SERIAL);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_PARNEW)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_PARNEW);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_PRINT_HEAP_AT_GC)) {
            // Remove PrintHeapAtGC output
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_PRINT_HEAP_AT_GC);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_SERIAL_BAILING)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_SERIAL_BAILING);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_SERIAL_GC_TIME_LIMIT_EXCEEDED)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_SERIAL_GC_TIME_LIMIT_EXCEEDED);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_BEGINNING_PARNEW_BAILING)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_BEGINNING_PARNEW_BAILING);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_CONCURRENT)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_CONCURRENT);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                if (!context.contains(TOKEN)) {
                    // Output now
                    this.logEntry = matcher.group(1);
                } else {
                    // Output later
                    entangledLogLines.add(matcher.group(1));
                }
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_SERIAL_CONCURRENT_MIXED)) {
            // Output serial part, save concurrent to output later
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_SERIAL_CONCURRENT_MIXED);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
                entangledLogLines.add(matcher.group(10));
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_PARNEW_CONCURRENT_MIXED)) {
            // Output ParNew part, save concurrent to output later
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_PARNEW_CONCURRENT_MIXED);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
                entangledLogLines.add(matcher.group(11));
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_PAR_NEW_FLS_STATISTICS)) {
            // Output ParNew par
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_PAR_NEW_FLS_STATISTICS);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_PRINT_HEAP_AT_GC)) {
            // Remove PrintHeapAtGC output
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_PRINT_HEAP_AT_GC);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_PRINT_CLASS_HISTOGRAM)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_PRINT_CLASS_HISTOGRAM);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_MIDDLE_CONCURRENT_MODE_FAILURE)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_MIDDLE_CONCURRENT_MODE_FAILURE);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_DURATION)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_DURATION);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            // Sometimes this is the end of a logging event
            if (entangledLogLines.size() > 0 && newLoggingEvent(nextLogEntry)) {
                clearEntangledLines(entangledLogLines);
            }
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
        } else if (logEntry.matches(REGEX_RETAIN_END)
                && !priorLogEntry.matches(REGEX_RETAIN_MIDDLE_PRINT_CLASS_HISTOGRAM)) {
            // End of logging event
            Pattern pattern = Pattern.compile(REGEX_RETAIN_END);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(1);
            }
            clearEntangledLines(entangledLogLines);
            context.remove(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.remove(TOKEN);
        } else if (logEntry.matches(REGEX_RETAIN_PAR_NEW)) {
            Pattern pattern = Pattern.compile(REGEX_RETAIN_PAR_NEW);
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                this.logEntry = matcher.group(4);
            }
            context.add(PreprocessAction.TOKEN_BEGINNING_OF_EVENT);
            context.add(TOKEN);
        }
    }

    public String getLogEntry() {
        return logEntry;
    }

    public String getName() {
        return JdkUtil.PreprocessActionType.CMS.toString();
    }

    /**
     * Determine if the logLine matches the logging pattern(s) for this event.
     *
     * @param logLine
     *            The log line to test.
     * @param priorLogLine
     *            The last log entry processed.
     * @param nextLogLine
     *            The next log entry processed.
     * @return true if the log line matches the event pattern, false otherwise.
     */
    public static final boolean match(String logLine, String priorLogLine, String nextLogLine) {
        return logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW_CONCURRENT)
                || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW_FLS_STATISTICS)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL_CONCURRENT)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL_BAILING)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL_GC_TIME_LIMIT_EXCEEDED)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL) || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW)
                || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW_BAILING)
                || logLine.matches(REGEX_RETAIN_BEGINNING_PRINT_HEAP_AT_GC)
                || logLine.matches(REGEX_RETAIN_MIDDLE_CONCURRENT_MODE_FAILURE)
                || logLine.matches(REGEX_RETAIN_MIDDLE_PRINT_CLASS_HISTOGRAM)
                || logLine.matches(REGEX_RETAIN_MIDDLE_CONCURRENT)
                || logLine.matches(REGEX_RETAIN_MIDDLE_SERIAL_CONCURRENT_MIXED)
                || logLine.matches(REGEX_RETAIN_MIDDLE_PARNEW_CONCURRENT_MIXED)
                || logLine.matches(REGEX_RETAIN_MIDDLE_PAR_NEW_FLS_STATISTICS)
                || logLine.matches(REGEX_RETAIN_MIDDLE_PRINT_HEAP_AT_GC) || logLine.matches(REGEX_RETAIN_END)
                || logLine.matches(REGEX_RETAIN_DURATION) || logLine.matches(REGEX_RETAIN_PAR_NEW);
    }

    /**
     * TODO: Move to superclass.
     * 
     * Convenience method to write out any saved log lines.
     * 
     * @param entangledLogLines
     *            Log lines to be output out of order.
     * @return
     */
    private final void clearEntangledLines(List<String> entangledLogLines) {
        if (entangledLogLines != null && entangledLogLines.size() > 0) {
            // Output any entangled log lines
            Iterator<String> iterator = entangledLogLines.iterator();
            while (iterator.hasNext()) {
                String logLine = iterator.next();
                this.logEntry = this.logEntry + System.getProperty("line.separator") + logLine;
            }
            // Reset entangled log lines
            entangledLogLines.clear();
        }
    }

    /**
     * Convenience method to test if a log line is the start of a new logging event or a complete logging event (vs. the
     * middle or end of a multi line logging event).
     * 
     * @param logLine
     *            The log line to test.
     * @return True if the line is the start of a new logging event or a complete logging event.
     */
    private boolean newLoggingEvent(String logLine) {
        return logLine == null || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW_CONCURRENT)
                || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW_FLS_STATISTICS)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL_CONCURRENT)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL_BAILING)
                || logLine.matches(REGEX_RETAIN_BEGINNING_SERIAL) || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW)
                || logLine.matches(REGEX_RETAIN_BEGINNING_PARNEW_BAILING)
                || logLine.matches(REGEX_RETAIN_BEGINNING_PRINT_HEAP_AT_GC);
    }
}