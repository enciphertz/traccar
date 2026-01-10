/*
 * Copyright 2018 - 2025 Encipher Company Limited
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
package tz.co.esync.notificators;

import tz.co.esync.model.Event;
import tz.co.esync.model.Notification;
import tz.co.esync.model.Position;
import tz.co.esync.model.User;
import tz.co.esync.notification.MessageException;
import tz.co.esync.notification.NotificationFormatter;
import tz.co.esync.notification.NotificationMessage;

public abstract class Notificator {

    private final NotificationFormatter notificationFormatter;

    public Notificator(NotificationFormatter notificationFormatter) {
        this.notificationFormatter = notificationFormatter;
    }

    public void send(Notification notification, User user, Event event, Position position) throws MessageException {
        var message = notificationFormatter.formatMessage(notification, user, event, position);
        send(user, message, event, position);
    }

    public void send(User user, NotificationMessage message, Event event, Position position) throws MessageException {
        throw new UnsupportedOperationException();
    }

}
