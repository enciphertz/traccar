# Test Data Summary

This directory contains comprehensive test data files for testing Traccar E-Sync enhancements, including real device information from `supported-devices.json`.

## Generated Files

### Core Test Data Files

1. **sample-obd-positions.json** (3 positions)
   - Sample GPS positions with comprehensive OBD data
   - Includes engine parameters, fuel data, and DTC codes
   - Ready to use with `dataset-converter.py`

2. **sample-dtc-codes.csv** (15 DTC codes)
   - Common Diagnostic Trouble Codes for testing
   - Includes P, B, C, and U code types
   - With descriptions and severity levels

3. **sample-fuel-data.json** (3 scenarios)
   - City driving, highway driving, and idle scenarios
   - Includes fuel consumption calculations
   - Demonstrates fuel economy calculations

### Supported Devices Files (Generated from supported-devices.json)

4. **sample-obd-devices.json** (50 devices)
   - Sample of OBD-capable devices from supported-devices.json
   - 247 total OBD-capable devices identified
   - Includes protocol, port, category, and OBD support flag
   - Devices: Freematics, Teltonika, GPS103, and more

5. **sample-device-positions.json** (15 positions)
   - Sample positions for 5 different OBD-capable devices
   - Includes device name, protocol, and full OBD data
   - Demonstrates how different devices send OBD parameters
   - Real device models: GPS-103-A, Freematics, etc.

6. **device-reference.json** (Comprehensive reference)
   - Statistics: 1,569 total devices, 247 OBD-capable, 254 protocols
   - Protocol distribution and counts
   - Sample recommended devices
   - Sample OBD-capable devices
   - Useful for understanding the device ecosystem

7. **sample-multitenant.json** (Updated with real devices)
   - Multi-tenant test configuration
   - Uses real device models from supported-devices.json
   - Three tenants with devices:
     - Acme Corporation: Freematics Traccar Edition, Freematics ONE+ Model A
     - Beta Logistics: Freematics ONE+ Model B, Freematics ONE+ Model H
     - Gamma Transport: TK103-2B
   - Includes protocol information and OBD support flags

## Statistics

From `supported-devices.json`:
- **Total Devices**: 1,569
- **Valid Devices**: 1,553 (with protocol, not clones)
- **OBD-Capable Devices**: 247
- **Recommended Devices**: 1 (Freematics Traccar Edition)
- **Unique Protocols**: 254

### OBD-Capable Protocols

The following protocols support OBD data:
- `freematics` - Freematics devices (recommended)
- `obddongle` - OBD dongle devices
- `teltonika` - Teltonika trackers
- `osmand` - OsmAnd devices
- `galileo` - Galileo devices
- `totem` - Totem devices
- `ruptela` - Ruptela devices
- `ulbotech` - Ulbotech devices
- `gps103` - Some GPS103 devices
- `gt06` - Some GT06 devices

## Regenerating Test Data

To regenerate test data files with the latest supported devices:

```bash
python tools/extract-supported-devices.py
```

This will:
1. Load `supported-devices.json` from project root
2. Identify OBD-capable devices
3. Categorize devices by protocol and capability
4. Generate/update all test data files
5. Update `sample-multitenant.json` with real device models

## Usage Examples

### Using Sample OBD Positions

```bash
# Convert sample positions to Traccar format
python tools/dataset-converter.py \
    tools/test-data/sample-obd-positions.json \
    --format generic \
    --mapping '{"lat":"lat","lon":"lon","timestamp":"timestamp"}' \
    --output traccar \
    --device-id TEST001
```

### Using Device Positions

```bash
# Load device positions for testing
python tools/dataset-converter.py \
    tools/test-data/sample-device-positions.json \
    --format generic \
    --mapping '{"id":"id","lat":"lat","lon":"lon","timestamp":"timestamp"}' \
    --output traccar
```

### Using Multi-Tenant Configuration

The `sample-multitenant.json` file can be used as a reference for:
- Understanding tenant structure
- Device assignment patterns
- Protocol and OBD capability information
- Test scenario planning

## File Formats

### JSON Files
All JSON files use standard JSON format with:
- Descriptive field names
- Consistent structure
- Comments in description fields

### CSV Files
CSV files use standard comma-separated format with:
- Header row
- UTF-8 encoding
- Standard DTC code format

## Integration with Test Tools

These files are designed to work with:
- `test-generator.py` - Enhanced with OBD data generation
- `test-data-loader.py` - Multi-tenant test scenarios
- `dataset-converter.py` - Format conversion
- `test-multitenant.py` - Tenant isolation testing
- Integration tests - Java test suite

## Notes

- Device models are real and extracted from `supported-devices.json`
- OBD capability is determined by protocol support
- Categories (car, truck, van, etc.) are auto-determined from device names
- All test data is synthetic but based on real device capabilities
- Positions include realistic OBD parameter correlations
