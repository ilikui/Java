package cn.ilikui.learning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Executable collection exercises for the Java learning repository.
 */
public final class CollectionLab {
    private CollectionLab() {
    }

    /**
     * Counts whitespace-separated words without case sensitivity.
     * Punctuation is intentionally not removed in this first exercise.
     *
     * @param text input text; null is treated as empty input
     * @return insertion-ordered word frequencies
     */
    public static Map<String, Integer> countWords(String text) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return counts;
        }

        for (String token : text.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!token.isEmpty()) {
                counts.merge(token, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Returns at most k entries ordered by count descending and key ascending.
     *
     * @throws IllegalArgumentException when k is negative
     */
    public static List<Map.Entry<String, Integer>> topK(
            Map<String, Integer> counts, int k) {
        Objects.requireNonNull(counts, "counts must not be null");
        if (k < 0) {
            throw new IllegalArgumentException("k must be >= 0");
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Computes the arithmetic mean as a double.
     * Empty input has no mathematical average and is rejected explicitly.
     */
    public static double safeAverage(List<Integer> values) {
        Objects.requireNonNull(values, "values must not be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }

        long sum = 0L;
        for (Integer value : values) {
            sum += Objects.requireNonNull(value, "values must not contain null");
        }
        return (double) sum / values.size();
    }

    public static void main(String[] args) {
        Map<String, Integer> counts = countWords("Java map java stream Map");
        System.out.println("词频：" + counts);
        System.out.println("Top 2：" + topK(counts, 2));
        System.out.println("平均值：" + safeAverage(new ArrayList<>(List.of(10, 20, 30))));
    }
}