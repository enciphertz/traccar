package tz.co.esync.fuel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.co.esync.model.FuelConsumption;
import tz.co.esync.model.ObdParameters;
import tz.co.esync.model.Position;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FuelCalculatorTest {

    private FuelCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new FuelCalculator();
    }

    @Test
    public void testCalculateFuelConsumption() {
        List<Position> positions = new ArrayList<>();
        List<ObdParameters> obdData = new ArrayList<>();
        
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + 3600000); // 1 hour later
        
        // Create positions
        Position pos1 = new Position();
        pos1.setId(1L);
        pos1.setDeviceId(100L);
        pos1.setFixTime(startTime);
        pos1.setValid(true);
        pos1.setLatitude(40.7128);
        pos1.setLongitude(-74.0060);
        positions.add(pos1);
        
        Position pos2 = new Position();
        pos2.setId(2L);
        pos2.setDeviceId(100L);
        pos2.setFixTime(endTime);
        pos2.setValid(true);
        pos2.setLatitude(40.7138);
        pos2.setLongitude(-74.0070);
        positions.add(pos2);
        
        // Create OBD data
        ObdParameters obd1 = new ObdParameters();
        obd1.setDeviceId(100L);
        obd1.setPositionId(1L);
        obd1.setTimestamp(startTime);
        obd1.setFuelConsumptionRate(10.0); // 10 L/h
        obdData.add(obd1);
        
        ObdParameters obd2 = new ObdParameters();
        obd2.setDeviceId(100L);
        obd2.setPositionId(2L);
        obd2.setTimestamp(endTime);
        obd2.setFuelConsumptionRate(10.0); // 10 L/h
        obdData.add(obd2);
        
        FuelConsumption fuel = calculator.calculateFuelConsumption(positions, obdData);
        
        assertNotNull(fuel);
        assertEquals(100L, fuel.getDeviceId());
        assertNotNull(fuel.getFuelConsumption());
        assertTrue(fuel.getFuelConsumption() > 0);
        assertNotNull(fuel.getDistance());
        assertNotNull(fuel.getFuelEconomy());
        assertNotNull(fuel.getStartTime());
        assertNotNull(fuel.getEndTime());
    }

    @Test
    public void testCalculateFuelRateFromMaf() {
        ObdParameters obd = new ObdParameters();
        obd.setMafAirFlow(50.0); // 50 g/s
        obd.setEngineRpm(2000);
        
        Double fuelRate = calculator.calculateFuelRateFromMaf(obd);
        
        assertNotNull(fuelRate);
        assertTrue(fuelRate > 0);
    }

    @Test
    public void testCalculateFuelRateFromMafNull() {
        ObdParameters obd = new ObdParameters();
        obd.setMafAirFlow(null);
        obd.setEngineRpm(2000);
        
        Double fuelRate = calculator.calculateFuelRateFromMaf(obd);
        
        assertNull(fuelRate);
    }

    @Test
    public void testCalculateFromPositions() {
        List<Position> positions = new ArrayList<>();
        
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + 3600000); // 1 hour later
        
        Position pos1 = new Position();
        pos1.setId(1L);
        pos1.setDeviceId(100L);
        pos1.setFixTime(startTime);
        pos1.setValid(true);
        pos1.setLatitude(40.7128);
        pos1.setLongitude(-74.0060);
        positions.add(pos1);
        
        Position pos2 = new Position();
        pos2.setId(2L);
        pos2.setDeviceId(100L);
        pos2.setFixTime(endTime);
        pos2.setValid(true);
        pos2.setLatitude(40.7138);
        pos2.setLongitude(-74.0070);
        pos2.set(Position.KEY_FUEL_CONSUMPTION, 8.5); // 8.5 L/h
        positions.add(pos2);
        
        FuelConsumption fuel = calculator.calculateFromPositions(positions);
        
        assertNotNull(fuel);
        assertEquals(100L, fuel.getDeviceId());
        assertNotNull(fuel.getDistance());
    }

    @Test
    public void testCalculateFuelConsumptionEmpty() {
        List<Position> positions = new ArrayList<>();
        List<ObdParameters> obdData = new ArrayList<>();
        
        FuelConsumption fuel = calculator.calculateFuelConsumption(positions, obdData);
        
        assertNotNull(fuel);
        assertNull(fuel.getFuelConsumption());
    }
}
