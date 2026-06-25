package org.eclipselabs.garbagecat; 

import com.macmario.io.file.ReadFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.macmario.main.MainTask;

import org.eclipselabs.garbagecat.domain.JvmRun;
import org.eclipselabs.garbagecat.service.GcManager;
import org.eclipselabs.garbagecat.util.Constants;
import org.eclipselabs.garbagecat.util.GcUtil;
import org.eclipselabs.garbagecat.util.jdk.Analysis;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.LogEventType;
import org.eclipselabs.garbagecat.util.jdk.Jvm;

public class GCMain extends MainTask {
    
    public GCMain(String[] args) {
        super();
        parseArgs(args);
    }

    public static final int REJECT_LIMIT = 1000;

        /**
     * Create Garbage Collection Analysis report.
     * 
     * @param jvmRun
     *            JVM run data.
     * @param reportFileName
     *            Output report file name.
     * 
     */
    public void createReport(JvmRun jvmRun, String reportFileName) {
        java.io.File reportFile = new java.io.File(reportFileName);
        java.io.FileWriter fileWriter = null;
        java.io.BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new java.io.FileWriter(reportFile);
            bufferedWriter = new java.io.BufferedWriter(fileWriter);

            // Bottlenecks
            List<String> bottlenecks = jvmRun.getBottlenecks();
            if (bottlenecks.size() > 0) {
                bufferedWriter.write("========================================" + System.getProperty("line.separator"));
                bufferedWriter.write("Throughput less than " + jvmRun.getThroughputThreshold() + "%"
                        + System.getProperty("line.separator"));
                bufferedWriter.write("----------------------------------------" + System.getProperty("line.separator"));
                Iterator<String> iterator = bottlenecks.iterator();
                while (iterator.hasNext()) {
                    bufferedWriter.write(iterator.next() + System.getProperty("line.separator"));
                }
            }

            // JVM information
            if (jvmRun.getJvm().getVersion() != null || jvmRun.getJvm().getOptions() != null
                    || jvmRun.getJvm().getMemory() != null) {
                bufferedWriter.write("========================================" + System.getProperty("line.separator"));
                bufferedWriter.write("JVM:" + System.getProperty("line.separator"));
                bufferedWriter.write("----------------------------------------" + System.getProperty("line.separator"));
                if (jvmRun.getJvm().getVersion() != null) {
                    bufferedWriter
                            .write("Version: " + jvmRun.getJvm().getVersion() + System.getProperty("line.separator"));
                }
                if (jvmRun.getJvm().getOptions() != null) {
                    bufferedWriter
                            .write("Options: " + jvmRun.getJvm().getOptions() + System.getProperty("line.separator"));
                }
                if (jvmRun.getJvm().getMemory() != null) {
                    bufferedWriter
                            .write("Memory: " + jvmRun.getJvm().getMemory() + System.getProperty("line.separator"));
                }
            }

            // Summary
            bufferedWriter.write("========================================" + System.getProperty("line.separator"));
            bufferedWriter.write("SUMMARY:" + System.getProperty("line.separator"));
            bufferedWriter.write("----------------------------------------" + System.getProperty("line.separator"));

            // GC stats
            bufferedWriter
                    .write("# GC Events: " + jvmRun.getBlockingEventCount() + System.getProperty("line.separator"));
            if (jvmRun.getBlockingEventCount() > 0) {
                bufferedWriter.write("Event Types: ");
                List<LogEventType> eventTypes = jvmRun.getEventTypes();
                Iterator<LogEventType> iterator = eventTypes.iterator();
                boolean firstEvent = true;           
                while (iterator.hasNext()) {
                    LogEventType eventType = iterator.next();
                    // Only report GC events
                    if (JdkUtil.isReportable(eventType)) {
                        if (!firstEvent) {
                            bufferedWriter.write(", ");
                        }
                        bufferedWriter.write(eventType.toString());
                        firstEvent = false;
                    }
                }
                bufferedWriter.write(System.getProperty("line.separator"));
                // Max heap occupancy.
                bufferedWriter.write("Max Heap Occupancy: " + jvmRun.getMaxHeapOccupancy() + "K"
                        + System.getProperty("line.separator"));
                // Max heap space.
                bufferedWriter.write(
                        "Max Heap Space: " + jvmRun.getMaxHeapSpace() + "K" + System.getProperty("line.separator"));
                if (jvmRun.getMaxPermSpace() > 0) {
                    // Max perm occupancy.
                    bufferedWriter.write("Max Perm/Metaspace Occupancy: " + jvmRun.getMaxPermOccupancy() + "K"
                            + System.getProperty("line.separator"));
                    // Max perm space.
                    bufferedWriter.write("Max Perm/Metaspace Space: " + jvmRun.getMaxPermSpace() + "K"
                            + System.getProperty("line.separator"));
                }
                // GC throughput
                bufferedWriter.write(
                        "GC Throughput: " + jvmRun.getGcThroughput() + "%" + System.getProperty("line.separator"));
                // GC max pause
                bufferedWriter.write(
                        "GC Max Pause: " + jvmRun.getMaxGcPause() + " ms" + System.getProperty("line.separator"));
                // GC total pause time
                bufferedWriter.write(
                        "GC Total Pause: " + jvmRun.getTotalGcPause() + " ms" + System.getProperty("line.separator"));
            }
            if (jvmRun.getStoppedTimeEventCount() > 0) {
                // Stopped time throughput
                bufferedWriter.write("Stopped Time Throughput: " + jvmRun.getStoppedTimeThroughput() + "%"
                        + System.getProperty("line.separator"));
                // Max stopped time
                bufferedWriter.write("Stopped Time Max Pause: " + jvmRun.getMaxStoppedTime() + " ms"
                        + System.getProperty("line.separator"));
                // Total stopped time
                bufferedWriter.write("Stopped Time Total: " + jvmRun.getTotalStoppedTime() + " ms"
                        + System.getProperty("line.separator"));
                // Ratio of GC vs. stopped time. 100 means all stopped time due to GC.
                if (jvmRun.getBlockingEventCount() > 0) {
                    bufferedWriter.write("GC/Stopped Ratio: " + jvmRun.getGcStoppedRatio() + "%"
                            + System.getProperty("line.separator"));
                }
            }
            // First/last timestamps
            if (jvmRun.getBlockingEventCount() > 0 || jvmRun.getStoppedTimeEventCount() > 0) {
                // First Timestamp
                bufferedWriter.write("First Timestamp: " + jvmRun.getFirstTimestamp() + " ms"
                        + System.getProperty("line.separator"));
                // Last Timestamp
                bufferedWriter.write(
                        "Last Timestamp: " + jvmRun.getLastTimestamp() + " ms" + System.getProperty("line.separator"));
            }

            bufferedWriter.write("========================================" + System.getProperty("line.separator"));

            // Analysis
            List<String> analysisKeys = jvmRun.getAnalysisKeys();
            if (!analysisKeys.isEmpty()) {

                // Determine analysis levels
                List<String> error = new ArrayList<String>();
                List<String> warn  = new ArrayList<String>();
                List<String> info  = new ArrayList<String>();

                Iterator<String> iterator = analysisKeys.iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String level = key.split("\\.")[0];
                    if (level.equals("error")) {
                        error.add(key);
                    } else if (level.equals("warn")) {
                        warn.add(key);
                    } else if (level.equals("info")) {
                        info.add(key);
                    } else {
                        throw new IllegalArgumentException("Unknown analysis level: " + level);
                    }
                }

                bufferedWriter.write("ANALYSIS:" + System.getProperty("line.separator"));

                iterator = error.iterator();
                boolean printHeader = true;
                // ERROR
                while (iterator.hasNext()) {
                    if (printHeader) {
                        bufferedWriter.write(
                                "----------------------------------------" + System.getProperty("line.separator"));
                        bufferedWriter.write("error" + System.getProperty("line.separator"));
                        bufferedWriter.write(
                                "----------------------------------------" + System.getProperty("line.separator"));
                    }
                    printHeader = false;
                    String key = iterator.next();
                    bufferedWriter.write("*");
                    bufferedWriter.write(GcUtil.getPropertyValue(Analysis.PROPERTY_FILE, key));
                    bufferedWriter.write(System.getProperty("line.separator"));
                }
                // WARN
                iterator = warn.iterator();
                printHeader = true;
                while (iterator.hasNext()) {
                    if (printHeader) {
                        bufferedWriter.write(
                                "----------------------------------------" + System.getProperty("line.separator"));
                        bufferedWriter.write("warn" + System.getProperty("line.separator"));
                        bufferedWriter.write(
                                "----------------------------------------" + System.getProperty("line.separator"));
                    }
                    printHeader = false;
                    String key = iterator.next();
                    bufferedWriter.write("*");
                    bufferedWriter.write(GcUtil.getPropertyValue(Analysis.PROPERTY_FILE, key));
                    bufferedWriter.write(System.getProperty("line.separator"));
                }
                // INFO
                iterator = info.iterator();
                printHeader = true;
                while (iterator.hasNext()) {
                    if (printHeader) {
                       bufferedWriter.write(
                                "----------------------------------------" + System.getProperty("line.separator"));
                        bufferedWriter.write("info" + System.getProperty("line.separator"));
                        bufferedWriter.write(
                                "----------------------------------------" + System.getProperty("line.separator"));
                    }
                    printHeader = false;
                    String key = iterator.next();
                    bufferedWriter.write("*");
                    bufferedWriter.write(GcUtil.getPropertyValue(Analysis.PROPERTY_FILE, key));
                    bufferedWriter.write(System.getProperty("line.separator"));
                }
                bufferedWriter.write("========================================" + System.getProperty("line.separator"));
            }

