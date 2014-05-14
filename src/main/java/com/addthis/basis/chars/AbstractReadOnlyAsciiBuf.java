package com.addthis.basis.chars;

public abstract class AbstractReadOnlyAsciiBuf extends AbstractReadOnlyUtfBuf {

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

    /**
     * Uses the cacheInstance for the hashCache (cacheHash? whichever dumb name Donnelly wanted)
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
