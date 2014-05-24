package com.addthis.basis.chars;

public class ByteArrayReadOnlyAsciiBuf extends ByteArrayReadOnlyUtfBuf {

    public ByteArrayReadOnlyAsciiBuf(byte[] data) {
        super(data);
    }

    public ByteArrayReadOnlyAsciiBuf(String javaString) {
        super(javaString);
    }

    @Override
    protected boolean knownAsciiOnly(int cacheInstance) {
        return true;
    }

    @Override
    public int length() {
        return _getByteLength();
    }

    @Override
    public char charAt(int index) {
        return (char) _getByte(index);
    }

    // start is inclusive, end is exclusive
    @Override
    public CharSequence subSequence(int start, int end) {
        return _getSubSequenceForByteBounds(start, end);
    }

    /**
     * Uses the cacheInstance for caching the hash instead
     */
    @Override
    public int hashCode() {
        int hash = packedIndexCache;
        if (hash == 0) {
            int length = length();
            for (int i = 0; i < length; i++) {
                char c = charAt(i);
                hash = 31 * hash + c;
            }
            packedIndexCache = hash;
        }
        return hash;
    }
}
