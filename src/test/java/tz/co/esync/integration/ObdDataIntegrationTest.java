package tz.co.esync.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tz.co.esync.BaseTest;
import tz.co.esync.model.Device;
import tz.co.esync.model.DtcCode;
import tz.co.esync.model.ObdParameters;
import tz.co.esync.model.Position;
import tz.co.esync.obd.ObdDataParser;
import tz.co.esync.storage.Storage;
import tz.co.esync.storage.StorageException;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Request;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ObdDataIntegrationTest extends BaseTest {

    private Storage storage;
    private ObdDataParser parser;

    @BeforeEach
    public void setUp() {
        storage = mock(Storage.class);
        parser = new ObdDataParser();
    }

    @Test
    public void testObdDataProcessingPipeline() throws StorageException {
        // Create a device
        Device device = new Device();
        device.setId(1L);
        device.setUniqueId("TEST001");
        device.setName("Test Vehicle");

        // Create position with OBD data
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());
        position.setValid(true);
        position.setLatitude(40.7128);
        position.setLongitude(-74.0060);
        position.setSpeed(50.0);

        // Add OBD parameters
        position.set(Position.KEY_RPM, 2500);
        position.set(Position.KEY_COOLANT_TEMP, 90);
        position.set(Position.KEY_FUEL_LEVEL, 75);
        position.set(Position.KEY_FUEL_CONSUMPTION, 8.5);
        position.set(Position.KEY_POWER, 13.2);
        position.set(Position.KEY_THROTTLE, 25);
        position.set(Position.KEY_ENGINE_LOAD, 45);

        // Parse OBD data
        ObdParameters obd = parser.parseObdData(position);

        assertNotNull(obd);
        assertEquals(1L, obd.getDeviceId());
        assertEquals(1L, obd.getPositionId());
        assertEquals(2500, obd.getEngineRpm());
        assertEquals(90, obd.getCoolantTemperature());
        assertEquals(75, obd.getFuelLevel());
        assertEquals(8.5, obd.getFuelConsumptionRate());
        assertEquals(13.2, obd.getBatteryVoltage());
        assertEquals(25, obd.getThrottlePosition());
        assertEquals(45, obd.getEngineLoad());
    }

    @Test
    public void testDtcCodeProcessing() throws StorageException {
        // Create position with DTC codes
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());
        position.set(Position.KEY_DTCS, "P0301,P0420");

        // Parse DTC codes
        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);

        assertNotNull(dtcCodes);
        assertEquals(2, dtcCodes.size());

        DtcCode dtc1 = dtcCodes.get(0);
        assertEquals("P0301", dtc1.getCode());
        assertEquals("P", dtc1.getCodeType());
        assertEquals("active", dtc1.getStatus());
        assertNotNull(dtc1.getDescription());
        assertNotEquals("Unknown DTC code", dtc1.getDescription());

        DtcCode dtc2 = dtcCodes.get(1);
        assertEquals("P0420", dtc2.getCode());
        assertEquals("P", dtc2.getCodeType());
    }

    @Test
    public void testObdDataStorage() throws StorageException {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());
        position.set(Position.KEY_RPM, 2500);
        position.set(Position.KEY_COOLANT_TEMP, 90);

        ObdParameters obd = parser.parseObdData(position);
        obd.setId(1L);

        // Mock storage to verify OBD data is stored
        when(storage.addObject(any(ObdParameters.class), any(Request.class))).thenReturn(1L);

        long obdId = storage.addObject(obd, new Request(new Columns.Exclude("id")));

        assertEquals(1L, obdId);
        verify(storage, times(1)).addObject(any(ObdParameters.class), any(Request.class));
    }

    @Test
    public void testDtcCodeStorage() throws StorageException {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());
        position.set(Position.KEY_DTCS, "P0301");

        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);
        assertFalse(dtcCodes.isEmpty());

        DtcCode dtc = dtcCodes.get(0);
        dtc.setId(1L);

        // Mock storage to verify DTC code is stored
        when(storage.addObject(any(DtcCode.class), any(Request.class))).thenReturn(1L);
        when(storage.getObject(eq(DtcCode.class), any(Request.class))).thenReturn(null);

        long dtcId = storage.addObject(dtc, new Request(new Columns.Exclude("id")));

        assertEquals(1L, dtcId);
        verify(storage, times(1)).addObject(any(DtcCode.class), any(Request.class));
    }

    @Test
    public void testObdDataWithCustomAttributes() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());

        // Add custom OBD attributes
        position.set("obd.rpm", 3000);
        position.set("obd.coolant_temp", 85);
        position.set("obd.throttle_position", 45);
        position.set("obd.engine_load", 60);
        position.set("obd.maf_air_flow", 35.5);
        position.set("obd.oil_pressure", 45.2);

        ObdParameters obd = parser.parseObdData(position);

        assertNotNull(obd);
        assertEquals(3000, obd.getEngineRpm());
        assertEquals(85, obd.getCoolantTemperature());
        assertEquals(45, obd.getThrottlePosition());
        assertEquals(60, obd.getEngineLoad());
        assertEquals(35.5, obd.getMafAirFlow());
        assertEquals(45.2, obd.getOilPressure());
    }

    @Test
    public void testMultipleDtcCodes() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());
        position.set(Position.KEY_DTCS, "P0301,P0420,B1234,C1201");

        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);

        assertEquals(4, dtcCodes.size());
        assertEquals("P0301", dtcCodes.get(0).getCode());
        assertEquals("P0420", dtcCodes.get(1).getCode());
        assertEquals("B1234", dtcCodes.get(2).getCode());
        assertEquals("C1201", dtcCodes.get(3).getCode());
    }

    @Test
    public void testEmptyObdData() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());

        ObdParameters obd = parser.parseObdData(position);

        assertNotNull(obd);
        assertEquals(1L, obd.getDeviceId());
        assertEquals(1L, obd.getPositionId());
        assertNull(obd.getEngineRpm());
        assertNull(obd.getCoolantTemperature());
    }

    @Test
    public void testDtcCodeTypes() {
        Position position = new Position();
        position.setId(1L);
        position.setDeviceId(1L);
        position.setFixTime(new Date());
        position.set(Position.KEY_DTCS, "P0301,B1234,C1201,U0100");

        List<DtcCode> dtcCodes = parser.parseDtcCodes(position);

        assertEquals(4, dtcCodes.size());
        assertEquals("P", dtcCodes.get(0).getCodeType());  // Powertrain
        assertEquals("B", dtcCodes.get(1).getCodeType());  // Body
        assertEquals("C", dtcCodes.get(2).getCodeType());  // Chassis
        assertEquals("U", dtcCodes.get(3).getCodeType());  // Network
    }
}
