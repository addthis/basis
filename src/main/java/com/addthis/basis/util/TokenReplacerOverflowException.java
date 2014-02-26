package com.addthis.basis.util;

public class TokenReplacerOverflowException extends Exception {
    public TokenReplacerOverflowException(String raw, long maxDepth) {
        super("Depth " + maxDepth + " exceeded trying to expand "
              + '"' + raw + '"');
    }
}