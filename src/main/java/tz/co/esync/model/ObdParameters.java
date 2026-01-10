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
package tz.co.esync.model;

import tz.co.esync.storage.StorageName;
import java.util.Date;

@StorageName("tc_obd_parameters")
public class ObdParameters extends BaseModel {
    
    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private long positionId;

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    private Date timestamp;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    // Engine parameters
    private Integer engineRpm;

    public Integer getEngineRpm() {
        return engineRpm;
    }

    public void setEngineRpm(Integer engineRpm) {
        this.engineRpm = engineRpm;
    }

    private Integer coolantTemperature;  // Celsius

    public Integer getCoolantTemperature() {
        return coolantTemperature;
    }

    public void setCoolantTemperature(Integer coolantTemperature) {
        this.coolantTemperature = coolantTemperature;
    }

    private Double oilPressure;

    public Double getOilPressure() {
        return oilPressure;
    }

    public void setOilPressure(Double oilPressure) {
        this.oilPressure = oilPressure;
    }

    private Double batteryVoltage;

    public Double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(Double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    private Integer throttlePosition;     // Percentage

    public Integer getThrottlePosition() {
        return throttlePosition;
    }

    public void setThrottlePosition(Integer throttlePosition) {
        this.throttlePosition = throttlePosition;
    }

    private Integer intakeAirTemperature;

    public Integer getIntakeAirTemperature() {
        return intakeAirTemperature;
    }

    public void setIntakeAirTemperature(Integer intakeAirTemperature) {
        this.intakeAirTemperature = intakeAirTemperature;
    }

    private Double mafAirFlow;            // g/s

    public Double getMafAirFlow() {
        return mafAirFlow;
    }

    public void setMafAirFlow(Double mafAirFlow) {
        this.mafAirFlow = mafAirFlow;
    }
    
    // Fuel parameters
    private Integer fuelLevel;            // Percentage

    public Integer getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(Integer fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    private Double fuelConsumptionRate;   // L/h

    public Double getFuelConsumptionRate() {
        return fuelConsumptionRate;
    }

    public void setFuelConsumptionRate(Double fuelConsumptionRate) {
        this.fuelConsumptionRate = fuelConsumptionRate;
    }

    private Double fuelPressure;

    public Double getFuelPressure() {
        return fuelPressure;
    }

    public void setFuelPressure(Double fuelPressure) {
        this.fuelPressure = fuelPressure;
    }
    
    // Vehicle parameters
    private Integer vehicleSpeed;         // km/h

    public Integer getVehicleSpeed() {
        return vehicleSpeed;
    }

    public void setVehicleSpeed(Integer vehicleSpeed) {
        this.vehicleSpeed = vehicleSpeed;
    }

    private Integer engineLoad;          // Percentage

    public Integer getEngineLoad() {
        return engineLoad;
    }

    public void setEngineLoad(Integer engineLoad) {
        this.engineLoad = engineLoad;
    }

    private Double timingAdvance;

    public Double getTimingAdvance() {
        return timingAdvance;
    }

    public void setTimingAdvance(Double timingAdvance) {
        this.timingAdvance = timingAdvance;
    }
}
