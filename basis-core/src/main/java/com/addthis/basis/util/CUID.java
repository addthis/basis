/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.basis.util;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;

public class CUID {
    private static final HashFunction fallbackHashFunc = Hashing.murmur3_128();

    /**
     * calls createGUID()
     */
    public static String createAPIKey() {
        return createGUID();
    }

    /**
     * Generates a 128-bit random string in the specified base as two 64-bit 0-padded strings
     *
     * @return 128-bit random string in the specified base as two 64-bit 0-padded strings
     * @deprecated - we now use CUIDs
     */
    @Deprecated
    public static String createGUID(int base) {
        return LessNumbers.nextLong(base) + LessNumbers.nextLong(base);
    }

    /**
     * Generates a 128-bit random string in the MAX base as two 64-bit 0-padded strings
     *
     * @return 128-bit random string in the MAX base as two 64-bit 0-padded strings
     * @deprecated - we now use CUIDs
     */
    @Deprecated
    public static String createGUID() {
        return createGUID(LessNumbers.MAX_BASE);
    }

    /**
     * Generates 64-bit time-domain prefixed semi-random identifier
     */
    public static String createCUID() {
        return createCUID(System.currentTimeMillis(), LessNumbers.random.nextInt());
    }

    public static String createCUID(long time, int rand) {
        return encodeCUID(((time / 1000L) << 32) | (rand & 0xffffffffL));
    }

    public static long cuidToTime(String cuid) {
        long val = decodeCUID(cuid);
        return (val >> 32) * 1000L;
    }

    public static long cuidToTime(long cuid) {
        return (cuid >> 32) * 1000L;
    }

    /**
     * Convert long cuid to String
     */
    public static String encodeCUID(long cuid) {
        return Long.toHexString(cuid);
    }

    /**
     * Decodes 64-bit base62 CUID or falls back to hash of
     * String if that fails.
     *
     * @param cuid
     * @return
     */
    public static long decodeCUID(String cuid) {
        if (cuid == null || cuid.length() == 0) {
            return 0L;
        }
        try {
            return LessNumbers.longFromBase(cuid, 16, true, false);
        } catch (Exception ex) {
            System.out.println("invalid cuid : " + cuid);
            return fallbackHashFunc.hashUnencodedChars(cuid).asLong();
        }
    }
}
