package com.jtprince.coordinateoffset.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@NullMarked
public class PartialStacktraceLogger {
    private final Logger logger;
    public PartialStacktraceLogger(Logger logger) {
        this.logger = logger;
    }

    private String getStackTraceString(String message, Exception e) {
        List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
        List<StackTraceElement> current = new ArrayList<>(Arrays.asList(Thread.currentThread().getStackTrace()));

        final int popBuffer = 2;
        int popped = 0;
        while (stack.size() > popBuffer+1 && current.size() > popBuffer+1 &&
            stack.get(stack.size()-popBuffer).equals(current.get(current.size()-popBuffer))) {
            stack.remove(stack.size()-1);
            current.remove(current.size()-1);
            popped++;
        }

        return message +
            "\nCaused by " + e.getClass().getName() + ": " + e.getMessage() + "\n  " +
            stack.stream().map(StackTraceElement::toString).collect(Collectors.joining("\n  ")) +
            "\n (+" + popped + " hidden frames)";
    }

    public void logStacktrace(String message, Exception e) {
        logger.severe(getStackTraceString(message, e));
    }

    private static class RateLimitEntry {
        long lastLoggedTime;
        int numUnloggedOccurrences = 0;
        @Nullable String lastUnloggedStacktrace;
        @Nullable Exception lastUnloggedException;
    }
    private record RateLimitEntryKey(Class<? extends Exception> clazz, String aggregateKey) {}
    private final HashMap<RateLimitEntryKey, RateLimitEntry> activeRateLimits = new HashMap<>();

    public boolean logStacktraceRateLimited(Logger logger, String message, Exception e,
                                            long rateLimitIntervalMs, String aggregateKey) {
        long currentTime = System.currentTimeMillis();
        RateLimitEntryKey key = new RateLimitEntryKey(e.getClass(), aggregateKey);
        RateLimitEntry entry = activeRateLimits.get(key);

        boolean print = true;
        if (entry == null) {
            entry = new RateLimitEntry();
            entry.lastLoggedTime = currentTime;
            activeRateLimits.put(key, entry);
        } else {
            if (currentTime - entry.lastLoggedTime < rateLimitIntervalMs) {
                print = false;
            }
        }

        if (print) {
            if (entry.numUnloggedOccurrences > 0) {
                logger.severe(entry.numUnloggedOccurrences +
                    " occurrence" + (entry.numUnloggedOccurrences == 1 ? "" : "s") +
                    " of the following exception " + (entry.numUnloggedOccurrences == 1 ? "was" : "were") +
                    " rate limited for " + aggregateKey +
                    " in the last " + (currentTime - entry.lastLoggedTime) + "ms.");
                entry.lastLoggedTime = currentTime;
                entry.numUnloggedOccurrences = 0;
                entry.lastUnloggedStacktrace = null;
                entry.lastUnloggedException = null;
            }
            logStacktrace(message, e);
        } else {
            entry.numUnloggedOccurrences++;
            entry.lastUnloggedStacktrace = getStackTraceString(message, e);
            entry.lastUnloggedException = e;
        }

        return print;
    }

    public void flushRateLimits(long minAgeMs) {
        long currentTime = System.currentTimeMillis();
        activeRateLimits.keySet().removeIf(key -> {
            RateLimitEntry entry = activeRateLimits.get(key);

            boolean flush = (currentTime - entry.lastLoggedTime >= minAgeMs);

            if (flush && entry.numUnloggedOccurrences > 0) {
                logger.severe(entry.numUnloggedOccurrences +
                    " occurrence" + (entry.numUnloggedOccurrences == 1 ? "" : "s") +
                    " of " + entry.lastUnloggedException.getClass().getSimpleName() +
                    " " + (entry.numUnloggedOccurrences == 1 ? "was" : "were") +
                    " rate limited for " + key.aggregateKey +
                    " in the last " + (currentTime - entry.lastLoggedTime) + "ms. Last stacktrace was:");
                logger.severe(entry.lastUnloggedStacktrace);
            }
            return flush;
        });
    }

}
