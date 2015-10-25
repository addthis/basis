package com.addthis.basis.util;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.assertEquals;

public class VarintTest {

    @Test
    public void signedIntLengthTest() {
        ByteBuf buffer1 = Unpooled.buffer();
        ByteBuf buffer2 = Unpooled.buffer();
        Varint.writeSignedVarInt(-1, buffer1);
        Varint.writeUnsignedVarInt(-1, buffer2);
        assertEquals(1, buffer1.readableBytes());
        assertEquals(5, buffer2.readableBytes());
    }

    @Test
    public void signedLongLengthTest() {
        ByteBuf buffer1 = Unpooled.buffer();
        ByteBuf buffer2 = Unpooled.buffer();
        Varint.writeSignedVarLong(-1, buffer1);
        Varint.writeUnsignedVarLong(-1, buffer2);
        assertEquals(1, buffer1.readableBytes());
        assertEquals(10, buffer2.readableBytes());
    }

    @Test
    public void signedIntRoundTrpTest() {
        ByteBuf buffer = Unpooled.buffer();
        Varint.writeSignedVarInt(-1, buffer);
        assertEquals(-1, Varint.readSignedVarInt(buffer));
    }

    @Test
    public void signedLongRoundTrpTest() {
        ByteBuf buffer = Unpooled.buffer();
        Varint.writeSignedVarLong(-1l, buffer);
        assertEquals(-1l, Varint.readSignedVarLong(buffer));
    }
}
