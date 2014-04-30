package com.addthis.basis.chars;

import java.util.Comparator;

/**
 * Implements Comparator for CharSequence and returns lexicographic
 * ordering.
 */
public class CharSequenceComparator implements Comparator<CharSequence> {

    public static final Comparator<CharSequence> INSTANCE = new CharSequenceComparator();

    @Override
    public int compare(CharSequence o1, CharSequence o2) {
        int o1Len = o1.length();
        int o2Len = o2.length();
        int minLength = Math.min(o1Len, o2Len);

        for (int i = 0; i< minLength; i++) {
            char o1Char = o1.charAt(i);
            char o2Char = o2.charAt(i);
            int charCompare = Character.compare(o1Char, o2Char);
            if (charCompare != 0) {
                return charCompare;
            }
        }

        // Cannot overflow because the lengths are bounded to the positive domain.
        //noinspection SubtractionInCompareTo
        return o1Len - o2Len;
    }
}
