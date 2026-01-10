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
package tz.co.esync.handler.events;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.esync.config.Config;
import tz.co.esync.config.Keys;
import tz.co.esync.helper.model.AttributeUtil;
import tz.co.esync.helper.model.PositionUtil;
import tz.co.esync.model.Device;
import tz.co.esync.model.Geofence;
import tz.co.esync.model.Position;
import tz.co.esync.session.cache.CacheManager;
import tz.co.esync.session.state.OverspeedProcessor;
import tz.co.esync.session.state.OverspeedState;
import tz.co.esync.storage.Storage;
import tz.co.esync.storage.StorageException;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Request;

public class OverspeedEventHandler extends BaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverspeedEventHandler.class);

    private final CacheManager cacheManager;
    private final Storage storage;

    private final long minimalDuration;
    private final boolean preferLowest;
    private final double multiplier;

    @Inject
    public OverspeedEventHandler(Config config, CacheManager cacheManager, Storage storage) {
        this.cacheManager = cacheManager;
        this.storage = storage;
        minimalDuration = config.getLong(Keys.EVENT_OVERSPEED_MINIMAL_DURATION) * 1000;
        preferLowest = config.getBoolean(Keys.EVENT_OVERSPEED_PREFER_LOWEST);
        multiplier = config.getDouble(Keys.EVENT_OVERSPEED_THRESHOLD_MULTIPLIER);
    }

    @Override
    public void onPosition(Position position, Callback callback) {

        long deviceId = position.getDeviceId();
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return;
        }
        if (!PositionUtil.isLatest(cacheManager, position)) {
            return;
        }

        double speedLimit = AttributeUtil.lookup(cacheManager, Keys.EVENT_OVERSPEED_LIMIT, deviceId);

        double positionSpeedLimit = position.getDouble(Position.KEY_SPEED_LIMIT);
        if (positionSpeedLimit > 0) {
            speedLimit = positionSpeedLimit;
        }

        double geofenceSpeedLimit = 0;
        long overspeedGeofenceId = 0;

        if (position.getGeofenceIds() != null) {
            for (long geofenceId : position.getGeofenceIds()) {
                Geofence geofence = cacheManager.getObject(Geofence.class, geofenceId);
                if (geofence != null) {
                    double currentSpeedLimit = geofence.getDouble(Keys.EVENT_OVERSPEED_LIMIT.getKey());
                    if (currentSpeedLimit > 0 && geofenceSpeedLimit == 0
                            || preferLowest && currentSpeedLimit < geofenceSpeedLimit
                            || !preferLowest && currentSpeedLimit > geofenceSpeedLimit) {
                        geofenceSpeedLimit = currentSpeedLimit;
                        overspeedGeofenceId = geofenceId;
                    }
                }
            }
        }
        if (geofenceSpeedLimit > 0) {
            speedLimit = geofenceSpeedLimit;
        }

        if (speedLimit == 0) {
            return;
        }

        OverspeedState state = OverspeedState.fromDevice(device);
        OverspeedProcessor.updateState(state, position, speedLimit, multiplier, minimalDuration, overspeedGeofenceId);
        if (state.isChanged()) {
            state.toDevice(device);
            try {
                storage.updateObject(device, new Request(
                        new Columns.Include("overspeedState", "overspeedTime", "overspeedGeofenceId"),
                        new Condition.Equals("id", device.getId())));
            } catch (StorageException e) {
                LOGGER.warn("Update device overspeed error", e);
            }
        }
        if (state.getEvent() != null) {
            callback.eventDetected(state.getEvent());
        }
    }

}
