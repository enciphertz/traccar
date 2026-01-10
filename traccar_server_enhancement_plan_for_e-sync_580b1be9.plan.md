---
name: Traccar Server Enhancement Plan for E-Sync
overview: ""
todos: []
---

# Traccar Server Enhancement Plan for E-Sync

## Executive Summary

This document provides a detailed enhancement plan for the Traccar server codebase to transform it into E-Sync's telemetry engine. The plan is based on actual codebase analysis and provides specific file locations, code changes, and implementation strategies.

## Current Traccar Architecture Analysis

### Key Findings from Codebase

**Architecture Strengths:**

- ✅ Handler-based pipeline architecture (extensible)
- ✅ Storage abstraction layer (easy to modify)
- ✅ Event-driven system (already supports events)
- ✅ Position attributes system (can store OBD data)
- ✅ Notification system (can extend for webhooks)
- ✅ Liquibase database migrations (safe schema changes)

**Current Structure:**

```
traccar-server/
├── src/main/java/org/traccar/
│   ├── model/              # Data models (Device, Position, Event)
│   ├── storage/            # Storage abstraction (DatabaseStorage)
│   ├── handler/            # Position processing pipeline
│   ├── api/                # REST API (Jersey)
│   ├── protocol/           # 250+ protocol handlers
│   ├── notification/       # Notification system
│   └── forward/            # Position forwarding
├── schema/                 # Liquibase migrations
└── build.gradle            # Gradle build (Java 17)
```

**Key Observations:**

1. **Position Model** already has OBD-related keys:

                                                - `KEY_DTCS`, `KEY_RPM`, `KEY_COOLANT_TEMP`, `KEY_FUEL`, `KEY_FUEL_CONSUMPTION`
                                                - Uses `ExtendedModel` with attributes Map for flexible data storage

2. **Handler Pipeline** is perfect for adding OBD processing:

                                                - `BasePositionHandler` → `DatabaseHandler` → `PositionForwardingHandler`
                                                - Can insert new handlers in the pipeline

3. **Storage Layer** is abstracted:

                                                - `Storage` interface → `DatabaseStorage` implementation
                                                - Can add tenant-aware queries without breaking existing code

4. **Event System** exists:

                                                - `Event` model with various event types
                                                - Event handlers in `handler/events/`
                                                - Can add DTC code events

5. **Notification System** is pluggable:

                                                - `Notificator` interface with multiple implementations
                                                - `NotificatorWeb` exists (can extend for E-Sync webhooks)

## Enhancement Strategy

### Approach: Minimal Invasive Changes

**Principle**: Enhance Traccar without breaking existing functionality

**Strategy**:

