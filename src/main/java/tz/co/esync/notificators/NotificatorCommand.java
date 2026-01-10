/*
 * Copyright 2023 - 2025 Encipher Company Limited
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
package tz.co.esync.notificators;

import tz.co.esync.database.CommandsManager;
import tz.co.esync.model.Command;
import tz.co.esync.model.Event;
import tz.co.esync.model.Notification;
import tz.co.esync.model.Position;
import tz.co.esync.model.User;
import tz.co.esync.notification.MessageException;
import tz.co.esync.storage.Storage;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NotificatorCommand extends Notificator {

    private final Storage storage;
    private final CommandsManager commandsManager;

    @Inject
    public NotificatorCommand(Storage storage, CommandsManager commandsManager) {
        super(null);
        this.storage = storage;
        this.commandsManager = commandsManager;
    }

    @Override
    public void send(Notification notification, User user, Event event, Position position) throws MessageException {

        if (notification == null || notification.getCommandId() <= 0) {
            throw new MessageException("Saved command not provided");
        }

        try {
            Command command = storage.getObject(Command.class, new Request(
                    new Columns.All(), new Condition.Equals("id", notification.getCommandId())));
            command.setDeviceId(event.getDeviceId());
            commandsManager.sendCommand(command);
        } catch (Exception e) {
            throw new MessageException(e);
        }
    }

}
