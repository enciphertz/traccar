package tz.co.esync.obd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.co.esync.model.DtcCode;
import tz.co.esync.model.ObdParameters;
import tz.co.esync.model.Position;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ObdDataParserTest {

    private ObdDataParser parser;

    @BeforeEach
    public void setUp() {
        parser = new ObdDataParser();
    }

    @Test
    public void testParseObdData() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(100L);
        position.setFixTime(new Date());
        
        position.set(Position.KEY_RPM, 2500);
        position.set(Position.KEY_COOLANT_TEMP, 90);
        position.set(Position.KEY_FUEL_LEVEL, 75);
        position.set(Position.KEY_FUEL_CONSUMPTION, 8.5);
        position.set(Position.KEY_POWER, 12.6);
        
        ObdParameters obd = parser.parseObdData(position);
        
        assertNotNull(obd);
        assertEquals(100L, obd.getDeviceId());
        assertEquals(1L, obd.getPositionId());
        assertEquals(2500, obd.getEngineRpm());
        assertEquals(90, obd.getCoolantTemperature());
        assertEquals(75, obd.getFuelLevel());
        assertEquals(8.5, obd.getFuelConsumptionRate());
        assertEquals(12.6, obd.getBatteryVoltage());
    }

    @Test
    public void testParseObdDataWithCustomAttributes() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(100L);
        position.setFixTime(new Date());
        
        position.set("obd.rpm", 3000);
        position.set("obd.coolant_temp", 85);
        position.set("obd.throttle_position", 45);
        position.set("obd.engine_load", 60);
        
        ObdParameters obd = parser.parseObdData(position);
        
        assertNotNull(obd);
        assertEquals(3000, obd.getEngineRpm());
        assertEquals(85, obd.getCoolantTemperature());
        assertEquals(45, obd.getThrottlePosition());
        assertEquals(60, obd.getEngineLoad());
    }

    @Test
    public void testParseDtcCodes() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(100L);
        position.setFixTime(new Date());
        
        position.set(Position.KEY_DTCS, "P0301,P0420,B1234");
        
        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);
        
        assertNotNull(dtcCodes);
        assertEquals(3, dtcCodes.size());
        
        DtcCode dtc1 = dtcCodes.get(0);
        assertEquals("P0301", dtc1.getCode());
        assertEquals("P", dtc1.getCodeType());
        assertEquals("active", dtc1.getStatus());
        assertNotNull(dtc1.getDescription());
        
        DtcCode dtc2 = dtcCodes.get(1);
        assertEquals("P0420", dtc2.getCode());
        assertEquals("P", dtc2.getCodeType());
        
        DtcCode dtc3 = dtcCodes.get(2);
        assertEquals("B1234", dtc3.getCode());
        assertEquals("B", dtc3.getCodeType());
    }

    @Test
    public void testParseDtcCodesSemicolonSeparated() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(100L);
        position.setFixTime(new Date());
        
        position.set(Position.KEY_DTCS, "P0301;P0420");
        
        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);
        
        assertNotNull(dtcCodes);
        assertEquals(2, dtcCodes.size());
    }

    @Test
    public void testParseDtcCodesEmpty() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(100L);
        position.setFixTime(new Date());
        
        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);
        
        assertNotNull(dtcCodes);
        assertTrue(dtcCodes.isEmpty());
    }

    @Test
    public void testParseObdDataEmpty() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(100L);
        position.setFixTime(new Date());
        
        ObdParameters obd = parser.parseObdData(position);
        
        assertNotNull(obd);
        assertEquals(100L, obd.getDeviceId());
        assertEquals(1L, obd.getPositionId());
        assertNull(obd.getEngineRpm());
        assertNull(obd.getCoolantTemperature());
    }
}
