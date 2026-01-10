/*
 * Copyright 2016 - 2025 Encipher Company Limited
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
package tz.co.esync.notification;

import org.apache.velocity.VelocityContext;
import tz.co.esync.database.LocaleManager;
import tz.co.esync.helper.model.UserUtil;
import tz.co.esync.model.Device;
import tz.co.esync.model.Driver;
import tz.co.esync.model.Event;
import tz.co.esync.model.Geofence;
import tz.co.esync.model.Maintenance;
import tz.co.esync.model.Notification;
import tz.co.esync.model.Position;
import tz.co.esync.model.Server;
import tz.co.esync.model.User;
import tz.co.esync.session.cache.CacheManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NotificationFormatter {

    private final LocaleManager localeManager;
    private final CacheManager cacheManager;
    private final TextTemplateFormatter textTemplateFormatter;

    @Inject
    public NotificationFormatter(
            LocaleManager localeManager, CacheManager cacheManager, TextTemplateFormatter textTemplateFormatter) {
        this.localeManager = localeManager;
        this.cacheManager = cacheManager;
        this.textTemplateFormatter = textTemplateFormatter;
    }

    public NotificationMessage formatMessage(
            Notification notification, User user, Event event, Position position) {

        Server server = cacheManager.getServer();
        Device device = cacheManager.getObject(Device.class, event.getDeviceId());

        VelocityContext velocityContext = textTemplateFormatter.prepareContext(server, user);

        velocityContext.put("notification", notification);
        velocityContext.put("device", device);
        velocityContext.put("event", event);
        velocityContext.put("translations", localeManager.getBundle(UserUtil.getLanguage(server, user)));
        if (position != null) {
            velocityContext.put("position", position);
            velocityContext.put("speedUnit", UserUtil.getSpeedUnit(server, user));
            velocityContext.put("distanceUnit", UserUtil.getDistanceUnit(server, user));
            velocityContext.put("volumeUnit", UserUtil.getVolumeUnit(server, user));
        }
        if (event.getGeofenceId() != 0) {
            velocityContext.put("geofence", cacheManager.getObject(Geofence.class, event.getGeofenceId()));
        }
        if (event.getMaintenanceId() != 0) {
            velocityContext.put("maintenance", cacheManager.getObject(Maintenance.class, event.getMaintenanceId()));
        }
        String driverUniqueId = event.getString(Position.KEY_DRIVER_UNIQUE_ID);
        if (driverUniqueId != null) {
            velocityContext.put("driver", cacheManager.getDeviceObjects(device.getId(), Driver.class).stream()
                    .filter(driver -> driver.getUniqueId().equals(driverUniqueId)).findFirst().orElse(null));
        }

        boolean priority = notification != null && notification.getBoolean("priority");
        return textTemplateFormatter.formatMessage(velocityContext, event.getType(), priority);
    }

}
