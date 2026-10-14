package cn.ilikui.learning;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectionLabTest {

    @Test
    void countWordsIsCaseInsensitiveAndPreservesFirstSeenOrder() {
        assertEquals(Map.of("java", 2, "map", 1, "stream", 1),
                CollectionLab.countWords("Java map java stream"));
        assertEquals(List.of("java", "map", "stream"),
                List.copyOf(CollectionLab.countWords("Java map java stream").keySet()));
    }

    @Test
    void countWordsHandlesNullBlankAndWhitespace() {
        assertTrue(CollectionLab.countWords(null).isEmpty());
        assertTrue(CollectionLab.countWords("  \t\n ").isEmpty());
        assertEquals(Map.of("a", 2, "b", 1), CollectionLab.countWords(" A  b a "));
    }

    @Test
    void topKUsesFrequencyThenAlphabeticalOrder() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("pear", 2);
        counts.put("apple", 2);
        counts.put("banana", 3);

        assertEquals(List.of(Map.entry("banana", 3), Map.entry("apple", 2)),
                CollectionLab.topK(counts, 2));
    }

    @Test
    void topKHandlesZeroAndRejectsNegativeK() {
        assertTrue(CollectionLab.topK(Map.of("a", 1), 0).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> CollectionLab.topK(Map.of("a", 1), -1));
        assertThrows(NullPointerException.class,
                () -> CollectionLab.topK(null, 1));
    }

    @Test
    void safeAverageUsesLongAccumulationAndSupportsNegativeNumbers() {
        assertEquals(2.0, CollectionLab.safeAverage(List.of(1, 2, 3)));
        assertEquals(-2.0, CollectionLab.safeAverage(List.of(-1, -2, -3)));
        assertEquals(2147483647.5,
                CollectionLab.safeAverage(List.of(Integer.MAX_VALUE, Integer.MAX_VALUE)));
    }

    @Test
    void safeAverageRejectsEmptyOrNullValues() {
        assertThrows(IllegalArgumentException.class,
                () -> CollectionLab.safeAverage(List.of()));
        assertThrows(NullPointerException.class,
                () -> CollectionLab.safeAverage(null));
        assertThrows(NullPointerException.class,
                () -> CollectionLab.safeAverage(java.util.Arrays.asList(1, null)));
    }
}