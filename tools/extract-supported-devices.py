#!/usr/bin/env python3

"""
Extract and categorize supported devices from supported-devices.json
Creates test data files with real device information
"""

import json
import sys
import os
from collections import defaultdict
from typing import Dict, List

# Protocols known to support OBD data
OBD_PROTOCOLS = [
    'freematics',
    'obddongle',
    'teltonika',
    'osmand',
    'galileo',
    'totem',
    'ruptela',
    'ulbotech',
    'gps103',  # Some GPS103 devices support OBD
    'gt06'     # Some GT06 devices support OBD
]

def load_supported_devices(file_path: str = 'supported-devices.json') -> List[Dict]:
    """Load supported devices from JSON file"""
    try:
        with open(file_path, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: {file_path} not found", file=sys.stderr)
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON: {e}", file=sys.stderr)
        sys.exit(1)

def categorize_devices(devices: List[Dict]) -> Dict:
    """Categorize devices by various criteria"""
    valid_devices = [d for d in devices if d.get('protocol') and not d.get('isClone', False)]
    
    obd_devices = [d for d in valid_devices if d.get('protocol') in OBD_PROTOCOLS]
    recommended_devices = [d for d in valid_devices if d.get('recommended', False)]
    
    # Group by protocol
    by_protocol = defaultdict(list)
    for device in valid_devices:
        protocol = device.get('protocol')
        if protocol:
            by_protocol[protocol].append(device)
    
    # Group by port
    by_port = defaultdict(list)
    for device in valid_devices:
        port = device.get('port')
        if port:
            by_port[port].append(device)
    
    return {
        'total': len(devices),
        'valid': len(valid_devices),
        'obd_capable': len(obd_devices),
        'recommended': len(recommended_devices),
        'protocols': len(by_protocol),
        'valid_devices': valid_devices,
        'obd_devices': obd_devices,
        'recommended_devices': recommended_devices,
        'by_protocol': dict(by_protocol),
        'by_port': dict(by_port)
    }

def determine_category(device_name: str) -> str:
    """Determine vehicle category from device name"""
    name_lower = device_name.lower()
    
    if any(x in name_lower for x in ['truck', 'freight', 'cargo', 'commercial', 'scania', 'volvo', 'iveco']):
        return 'truck'
    elif any(x in name_lower for x in ['van', 'delivery', 'transit', 'sprinter']):
        return 'van'
    elif any(x in name_lower for x in ['motorcycle', 'bike', 'scooter', 'moto']):
        return 'motorcycle'
    elif any(x in name_lower for x in ['bus', 'coach']):
        return 'bus'
    else:
        return 'car'

def create_obd_devices_file(obd_devices: List[Dict], output_path: str):
    """Create sample OBD devices file"""
    # Take a sample of OBD devices (up to 50)
    sample = obd_devices[:50]
    
    devices_data = {
        'description': 'Sample OBD-capable devices from supported-devices.json',
        'total_obd_devices': len(obd_devices),
        'sample_size': len(sample),
        'devices': [
            {
                'device': d['device'],
                'protocol': d['protocol'],
                'port': d.get('port'),
                'recommended': d.get('recommended', False),
                'category': determine_category(d['device']),
                'supportsObd': True
            }
            for d in sample
        ]
    }
    
    with open(output_path, 'w') as f:
        json.dump(devices_data, f, indent=2)
    
    print(f"Created {output_path} with {len(sample)} OBD-capable devices")

def create_device_positions_file(obd_devices: List[Dict], output_path: str):
    """Create sample positions for OBD devices"""
    import random
    from datetime import datetime, timedelta
    
    # Select 5 devices for sample positions
    sample_devices = random.sample(obd_devices[:20], min(5, len(obd_devices[:20])))
    
    positions = []
    base_time = int(datetime.now().timestamp())
    
    for device in sample_devices:
        device_id = f"DEV{device['device'].replace(' ', '').upper()[:10]}"
        
        # Generate 3 positions per device
        for i in range(3):
            position = {
                'id': device_id,
                'timestamp': base_time + i * 60,
                'lat': 40.7128 + random.uniform(-0.1, 0.1),
                'lon': -74.0060 + random.uniform(-0.1, 0.1),
                'speed': random.randint(0, 80),
                'bearing': random.randint(0, 360),
                'altitude': random.randint(0, 100),
                'valid': True,
                'device': device['device'],
                'protocol': device['protocol']
            }
            
            # Add OBD data
            if device['protocol'] in OBD_PROTOCOLS:
                position.update({
                    'rpm': random.randint(800, 3500),
                    'coolantTemp': random.randint(80, 105),
                    'throttle': random.randint(0, 100),
                    'engineLoad': random.randint(20, 90),
                    'fuelLevel': random.randint(20, 100),
                    'fuelConsumption': round(random.uniform(5.0, 15.0), 2),
                    'power': round(random.uniform(12.0, 14.5), 2)
                })
                
                # Occasionally add DTC codes
                if random.random() < 0.3:
                    position['dtcs'] = random.choice(['P0301', 'P0420', 'P0171'])
            
            positions.append(position)
    
    with open(output_path, 'w') as f:
        json.dump(positions, f, indent=2)
    
    print(f"Created {output_path} with positions for {len(sample_devices)} devices")

def create_device_reference_file(categories: Dict, output_path: str):
    """Create comprehensive device reference file"""
    reference = {
        'description': 'Comprehensive reference of supported devices',
        'statistics': {
            'total_devices': categories['total'],
            'valid_devices': categories['valid'],
            'obd_capable': categories['obd_capable'],
            'recommended': categories['recommended'],
            'unique_protocols': categories['protocols']
        },
        'protocols': {
            protocol: {
                'count': len(devices),
                'obd_support': protocol in OBD_PROTOCOLS,
                'sample_devices': [d['device'] for d in devices[:5]]
            }
            for protocol, devices in categories['by_protocol'].items()
        },
        'sample_recommended': [
            {
                'device': d['device'],
                'protocol': d['protocol'],
                'port': d.get('port'),
                'category': determine_category(d['device'])
            }
            for d in categories['recommended_devices']
        ],
        'sample_obd_devices': [
            {
                'device': d['device'],
                'protocol': d['protocol'],
                'port': d.get('port'),
                'category': determine_category(d['device'])
            }
            for d in categories['obd_devices'][:20]
        ]
    }
    
    with open(output_path, 'w') as f:
        json.dump(reference, f, indent=2)
    
    print(f"Created {output_path} with device reference")

def update_multitenant_file(obd_devices: List[Dict], output_path: str):
    """Update sample-multitenant.json with real device models"""
    # Select devices for each tenant
    selected_devices = {
        'acme': obd_devices[0] if len(obd_devices) > 0 else None,
        'acme2': obd_devices[1] if len(obd_devices) > 1 else None,
        'beta': obd_devices[2] if len(obd_devices) > 2 else None,
        'beta2': obd_devices[3] if len(obd_devices) > 3 else None,
        'gamma': obd_devices[4] if len(obd_devices) > 4 else None
    }
    
    multitenant_data = {
        'description': 'Multi-tenant test configuration with real supported devices',
        'tenants': [
            {
                'name': 'Acme Corporation',
                'schemaName': 'acme_corp',
                'esyncTenantId': 'esync-acme-001',
                'active': True,
                'devices': [
                    {
                        'name': 'Acme Vehicle 1',
                        'uniqueId': 'ACME001',
                        'esyncVehicleId': 'esync-vehicle-acme-001',
                        'model': selected_devices['acme']['device'] if selected_devices['acme'] else 'Ford Transit',
                        'protocol': selected_devices['acme']['protocol'] if selected_devices['acme'] else 'osmand',
                        'category': determine_category(selected_devices['acme']['device']) if selected_devices['acme'] else 'van',
                        'supportsObd': selected_devices['acme'] is not None
                    },
                    {
                        'name': 'Acme Vehicle 2',
                        'uniqueId': 'ACME002',
                        'esyncVehicleId': 'esync-vehicle-acme-002',
                        'model': selected_devices['acme2']['device'] if selected_devices['acme2'] else 'Mercedes Sprinter',
                        'protocol': selected_devices['acme2']['protocol'] if selected_devices['acme2'] else 'osmand',
                        'category': determine_category(selected_devices['acme2']['device']) if selected_devices['acme2'] else 'truck',
                        'supportsObd': selected_devices['acme2'] is not None
                    }
                ]
            },
            {
                'name': 'Beta Logistics',
                'schemaName': 'beta_logistics',
                'esyncTenantId': 'esync-beta-001',
                'active': True,
                'devices': [
                    {
                        'name': 'Beta Vehicle 1',
                        'uniqueId': 'BETA001',
                        'esyncVehicleId': 'esync-vehicle-beta-001',
                        'model': selected_devices['beta']['device'] if selected_devices['beta'] else 'Volvo FH',
                        'protocol': selected_devices['beta']['protocol'] if selected_devices['beta'] else 'osmand',
                        'category': determine_category(selected_devices['beta']['device']) if selected_devices['beta'] else 'truck',
                        'supportsObd': selected_devices['beta'] is not None
                    },
                    {
                        'name': 'Beta Vehicle 2',
                        'uniqueId': 'BETA002',
                        'esyncVehicleId': 'esync-vehicle-beta-002',
                        'model': selected_devices['beta2']['device'] if selected_devices['beta2'] else 'Scania R',
                        'protocol': selected_devices['beta2']['protocol'] if selected_devices['beta2'] else 'osmand',
                        'category': determine_category(selected_devices['beta2']['device']) if selected_devices['beta2'] else 'truck',
                        'supportsObd': selected_devices['beta2'] is not None
                    }
                ]
            },
            {
                'name': 'Gamma Transport',
                'schemaName': 'gamma_transport',
                'esyncTenantId': 'esync-gamma-001',
                'active': True,
                'devices': [
                    {
                        'name': 'Gamma Vehicle 1',
                        'uniqueId': 'GAMMA001',
                        'esyncVehicleId': 'esync-vehicle-gamma-001',
                        'model': selected_devices['gamma']['device'] if selected_devices['gamma'] else 'Iveco Daily',
                        'protocol': selected_devices['gamma']['protocol'] if selected_devices['gamma'] else 'osmand',
                        'category': determine_category(selected_devices['gamma']['device']) if selected_devices['gamma'] else 'van',
                        'supportsObd': selected_devices['gamma'] is not None
                    }
                ]
            }
        ],
        'testScenarios': [
            {
                'name': 'Tenant Isolation Test',
                'description': 'Verify tenants cannot access each other\'s data',
                'steps': [
                    'Create tenants with devices',
                    'Query devices with tenant1 context',
                    'Verify only tenant1 devices are returned',
                    'Query devices with tenant2 context',
                    'Verify only tenant2 devices are returned'
                ]
            },
            {
                'name': 'Cross-Tenant Access Test',
                'description': 'Verify cross-tenant access is blocked',
                'steps': [
                    'Get device ID from tenant1',
                    'Try to access device with tenant2 context',
                    'Verify access is denied or filtered'
                ]
            },
            {
                'name': 'Schema Switching Test',
                'description': 'Verify PostgreSQL schema switching works',
                'steps': [
                    'Create device with tenant1 context',
                    'Verify device is stored in tenant1 schema',
                    'Query device with tenant1 context',
                    'Verify device is accessible'
                ]
            }
        ]
    }
    
    with open(output_path, 'w') as f:
        json.dump(multitenant_data, f, indent=2)
    
    print(f"Updated {output_path} with real device models")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    test_data_dir = os.path.join(script_dir, 'test-data')
    
    # Ensure test-data directory exists
    os.makedirs(test_data_dir, exist_ok=True)
    
    # Load devices
    devices_file = os.path.join(project_root, 'supported-devices.json')
    devices = load_supported_devices(devices_file)
    
    # Categorize
    categories = categorize_devices(devices)
    
    print("=" * 60)
    print("Supported Devices Analysis")
    print("=" * 60)
    print(f"Total devices: {categories['total']}")
    print(f"Valid devices: {categories['valid']}")
    print(f"OBD-capable devices: {categories['obd_capable']}")
    print(f"Recommended devices: {categories['recommended']}")
    print(f"Unique protocols: {categories['protocols']}")
    print("=" * 60)
    print()
    
    # Create output files
    obd_devices_file = os.path.join(test_data_dir, 'sample-obd-devices.json')
    device_positions_file = os.path.join(test_data_dir, 'sample-device-positions.json')
    device_reference_file = os.path.join(test_data_dir, 'device-reference.json')
    multitenant_file = os.path.join(test_data_dir, 'sample-multitenant.json')
    
    create_obd_devices_file(categories['obd_devices'], obd_devices_file)
    create_device_positions_file(categories['obd_devices'], device_positions_file)
    create_device_reference_file(categories, device_reference_file)
    update_multitenant_file(categories['obd_devices'], multitenant_file)
    
    print()
    print("=" * 60)
    print("Test data files created successfully!")
    print("=" * 60)

if __name__ == '__main__':
    main()
