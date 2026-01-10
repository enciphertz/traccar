package tz.co.esync.obd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DtcCodeDatabaseTest {

    @Test
    public void testLookup() {
        DtcCodeDatabase database = DtcCodeDatabase.getInstance();
        
        String description = database.lookup("P0301");
        assertNotNull(description);
        assertFalse(description.isEmpty());
        assertNotEquals("Unknown DTC code", description);
    }

    @Test
    public void testLookupUnknown() {
        DtcCodeDatabase database = DtcCodeDatabase.getInstance();
        
        String description = database.lookup("P9999");
        assertNotNull(description);
        assertEquals("Unknown DTC code", description);
    }

    @Test
    public void testSingleton() {
        DtcCodeDatabase db1 = DtcCodeDatabase.getInstance();
        DtcCodeDatabase db2 = DtcCodeDatabase.getInstance();
        
        assertSame(db1, db2);
    }

    @Test
    public void testCommonCodes() {
        DtcCodeDatabase database = DtcCodeDatabase.getInstance();
        
        // Test some common codes
        assertNotNull(database.lookup("P0301"));
        assertNotNull(database.lookup("P0302"));
        assertNotNull(database.lookup("P0420"));
        assertNotNull(database.lookup("P0171"));
        assertNotNull(database.lookup("P0172"));
    }
}
