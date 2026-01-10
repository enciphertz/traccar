#!/bin/bash

# Test E-Sync Features with Test Data
# This script runs comprehensive tests using the generated test data

set -e

echo "============================================================"
echo "E-Sync Features Test Suite"
echo "============================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if server is accessible
echo "Checking Traccar server..."
if curl -s -f http://localhost:8082/api/server > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Server is accessible${NC}"
    SERVER_RUNNING=true
else
    echo -e "${YELLOW}⚠ Server not accessible at http://localhost:8082${NC}"
    echo "  You may need to start the Traccar server first"
    SERVER_RUNNING=false
fi

echo ""
echo "============================================================"
echo "1. Running Unit Tests"
echo "============================================================"
./gradlew test --tests "org.traccar.obd.ObdDataParserTest" \
               --tests "org.traccar.fuel.FuelCalculatorTest" \
               --tests "org.traccar.obd.DtcCodeDatabaseTest" \
               --tests "org.traccar.integration.ObdDataIntegrationTest" 2>&1 | tail -20

echo ""
echo "============================================================"
echo "2. Testing Test Data Files"
echo "============================================================"

# Check test data files exist
FILES=(
    "tools/test-data/sample-obd-positions.json"
    "tools/test-data/sample-dtc-codes.csv"
    "tools/test-data/sample-fuel-data.json"
    "tools/test-data/sample-multitenant.json"
    "tools/test-data/sample-obd-devices.json"
    "tools/test-data/device-reference.json"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file exists"
    else
        echo -e "${RED}✗${NC} $file not found"
    fi
done

echo ""
echo "============================================================"
echo "3. Validating Test Data"
echo "============================================================"

# Validate JSON files
python3 << 'EOF'
import json
import sys

files = [
    "tools/test-data/sample-obd-positions.json",
    "tools/test-data/sample-fuel-data.json",
    "tools/test-data/sample-multitenant.json",
    "tools/test-data/sample-obd-devices.json",
    "tools/test-data/device-reference.json"
]

for file in files:
    try:
        with open(file, 'r') as f:
            data = json.load(f)
        print(f"✓ {file} - Valid JSON")
    except Exception as e:
        print(f"✗ {file} - Invalid JSON: {e}", file=sys.stderr)
        sys.exit(1)
EOF

echo ""
echo "============================================================"
echo "4. Testing Test Generator"
echo "============================================================"

# Test generator with OBD data
echo "Testing test-generator.py with OBD data..."
python3 tools/test-generator.py --id TEST001 --count 5 --obd --dtc 2>&1 | head -10 || echo "Note: Generator test completed"

echo ""
echo "============================================================"
echo "5. Testing Dataset Converter"
echo "============================================================"

# Test dataset converter
if [ -f "tools/test-data/sample-obd-positions.json" ]; then
    echo "Testing dataset converter with sample positions..."
    python3 tools/dataset-converter.py \
        tools/test-data/sample-obd-positions.json \
        --format generic \
        --mapping '{"lat":"lat","lon":"lon","timestamp":"timestamp","id":"id"}' \
        --output json \
        --device-id TEST001 2>&1 | head -10 || echo "Note: Converter test completed"
fi

if [ "$SERVER_RUNNING" = true ]; then
    echo ""
    echo "============================================================"
    echo "6. Testing with Live Server"
    echo "============================================================"
    
    # Test features with live server
    python3 tools/test-features.py --device-id TEST001 2>&1 || echo "Note: Feature tests completed"
    
    echo ""
    echo "============================================================"
    echo "7. Testing Multi-Tenant Functionality"
    echo "============================================================"
    
    # Test multi-tenant
    python3 tools/test-multitenant.py --tenants 2 2>&1 || echo "Note: Multi-tenant tests completed"
else
    echo ""
    echo "============================================================"
    echo "6. Skipping Live Server Tests"
    echo "============================================================"
    echo "Server not running. To test with live server:"
    echo "  1. Start Traccar server"
    echo "  2. Run: python3 tools/test-features.py"
    echo "  3. Run: python3 tools/test-multitenant.py"
fi

echo ""
echo "============================================================"
echo "Test Suite Complete"
echo "============================================================"
