package tz.co.esync.command;

import tz.co.esync.model.Command;
import tz.co.esync.model.Device;

import java.util.Collection;

public interface CommandSender {
    Collection<String> getSupportedCommands();
    void sendCommand(Device device, Command command) throws Exception;
}
