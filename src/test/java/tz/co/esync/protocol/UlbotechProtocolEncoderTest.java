package tz.co.esync.protocol;

import org.junit.jupiter.api.Test;
import tz.co.esync.ProtocolTest;
import tz.co.esync.model.Command;

public class UlbotechProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncode() throws Exception {

        var encoder = inject(new UlbotechProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "UNO;13912345678");

        verifyCommand(encoder, command, buffer("*TS01,UNO;13912345678#"));

    }

}
