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
package tz.co.esync.fuel;

import tz.co.esync.helper.DistanceCalculator;
import tz.co.esync.model.FuelConsumption;
import tz.co.esync.model.ObdParameters;
import tz.co.esync.model.Position;

import java.util.Date;
import java.util.List;

public class FuelCalculator {
    
    /**
     * Calculate fuel consumption from OBD data and positions
     */
    public FuelConsumption calculateFuelConsumption(
            List<Position> positions, 
            List<ObdParameters> obdData) {
        
        FuelConsumption fuel = new FuelConsumption();
        
        if (obdData == null || obdData.isEmpty() || positions == null || positions.size() < 2) {
            return fuel;
        }
        
        double totalFuel = 0.0;
        double totalDistance = 0.0;
        Date startTime = null;
        Date endTime = null;
        
        for (int i = 1; i < obdData.size(); i++) {
            ObdParameters prev = obdData.get(i - 1);
            ObdParameters curr = obdData.get(i);
            
            if (startTime == null) {
                startTime = prev.getTimestamp();
            }
            endTime = curr.getTimestamp();
            
            // Calculate time difference (hours)
            long timeDiff = curr.getTimestamp().getTime() - prev.getTimestamp().getTime();
            double hours = timeDiff / (1000.0 * 60.0 * 60.0);
            
            // Calculate fuel consumed (L/h * hours)
            if (curr.getFuelConsumptionRate() != null && hours > 0) {
                double fuelConsumed = curr.getFuelConsumptionRate() * hours;
                totalFuel += fuelConsumed;
            }
            
            // Calculate distance from positions
            if (i < positions.size()) {
                double distance = calculateDistance(
                    positions.get(i - 1), 
                    positions.get(i)
                );
                totalDistance += distance;
            }
        }
        
        // Set device ID from first position
        if (!positions.isEmpty()) {
            fuel.setDeviceId(positions.get(0).getDeviceId());
        }
        
        // Calculate fuel economy
        if (totalDistance > 0 && totalFuel > 0) {
            fuel.setFuelEconomy(totalDistance / totalFuel);  // km/L
            fuel.setFuelConsumption(totalFuel);              // Liters
            fuel.setDistance(totalDistance);                 // Kilometers
            fuel.setStartTime(startTime);
            fuel.setEndTime(endTime);
        }
        
        return fuel;
    }
    
    /**
     * Calculate fuel consumption rate from MAF (Mass Air Flow)
     */
    public Double calculateFuelRateFromMaf(ObdParameters obd) {
        if (obd.getMafAirFlow() == null || obd.getEngineRpm() == null) {
            return null;
        }
        
        // Simplified formula: Fuel rate ≈ MAF * (RPM/1000) * conversion factor
        // Actual formula depends on vehicle and fuel type
        double maf = obd.getMafAirFlow();  // g/s
        double rpm = obd.getEngineRpm();
        
        // Conversion: g/s to L/h
        // Assuming gasoline: 1L ≈ 750g, air/fuel ratio ≈ 14.7:1
        double fuelRate = (maf / 14.7) * (rpm / 1000.0) * (3600.0 / 750.0);
        
        return fuelRate;
    }
    
    /**
     * Calculate fuel consumption from position attributes (fallback method)
     */
    public FuelConsumption calculateFromPositions(List<Position> positions) {
        FuelConsumption fuel = new FuelConsumption();
        
        if (positions == null || positions.size() < 2) {
            return fuel;
        }
        
        double totalFuel = 0.0;
        double totalDistance = 0.0;
        Date startTime = positions.get(0).getFixTime();
        Date endTime = positions.get(positions.size() - 1).getFixTime();
        
        for (int i = 1; i < positions.size(); i++) {
            Position prev = positions.get(i - 1);
            Position curr = positions.get(i);
            
            // Calculate distance
            double distance = calculateDistance(prev, curr);
            totalDistance += distance;
            
            // Get fuel consumption rate if available
            if (curr.hasAttribute(Position.KEY_FUEL_CONSUMPTION)) {
                double fuelRate = curr.getDouble(Position.KEY_FUEL_CONSUMPTION);
                long timeDiff = curr.getFixTime().getTime() - prev.getFixTime().getTime();
                double hours = timeDiff / (1000.0 * 60.0 * 60.0);
                if (hours > 0) {
                    totalFuel += fuelRate * hours;
                }
            }
        }
        
        fuel.setDeviceId(positions.get(0).getDeviceId());
        
        if (totalDistance > 0 && totalFuel > 0) {
            fuel.setFuelEconomy(totalDistance / totalFuel);
            fuel.setFuelConsumption(totalFuel);
            fuel.setDistance(totalDistance);
            fuel.setStartTime(startTime);
            fuel.setEndTime(endTime);
        }
        
        return fuel;
    }
    
    private double calculateDistance(Position pos1, Position pos2) {
        // Use DistanceCalculator helper - returns distance in meters, convert to kilometers
        if (pos1.getValid() && pos2.getValid()) {
            double distanceMeters = DistanceCalculator.distance(
                pos1.getLatitude(), pos1.getLongitude(),
                pos2.getLatitude(), pos2.getLongitude()
            );
            return distanceMeters / 1000.0;  // Convert meters to kilometers
        }
        return 0.0;
    }
}
