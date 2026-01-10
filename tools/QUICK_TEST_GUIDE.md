# Quick Test Guide

## Testing Without Server (Dry-Run Mode)

You can test the test data generation without needing a running Traccar server using the `--dry-run` flag.

### Generate Test Positions with OBD Data

```bash
python3 tools/test-generator.py \
    --id TEST001 \
    --count 100 \
    --obd \
    --dtc \
    --dry-run \
    --output test-positions.json
```

This will:
- Generate 100 positions with OBD data
- Include DTC codes (5% chance per position)
- Save to `test-positions.json` without connecting to server
- Show statistics about generated data

### Example Output

```
Starting test generator: device=TEST001, server=localhost:5055, obd=True, dtc=True
DRY-RUN mode: Generating data without sending to server

✓ Generated 100 positions
✓ Saved to test-positions.json

Statistics:
  - Positions with OBD data: 100
  - Positions with DTC codes: 5
  - Total positions: 100
```

### Validate Generated Data

```bash
python3 -c "
import json
data = json.load(open('test-positions.json'))
print(f'Total positions: {len(data)}')
print(f'With OBD: {sum(1 for p in data if \"rpm\" in p)}')
print(f'With DTC: {sum(1 for p in data if \"dtcs\" in p)}')
print(f'OBD parameters: {set(k for p in data for k in p if k in [\"rpm\", \"coolantTemp\", \"fuelLevel\", \"fuelConsumption\"])}')
"
```

## Testing With Server

Once your Traccar server is running:

### 1. Start the Server

```bash
./gradlew bootRun
```

### 2. Generate and Send Test Data

```bash
# Without dry-run, sends to server
python3 tools/test-generator.py \
    --id TEST001 \
    --count 100 \
    --obd \
    --dtc \
    --server localhost:8082
```

### 3. Test Features

```bash
python3 tools/test-features.py \
    --server http://localhost:8082 \
    --username admin \
    --password admin \
    --device-id TEST001
```

### 4. Test Multi-Tenant

```bash
python3 tools/test-multitenant.py \
    --server http://localhost:8082 \
    --tenants 3
```

## Quick Validation

Validate all test data files:

```bash
python3 tools/validate-test-data.py
```

## Test Data Files

All test data files are in `tools/test-data/`:
- `sample-obd-positions.json` - Sample positions with OBD
- `sample-fuel-data.json` - Fuel consumption scenarios
- `sample-dtc-codes.csv` - Common DTC codes
- `sample-multitenant.json` - Multi-tenant configuration
- `sample-obd-devices.json` - OBD-capable devices
- `device-reference.json` - Device statistics

## Tips

1. **Dry-run first**: Always test with `--dry-run` to validate data generation
2. **Small counts**: Start with `--count 10` to verify everything works
3. **Check output**: Review generated JSON files to ensure data looks correct
4. **Server logs**: Check Traccar server logs when testing with live server
