package ch.ksrminecraft.kSROITC.utils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

    private RandomUtil() {}

    /** Zufällige Ganzzahl im Intervall [min, max] (inklusive). */
    public static int nextInt(int min, int maxInclusive) {
        if (maxInclusive < min) throw new IllegalArgumentException("max < min");
        return ThreadLocalRandom.current().nextInt(min, maxInclusive + 1);
    }

    /** true mit gegebener Wahrscheinlichkeit (0.0–1.0). */
    public static boolean chance(double probability) {
        if (probability <= 0) return false;
        if (probability >= 1) return true;
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /** Zufälliges Element oder null bei leerer Liste. */
    public static <T> T choice(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /** Kopie als zufällige Reihenfolge. */
    public static <T> List<T> shuffled(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy;
    }

    /** Bis zu n unterschiedliche Elemente (Reihenfolge zufällig). */
    public static <T> List<T> pick(List<T> list, int n) {
        if (list == null || list.isEmpty() || n <= 0) return Collections.emptyList();
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(n, copy.size()));
    }

    /** Clamp für Zahlen. */
    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
