package tz.co.esync.protocol;

import org.junit.jupiter.api.Test;
import tz.co.esync.ProtocolTest;

public class SanulProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new SanulProtocolDecoder(null));

        verifyNull(decoder, binary(
                "aa007020000100000000000033353333353830313831353431313700000000000000000000"));

    }

}
