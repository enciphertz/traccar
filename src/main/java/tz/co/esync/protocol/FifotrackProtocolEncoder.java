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

import tz.co.esync.StringProtocolEncoder;
import tz.co.esync.helper.Checksum;
import tz.co.esync.model.Command;
import tz.co.esync.Protocol;

public class FifotrackProtocolEncoder extends StringProtocolEncoder {

    public FifotrackProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private Object formatCommand(Command command, String content) {
        String uniqueId = getUniqueId(command.getDeviceId());
        int length = 1 + uniqueId.length() + 3 + content.length();
        String result = String.format("##%02d,%s,1,%s*", length, uniqueId, content);
        result += Checksum.sum(result) + "\r\n";
        return result;
    }

    @Override
    protected Object encodeCommand(Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> formatCommand(command, command.getString(Command.KEY_DATA));
            case Command.TYPE_REQUEST_PHOTO -> formatCommand(command, "D05,3");
            default -> null;
        };
    }

}
