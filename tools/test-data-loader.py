#!/usr/bin/env python3

"""
Comprehensive Test Data Loader for Traccar E-Sync Enhancements
Generates multi-tenant test scenarios with realistic OBD data, DTC codes, and fuel consumption
"""

import sys
import json
import argparse
import random
import time
import math
import requests
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional

class TestDataLoader:
    """Generate and load comprehensive test data for Traccar"""
    
    def __init__(self, server: str = 'http://localhost:8082', username: str = 'admin', password: str = 'admin'):
        self.server = server.rstrip('/')
        self.username = username
        self.password = password
        self.session = requests.Session()
        self.auth_token = None
        self.tenants = []
        self.devices = []
    
    def login(self) -> bool:
        """Login to Traccar and get session token"""
        try:
            response = self.session.post(
                f'{self.server}/api/session',
                json={'email': self.username, 'password': self.password}
            )
            if response.status_code == 200:
                data = response.json()
                self.auth_token = data.get('token')
                # Set authorization header
                self.session.headers.update({'Authorization': f'Bearer {self.auth_token}'})
                print(f"Logged in successfully as {self.username}")
                return True
            else:
                print(f"Login failed: {response.status_code} - {response.text}", file=sys.stderr)
                return False
        except Exception as e:
            print(f"Login error: {e}", file=sys.stderr)
            return False
    
    def create_tenant(self, name: str, schema_name: str, esync_tenant_id: str) -> Optional[Dict]:
        """Create a tenant via API"""
        try:
            tenant_data = {
                'name': name,
                'schemaName': schema_name,
                'esyncTenantId': esync_tenant_id,
                'active': True
            }
            response = self.session.post(
                f'{self.server}/api/tenants',
                json=tenant_data
            )
            if response.status_code in [200, 201]:
                tenant = response.json()
                print(f"Created tenant: {name} (ID: {tenant.get('id')})")
                return tenant
            else:
                print(f"Failed to create tenant: {response.status_code} - {response.text}", file=sys.stderr)
                return None
        except Exception as e:
            print(f"Error creating tenant: {e}", file=sys.stderr)
            return None
    
    def create_device(self, name: str, unique_id: str, tenant_id: Optional[int] = None,
                     esync_vehicle_id: Optional[str] = None) -> Optional[Dict]:
        """Create a device via API"""
        try:
            device_data = {
                'name': name,
                'uniqueId': unique_id,
                'model': 'Test Vehicle',
                'category': 'car'
            }
            
            if tenant_id:
                device_data['tenantId'] = tenant_id
            if esync_vehicle_id:
                device_data['esyncVehicleId'] = esync_vehicle_id
            
            headers = {}
            if tenant_id:
                headers['X-Tenant-ID'] = str(tenant_id)
            
            response = self.session.post(
                f'{self.server}/api/devices',
                json=device_data,
                headers=headers
            )
            if response.status_code in [200, 201]:
                device = response.json()
                print(f"Created device: {name} (ID: {device.get('id')}, UniqueID: {unique_id})")
                return device
            else:
                print(f"Failed to create device: {response.status_code} - {response.text}", file=sys.stderr)
                return None
        except Exception as e:
            print(f"Error creating device: {e}", file=sys.stderr)
            return None
    
    def send_position(self, device_id: str, position_data: Dict[str, Any], 
                     tenant_id: Optional[int] = None) -> bool:
        """Send position data to Traccar"""
        try:
            headers = {}
            if tenant_id:
                headers['X-Tenant-ID'] = str(tenant_id)
            
            params = {
                'id': device_id,
                'timestamp': int(time.time()),
                **position_data
            }
            
            # Send via HTTP GET (simulating device protocol)
            response = self.session.get(
                f'{self.server}/api/gps',
                params=params,
                headers=headers,
                timeout=5
            )
            return response.status_code == 200
        except Exception as e:
            print(f"Error sending position: {e}", file=sys.stderr)
            return False
    
    def generate_trip(self, device_id: str, start_time: datetime, duration_minutes: int,
                     start_lat: float, start_lon: float, end_lat: float, end_lon: float,
                     include_obd: bool = True, include_dtc: bool = False,
                     tenant_id: Optional[int] = None, interval_seconds: int = 30) -> int:
        """Generate a realistic trip with positions and OBD data"""
        
        num_points = (duration_minutes * 60) // interval_seconds
        positions_sent = 0
        
        # DTC codes that might appear during trip
        dtc_codes = ['P0301', 'P0420', 'P0171', 'B1234']
        dtc_injected = False
        
        for i in range(num_points):
            # Interpolate position
            progress = i / num_points
            lat = start_lat + (end_lat - start_lat) * progress
            lon = start_lon + (end_lon - start_lon) * progress
            
            # Calculate speed (accelerate, cruise, decelerate)
            if progress < 0.1:
                speed = progress * 60  # Accelerating
            elif progress > 0.9:
                speed = (1 - progress) * 60  # Decelerating
            else:
                speed = 50 + random.uniform(-5, 5)  # Cruising
            
            ignition = speed > 5
            
            position_data = {
                'lat': lat,
                'lon': lon,
                'speed': speed,
                'bearing': self._calculate_bearing(start_lat, start_lon, end_lat, end_lon),
                'altitude': 50 + random.uniform(-10, 10),
                'valid': True
            }
            
            if ignition:
                position_data['ignition'] = 'true'
            
            # Add OBD data if requested
            if include_obd:
                obd_data = self._generate_obd_data(speed, ignition)
                position_data.update(obd_data)
            
            # Inject DTC code occasionally
            if include_dtc and not dtc_injected and random.random() < 0.1:
                position_data['dtcs'] = random.choice(dtc_codes)
                dtc_injected = True
            
            # Send position
            if self.send_position(device_id, position_data, tenant_id):
                positions_sent += 1
            
            # Small delay to avoid overwhelming server
            time.sleep(0.1)
        
        return positions_sent
    
    def _generate_obd_data(self, speed: float, ignition: bool) -> Dict[str, Any]:
        """Generate realistic OBD data"""
        obd = {}
        
        if ignition and speed > 0:
            obd['rpm'] = max(800, int(speed * 50 + random.randint(-200, 200)))
            obd['coolantTemp'] = random.randint(85, 105)
            obd['throttle'] = min(100, int(speed / 2 + random.randint(-5, 10)))
            obd['engineLoad'] = min(95, int(speed * 1.5 + random.randint(-10, 15)))
            obd['mafAirFlow'] = round(speed * 2.5 + random.uniform(-5, 10), 2)
            obd['fuelConsumption'] = round(speed * 0.15 + random.uniform(-0.5, 1.0), 2)
            obd['obdSpeed'] = int(speed * 1.852)  # Convert to km/h
        elif ignition:
            obd['rpm'] = random.randint(750, 900)
            obd['coolantTemp'] = random.randint(80, 95)
            obd['throttle'] = random.randint(0, 5)
            obd['engineLoad'] = random.randint(15, 25)
            obd['mafAirFlow'] = round(random.uniform(5, 15), 2)
            obd['fuelConsumption'] = round(random.uniform(0.5, 2.0), 2)
            obd['obdSpeed'] = 0
        else:
            obd['rpm'] = 0
            obd['coolantTemp'] = random.randint(20, 40)
            obd['throttle'] = 0
            obd['engineLoad'] = 0
            obd['mafAirFlow'] = 0
            obd['fuelConsumption'] = 0
            obd['obdSpeed'] = 0
        
        obd['fuelLevel'] = random.randint(10, 100)
        obd['power'] = round(random.uniform(11.8, 14.2), 2)
        
        if random.random() < 0.7:
            obd['oilPressure'] = round(random.uniform(30, 60), 2)
        if random.random() < 0.6:
            obd['intakeAirTemp'] = random.randint(20, 50)
        
        return obd
    
    def _calculate_bearing(self, lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Calculate bearing between two points"""
        lat1_rad = math.radians(lat1)
        lat2_rad = math.radians(lat2)
        dlon_rad = math.radians(lon2 - lon1)
        
        y = math.sin(dlon_rad) * math.cos(lat2_rad)
        x = math.cos(lat1_rad) * math.sin(lat2_rad) - math.sin(lat1_rad) * math.cos(lat2_rad) * math.cos(dlon_rad)
        
        bearing = math.atan2(y, x)
        bearing = math.degrees(bearing)
        bearing = (bearing + 360) % 360
        
        return bearing
    
    def load_test_scenario(self, num_tenants: int, devices_per_tenant: int,
                          positions_per_device: int, include_obd: bool = True,
                          include_dtc: bool = False):
        """Load comprehensive test scenario"""
        
        print(f"Loading test scenario:")
        print(f"  Tenants: {num_tenants}")
        print(f"  Devices per tenant: {devices_per_tenant}")
        print(f"  Positions per device: {positions_per_device}")
        print(f"  Include OBD: {include_obd}")
        print(f"  Include DTC: {include_dtc}")
        print()
        
        # Login
        if not self.login():
            print("Failed to login. Exiting.", file=sys.stderr)
            sys.exit(1)
        
        # Create tenants
        for t in range(num_tenants):
            tenant = self.create_tenant(
                name=f'Test Tenant {t+1}',
                schema_name=f'tenant_{t+1}',
                esync_tenant_id=f'esync-tenant-{t+1}'
            )
            if tenant:
                self.tenants.append(tenant)
        
        # Create devices for each tenant
        device_counter = 1
        for tenant in self.tenants:
            tenant_id = tenant.get('id')
            
            for d in range(devices_per_tenant):
                device = self.create_device(
                    name=f'Test Vehicle {device_counter}',
                    unique_id=f'TEST{device_counter:06d}',
                    tenant_id=tenant_id,
                    esync_vehicle_id=f'esync-vehicle-{device_counter}'
                )
                if device:
                    self.devices.append((device, tenant_id))
                device_counter += 1
        
        # Generate positions for each device
        total_positions = 0
        for device, tenant_id in self.devices:
            device_id = device.get('uniqueId')
            print(f"Generating positions for device {device_id}...")
            
            # Generate multiple trips
            num_trips = max(1, positions_per_device // 50)
            for trip in range(num_trips):
                # Random start/end points
                start_lat = 40.7128 + random.uniform(-0.1, 0.1)
                start_lon = -74.0060 + random.uniform(-0.1, 0.1)
                end_lat = start_lat + random.uniform(-0.05, 0.05)
                end_lon = start_lon + random.uniform(-0.05, 0.05)
                
                start_time = datetime.now() - timedelta(hours=num_trips - trip)
                duration = random.randint(10, 60)  # 10-60 minutes
                
                sent = self.generate_trip(
                    device_id=device_id,
                    start_time=start_time,
                    duration_minutes=duration,
                    start_lat=start_lat,
                    start_lon=start_lon,
                    end_lat=end_lat,
                    end_lon=end_lon,
                    include_obd=include_obd,
                    include_dtc=include_dtc,
                    tenant_id=tenant_id,
                    interval_seconds=30
                )
                total_positions += sent
        
        print(f"\nTest scenario loaded successfully!")
        print(f"  Created {len(self.tenants)} tenants")
        print(f"  Created {len(self.devices)} devices")
        print(f"  Sent {total_positions} positions")


def main():
    parser = argparse.ArgumentParser(description='Load comprehensive test data for Traccar')
    parser.add_argument('--server', default='http://localhost:8082', help='Traccar server URL')
    parser.add_argument('--username', default='admin', help='Username')
    parser.add_argument('--password', default='admin', help='Password')
    parser.add_argument('--tenants', type=int, default=3, help='Number of tenants')
    parser.add_argument('--devices-per-tenant', type=int, default=5, help='Devices per tenant')
    parser.add_argument('--positions-per-device', type=int, default=100, help='Positions per device')
    parser.add_argument('--include-obd', action='store_true', default=True, help='Include OBD data')
    parser.add_argument('--include-dtc', action='store_true', help='Include DTC codes')
    parser.add_argument('--no-obd', dest='include_obd', action='store_false', help='Exclude OBD data')
    
    args = parser.parse_args()
    
    loader = TestDataLoader(
        server=args.server,
        username=args.username,
        password=args.password
    )
    
    loader.load_test_scenario(
        num_tenants=args.tenants,
        devices_per_tenant=args.devices_per_tenant,
        positions_per_device=args.positions_per_device,
        include_obd=args.include_obd,
        include_dtc=args.include_dtc
    )


if __name__ == '__main__':
    main()
