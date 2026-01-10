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

import java.util.HashMap;
import java.util.Map;

public class DtcCodeDatabase {
    
    private static DtcCodeDatabase instance;
    private final Map<String, String> codeDescriptions;
    
    private DtcCodeDatabase() {
        codeDescriptions = new HashMap<>();
        loadDtcCodes();
    }
    
    public static DtcCodeDatabase getInstance() {
        if (instance == null) {
            instance = new DtcCodeDatabase();
        }
        return instance;
    }
    
    public String lookup(String code) {
        return codeDescriptions.getOrDefault(code, "Unknown DTC code");
    }
    
    private void loadDtcCodes() {
        // Load common DTC codes
        // P0xxx - Generic powertrain
        codeDescriptions.put("P0301", "Cylinder 1 Misfire Detected");
        codeDescriptions.put("P0302", "Cylinder 2 Misfire Detected");
        codeDescriptions.put("P0303", "Cylinder 3 Misfire Detected");
        codeDescriptions.put("P0304", "Cylinder 4 Misfire Detected");
        codeDescriptions.put("P0305", "Cylinder 5 Misfire Detected");
        codeDescriptions.put("P0306", "Cylinder 6 Misfire Detected");
        codeDescriptions.put("P0420", "Catalyst System Efficiency Below Threshold");
        codeDescriptions.put("P0171", "System Too Lean (Bank 1)");
        codeDescriptions.put("P0172", "System Too Rich (Bank 1)");
        codeDescriptions.put("P0174", "System Too Lean (Bank 2)");
        codeDescriptions.put("P0175", "System Too Rich (Bank 2)");
        codeDescriptions.put("P0101", "Mass Air Flow (MAF) Circuit Range/Performance");
        codeDescriptions.put("P0102", "Mass Air Flow (MAF) Circuit Low Input");
        codeDescriptions.put("P0103", "Mass Air Flow (MAF) Circuit High Input");
        codeDescriptions.put("P0112", "Intake Air Temperature (IAT) Circuit Low Input");
        codeDescriptions.put("P0113", "Intake Air Temperature (IAT) Circuit High Input");
        codeDescriptions.put("P0128", "Coolant Thermostat (Coolant Temperature Below Thermostat Regulating Temperature)");
        codeDescriptions.put("P0131", "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1)");
        codeDescriptions.put("P0132", "O2 Sensor Circuit High Voltage (Bank 1 Sensor 1)");
        codeDescriptions.put("P0133", "O2 Sensor Circuit Slow Response (Bank 1 Sensor 1)");
        codeDescriptions.put("P0135", "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 1)");
        codeDescriptions.put("P0136", "O2 Sensor Circuit Malfunction (Bank 1 Sensor 2)");
        codeDescriptions.put("P0137", "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 2)");
        codeDescriptions.put("P0138", "O2 Sensor Circuit High Voltage (Bank 1 Sensor 2)");
        codeDescriptions.put("P0140", "O2 Sensor Circuit No Activity Detected (Bank 1 Sensor 2)");
        codeDescriptions.put("P0141", "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 2)");
        codeDescriptions.put("P0171", "System Too Lean (Bank 1)");
        codeDescriptions.put("P0172", "System Too Rich (Bank 1)");
        codeDescriptions.put("P0201", "Injector Circuit Malfunction - Cylinder 1");
        codeDescriptions.put("P0202", "Injector Circuit Malfunction - Cylinder 2");
        codeDescriptions.put("P0203", "Injector Circuit Malfunction - Cylinder 3");
        codeDescriptions.put("P0204", "Injector Circuit Malfunction - Cylinder 4");
        codeDescriptions.put("P0205", "Injector Circuit Malfunction - Cylinder 5");
        codeDescriptions.put("P0206", "Injector Circuit Malfunction - Cylinder 6");
        codeDescriptions.put("P0217", "Engine Coolant Over Temperature Condition");
        codeDescriptions.put("P0218", "Transmission Fluid Over Temperature Condition");
        codeDescriptions.put("P0222", "Throttle/Pedal Position Sensor/Switch B Circuit Low Input");
        codeDescriptions.put("P0223", "Throttle/Pedal Position Sensor/Switch B Circuit High Input");
        codeDescriptions.put("P0230", "Fuel Pump Primary Circuit Malfunction");
        codeDescriptions.put("P0231", "Fuel Pump Secondary Circuit Low");
        codeDescriptions.put("P0232", "Fuel Pump Secondary Circuit High");
        codeDescriptions.put("P0300", "Random/Multiple Cylinder Misfire Detected");
        codeDescriptions.put("P0301", "Cylinder 1 Misfire Detected");
        codeDescriptions.put("P0302", "Cylinder 2 Misfire Detected");
        codeDescriptions.put("P0303", "Cylinder 3 Misfire Detected");
        codeDescriptions.put("P0304", "Cylinder 4 Misfire Detected");
        codeDescriptions.put("P0305", "Cylinder 5 Misfire Detected");
        codeDescriptions.put("P0306", "Cylinder 6 Misfire Detected");
        codeDescriptions.put("P0307", "Cylinder 7 Misfire Detected");
        codeDescriptions.put("P0308", "Cylinder 8 Misfire Detected");
        codeDescriptions.put("P0316", "Misfire Detected on Startup (First 1000 Revolutions)");
        codeDescriptions.put("P0325", "Knock Sensor 1 Circuit Malfunction (Bank 1 or Single Sensor)");
        codeDescriptions.put("P0326", "Knock Sensor 1 Circuit Range/Performance (Bank 1 or Single Sensor)");
        codeDescriptions.put("P0335", "Crankshaft Position Sensor A Circuit Malfunction");
        codeDescriptions.put("P0336", "Crankshaft Position Sensor A Circuit Range/Performance");
        codeDescriptions.put("P0340", "Camshaft Position Sensor Circuit Malfunction");
        codeDescriptions.put("P0341", "Camshaft Position Sensor Circuit Range/Performance");
        codeDescriptions.put("P0342", "Camshaft Position Sensor Circuit Low Input");
        codeDescriptions.put("P0343", "Camshaft Position Sensor Circuit High Input");
        codeDescriptions.put("P0401", "Exhaust Gas Recirculation (EGR) Flow Insufficient Detected");
        codeDescriptions.put("P0402", "Exhaust Gas Recirculation (EGR) Flow Excessive Detected");
        codeDescriptions.put("P0403", "Exhaust Gas Recirculation (EGR) Circuit Malfunction");
        codeDescriptions.put("P0404", "Exhaust Gas Recirculation (EGR) Circuit Range/Performance");
        codeDescriptions.put("P0405", "Exhaust Gas Recirculation (EGR) Sensor A Circuit Low");
        codeDescriptions.put("P0406", "Exhaust Gas Recirculation (EGR) Sensor A Circuit High");
        codeDescriptions.put("P0410", "Secondary Air Injection System Malfunction");
        codeDescriptions.put("P0411", "Secondary Air Injection System Incorrect Flow Detected");
        codeDescriptions.put("P0412", "Secondary Air Injection System Switching Valve A Circuit Malfunction");
        codeDescriptions.put("P0413", "Secondary Air Injection System Switching Valve A Circuit Open");
        codeDescriptions.put("P0414", "Secondary Air Injection System Switching Valve A Circuit Shorted");
        codeDescriptions.put("P0415", "Secondary Air Injection System Switching Valve B Circuit Malfunction");
        codeDescriptions.put("P0416", "Secondary Air Injection System Switching Valve B Circuit Open");
        codeDescriptions.put("P0417", "Secondary Air Injection System Switching Valve B Circuit Shorted");
        codeDescriptions.put("P0420", "Catalyst System Efficiency Below Threshold (Bank 1)");
        codeDescriptions.put("P0421", "Warm Up Catalyst Efficiency Below Threshold (Bank 1)");
        codeDescriptions.put("P0422", "Main Catalyst Efficiency Below Threshold (Bank 1)");
        codeDescriptions.put("P0423", "Heated Catalyst Efficiency Below Threshold (Bank 1)");
        codeDescriptions.put("P0430", "Catalyst System Efficiency Below Threshold (Bank 2)");
        codeDescriptions.put("P0431", "Warm Up Catalyst Efficiency Below Threshold (Bank 2)");
        codeDescriptions.put("P0432", "Main Catalyst Efficiency Below Threshold (Bank 2)");
        codeDescriptions.put("P0433", "Heated Catalyst Efficiency Below Threshold (Bank 2)");
        codeDescriptions.put("P0440", "Evaporative Emission Control System Malfunction");
        codeDescriptions.put("P0441", "Evaporative Emission Control System Incorrect Purge Flow");
        codeDescriptions.put("P0442", "Evaporative Emission Control System Leak Detected (Small Leak)");
        codeDescriptions.put("P0443", "Evaporative Emission Control System Purge Control Valve Circuit Malfunction");
        codeDescriptions.put("P0444", "Evaporative Emission Control System Purge Control Valve Circuit Open");
        codeDescriptions.put("P0445", "Evaporative Emission Control System Purge Control Valve Circuit Shorted");
        codeDescriptions.put("P0446", "Evaporative Emission Control System Vent Control Circuit Malfunction");
        codeDescriptions.put("P0447", "Evaporative Emission Control System Vent Control Circuit Open");
        codeDescriptions.put("P0448", "Evaporative Emission Control System Vent Control Circuit Shorted");
        codeDescriptions.put("P0449", "Evaporative Emission Control System Vent Valve/Solenoid Circuit Malfunction");
        codeDescriptions.put("P0450", "Evaporative Emission Control System Pressure Sensor Malfunction");
        codeDescriptions.put("P0451", "Evaporative Emission Control System Pressure Sensor Range/Performance");
        codeDescriptions.put("P0452", "Evaporative Emission Control System Pressure Sensor Low Input");
        codeDescriptions.put("P0453", "Evaporative Emission Control System Pressure Sensor High Input");
        codeDescriptions.put("P0454", "Evaporative Emission Control System Pressure Sensor Intermittent");
        codeDescriptions.put("P0455", "Evaporative Emission Control System Leak Detected (Gross Leak)");
        codeDescriptions.put("P0456", "Evaporative Emission Control System Leak Detected (Very Small Leak)");
        codeDescriptions.put("P0457", "Evaporative Emission Control System Leak Detected (Fuel Cap Loose/Off)");
        codeDescriptions.put("P0458", "Evaporative Emission Control System Purge Control Valve Circuit Low");
        codeDescriptions.put("P0459", "Evaporative Emission Control System Purge Control Valve Circuit High");
        codeDescriptions.put("P0460", "Fuel Level Sensor Circuit Malfunction");
        codeDescriptions.put("P0461", "Fuel Level Sensor Circuit Range/Performance");
        codeDescriptions.put("P0462", "Fuel Level Sensor Circuit Low Input");
        codeDescriptions.put("P0463", "Fuel Level Sensor Circuit High Input");
        codeDescriptions.put("P0464", "Fuel Level Sensor Circuit Intermittent");
        codeDescriptions.put("P0465", "Purge Flow Sensor Circuit Malfunction");
        codeDescriptions.put("P0466", "Purge Flow Sensor Circuit Range/Performance");
        codeDescriptions.put("P0467", "Purge Flow Sensor Circuit Low Input");
        codeDescriptions.put("P0468", "Purge Flow Sensor Circuit High Input");
        codeDescriptions.put("P0469", "Purge Flow Sensor Circuit Intermittent");
        codeDescriptions.put("P0470", "Exhaust Pressure Sensor Malfunction");
        codeDescriptions.put("P0471", "Exhaust Pressure Sensor Range/Performance");
        codeDescriptions.put("P0472", "Exhaust Pressure Sensor Low");
        codeDescriptions.put("P0473", "Exhaust Pressure Sensor High");
        codeDescriptions.put("P0474", "Exhaust Pressure Sensor Intermittent");
        codeDescriptions.put("P0475", "Exhaust Pressure Control Valve Malfunction");
        codeDescriptions.put("P0476", "Exhaust Pressure Control Valve Range/Performance");
        codeDescriptions.put("P0477", "Exhaust Pressure Control Valve Low");
        codeDescriptions.put("P0478", "Exhaust Pressure Control Valve High");
        codeDescriptions.put("P0479", "Exhaust Pressure Control Valve Intermittent");
        codeDescriptions.put("P0480", "Cooling Fan 1 Control Circuit Malfunction");
        codeDescriptions.put("P0481", "Cooling Fan 2 Control Circuit Malfunction");
        codeDescriptions.put("P0482", "Cooling Fan 3 Control Circuit Malfunction");
        codeDescriptions.put("P0483", "Cooling Fan Rationality Check Malfunction");
        codeDescriptions.put("P0484", "Cooling Fan Circuit Over Current");
        codeDescriptions.put("P0485", "Cooling Fan Power/Ground Circuit Malfunction");
        
        // B0xxx - Body codes
        codeDescriptions.put("B0001", "Driver Airbag Circuit Short to Battery");
        codeDescriptions.put("B0002", "Driver Airbag Circuit Short to Ground");
        codeDescriptions.put("B0003", "Driver Airbag Circuit Resistance Out of Range");
        
        // C0xxx - Chassis codes
        codeDescriptions.put("C0001", "ABS Control Module");
        codeDescriptions.put("C0002", "ABS Control Module Signal");
        
        // U0xxx - Network codes
        codeDescriptions.put("U0001", "High Speed CAN Communication Bus");
        codeDescriptions.put("U0002", "High Speed CAN Communication Bus Performance");
        codeDescriptions.put("U0003", "High Speed CAN Communication Bus (+) Open");
        codeDescriptions.put("U0004", "High Speed CAN Communication Bus (+) Low");
        codeDescriptions.put("U0005", "High Speed CAN Communication Bus (+) High");
        codeDescriptions.put("U0006", "High Speed CAN Communication Bus (-) Open");
        codeDescriptions.put("U0007", "High Speed CAN Communication Bus (-) Low");
        codeDescriptions.put("U0008", "High Speed CAN Communication Bus (-) High");
        codeDescriptions.put("U0009", "High Speed CAN Communication Bus (-) Shorted to Bus (+)");
        codeDescriptions.put("U0010", "Medium Speed CAN Communication Bus");
        codeDescriptions.put("U0011", "Medium Speed CAN Communication Bus Performance");
        codeDescriptions.put("U0012", "Medium Speed CAN Communication Bus (+) Open");
        codeDescriptions.put("U0013", "Medium Speed CAN Communication Bus (+) Low");
        codeDescriptions.put("U0014", "Medium Speed CAN Communication Bus (+) High");
        codeDescriptions.put("U0015", "Medium Speed CAN Communication Bus (-) Open");
        codeDescriptions.put("U0016", "Medium Speed CAN Communication Bus (-) Low");
        codeDescriptions.put("U0017", "Medium Speed CAN Communication Bus (-) High");
        codeDescriptions.put("U0018", "Medium Speed CAN Communication Bus (-) Shorted to Bus (+)");
        codeDescriptions.put("U0019", "Low Speed CAN Communication Bus");
        codeDescriptions.put("U0020", "Low Speed CAN Communication Bus Performance");
        codeDescriptions.put("U0021", "Low Speed CAN Communication Bus (+) Open");
        codeDescriptions.put("U0022", "Low Speed CAN Communication Bus (+) Low");
        codeDescriptions.put("U0023", "Low Speed CAN Communication Bus (+) High");
        codeDescriptions.put("U0024", "Low Speed CAN Communication Bus (-) Open");
        codeDescriptions.put("U0025", "Low Speed CAN Communication Bus (-) Low");
        codeDescriptions.put("U0026", "Low Speed CAN Communication Bus (-) High");
        codeDescriptions.put("U0027", "Low Speed CAN Communication Bus (-) Shorted to Bus (+)");
        codeDescriptions.put("U0028", "Vehicle Communication Bus A");
        codeDescriptions.put("U0029", "Vehicle Communication Bus A Performance");
        codeDescriptions.put("U0030", "Vehicle Communication Bus A (+) Open");
        codeDescriptions.put("U0031", "Vehicle Communication Bus A (+) Low");
        codeDescriptions.put("U0032", "Vehicle Communication Bus A (+) High");
        codeDescriptions.put("U0033", "Vehicle Communication Bus A (-) Open");
        codeDescriptions.put("U0034", "Vehicle Communication Bus A (-) Low");
        codeDescriptions.put("U0035", "Vehicle Communication Bus A (-) High");
        codeDescriptions.put("U0036", "Vehicle Communication Bus A (-) Shorted to Bus A (+)");
        codeDescriptions.put("U0037", "Vehicle Communication Bus B");
        codeDescriptions.put("U0038", "Vehicle Communication Bus B Performance");
        codeDescriptions.put("U0039", "Vehicle Communication Bus B (+) Open");
        codeDescriptions.put("U0040", "Vehicle Communication Bus B (+) Low");
        codeDescriptions.put("U0041", "Vehicle Communication Bus B (+) High");
        codeDescriptions.put("U0042", "Vehicle Communication Bus B (-) Open");
        codeDescriptions.put("U0043", "Vehicle Communication Bus B (-) Low");
        codeDescriptions.put("U0044", "Vehicle Communication Bus B (-) High");
        codeDescriptions.put("U0045", "Vehicle Communication Bus B (-) Shorted to Bus B (+)");
        codeDescriptions.put("U0046", "Vehicle Communication Bus C");
        codeDescriptions.put("U0047", "Vehicle Communication Bus C Performance");
        codeDescriptions.put("U0048", "Vehicle Communication Bus C (+) Open");
        codeDescriptions.put("U0049", "Vehicle Communication Bus C (+) Low");
        codeDescriptions.put("U0050", "Vehicle Communication Bus C (+) High");
        codeDescriptions.put("U0051", "Vehicle Communication Bus C (-) Open");
        codeDescriptions.put("U0052", "Vehicle Communication Bus C (-) Low");
        codeDescriptions.put("U0053", "Vehicle Communication Bus C (-) High");
        codeDescriptions.put("U0054", "Vehicle Communication Bus C (-) Shorted to Bus C (+)");
        codeDescriptions.put("U0055", "Vehicle Communication Bus D");
        codeDescriptions.put("U0056", "Vehicle Communication Bus D Performance");
        codeDescriptions.put("U0057", "Vehicle Communication Bus D (+) Open");
        codeDescriptions.put("U0058", "Vehicle Communication Bus D (+) Low");
        codeDescriptions.put("U0059", "Vehicle Communication Bus D (+) High");
        codeDescriptions.put("U0060", "Vehicle Communication Bus D (-) Open");
        codeDescriptions.put("U0061", "Vehicle Communication Bus D (-) Low");
        codeDescriptions.put("U0062", "Vehicle Communication Bus D (-) High");
        codeDescriptions.put("U0063", "Vehicle Communication Bus D (-) Shorted to Bus D (+)");
        codeDescriptions.put("U0064", "Vehicle Communication Bus E");
        codeDescriptions.put("U0065", "Vehicle Communication Bus E Performance");
        codeDescriptions.put("U0066", "Vehicle Communication Bus E (+) Open");
        codeDescriptions.put("U0067", "Vehicle Communication Bus E (+) Low");
        codeDescriptions.put("U0068", "Vehicle Communication Bus E (+) High");
        codeDescriptions.put("U0069", "Vehicle Communication Bus E (-) Open");
        codeDescriptions.put("U0070", "Vehicle Communication Bus E (-) Low");
        codeDescriptions.put("U0071", "Vehicle Communication Bus E (-) High");
        codeDescriptions.put("U0072", "Vehicle Communication Bus E (-) Shorted to Bus E (+)");
        codeDescriptions.put("U0073", "Control Module Communication Bus Off");
        codeDescriptions.put("U0074", "Reserved by SAE");
        codeDescriptions.put("U0075", "Reserved by SAE");
        codeDescriptions.put("U0076", "Reserved by SAE");
        codeDescriptions.put("U0077", "Reserved by SAE");
        codeDescriptions.put("U0078", "Reserved by SAE");
        codeDescriptions.put("U0079", "Reserved by SAE");
        codeDescriptions.put("U0080", "Vehicle Communication Bus A");
        codeDescriptions.put("U0081", "Vehicle Communication Bus A Performance");
        codeDescriptions.put("U0082", "Vehicle Communication Bus A (+) Open");
        codeDescriptions.put("U0083", "Vehicle Communication Bus A (+) Low");
        codeDescriptions.put("U0084", "Vehicle Communication Bus A (+) High");
        codeDescriptions.put("U0085", "Vehicle Communication Bus A (-) Open");
        codeDescriptions.put("U0086", "Vehicle Communication Bus A (-) Low");
        codeDescriptions.put("U0087", "Vehicle Communication Bus A (-) High");
        codeDescriptions.put("U0088", "Vehicle Communication Bus A (-) Shorted to Bus A (+)");
        codeDescriptions.put("U0089", "Vehicle Communication Bus B");
        codeDescriptions.put("U0090", "Vehicle Communication Bus B Performance");
        codeDescriptions.put("U0091", "Vehicle Communication Bus B (+) Open");
        codeDescriptions.put("U0092", "Vehicle Communication Bus B (+) Low");
        codeDescriptions.put("U0093", "Vehicle Communication Bus B (+) High");
        codeDescriptions.put("U0094", "Vehicle Communication Bus B (-) Open");
        codeDescriptions.put("U0095", "Vehicle Communication Bus B (-) Low");
        codeDescriptions.put("U0096", "Vehicle Communication Bus B (-) High");
        codeDescriptions.put("U0097", "Vehicle Communication Bus B (-) Shorted to Bus B (+)");
        codeDescriptions.put("U0098", "Vehicle Communication Bus C");
        codeDescriptions.put("U0099", "Vehicle Communication Bus C Performance");
        codeDescriptions.put("U0100", "Lost Communication with ECM/PCM A");
        codeDescriptions.put("U0101", "Lost Communication with TCM");
        codeDescriptions.put("U0102", "Lost Communication with Transfer Case Control Module");
        codeDescriptions.put("U0103", "Lost Communication with Gear Shift Module");
        codeDescriptions.put("U0104", "Lost Communication with Cruise Control Module");
        codeDescriptions.put("U0105", "Lost Communication with Fuel Injector Control Module");
        codeDescriptions.put("U0106", "Lost Communication with Glow Plug Control Module");
        codeDescriptions.put("U0107", "Lost Communication with Throttle Actuator Control Module");
        codeDescriptions.put("U0108", "Lost Communication with Alternative Fuel Control Module");
        codeDescriptions.put("U0109", "Lost Communication with Fuel Pump Control Module");
        codeDescriptions.put("U0110", "Lost Communication with Drive Motor Control Module A");
        codeDescriptions.put("U0111", "Lost Communication with Battery Energy Control Module A");
        codeDescriptions.put("U0112", "Lost Communication with Battery Energy Control Module B");
        codeDescriptions.put("U0113", "Lost Communication with Emissions Critical Control Information");
        codeDescriptions.put("U0114", "Lost Communication with Four-Wheel Drive Clutch Control Module");
        codeDescriptions.put("U0115", "Lost Communication with ECM/PCM B");
        codeDescriptions.put("U0116", "Lost Communication with Coolant Temperature Control Module");
        codeDescriptions.put("U0117", "Lost Communication with Transmission Control Module");
        codeDescriptions.put("U0118", "Lost Communication with Transmission Control Module");
        codeDescriptions.put("U0119", "Lost Communication with Hybrid Control Module");
        codeDescriptions.put("U0120", "Lost Communication with Hybrid Control Module");
        codeDescriptions.put("U0121", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0122", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0123", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0124", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0125", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0126", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0127", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0128", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0129", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0130", "Lost Communication with Body Control Module");
        codeDescriptions.put("U0131", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0132", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0133", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0134", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0135", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0136", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0137", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0138", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0139", "Lost Communication with Power Steering Control Module");
        codeDescriptions.put("U0140", "Lost Communication with Body Control Module A");
        codeDescriptions.put("U0141", "Lost Communication with Body Control Module B");
        codeDescriptions.put("U0142", "Lost Communication with Body Control Module C");
        codeDescriptions.put("U0143", "Lost Communication with Body Control Module D");
        codeDescriptions.put("U0144", "Lost Communication with Body Control Module E");
        codeDescriptions.put("U0145", "Lost Communication with Body Control Module F");
        codeDescriptions.put("U0146", "Lost Communication with Body Control Module G");
        codeDescriptions.put("U0147", "Lost Communication with Body Control Module H");
        codeDescriptions.put("U0148", "Lost Communication with Body Control Module I");
        codeDescriptions.put("U0149", "Lost Communication with Body Control Module J");
        codeDescriptions.put("U0150", "Lost Communication with Body Control Module K");
        codeDescriptions.put("U0151", "Lost Communication with Body Control Module L");
        codeDescriptions.put("U0152", "Lost Communication with Body Control Module M");
        codeDescriptions.put("U0153", "Lost Communication with Body Control Module N");
        codeDescriptions.put("U0154", "Lost Communication with Body Control Module O");
        codeDescriptions.put("U0155", "Lost Communication with Gateway A");
        codeDescriptions.put("U0156", "Lost Communication with Gateway B");
        codeDescriptions.put("U0157", "Lost Communication with Gateway C");
        codeDescriptions.put("U0158", "Lost Communication with Gateway D");
        codeDescriptions.put("U0159", "Lost Communication with Gateway E");
        codeDescriptions.put("U0160", "Lost Communication with Gateway F");
        codeDescriptions.put("U0161", "Lost Communication with Gateway G");
        codeDescriptions.put("U0162", "Lost Communication with Gateway H");
        codeDescriptions.put("U0163", "Lost Communication with Gateway I");
        codeDescriptions.put("U0164", "Lost Communication with Gateway J");
        codeDescriptions.put("U0165", "Lost Communication with Gateway K");
        codeDescriptions.put("U0166", "Lost Communication with Gateway L");
        codeDescriptions.put("U0167", "Lost Communication with Gateway M");
        codeDescriptions.put("U0168", "Lost Communication with Gateway N");
        codeDescriptions.put("U0169", "Lost Communication with Gateway O");
        codeDescriptions.put("U0170", "Lost Communication with Gateway P");
        codeDescriptions.put("U0171", "Lost Communication with Gateway Q");
        codeDescriptions.put("U0172", "Lost Communication with Gateway R");
        codeDescriptions.put("U0173", "Lost Communication with Gateway S");
        codeDescriptions.put("U0174", "Lost Communication with Gateway T");
        codeDescriptions.put("U0175", "Lost Communication with Gateway U");
        codeDescriptions.put("U0176", "Lost Communication with Gateway V");
        codeDescriptions.put("U0177", "Lost Communication with Gateway W");
        codeDescriptions.put("U0178", "Lost Communication with Gateway X");
        codeDescriptions.put("U0179", "Lost Communication with Gateway Y");
        codeDescriptions.put("U0180", "Lost Communication with Gateway Z");
        codeDescriptions.put("U0181", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0182", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0183", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0184", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0185", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0186", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0187", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0188", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0189", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0190", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0191", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0192", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0193", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0194", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0195", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0196", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0197", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0198", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0199", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
        codeDescriptions.put("U0200", "Lost Communication with Anti-Lock Brake System (ABS) Control Module");
    }
}
