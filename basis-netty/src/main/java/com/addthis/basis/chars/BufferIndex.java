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

package com.addthis.basis.chars;

/**
 * a little helper struct used in some non-ascii (slow path) methods of utf bufs. It isn't terribly
 * efficient, but it simplified things enough to let the fast path methods get inlined by the jit,
 * and ascii performance is our top priority at the moment.
 */
class BufferIndex {

    int charIndex;
    int byteOffset;
    int byteIndex;

    public BufferIndex(int charIndex, int byteOffset, int byteIndex) {
        this.charIndex = charIndex;
        this.byteOffset = byteOffset;
        this.byteIndex = byteIndex;
    }

    public BufferIndex(int cacheInstance) {
        this.charIndex = AbstractReadOnlyUtfBuf.cacheCharIndex(cacheInstance);
        this.byteOffset = AbstractReadOnlyUtfBuf.cacheByteOffset(cacheInstance);
        this.byteIndex = AbstractReadOnlyUtfBuf.byteIndex(byteOffset, charIndex);
    }
}
