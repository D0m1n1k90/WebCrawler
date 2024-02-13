package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CrawlInternalTask extends RecursiveTask<Boolean> {
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;

    public CrawlInternalTask(String url,
                             Instant deadline,
                             int maxDepth,
                             Map<String, Integer> counts,
                             Set<String> visitedUrls,
                             Clock clock,
                             List<Pattern> ignoredUrls,
                             PageParserFactory parserFactory) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    protected Boolean compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return false;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return false;
            }
        }

        synchronized (visitedUrls) {
            if (visitedUrls.contains(url)) {
                return false;
            }
            visitedUrls.add(url);
        }

        PageParser.Result result = parserFactory.get(url).parse();
        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : e.getValue() + v);
        }

        List<CrawlInternalTask> subtasks = new ArrayList<>();
        for (String link : result.getLinks()) {
            subtasks.add(new CrawlInternalTask(link, deadline, maxDepth - 1, counts, visitedUrls, clock, ignoredUrls, parserFactory));
        }

        invokeAll(subtasks);

        return true;
    }
}
