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

import io.netty.buffer.ByteBufHolder;

/**
 * A variation on ByteBufs for Character Strings. This variation has three primary goals:
 *
 * 1. Faster serialization and deserialization. Character Strings that are only
 * infrequently treated as anything more than byte sequences waste a lot of CPU
 * and (although also sort of CPU) heap garbage. This is especially egregious for
 * the all too frequent case of deserializing a string, passing it around a few
 * threads, and then serializing it again, but it is almost as bad when the only
 * operations are comparisons to other Strings.
 *
 * 1 (Example). In hydra, bundles are sent from a query worker to the master with
 * many String values serialized as byte arrays in the UTF-8 format. It is entirely
 * possible for that String to be passed to the user without ever being manipulated.
 * That means it was deserialized and then reserialized back to the same byte array
 * for essentially no reason. That worst case could be resolved by lazy loading or
 * a special un-deserializable value, but this does not scale well for the long tail
 * of few, low intensity operations like comparisons to other Character Strings.
 * Additionally, a lazy loading implementation would be likely implemented as a wrapper
 * class. That would cause another layer of indirection and memory waste. This solution
 * is closer to 'lazy loading of chars', which actually turns out to be pretty cheap.
 *
 * 2. Reduced memory overhead. Standard java char types are 16 bits, but for the common case
 * of all or mostly ASCII characters, this is twice (or near that) as much memory as needed.
 *
 * 3. More flexible char[] semantics similar to the difference between byte[]s and ByteBufs. Eg. decreasing
 * the number of readable values is possible as a constant time operation without creating a new array.
 * String itself is also really, deeply, into making char[] copies. See AsciiSequence.toString() for
 * an example of easy it can be to accidentally make lots of array copies, and how hard it is to avoid even
 * when you are trying to. (in hydra, AbstractBufferingHttpBundleEncoder ran into a similar issue where it
 * was mistakenly creating an unnecessary copy).
 *
 * * * *
 * Secondary goals/ benefits:
 * * * *
 *
 * - Specializing in one encoding with one backing structure allows for much more efficient
 * encode and decode methods than those in the standard library due to abstraction limitations.
 *
 * - Gets around some of the other more egregious inefficiencies with jdk UTF-8 encoding/ decoding
 * like decoding pre-allocating three times as much space as needed for the ASCII only case and
 * then cutting down by re-allocating to the smaller char array. This implementation allows and
 * encourages providing hints about how much to allocate, and should be able to more easily support
 * correcting under-estimates (as far as I can tell, the JDK NIO coding library does support that --
 * it just isn't actually used anywhere I can find. Possibly because benchmarks showed it wasn't worth
 * it, but it is also possible that was due to limitations we do not have here).
 *
 * - Using CharSequence here and other places gives us more options with respect to optimizing
 * things like sub-string semantics (shared/ unshared), and efficient streaming cache hit
 * detection.
 *
 * - Using ByteBufs directly makes integration with other ByteBuf based IO easy and efficient.
 *
 * This interface combines several related ones and additionally imposes the following contracts:
 *
 * - all backing data should be stored in UTF-8 format only. UTF-8 is the one
 * true format, and heretics will be persecuted without remorse.
 *
 * - hashCode and equals should return consistent values across implementations
 * for the same underlying logic character sequence.
 * -- for lack of other motivations, but for possibly no actual benefit, this
 * will be the same values that an equivilent String representation would return.
 *
 * - compareTo should perform lexicographical string comparison.
 * -- Note that while such comparisons are likely to be consistent with other
 * CharSequence implementations, we cannot actually guarantee that to be the
 * case because CharSequence does not require it. Accordingly, we do not derive
 * much benefit from declaring Comparable of type CharSequence because eg.
 * native Strings declare Comparable only for other Strings.
 * -- Also note that the UTF-8 format (which you are required to implement)
 * should be able to do lexicographical comparisons without converting to chars
 * (byte-wise comparison should suffice).
 *
 * Component reasoning
 *
 * CharSequence: to sub in for arbitrary String usages
 *
 * Appendable: Convenient for building CharSequences, and CharBufs are likely efficient at doing so
 *
 * Comparable: so that CharBuf only CharSequence environments can use sorted data structures
 *
 * ByteBufHolder: subject to change, but helpful for resource management, and exposing
 * the underlying data store for more efficient operations than per-char method calls.
 * Possible replacements for ByteBufHolder might be directly extending ByteBuf with more/
 * different char methods, or simply creating a whole char based equivalent with conversions.
 *
 * Maybe add Iteratable Character, or primitive equivalent?
 */
public interface CharBuf extends ReadableCharBuf, Appendable, ByteBufHolder {

    /**
     * Return value should be consistent across CharBuf implementations for the
     * same underlying logical CharSequence.
     */
    @Override
    public int hashCode();

    /**
     * Should return true for any CharBuf that represents the same underlying logical
     * CharSequence.
     */
    @Override
    public boolean equals(Object obj);

}
