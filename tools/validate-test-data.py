#!/usr/bin/env python3

"""
Validate Test Data Files
Validates all test data files and provides statistics
"""

import json
import csv
import sys
import os
from typing import Dict, List, Any

def validate_json_file(filepath: str) -> tuple[bool, str, Any]:
    """Validate JSON file and return data"""
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
        return True, "Valid JSON", data
    except FileNotFoundError:
        return False, "File not found", None
    except json.JSONDecodeError as e:
        return False, f"Invalid JSON: {e}", None
    except Exception as e:
        return False, f"Error: {e}", None

def validate_csv_file(filepath: str) -> tuple[bool, str, int]:
    """Validate CSV file and return row count"""
    try:
        with open(filepath, 'r') as f:
            reader = csv.DictReader(f)
            rows = list(reader)
        return True, f"Valid CSV with {len(rows)} rows", len(rows)
    except FileNotFoundError:
        return False, "File not found", 0
    except Exception as e:
        return False, f"Error: {e}", 0

def analyze_obd_positions(data: List[Dict]) -> Dict[str, Any]:
    """Analyze OBD positions data"""
    if not isinstance(data, list):
        return {}
    
    stats = {
        'total_positions': len(data),
        'with_obd': 0,
        'with_dtc': 0,
        'obd_parameters': set(),
        'dtc_codes': set()
    }
    
    for pos in data:
        has_obd = any(key in pos for key in ['rpm', 'coolantTemp', 'fuelLevel', 'fuelConsumption'])
        if has_obd:
            stats['with_obd'] += 1
            for key in ['rpm', 'coolantTemp', 'throttle', 'engineLoad', 'mafAirFlow', 
                       'fuelLevel', 'fuelConsumption', 'power', 'oilPressure']:
                if key in pos:
                    stats['obd_parameters'].add(key)
        
        if 'dtcs' in pos:
            stats['with_dtc'] += 1
            dtcs = str(pos['dtcs']).split(',')
            stats['dtc_codes'].update(dtcs)
    
    stats['obd_parameters'] = sorted(stats['obd_parameters'])
    stats['dtc_codes'] = sorted(stats['dtc_codes'])
    return stats

def analyze_fuel_data(data: Dict) -> Dict[str, Any]:
    """Analyze fuel consumption data"""
    if not isinstance(data, dict) or 'scenarios' not in data:
        return {}
    
    scenarios = data.get('scenarios', [])
    stats = {
        'total_scenarios': len(scenarios),
        'scenario_names': [s.get('name') for s in scenarios],
        'total_positions': sum(len(s.get('positions', [])) for s in scenarios)
    }
    return stats

def analyze_obd_devices(data: Dict) -> Dict[str, Any]:
    """Analyze OBD devices data"""
    if not isinstance(data, dict):
        return {}
    
    devices = data.get('devices', [])
    stats = {
        'total_devices': len(devices),
        'protocols': set(),
        'categories': set(),
        'recommended': 0
    }
    
    for device in devices:
        if device.get('protocol'):
            stats['protocols'].add(device['protocol'])
        if device.get('category'):
            stats['categories'].add(device['category'])
        if device.get('recommended'):
            stats['recommended'] += 1
    
    stats['protocols'] = sorted(stats['protocols'])
    stats['categories'] = sorted(stats['categories'])
    return stats

def analyze_multitenant(data: Dict) -> Dict[str, Any]:
    """Analyze multi-tenant data"""
    if not isinstance(data, dict):
        return {}
    
    tenants = data.get('tenants', [])
    stats = {
        'total_tenants': len(tenants),
        'total_devices': sum(len(t.get('devices', [])) for t in tenants),
        'tenant_names': [t.get('name') for t in tenants]
    }
    return stats

def main():
    print("=" * 60)
    print("Test Data Validation and Analysis")
    print("=" * 60)
    print()
    
    test_data_dir = 'tools/test-data'
    
    # Test files to validate
    files = [
        ('sample-obd-positions.json', 'json', analyze_obd_positions),
        ('sample-fuel-data.json', 'json', analyze_fuel_data),
        ('sample-obd-devices.json', 'json', analyze_obd_devices),
        ('sample-multitenant.json', 'json', analyze_multitenant),
        ('device-reference.json', 'json', None),
        ('sample-dtc-codes.csv', 'csv', None)
    ]
    
    all_valid = True
    
    for filename, filetype, analyzer in files:
        filepath = os.path.join(test_data_dir, filename)
        print(f"Validating {filename}...")
        
        if filetype == 'json':
            valid, message, data = validate_json_file(filepath)
            if valid:
                print(f"  ✓ {message}")
                if analyzer and data:
                    stats = analyzer(data)
                    for key, value in stats.items():
                        if isinstance(value, (list, set)):
                            print(f"    - {key}: {len(value)} items")
                            if len(value) <= 10:
                                print(f"      {', '.join(str(v) for v in value)}")
                        else:
                            print(f"    - {key}: {value}")
            else:
                print(f"  ✗ {message}")
                all_valid = False
        else:
            valid, message, count = validate_csv_file(filepath)
            if valid:
                print(f"  ✓ {message}")
            else:
                print(f"  ✗ {message}")
                all_valid = False
        
        print()
    
    print("=" * 60)
    if all_valid:
        print("✓ All test data files are valid!")
        print()
        print("You can now use these files with:")
        print("  - test-generator.py --obd --dtc")
        print("  - test-data-loader.py")
        print("  - dataset-converter.py")
        print("  - test-multitenant.py")
        return 0
    else:
        print("✗ Some test data files have errors")
        return 1

if __name__ == '__main__':
    sys.exit(main())
