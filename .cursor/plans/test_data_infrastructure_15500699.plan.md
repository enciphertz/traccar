---
name: Test Data Infrastructure
overview: Create a comprehensive test data infrastructure that includes enhanced test generators, dataset converters for real-world OBD/GPS data, and a multi-tenant test data loader to validate all E-Sync enhancements.
todos:
  - id: test-1
    content: Enhance test-generator.py with comprehensive OBD parameters, DTC codes, and fuel data
    status: completed
  - id: test-2
    content: Create dataset-converter.py for VED, OpenLKA, and generic CSV/JSON formats
    status: completed
  - id: test-3
    content: Create test-data-loader.py for multi-tenant test scenarios and batch loading
    status: completed
    dependencies:
      - test-1
  - id: test-4
    content: Create test-multitenant.py for tenant isolation testing
    status: completed
    dependencies:
      - test-3
  - id: test-5
    content: Create sample data files in tools/test-data/ directory
    status: completed
  - id: test-6
    content: Create ObdDataIntegrationTest.java for integration testing
    status: completed
    dependencies:
      - test-3
---

# Test Data Infrastructure for E-Sync Enhancements

## Overview

Create a comprehensive test data solution that supports:

1. Enhanced test generators with full OBD data
2. Converters for real-world open source datasets
3. Multi-tenant test scenarios
4. Integration testing with realistic data patterns

## Available Open Source Datasets

Based on research, we have access to:

- **Vehicle Energy Dataset (VED)**: 383 cars, GPS trajectories, fuel/energy data, 374K miles
- **OpenLKA Dataset**: 400 hours of driving, CAN bus streams, 50+ vehicle models
- **OBDb**: Community DTC code database with diagnostic parameters
- **pyOBD/OpenOBD**: Tools for generating realistic OBD-II data

## Implementation Plan

### 1. Enhance Existing Test Generator

**File**: `tools/test-generator.py`

Enhance to include:

- Full OBD parameter set (RPM, coolant temp, throttle, engine load, MAF, etc.)
- DTC code generation (random codes from DtcCodeDatabase)
- Fuel consumption rate calculations
- Multi-device support
- Tenant ID support via headers

**Key additions**:

```python
# Add OBD parameters
obd_params = {
    'rpm': random.randint(800, 4000),
    'coolantTemp': random.randint(80, 110),
    'throttlePosition': random.randint(0, 100),
    'engineLoad': random.randint(20, 90),
    'mafAirFlow': random.uniform(10.0, 150.0),
    'fuelLevel': random.randint(10, 100),
    'fuelConsumption': random.uniform(5.0, 15.0),
    'batteryVoltage': random.uniform(11.5, 14.5)
}

# Add DTC codes (occasionally)
if random.random() < 0.05:  # 5% chance
    dtc_codes = random.sample(['P0301', 'P0420', 'P0171', 'B1234'], 1)
    params['dtcs'] = ','.join(dtc_codes)
```

### 2. Create Dataset Converters

**New File**: `tools/dataset-converter.py`

Create converters for:

- **VED Dataset**: Convert CSV/JSON format to Traccar positions with OBD attributes
- **OpenLKA Dataset**: Extract CAN bus data and convert to OBD parameters
- **Generic CSV/JSON**: Flexible converter for any GPS+OBD dataset

**Structure**:

```python
class DatasetConverter:
    def convert_ved(self, ved_file, output_format='traccar'):
        # Parse VED format: GPS + fuel + speed + energy
        # Map to Traccar Position attributes
        
    def convert_openlka(self, can_bus_file, output_format='traccar'):
        # Parse CAN bus streams
        # Extract OBD parameters
        # Generate positions
        
    def convert_generic(self, csv_file, column_mapping):
        # Flexible CSV converter with column mapping
```

### 3. Comprehensive Test Data Loader

**New File**: `tools/test-data-loader.py`

Create a comprehensive loader that:

- Generates multi-tenant test scenarios
- Creates realistic device hierarchies
- Loads positions with OBD data via Traccar API
- Supports batch loading
- Includes DTC code scenarios

**Features**:

- Tenant creation and device assignment
- Realistic trip generation (start/stop, idle, driving)
- OBD data correlation with driving patterns
- DTC code injection at realistic intervals
- Fuel consumption validation

**Usage**:

```bash
python tools/test-data-loader.py \
    --tenants 3 \
    --devices-per-tenant 5 \
    --positions-per-device 1000 \
    --include-obd \
    --include-dtc \
    --server localhost:8082
```

### 4. Multi-Tenant Test Scenarios

**New File**: `tools/test-multitenant.py`

Create scenarios for:

- Tenant isolation testing
- Cross-tenant data access validation
- Schema switching verification
- Tenant context filter testing

### 5. Integration Test Suite

**New File**: `src/test/java/org/traccar/integration/ObdDataIntegrationTest.java`

Create integration tests that:

- Load test data via API
- Verify OBD data processing pipeline
- Validate DTC code detection and events
- Test fuel consumption calculations
- Verify tenant isolation

### 6. Sample Data Files

**New Directory**: `tools/test-data/`

Include:

- `sample-obd-positions.json` - Sample positions with OBD data
- `sample-dtc-codes.csv` - Common DTC codes for testing
- `sample-fuel-data.json` - Fuel consumption test scenarios
- `sample-multitenant.json` - Multi-tenant test configuration

## File Structure

```
tools/
├── test-generator.py (enhanced)
├── dataset-converter.py (new)
├── test-data-loader.py (new)
├── test-multitenant.py (new)
└── test-data/
    ├── sample-obd-positions.json
    ├── sample-dtc-codes.csv
    ├── sample-fuel-data.json
    └── sample-multitenant.json

src/test/java/org/traccar/integration/
└── ObdDataIntegrationTest.java (new)
```

## Implementation Details

### Enhanced Test Generator

- Add all OBD parameters from ObdParameters model
- Generate realistic correlations (RPM with speed, fuel with load)
- Include DTC code generation with realistic patterns
- Support tenant ID via X-Tenant-ID header
- Add fuel consumption rate calculations

### Dataset Converter

- Support VED format (CSV with GPS, fuel, speed columns)
- Support OpenLKA CAN bus format
- Generic CSV converter with column mapping
- Output to Traccar API format or direct database insertion

### Test Data Loader

- Create tenants via API
- Create devices with tenant assignment
- Generate realistic position sequences
- Inject OBD data at appropriate intervals
- Generate DTC codes based on vehicle "health" simulation
- Calculate and validate fuel consumption

## Testing Scenarios

1. **Basic OBD Data**: Generate positions with all OBD parameters
2. **DTC Code Detection**: Inject DTC codes and verify event generation
3. **Fuel Consumption**: Generate trip data and verify calculations
4. **Multi-Tenant Isolation**: Create multiple tenants and verify data separation
5. **API Endpoints**: Test all new endpoints with generated data
6. **Real-World Patterns**: Load VED dataset and process through pipeline

## Dependencies

- Python 3.x (for test tools)
- requests library (for API calls)
- pandas (for dataset conversion)
- numpy (for data generation)

## Benefits

- Comprehensive test coverage for all enhancements
- Real-world data patterns from open source datasets
- Multi-tenant testing scenarios
- Reusable test data generation
- Integration test support
- Documentation through sample data files