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
package tz.co.esync.handler;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.esync.model.DtcCode;
import tz.co.esync.model.Event;
import tz.co.esync.model.ObdParameters;
import tz.co.esync.model.Position;
import tz.co.esync.obd.ObdDataParser;
import tz.co.esync.storage.Storage;
import tz.co.esync.storage.StorageException;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Request;

import java.util.List;

public class ObdDataHandler extends BasePositionHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ObdDataHandler.class);
    
    private final Storage storage;
    private final ObdDataParser parser;
    
    @Inject
    public ObdDataHandler(Storage storage, ObdDataParser parser) {
        this.storage = storage;
        this.parser = parser;
    }
    
    @Override
    public void onPosition(Position position, Callback callback) {
        try {
            // Check if position has OBD data
            if (!hasObdData(position)) {
                callback.processed(false);
                return;
            }
            
            // Parse OBD parameters
            ObdParameters obd = parser.parseObdData(position);
            if (obd != null && hasValidObdData(obd)) {
                try {
                    obd.setId(storage.addObject(obd, new Request(new Columns.Exclude("id"))));
                } catch (StorageException e) {
                    LOGGER.warn("Failed to store OBD parameters for device {}", position.getDeviceId(), e);
                }
            }
            
            // Parse DTC codes
            List<DtcCode> dtcCodes = parser.parseDtcCodes(position);
            for (DtcCode dtc : dtcCodes) {
                try {
                    processDtcCode(dtc, position);
                } catch (StorageException e) {
                    LOGGER.warn("Failed to process DTC code {} for device {}", dtc.getCode(), position.getDeviceId(), e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to process OBD data for device {}", position.getDeviceId(), e);
        }
        
        callback.processed(false);
    }
    
    private boolean hasObdData(Position position) {
        return position.hasAttribute(Position.KEY_RPM)
            || position.hasAttribute(Position.KEY_COOLANT_TEMP)
            || position.hasAttribute(Position.KEY_FUEL_LEVEL)
            || position.hasAttribute(Position.KEY_FUEL_CONSUMPTION)
            || position.hasAttribute(Position.KEY_DTCS)
            || position.getAttributes().keySet().stream()
                .anyMatch(key -> key != null && key.startsWith("obd."));
    }
    
    private boolean hasValidObdData(ObdParameters obd) {
        return obd.getEngineRpm() != null
            || obd.getCoolantTemperature() != null
            || obd.getFuelLevel() != null
            || obd.getFuelConsumptionRate() != null
            || obd.getBatteryVoltage() != null
            || obd.getThrottlePosition() != null
            || obd.getEngineLoad() != null
            || obd.getVehicleSpeed() != null;
    }
    
    private void processDtcCode(DtcCode dtc, Position position) throws StorageException {
        // Check if DTC already exists as active
        Condition deviceCondition = new Condition.Equals("deviceId", dtc.getDeviceId());
        Condition codeCondition = new Condition.Equals("code", dtc.getCode());
        Condition statusCondition = new Condition.Equals("status", "active");
        Condition combinedCondition = new Condition.And(
            new Condition.And(deviceCondition, codeCondition),
            statusCondition
        );
        
        DtcCode existing = storage.getObject(DtcCode.class, new Request(combinedCondition));
        
        if (existing != null) {
            // Update last occurred time
            existing.setLastOccurred(dtc.getTimestamp());
            storage.updateObject(existing, new Request(
                new Columns.Include("lastOccurred")
            ));
        } else {
            // New DTC code - store and trigger event
            dtc.setId(storage.addObject(dtc, new Request(new Columns.Exclude("id"))));
            triggerDtcEvent(dtc, position);
        }
    }
    
    private void triggerDtcEvent(DtcCode dtc, Position position) throws StorageException {
        Event event = new Event(Event.TYPE_DTC_CODE, position);
        event.set("dtcCode", dtc.getCode());
        event.set("dtcDescription", dtc.getDescription());
        event.set("dtcType", dtc.getCodeType());
        event.set("severity", determineSeverity(dtc));
        
        storage.addObject(event, new Request(new Columns.Exclude("id")));
    }
    
    private String determineSeverity(DtcCode dtc) {
        // Determine severity based on DTC code type
        switch (dtc.getCodeType()) {
            case "P":  // Powertrain
                if (dtc.getCode() != null && dtc.getCode().startsWith("P03")) {
                    return "high";  // Misfire codes
                }
                if (dtc.getCode() != null && dtc.getCode().startsWith("P02")) {
                    return "high";  // Fuel injector codes
                }
                return "medium";
            case "B":  // Body
            case "C":  // Chassis
                return "low";
            case "U":  // Network
                return "high";
            default:
                return "medium";
        }
    }
}
