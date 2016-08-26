package com.afollestad.iconrequest;

/**
 * @author Aidan Follestad (afollestad)
 */
class PerfUtil {

    private static String task;
    private static long start = -1;

    public static void begin(String task) {
        PerfUtil.task = task;
        start = System.currentTimeMillis();
    }

    public static void end() {
        if (start == -1 || task == null) return;
        final long end = System.currentTimeMillis();
        final double diff = (double) (end - start);
        final double seconds = diff / 1000d;
        IRLog.log("IconRequestPerf", "Completed " + task + " in " + diff + "ms (" + seconds + "s).");
        start = -1;
        task = null;
    }
}
