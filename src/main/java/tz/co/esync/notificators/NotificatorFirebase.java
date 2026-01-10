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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.esync.config.Config;
import tz.co.esync.config.Keys;
import tz.co.esync.model.Event;
import tz.co.esync.model.ObjectOperation;
import tz.co.esync.model.Position;
import tz.co.esync.model.User;
import tz.co.esync.notification.MessageException;
import tz.co.esync.notification.NotificationFormatter;
import tz.co.esync.notification.NotificationMessage;
import tz.co.esync.session.cache.CacheManager;
import tz.co.esync.storage.Storage;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Singleton
public class NotificatorFirebase extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorFirebase.class);

    private final Storage storage;
    private final CacheManager cacheManager;
    private final FirebaseMessaging firebaseMessaging;

    @Inject
    public NotificatorFirebase(
            Config config, NotificationFormatter notificationFormatter,
            Storage storage, CacheManager cacheManager) throws IOException {
        super(notificationFormatter);
        this.storage = storage;
        this.cacheManager = cacheManager;

        InputStream serviceAccount = new ByteArrayInputStream(
                config.getString(Keys.NOTIFICATOR_FIREBASE_SERVICE_ACCOUNT).getBytes());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        firebaseMessaging = FirebaseMessaging.getInstance(
                FirebaseApp.initializeApp(options, "manager"));
    }

    @Override
    public void send(User user, NotificationMessage message, Event event, Position position) throws MessageException {
        if (user.hasAttribute("notificationTokens")) {

            List<String> registrationTokens = new ArrayList<>(
                    Arrays.asList(user.getString("notificationTokens").split("[, ]")));

            var androidConfig = AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder().setSound("default").build());

            var apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").build());

            if (message.priority()) {
                androidConfig.setPriority(AndroidConfig.Priority.HIGH);
                apnsConfig.putHeader("apns-priority", "10");
            }

            var messageBuilder = MulticastMessage.builder()
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(message.subject())
                            .setBody(message.digest())
                            .build())
                    .setAndroidConfig(androidConfig.build())
                    .setApnsConfig(apnsConfig.build())
                    .addAllTokens(registrationTokens);

            if (event != null) {
                messageBuilder.putData("eventId", String.valueOf(event.getId()));
            }

            try {
                var result = firebaseMessaging.sendEachForMulticast(messageBuilder.build());
                List<String> failedTokens = new LinkedList<>();
                var iterator = result.getResponses().listIterator();
                while (iterator.hasNext()) {
                    int index = iterator.nextIndex();
                    var response = iterator.next();
                    if (!response.isSuccessful()) {
                        MessagingErrorCode error = response.getException().getMessagingErrorCode();
                        if (error == MessagingErrorCode.INVALID_ARGUMENT || error == MessagingErrorCode.UNREGISTERED) {
                            failedTokens.add(registrationTokens.get(index));
                        }
                        LOGGER.warn("Firebase user {} error", user.getId(), response.getException());
                    }
                }
                if (!failedTokens.isEmpty()) {
                    registrationTokens.removeAll(failedTokens);
                    if (registrationTokens.isEmpty()) {
                        user.removeAttribute("notificationTokens");
                    } else {
                        user.set("notificationTokens", String.join(",", registrationTokens));
                    }
                    storage.updateObject(user, new Request(
                            new Columns.Include("attributes"),
                            new Condition.Equals("id", user.getId())));
                    cacheManager.invalidateObject(true, User.class, user.getId(), ObjectOperation.UPDATE);
                }
            } catch (Exception e) {
                LOGGER.warn("Firebase error", e);
            }
        }
    }

}