            // Unidentified log lines
            List<String> unidentifiedLogLines = jvmRun.getUnidentifiedLogLines();
            if (!unidentifiedLogLines.isEmpty()) {
                bufferedWriter.write(unidentifiedLogLines.size() + " UNIDENTIFIED LOG LINE(S):"
                        + System.getProperty("line.separator"));
                bufferedWriter.write("----------------------------------------" + System.getProperty("line.separator"));

                Iterator<String> iterator = unidentifiedLogLines.iterator();
                while (iterator.hasNext()) {
                    String unidentifiedLogLine = iterator.next();
                    bufferedWriter.write(unidentifiedLogLine);
                    bufferedWriter.write(System.getProperty("line.separator"));
                }
                bufferedWriter.write("========================================" + System.getProperty("line.separator"));
            }
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } finally {
            // Close streams
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }
                             
    }
    
    public void scan() {
        boolean reorder = true;
        String jvmOptions =null; 
        Date   jvmStartDate= new Date();
        GcManager jvmManager = new GcManager();
        ReadFile f = new ReadFile( getProperty("logfile") );
        
        System.out.println("logFile:"+f.getFQDNFileName());
        // Store garbage collection logging in data store.
        jvmManager.store(f.getFile(), reorder);

                // Create report
                Jvm jvm = new Jvm(jvmOptions, jvmStartDate);
                // Determine report options
                int throughputThreshold = Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD;
                //if (cmd.hasOption(Constants.OPTION_THRESHOLD_LONG)) {
                 //   throughputThreshold = Integer.parseInt(cmd.getOptionValue(Constants.OPTION_THRESHOLD_SHORT));
                //}
                JvmRun jvmRun = jvmManager.getJvmRun(jvm, throughputThreshold);
                String outputFileName=getProperty("out");
                System.out.println("outfile:"+outputFileName);
                createReport(jvmRun, outputFileName);


        
    }
    
    public static void main(String[] args) {
                GCMain m = new GCMain(args);
                m.scan();
    }
}
