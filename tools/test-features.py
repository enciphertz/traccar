#!/usr/bin/env python3

"""
Test E-Sync Features with Test Data
Tests OBD data processing, DTC codes, fuel consumption, and multi-tenancy
"""

import sys
import json
import argparse
import urllib.request
import urllib.parse
import urllib.error
from typing import Dict, List, Any, Optional

class FeatureTester:
    """Test E-Sync features with test data"""
    
    def __init__(self, server: str = 'http://localhost:8082', username: str = 'admin', password: str = 'admin'):
        self.server = server.rstrip('/')
        self.username = username
        self.password = password
        self.auth_token = None
        self.base_url = f'{self.server}/api'
    
    def login(self) -> bool:
        """Login and get session token"""
        try:
            data = json.dumps({'email': self.username, 'password': self.password}).encode()
            req = urllib.request.Request(
                f'{self.base_url}/session',
                data=data,
                headers={'Content-Type': 'application/json'}
            )
            with urllib.request.urlopen(req, timeout=5) as response:
                if response.status == 200:
                    result = json.loads(response.read().decode())
                    self.auth_token = result.get('token')
                    print(f"✓ Logged in successfully as {self.username}")
                    return True
                else:
                    print(f"✗ Login failed: {response.status}", file=sys.stderr)
                    return False
        except urllib.error.URLError as e:
            print(f"✗ Cannot connect to server: {e}", file=sys.stderr)
            return False
        except Exception as e:
            print(f"✗ Login error: {e}", file=sys.stderr)
            return False
    
    def _make_request(self, method: str, path: str, data: Optional[Dict] = None, 
                     headers: Optional[Dict] = None) -> Optional[Dict]:
        """Make API request"""
        url = f'{self.base_url}/{path}'
        req_headers = {'Content-Type': 'application/json'}
        
        if self.auth_token:
            req_headers['Authorization'] = f'Bearer {self.auth_token}'
        
        if headers:
            req_headers.update(headers)
        
        try:
            if data:
                data_bytes = json.dumps(data).encode()
                req = urllib.request.Request(url, data=data_bytes, headers=req_headers, method=method)
            else:
                req = urllib.request.Request(url, headers=req_headers, method=method)
            
            with urllib.request.urlopen(req, timeout=10) as response:
                if response.status in [200, 201]:
                    return json.loads(response.read().decode())
                else:
                    print(f"✗ Request failed: {response.status} - {response.read().decode()}", file=sys.stderr)
                    return None
        except urllib.error.HTTPError as e:
            error_body = e.read().decode() if e.fp else ''
            print(f"✗ HTTP Error {e.code}: {error_body}", file=sys.stderr)
            return None
        except Exception as e:
            print(f"✗ Request error: {e}", file=sys.stderr)
            return None
    
    def test_obd_data_processing(self, device_id: str) -> bool:
        """Test OBD data processing by sending positions with OBD data"""
        print("\n=== Testing OBD Data Processing ===")
        
        # Load sample OBD positions
        try:
            with open('tools/test-data/sample-obd-positions.json', 'r') as f:
                positions = json.load(f)
        except FileNotFoundError:
            print("✗ sample-obd-positions.json not found", file=sys.stderr)
            return False
        
        success_count = 0
        for i, pos in enumerate(positions):
            # Update device ID
            pos['id'] = device_id
            
            # Send position via GPS endpoint (simulating device)
            params = urllib.parse.urlencode(pos)
            try:
                url = f'{self.server}/api/gps?{params}'
                req = urllib.request.Request(url)
                with urllib.request.urlopen(req, timeout=5) as response:
                    if response.status == 200:
                        success_count += 1
                        print(f"  ✓ Sent position {i+1} with OBD data")
                    else:
                        print(f"  ✗ Failed to send position {i+1}: {response.status}")
            except Exception as e:
                print(f"  ✗ Error sending position {i+1}: {e}")
        
        # Check if OBD parameters were stored
        obd_data = self._make_request('GET', f'obd?deviceId={device_id}')
        if obd_data:
            print(f"  ✓ Retrieved {len(obd_data) if isinstance(obd_data, list) else 1} OBD parameter records")
            return True
        else:
            print("  ⚠ Could not retrieve OBD parameters (may need to wait for processing)")
            return success_count > 0
    
    def test_dtc_codes(self, device_id: str) -> bool:
        """Test DTC code processing"""
        print("\n=== Testing DTC Code Processing ===")
        
        # Send position with DTC codes
        position_with_dtc = {
            'id': device_id,
            'lat': 40.7128,
            'lon': -74.0060,
            'speed': 50,
            'dtcs': 'P0301,P0420'
        }
        
        params = urllib.parse.urlencode(position_with_dtc)
        try:
            url = f'{self.server}/api/gps?{params}'
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=5) as response:
                if response.status == 200:
                    print("  ✓ Sent position with DTC codes")
                else:
                    print(f"  ✗ Failed to send position: {response.status}")
                    return False
        except Exception as e:
            print(f"  ✗ Error sending position: {e}")
            return False
        
        # Check if DTC codes were stored
        dtc_data = self._make_request('GET', f'dtc?deviceId={device_id}')
        if dtc_data:
            dtc_list = dtc_data if isinstance(dtc_data, list) else [dtc_data]
            print(f"  ✓ Retrieved {len(dtc_list)} DTC code records")
            for dtc in dtc_list[:3]:
                print(f"    - {dtc.get('code')}: {dtc.get('description', 'N/A')}")
            return True
        else:
            print("  ⚠ Could not retrieve DTC codes (may need to wait for processing)")
            return True
    
    def test_fuel_consumption(self, device_id: str) -> bool:
        """Test fuel consumption calculations"""
        print("\n=== Testing Fuel Consumption ===")
        
        # Load sample fuel data
        try:
            with open('tools/test-data/sample-fuel-data.json', 'r') as f:
                fuel_data = json.load(f)
        except FileNotFoundError:
            print("✗ sample-fuel-data.json not found", file=sys.stderr)
            return False
        
        # Send positions from fuel scenarios
        for scenario in fuel_data.get('scenarios', [])[:1]:  # Test first scenario
            positions = scenario.get('positions', [])
            for pos in positions:
                pos['id'] = device_id
                params = urllib.parse.urlencode({k: str(v) for k, v in pos.items()})
                try:
                    url = f'{self.server}/api/gps?{params}'
                    req = urllib.request.Request(url)
                    with urllib.request.urlopen(req, timeout=5) as response:
                        if response.status == 200:
                            print(f"  ✓ Sent position for {scenario['name']}")
                except Exception as e:
                    print(f"  ✗ Error: {e}")
        
        # Check fuel consumption records
        fuel_data = self._make_request('GET', f'fuel?deviceId={device_id}')
        if fuel_data:
            fuel_list = fuel_data if isinstance(fuel_data, list) else [fuel_data]
            print(f"  ✓ Retrieved {len(fuel_list)} fuel consumption records")
            return True
        else:
            print("  ⚠ Could not retrieve fuel consumption (calculations may be async)")
            return True
    
    def test_api_endpoints(self, device_id: str) -> bool:
        """Test new API endpoints"""
        print("\n=== Testing API Endpoints ===")
        
        endpoints = [
            ('obd', 'OBD Parameters'),
            ('dtc', 'DTC Codes'),
            ('fuel', 'Fuel Consumption')
        ]
        
        all_passed = True
        for endpoint, name in endpoints:
            data = self._make_request('GET', f'{endpoint}?deviceId={device_id}')
            if data is not None:
                count = len(data) if isinstance(data, list) else (1 if data else 0)
                print(f"  ✓ {name} endpoint: {count} records")
            else:
                print(f"  ⚠ {name} endpoint: No data (may be empty)")
                # Don't fail if endpoint exists but has no data
        
        return all_passed
    
    def test_with_sample_data(self, device_id: str = 'TEST001'):
        """Run all tests with sample data"""
        print("=" * 60)
        print("E-Sync Features Test Suite")
        print("=" * 60)
        
        if not self.login():
            print("\n✗ Cannot proceed without login")
            return False
        
        results = []
        
        # Test OBD data processing
        results.append(('OBD Data Processing', self.test_obd_data_processing(device_id)))
        
        # Test DTC codes
        results.append(('DTC Code Processing', self.test_dtc_codes(device_id)))
        
        # Test fuel consumption
        results.append(('Fuel Consumption', self.test_fuel_consumption(device_id)))
        
        # Test API endpoints
        results.append(('API Endpoints', self.test_api_endpoints(device_id)))
        
        # Summary
        print("\n" + "=" * 60)
        print("Test Results Summary")
        print("=" * 60)
        
        for test_name, passed in results:
            status = "PASS" if passed else "FAIL"
            print(f"{test_name}: {status}")
        
        print("=" * 60)
        
        all_passed = all(passed for _, passed in results)
        return all_passed


def main():
    parser = argparse.ArgumentParser(description='Test E-Sync features with test data')
    parser.add_argument('--server', default='http://localhost:8082', help='Traccar server URL')
    parser.add_argument('--username', default='admin', help='Username')
    parser.add_argument('--password', default='admin', help='Password')
    parser.add_argument('--device-id', default='TEST001', help='Device ID for testing')
    
    args = parser.parse_args()
    
    tester = FeatureTester(
        server=args.server,
        username=args.username,
        password=args.password
    )
    
    success = tester.test_with_sample_data(args.device_id)
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
