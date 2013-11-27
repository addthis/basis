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


public abstract class TokenReplacer {
    public static class Region {
        private final String start;
        private final String end;

        public Region(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

    private final Region[] regions;

    public TokenReplacer(String begin, String end) {
        this(new Region(begin, end));
    }

    public TokenReplacer(Region region) {
        this(new Region[]{region});
    }

    public TokenReplacer(Region regions[]) {
        this.regions = regions;
    }

    /**
     * implement this method to unlock the power of this class
     */
    public abstract String replace(Region region, String src);

    public String process(String raw) {
        if (raw == null) {
            return null;
        }
        while (true) {
            boolean modified = false;
            for (Region region : regions) {
                int next = raw.indexOf(region.start);
                if (next >= 0) {
                    int end = raw.indexOf(region.end, next + region.end.length());
                    if (end > 0) {
                        String replace = replace(region, raw.substring(next + region.start.length(), end));
                        if (replace == null) {
                            replace = "";
                        }
                        raw = raw.replace(raw.substring(next, end + region.end.length()), replace);
                        modified = true;
                    }
                }
            }
            if (modified) {
                continue;
            }
            break;
        }
        return raw;
    }
}
