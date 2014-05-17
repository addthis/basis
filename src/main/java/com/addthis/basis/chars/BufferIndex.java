package com.addthis.basis.chars;

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
