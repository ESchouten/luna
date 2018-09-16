package io.luna.util;

import com.google.common.base.Joiner;

/**
 * A static-utility class that contains functions for manipulating strings.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class StringUtils {

    /**
     * An empty array of strings.
     */
    public static final String[] EMPTY_ARRAY = {};

    /**
     * An array containing valid {@code char}s.
     */
    public static final char VALID_CHARACTERS[] = {'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', ':', ';', '.', '>', '<', ',', '"',
            '[', ']', '|', '?', '/', '`'};

    /**
     * A {@link Joiner} that joins strings together with a ",".
     */
    public static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

    /**
     * Computes the indefinite article of {@code thing}.
     *
     * @param thing The thing to compute for.
     * @return The article.
     */
    public static String computeArticle(Object thing) {
        char first = thing.toString().toLowerCase().charAt(0);
        boolean vowel = "aeiouAEIOU".indexOf(first) != -1;
        return vowel ? "an" : "a";
    }

    /**
     * Appends the indefinite article of {@code thing} to {@code thing}.
     *
     * @param thing The thing to compute for and append.
     * @return The appended article.
     */
    public static String addArticle(Object thing) {
        String asString = thing.toString();
        return computeArticle(asString) + " " + asString;
    }

    /**
     * Encodes {@code s} to a base-37 {@code long}.
     *
     * @param s The string to encode.
     * @return The encoded string.
     */
    public static long encodeToBase37(String s) {
        long l = 0L;
        for (int i = 0; i < s.length() && i < 12; i++) {
            char c = s.charAt(i);
            l *= 37L;
            if (c >= 'A' && c <= 'Z') {
                l += (1 + c) - 65;
            } else if (c >= 'a' && c <= 'z') {
                l += (1 + c) - 97;
            } else if (c >= '0' && c <= '9') {
                l += (27 + c) - 48;
            }
        }
        while (l % 37L == 0L && l != 0L) {
            l /= 37L;
        }
        return l;
    }

    /**
     * Capitalizes a String value.
     *
     * @param s The String to capitalize.
     * @return The capitalized String.
     */
    public static String capitalize(String s) {
        if (!s.isEmpty()) {
            String capital = s.substring(0, 1).toUpperCase();
            StringBuilder builder = new StringBuilder(s);

            builder.setCharAt(0, capital.charAt(0));
            return builder.toString();
        }
        return s;
    }

    /**
     * A private constructor to discourage external instantiation.
     */
    private StringUtils() {
    }
}
