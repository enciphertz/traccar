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
package tz.co.esync.obd;

import tz.co.esync.model.DtcCode;
import tz.co.esync.model.ObdParameters;
import tz.co.esync.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObdDataParser {
    
    /**
     * Parse OBD data from position attributes
     */
    public ObdParameters parseObdData(Position position) {
        ObdParameters obd = new ObdParameters();
        obd.setDeviceId(position.getDeviceId());
        obd.setPositionId(position.getId());
        obd.setTimestamp(position.getFixTime());
        
        Map<String, Object> attributes = position.getAttributes();
        
        // Parse engine RPM
        if (position.hasAttribute(Position.KEY_RPM)) {
            obd.setEngineRpm(position.getInteger(Position.KEY_RPM));
        }
        
        // Parse coolant temperature
        if (position.hasAttribute(Position.KEY_COOLANT_TEMP)) {
            obd.setCoolantTemperature(position.getInteger(Position.KEY_COOLANT_TEMP));
        }
        
        // Parse fuel level
        if (position.hasAttribute(Position.KEY_FUEL_LEVEL)) {
            Object fuelLevel = attributes.get(Position.KEY_FUEL_LEVEL);
            if (fuelLevel instanceof Number) {
                obd.setFuelLevel(((Number) fuelLevel).intValue());
            }
        }
        
        // Parse fuel consumption rate
        if (position.hasAttribute(Position.KEY_FUEL_CONSUMPTION)) {
            obd.setFuelConsumptionRate(position.getDouble(Position.KEY_FUEL_CONSUMPTION));
        }
        
        // Parse battery voltage
        if (position.hasAttribute(Position.KEY_POWER)) {
            obd.setBatteryVoltage(position.getDouble(Position.KEY_POWER));
        }
        
        // Parse throttle position
        if (position.hasAttribute(Position.KEY_THROTTLE)) {
            obd.setThrottlePosition(position.getInteger(Position.KEY_THROTTLE));
        }
        
        // Parse engine load
        if (position.hasAttribute(Position.KEY_ENGINE_LOAD)) {
            obd.setEngineLoad(position.getInteger(Position.KEY_ENGINE_LOAD));
        }
        
        // Parse OBD speed
        if (position.hasAttribute(Position.KEY_OBD_SPEED)) {
            obd.setVehicleSpeed(position.getInteger(Position.KEY_OBD_SPEED));
        }
        
        // Parse additional OBD attributes
        parseCustomObdAttributes(obd, attributes);
        
        return obd;
    }
    
    /**
     * Parse DTC codes from position attributes
     */
    public List<DtcCode> parseDtcCodes(Position position) {
        List<DtcCode> dtcCodes = new ArrayList<>();
        
        if (!position.hasAttribute(Position.KEY_DTCS)) {
            return dtcCodes;
        }
        
        String dtcString = position.getString(Position.KEY_DTCS);
        if (dtcString == null || dtcString.isEmpty()) {
            return dtcCodes;
        }
        
        // Parse format: "P0301,P0420,B1234" or "P0301;P0420" or space-separated
        String[] codes = dtcString.split("[,;\\s]+");
        
        for (String codeStr : codes) {
            String code = codeStr.trim();
            if (!code.isEmpty()) {
                DtcCode dtc = new DtcCode();
                dtc.setDeviceId(position.getDeviceId());
                dtc.setPositionId(position.getId());
                dtc.setTimestamp(position.getFixTime());
                dtc.setCode(code);
                dtc.setCodeType(extractCodeType(code));
                dtc.setDescription(lookupDtcDescription(code));
                dtc.setStatus("active");
                dtc.setFirstOccurred(position.getFixTime());
                dtc.setLastOccurred(position.getFixTime());
                
                dtcCodes.add(dtc);
            }
        }
        
        return dtcCodes;
    }
    
    private String extractCodeType(String code) {
        if (code != null && code.length() > 0) {
            char firstChar = code.charAt(0);
            if (firstChar == 'P' || firstChar == 'B' || firstChar == 'C' || firstChar == 'U') {
                return String.valueOf(firstChar);
            }
        }
        return "P";  // Default to Powertrain
    }
    
    private String lookupDtcDescription(String code) {
        return DtcCodeDatabase.getInstance().lookup(code);
    }
    
    private void parseCustomObdAttributes(ObdParameters obd, Map<String, Object> attributes) {
        // Parse device-specific OBD attributes
        // e.g., "obd.rpm", "obd.temp", etc.
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("obd.")) {
                String obdKey = key.substring(4);  // Remove "obd." prefix
                parseObdAttribute(obd, obdKey, entry.getValue());
            }
        }
    }
    
    private void parseObdAttribute(ObdParameters obd, String key, Object value) {
        if (value == null) {
            return;
        }
        
        String lowerKey = key.toLowerCase();
        switch (lowerKey) {
            case "rpm":
                obd.setEngineRpm(parseInteger(value));
                break;
            case "coolanttemp":
            case "coolant_temp":
            case "coolanttemperature":
                obd.setCoolantTemperature(parseInteger(value));
                break;
            case "fuellevel":
            case "fuel_level":
                obd.setFuelLevel(parseInteger(value));
                break;
            case "fuelconsumption":
            case "fuel_consumption":
            case "fuelconsumptionrate":
                obd.setFuelConsumptionRate(parseDouble(value));
                break;
            case "batteryvoltage":
            case "battery_voltage":
            case "voltage":
                obd.setBatteryVoltage(parseDouble(value));
                break;
            case "throttleposition":
            case "throttle_position":
            case "throttle":
                obd.setThrottlePosition(parseInteger(value));
                break;
            case "engineload":
            case "engine_load":
                obd.setEngineLoad(parseInteger(value));
                break;
            case "vehiclespeed":
            case "vehicle_speed":
            case "speed":
                obd.setVehicleSpeed(parseInteger(value));
                break;
            case "oilpressure":
            case "oil_pressure":
                obd.setOilPressure(parseDouble(value));
                break;
            case "intakeairtemp":
            case "intake_air_temp":
            case "intakeairtemperature":
                obd.setIntakeAirTemperature(parseInteger(value));
                break;
            case "mafairflow":
            case "maf_air_flow":
            case "maf":
                obd.setMafAirFlow(parseDouble(value));
                break;
            case "fuelpressure":
            case "fuel_pressure":
                obd.setFuelPressure(parseDouble(value));
                break;
            case "timingadvance":
            case "timing_advance":
                obd.setTimingAdvance(parseDouble(value));
                break;
        }
    }
    
    private Integer parseInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