1. **Add new models** (don't modify existing extensively)
2. **Add new handlers** (extend pipeline)
3. **Extend storage** (add tenant-aware methods)
4. **Add new API endpoints** (E-Sync specific)
5. **Extend notification** (E-Sync webhook notificator)

## Enhancement 1: Multi-Tenancy Support

### Current State Analysis

**Device Model** (`src/main/java/org/traccar/model/Device.java`):

- Extends `GroupedModel` (has `groupId`)
- No tenant concept currently
- Single database schema

**Storage** (`src/main/java/org/traccar/storage/DatabaseStorage.java`):

- Direct SQL queries
- No schema switching
- No tenant filtering

### Implementation Plan

#### 1.1 Create Tenant Model

**New File**: `src/main/java/org/traccar/model/Tenant.java`

```java
package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_tenants")
public class Tenant extends BaseModel {
    
    private String name;
    private String schemaName;  // PostgreSQL schema name
    private String esyncTenantId;  // E-Sync tenant ID mapping
    private boolean active;
    private Date createdAt;
    private Date updatedAt;
    
    // Getters and setters
}
```

#### 1.2 Add Tenant Reference to Device

**Modify**: `src/main/java/org/traccar/model/Device.java`

```java
// Add after line 137 (after category field)
private Long tenantId;

public Long getTenantId() {
    return tenantId;
}

public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
}

private String esyncVehicleId;  // E-Sync vehicle ID mapping

public String getEsyncVehicleId() {
    return esyncVehicleId;
}

public void setEsyncVehicleId(String esyncVehicleId) {
    this.esyncVehicleId = esyncVehicleId;
}
```

#### 1.3 Create Tenant-Aware Storage Wrapper

**New File**: `src/main/java/org/traccar/storage/TenantAwareStorage.java`

```java
package org.traccar.storage;

import jakarta.inject.Inject;
import org.traccar.model.Tenant;
import org.traccar.session.cache.CacheManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TenantAwareStorage extends Storage {
    
    private final Storage delegate;
    private final CacheManager cacheManager;
    private final DataSource dataSource;
    private final ThreadLocal<Long> tenantContext = new ThreadLocal<>();
    
    @Inject
    public TenantAwareStorage(Storage delegate, CacheManager cacheManager, DataSource dataSource) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
        this.dataSource = dataSource;
    }
    
    public void setTenantContext(Long tenantId) {
        tenantContext.set(tenantId);
        if (tenantId != null) {
            setSchema(tenantId);
        }
    }
    
    public void clearTenantContext() {
        tenantContext.remove();
        resetSchema();
    }
    
    private void setSchema(Long tenantId) {
        try (Connection connection = dataSource.getConnection()) {
            Tenant tenant = cacheManager.getObject(Tenant.class, tenantId);
            if (tenant != null && tenant.getSchemaName() != null) {
                connection.createStatement().execute(
                    "SET search_path TO " + tenant.getSchemaName() + ", public");
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }
    
    private void resetSchema() {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SET search_path TO public");
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException {
        // Add tenant filter if tenant context is set
        if (tenantContext.get() != null) {
            request = addTenantFilter(request, clazz);
        }
        return delegate.getObjects(clazz, request);
    }
    
    // Override all Storage methods to delegate with tenant filtering
    // ... (similar pattern for all methods)
    
    private <T> Request addTenantFilter(Request request, Class<T> clazz) {
        // Add tenant condition to request
        if (clazz == Device.class) {
            request = new Request(request.getCondition().merge(
                new Condition.Equals("tenantId", tenantContext.get())
            ));
        }
        return request;
    }
}
```

#### 1.4 Database Schema Changes

**New Migration**: `schema/changelog-esync-1.0.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
  xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.4.xsd"
  logicalFilePath="changelog-esync-1.0">

  <changeSet author="esync" id="esync-1.0-tenants">
    <createTable tableName="tc_tenants">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="name" type="VARCHAR(255)">
        <constraints nullable="false"/>
      </column>
      <column name="schema_name" type="VARCHAR(63)">
        <constraints nullable="false" unique="true"/>
      </column>
      <column name="esync_tenant_id" type="VARCHAR(255)">
        <constraints nullable="false" unique="true"/>
      </column>
      <column name="active" type="BOOLEAN" defaultValueBoolean="true">
        <constraints nullable="false"/>
      </column>
      <column name="created_at" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
      <column name="updated_at" type="TIMESTAMP" defaultValueDate="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
    </createTable>
    
    <createIndex indexName="tc_tenants_esync_tenant_id_idx" tableName="tc_tenants">
      <column name="esync_tenant_id"/>
    </createIndex>
  </changeSet>

  <changeSet author="esync" id="esync-1.0-device-tenant">
    <addColumn tableName="tc_devices">
      <column name="tenant_id" type="BIGINT"/>
      <column name="esync_vehicle_id" type="VARCHAR(255)"/>
    </addColumn>
    
    <addForeignKeyConstraint
      baseTableName="tc_devices"
      baseColumnNames="tenant_id"
      referencedTableName="tc_tenants"
      referencedColumnNames="id"
      constraintName="fk_devices_tenant"/>
    
    <createIndex indexName="tc_devices_tenant_id_idx" tableName="tc_devices">
      <column name="tenant_id"/>
    </createIndex>
    
    <createIndex indexName="tc_devices_esync_vehicle_id_idx" tableName="tc_devices">
      <column name="esync_vehicle_id"/>
    </createIndex>
  </changeSet>

  <changeSet author="esync" id="esync-1.0-tenant-schema-function">
    <sql>
      CREATE OR REPLACE FUNCTION create_tenant_schema(tenant_schema_name VARCHAR)
      RETURNS VOID AS $$
      BEGIN
        EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', tenant_schema_name);
        
        -- Copy all tables to tenant schema
        EXECUTE format('CREATE TABLE %I.tc_devices (LIKE public.tc_devices INCLUDING ALL)', tenant_schema_name);
        EXECUTE format('CREATE TABLE %I.tc_positions (LIKE public.tc_positions INCLUDING ALL)', tenant_schema_name);
        EXECUTE format('CREATE TABLE %I.tc_events (LIKE public.tc_events INCLUDING ALL)', tenant_schema_name);
        EXECUTE format('CREATE TABLE %I.tc_geofences (LIKE public.tc_geofences INCLUDING ALL)', tenant_schema_name);
        -- ... copy all other tenant-specific tables
      END;
      $$ LANGUAGE plpgsql;
    </sql>
  </changeSet>

</databaseChangeLog>
```

#### 1.5 Add Tenant Context Middleware

**New File**: `src/main/java/org/traccar/api/security/TenantContextFilter.java`

```java
package org.traccar.api.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import org.traccar.storage.Storage;
import org.traccar.storage.TenantAwareStorage;

@Provider
public class TenantContextFilter implements ContainerRequestFilter {
    
    private final TenantAwareStorage tenantAwareStorage;
    
    @Inject
    public TenantContextFilter(TenantAwareStorage tenantAwareStorage) {
        this.tenantAwareStorage = tenantAwareStorage;
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Extract tenant ID from header
        String tenantIdHeader = requestContext.getHeaderString("X-Tenant-ID");
        if (tenantIdHeader != null) {
            try {
                Long tenantId = Long.parseLong(tenantIdHeader);
                tenantAwareStorage.setTenantContext(tenantId);
            } catch (NumberFormatException e) {
                // Invalid tenant ID, ignore
            }
        }
    }
}
```

**Modify**: `src/main/java/org/traccar/api/resource/DeviceResource.java`

```java
// Add tenant context extraction
@Context
private HttpServletRequest request;

private Long getTenantId() {
    String tenantId = request.getHeader("X-Tenant-ID");
    if (tenantId != null) {
        return Long.parseLong(tenantId);
    }
    return null;
}

@GET
public Stream<Device> get() {
    Long tenantId = getTenantId();
    if (tenantId != null) {
        // Filter by tenant
        return storage.getObjects(Device.class, new Request(
            new Condition.Equals("tenantId", tenantId)
        )).stream();
    }
    // Existing logic for non-tenant requests
    return super.get();
}
```

### Implementation Timeline: 4-6 weeks

## Enhancement 2: OBD/DTC Data Processing

### Current State Analysis

**Position Model** already supports:

- `KEY_DTCS` - DTC codes
- `KEY_RPM` - Engine RPM
- `KEY_COOLANT_TEMP` - Coolant temperature
- `KEY_FUEL`, `KEY_FUEL_LEVEL`, `KEY_FUEL_CONSUMPTION` - Fuel data
- Attributes Map for flexible OBD data storage

**Missing**:

- Structured OBD data storage
- DTC code parsing and management
- OBD data processing pipeline

### Implementation Plan

#### 2.1 Create OBD Data Models

**New File**: `src/main/java/org/traccar/model/ObdParameters.java`

```java
package org.traccar.model;

import org.traccar.storage.StorageName;
import java.util.Date;

@StorageName("tc_obd_parameters")
public class ObdParameters extends BaseModel {
    
    private long deviceId;
    private long positionId;
    private Date timestamp;
    
    // Engine parameters
    private Integer engineRpm;
    private Integer coolantTemperature;  // Celsius
    private Double oilPressure;
    private Double batteryVoltage;
    private Integer throttlePosition;     // Percentage
    private Integer intakeAirTemperature;
    private Double mafAirFlow;            // g/s
    
    // Fuel parameters
    private Integer fuelLevel;            // Percentage
    private Double fuelConsumptionRate;   // L/h
    private Double fuelPressure;
    
    // Vehicle parameters
    private Integer vehicleSpeed;         // km/h
    private Integer engineLoad;          // Percentage
    private Double timingAdvance;
    
    // Getters and setters
}
```

**New File**: `src/main/java/org/traccar/model/DtcCode.java`

```java
package org.traccar.model;

import org.traccar.storage.StorageName;
import java.util.Date;

@StorageName("tc_dtc_codes")
public class DtcCode extends BaseModel {
    
    private long deviceId;
    private long positionId;
    private Date timestamp;
    
    private String code;           // e.g., "P0301"
    private String codeType;        // P, B, C, U
    private String description;
    private String status;          // active, pending, permanent, cleared
    private String freezeFrameData; // JSON
    
    private Date firstOccurred;
    private Date lastOccurred;
    private Date clearedAt;
    
    // Getters and setters
}
```

#### 2.2 Create OBD Data Parser

**New File**: `src/main/java/org/traccar/obd/ObdDataParser.java`

```java
package org.traccar.obd;

import org.traccar.model.DtcCode;
import org.traccar.model.ObdParameters;
import org.traccar.model.Position;
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
        if (attributes.containsKey(Position.KEY_RPM)) {
            obd.setEngineRpm(position.getInteger(Position.KEY_RPM));
        }
        
        // Parse coolant temperature
        if (attributes.containsKey(Position.KEY_COOLANT_TEMP)) {
            obd.setCoolantTemperature(position.getInteger(Position.KEY_COOLANT_TEMP));
        }
        
        // Parse fuel level
        if (attributes.containsKey(Position.KEY_FUEL_LEVEL)) {
            obd.setFuelLevel(position.getInteger(Position.KEY_FUEL_LEVEL));
        }
        
        // Parse fuel consumption rate
        if (attributes.containsKey(Position.KEY_FUEL_CONSUMPTION)) {
            obd.setFuelConsumptionRate(position.getDouble(Position.KEY_FUEL_CONSUMPTION));
        }
        
        // Parse battery voltage
        if (attributes.containsKey(Position.KEY_POWER)) {
            obd.setBatteryVoltage(position.getDouble(Position.KEY_POWER));
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
        
        // Parse format: "P0301,P0420,B1234" or "P0301;P0420"
        String[] codes = dtcString.split("[,;]");
        
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
        if (code.length() > 0) {
            char firstChar = code.charAt(0);
            if (firstChar == 'P' || firstChar == 'B' || firstChar == 'C' || firstChar == 'U') {
                return String.valueOf(firstChar);
            }
        }
        return "P";  // Default to Powertrain
    }
    
    private String lookupDtcDescription(String code) {
        // Use DTC code database
        return DtcCodeDatabase.getInstance().lookup(code);
    }
    
    private void parseCustomObdAttributes(ObdParameters obd, Map<String, Object> attributes) {
        // Parse device-specific OBD attributes
        // e.g., "obd.rpm", "obd.temp", etc.
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("obd.")) {
                String obdKey = key.substring(4);  // Remove "obd." prefix
                parseObdAttribute(obd, obdKey, entry.getValue());
            }
        }
    }
    
    private void parseObdAttribute(ObdParameters obd, String key, Object value) {
        switch (key.toLowerCase()) {
            case "rpm":
                obd.setEngineRpm(parseInteger(value));
                break;
            case "coolanttemp":
            case "coolant_temp":
                obd.setCoolantTemperature(parseInteger(value));
                break;
            case "fuellevel":
            case "fuel_level":
                obd.setFuelLevel(parseInteger(value));
                break;
            case "fuelconsumption":
            case "fuel_consumption":
                obd.setFuelConsumptionRate(parseDouble(value));
                break;
            // ... more mappings
        }
    }
    
    private Integer parseInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
```

#### 2.3 Create DTC Code Database

**New File**: `src/main/java/org/traccar/obd/DtcCodeDatabase.java`

```java
package org.traccar.obd;

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
        codeDescriptions.put("P0420", "Catalyst System Efficiency Below Threshold");
        codeDescriptions.put("P0171", "System Too Lean (Bank 1)");
        codeDescriptions.put("P0172", "System Too Rich (Bank 1)");
        // ... load from file or database
    }
}
```

#### 2.4 Create OBD Data Processor Handler

**New File**: `src/main/java/org/traccar/handler/ObdDataHandler.java`

```java
package org.traccar.handler;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.DtcCode;
import org.traccar.model.Event;
import org.traccar.model.ObdParameters;
import org.traccar.model.Position;
import org.traccar.obd.ObdDataParser;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

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
                obd.setId(storage.addObject(obd, new Request(new Columns.Exclude("id"))));
            }
            
            // Parse DTC codes
            List<DtcCode> dtcCodes = parser.parseDtcCodes(position);
            for (DtcCode dtc : dtcCodes) {
                processDtcCode(dtc);
            }
            
        } catch (StorageException e) {
            LOGGER.warn("Failed to process OBD data", e);
        }
        
        callback.processed(false);
    }
    
    private boolean hasObdData(Position position) {
        return position.hasAttribute(Position.KEY_RPM)
            || position.hasAttribute(Position.KEY_COOLANT_TEMP)
            || position.hasAttribute(Position.KEY_FUEL_LEVEL)
            || position.hasAttribute(Position.KEY_DTCS)
            || position.getAttributes().keySet().stream()
                .anyMatch(key -> key.startsWith("obd."));
    }
    
    private boolean hasValidObdData(ObdParameters obd) {
        return obd.getEngineRpm() != null
            || obd.getCoolantTemperature() != null
            || obd.getFuelLevel() != null
            || obd.getFuelConsumptionRate() != null;
    }
    
    private void processDtcCode(DtcCode dtc) throws StorageException {
        // Check if DTC already exists
        DtcCode existing = storage.getObject(DtcCode.class, new Request(
            new Condition.And(
                new Condition.Equals("deviceId", dtc.getDeviceId()),
                new Condition.Equals("code", dtc.getCode()),
                new Condition.Equals("status", "active")
            )
        ));
        
        if (existing != null) {
            // Update last occurred time
            existing.setLastOccurred(dtc.getTimestamp());
            storage.updateObject(existing, new Request(
                new Columns.Include("lastOccurred")
            ));
        } else {
            // New DTC code - store and trigger event
            dtc.setId(storage.addObject(dtc, new Request(new Columns.Exclude("id"))));
            triggerDtcEvent(dtc);
        }
    }
    
    private void triggerDtcEvent(DtcCode dtc) throws StorageException {
        // Get position for event
        Position position = storage.getObject(Position.class, new Request(
            new Condition.Equals("id", dtc.getPositionId())
        ));
        
        if (position != null) {
            Event event = new Event(Event.TYPE_DTC_CODE, position);
            event.set("dtcCode", dtc.getCode());
            event.set("dtcDescription", dtc.getDescription());
            event.set("dtcType", dtc.getCodeType());
            event.set("severity", determineSeverity(dtc));
            
            storage.addObject(event, new Request(new Columns.Exclude("id")));
        }
    }
    
    private String determineSeverity(DtcCode dtc) {
        // Determine severity based on DTC code type
        switch (dtc.getCodeType()) {
            case "P":  // Powertrain
                if (dtc.getCode().startsWith("P03")) {
                    return "high";  // Misfire codes
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
```

#### 2.5 Add DTC Code Event Type

**Modify**: `src/main/java/org/traccar/model/Event.java`

```java
// Add after line 66 (after TYPE_MEDIA)
public static final String TYPE_DTC_CODE = "dtcCode";
public static final String TYPE_DTC_CLEARED = "dtcCleared";
```

#### 2.6 Database Schema for OBD Data

**Add to**: `schema/changelog-esync-1.0.xml`

```xml
<changeSet author="esync" id="esync-1.0-obd-parameters">
  <createTable tableName="tc_obd_parameters">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="device_id" type="BIGINT">
      <constraints nullable="false"/>
    </column>
    <column name="position_id" type="BIGINT">
      <constraints nullable="false"/>
    </column>
    <column name="timestamp" type="TIMESTAMP">
      <constraints nullable="false"/>
    </column>
    
    <!-- Engine parameters -->
    <column name="engine_rpm" type="INTEGER"/>
    <column name="coolant_temperature" type="INTEGER"/>
    <column name="oil_pressure" type="DOUBLE"/>
    <column name="battery_voltage" type="DOUBLE"/>
    <column name="throttle_position" type="INTEGER"/>
    <column name="intake_air_temperature" type="INTEGER"/>
    <column name="maf_air_flow" type="DOUBLE"/>
    
    <!-- Fuel parameters -->
    <column name="fuel_level" type="INTEGER"/>
    <column name="fuel_consumption_rate" type="DOUBLE"/>
    <column name="fuel_pressure" type="DOUBLE"/>
    
    <!-- Vehicle parameters -->
    <column name="vehicle_speed" type="INTEGER"/>
    <column name="engine_load" type="INTEGER"/>
    <column name="timing_advance" type="DOUBLE"/>
  </createTable>
  
  <addForeignKeyConstraint
    baseTableName="tc_obd_parameters"
    baseColumnNames="device_id"
    referencedTableName="tc_devices"
    referencedColumnNames="id"
    constraintName="fk_obd_parameters_device"/>
  
  <addForeignKeyConstraint
    baseTableName="tc_obd_parameters"
    baseColumnNames="position_id"
    referencedTableName="tc_positions"
    referencedColumnNames="id"
    constraintName="fk_obd_parameters_position"/>
  
  <createIndex indexName="tc_obd_parameters_device_time_idx" tableName="tc_obd_parameters">
    <column name="device_id"/>
    <column name="timestamp"/>
  </createIndex>
  
  <createIndex indexName="tc_obd_parameters_position_idx" tableName="tc_obd_parameters">
    <column name="position_id"/>
  </createIndex>
</changeSet>

<changeSet author="esync" id="esync-1.0-dtc-codes">
  <createTable tableName="tc_dtc_codes">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="device_id" type="BIGINT">
      <constraints nullable="false"/>
    </column>
    <column name="position_id" type="BIGINT">
      <constraints nullable="false"/>
    </column>
    <column name="timestamp" type="TIMESTAMP">
      <constraints nullable="false"/>
    </column>
    
    <column name="code" type="VARCHAR(10)">
      <constraints nullable="false"/>
    </column>
    <column name="code_type" type="CHAR(1)"/>
    <column name="description" type="TEXT"/>
    <column name="status" type="VARCHAR(20)" defaultValue="active">
      <constraints nullable="false"/>
    </column>
    <column name="freeze_frame_data" type="TEXT"/>
    
    <column name="first_occurred" type="TIMESTAMP"/>
    <column name="last_occurred" type="TIMESTAMP"/>
    <column name="cleared_at" type="TIMESTAMP"/>
  </createTable>
  
  <addForeignKeyConstraint
    baseTableName="tc_dtc_codes"
    baseColumnNames="device_id"
    referencedTableName="tc_devices"
    referencedColumnNames="id"
    constraintName="fk_dtc_codes_device"/>
  
  <createIndex indexName="tc_dtc_codes_device_idx" tableName="tc_dtc_codes">
    <column name="device_id"/>
  </createIndex>
  
  <createIndex indexName="tc_dtc_codes_status_idx" tableName="tc_dtc_codes">
    <column name="status"/>
  </createIndex>
  
  <createIndex indexName="tc_dtc_codes_code_idx" tableName="tc_dtc_codes">
    <column name="code"/>
  </createIndex>
</changeSet>
```

#### 2.7 Register Handler in Pipeline

**Modify**: `src/main/java/org/traccar/PipelineBuilder.java`

```java
// Add ObdDataHandler to pipeline (after DatabaseHandler, before PositionForwardingHandler)
handlers.add(injector.getInstance(ObdDataHandler.class));
```

### Implementation Timeline: 4-6 weeks

## Enhancement 3: Fuel Consumption Calculations

### Implementation Plan

#### 3.1 Create Fuel Calculator Service

**New File**: `src/main/java/org/traccar/fuel/FuelCalculator.java`

```java
package org.traccar.fuel;

import org.traccar.model.ObdParameters;
import org.traccar.model.Position;
import java.util.List;

public class FuelCalculator {
    
    /**
     * Calculate fuel consumption from OBD data and positions
     */
    public FuelConsumption calculateFuelConsumption(
            List<Position> positions, 
            List<ObdParameters> obdData) {
        
        FuelConsumption fuel = new FuelConsumption();
        
        if (obdData.isEmpty() || positions.size() < 2) {
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
    
    private double calculateDistance(Position pos1, Position pos2) {
        // Haversine formula for distance calculation
        return DistanceCalculator.distance(
            pos1.getLatitude(), pos1.getLongitude(),
            pos2.getLatitude(), pos2.getLongitude()
        );
    }
}
```

**New File**: `src/main/java/org/traccar/model/FuelConsumption.java`

```java

package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_fuel_consumption")

public class FuelConsumption extends BaseModel {

private long deviceId;

private Date startTime;

private Date endTime;

private Double fuelConsumption;      // Liters

private Double distance;              // Kilometers

private Double fuelEconomy;           // km/L

private Double fuelCost;              // Currency amount