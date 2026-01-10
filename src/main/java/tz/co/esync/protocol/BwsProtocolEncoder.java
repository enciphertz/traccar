/*
 * Copyright 2025 Encipher Company Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tz.co.esync.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import tz.co.esync.BaseProtocolEncoder;
import tz.co.esync.Protocol;
import tz.co.esync.helper.Checksum;
import tz.co.esync.helper.DataConverter;
import tz.co.esync.model.Command;

public class BwsProtocolEncoder extends BaseProtocolEncoder {

    public BwsProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(long deviceId, int value) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x00);
        buf.writeBytes(DataConverter.parseHex(getUniqueId(deviceId).substring(0, 8)));
        buf.writeByte(BwsProtocolDecoder.MSG_ACTION);
        buf.writeByte(0);
        buf.writeByte(value);
        buf.writeByte(Checksum.crc8(Checksum.CRC8_DALLAS, buf.nioBuffer()));
        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        return switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP -> encodeContent(command.getDeviceId(), 0);
            case Command.TYPE_ENGINE_RESUME -> encodeContent(command.getDeviceId(), 1);
            default -> null;
        };
    }

}
