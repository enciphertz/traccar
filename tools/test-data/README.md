# Test Data for Traccar E-Sync Enhancements

This directory contains sample test data files for testing OBD data processing, DTC codes, fuel consumption, and multi-tenant scenarios.

## Files

### sample-obd-positions.json
Sample GPS positions with comprehensive OBD data including:
- Engine parameters (RPM, coolant temp, throttle, engine load)
- Fuel data (fuel level, fuel consumption)
- Vehicle parameters (speed, battery voltage, oil pressure)
- DTC codes (in one position)

### sample-dtc-codes.csv
Common DTC codes for testing, including:
- Powertrain codes (P-codes)
- Body codes (B-codes)
- Chassis codes (C-codes)
- Network codes (U-codes)

Each code includes type, description, and severity level.

### sample-fuel-data.json
Fuel consumption test scenarios including:
- City driving scenario
- Highway driving scenario
- Idle period scenario

Each scenario includes positions with fuel consumption data and calculated fuel economy.

### sample-multitenant.json
Multi-tenant test configuration with:
- Three sample tenants (Acme Corporation, Beta Logistics, Gamma Transport)
- Devices assigned to each tenant using real supported devices from `supported-devices.json`
- Test scenarios for tenant isolation and cross-tenant access
- Device information including protocol, port, and OBD capability

### sample-obd-devices.json
Sample of OBD-capable devices extracted from `supported-devices.json`:
- 50 OBD-capable devices (out of 247 total)
- Includes Freematics, Teltonika, and other OBD-supporting devices
- Device metadata: protocol, port, category, OBD support flag

### sample-device-positions.json
Sample positions generated for real supported devices:
- Positions for 5 different OBD-capable devices
- Includes device name, protocol, and full OBD data
- Demonstrates how different devices send OBD parameters

### device-reference.json
Comprehensive reference of all supported devices:
- Statistics: 1569 total devices, 247 OBD-capable, 254 protocols
- Protocol distribution
- Sample devices (recommended and OBD-capable)
- Useful for understanding device ecosystem

## Usage

These files can be used with:
- `test-data-loader.py` - Load comprehensive test scenarios
- `dataset-converter.py` - Convert to Traccar format
- Integration tests - Reference data for testing

## Generating Test Data with Supported Devices

To regenerate test data files with the latest supported devices:

```bash
python tools/extract-supported-devices.py
```

This script:
- Extracts all valid devices from `supported-devices.json`
- Identifies OBD-capable devices (247 devices)
- Categorizes devices by protocol and capability
- Updates sample data files with real device models

## Examples

```bash
# Load sample OBD positions
python tools/dataset-converter.py \
    tools/test-data/sample-obd-positions.json \
    --format generic \
    --mapping tools/test-data/column-mapping.json \
    --output traccar

# Load multi-tenant scenario with generic devices
python tools/test-data-loader.py \
    --tenants 3 \
    --devices-per-tenant 2 \
    --positions-per-device 50 \
    --include-obd \
    --include-dtc
```

## Supported Devices Statistics

- **Total Devices**: 1,569 total devices
- **Valid Devices**: 1,553 devices (with protocol, not clones)
- **OBD-Capable**: 247 devices (protocols: freematics, teltonika, osmand, galileo, totem, ruptela, ulbotech, gps103, gt06)
- **Protocols**: 254 unique protocols
- **Recommended**: 1 device (Freematics Traccar Edition)

The test data files automatically use real device models from `supported-devices.json`, making test scenarios more realistic and representative of actual deployments.
