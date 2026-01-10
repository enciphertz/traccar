package tz.co.esync.handler.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tz.co.esync.BaseTest;
import tz.co.esync.model.Position;
import tz.co.esync.session.cache.CacheManager;

import static org.mockito.Mockito.mock;

public class IgnitionEventHandlerTest extends BaseTest {
    
    @Test
    public void testIgnitionEventHandler() {
        
        IgnitionEventHandler ignitionEventHandler = new IgnitionEventHandler(mock(CacheManager.class));
        
        Position position = new Position();
        position.set(Position.KEY_IGNITION, true);
        position.setValid(true);
        ignitionEventHandler.analyzePosition(position, Assertions::assertNull);
    }

}
