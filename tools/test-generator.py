#!/usr/bin/env python3

import sys
import math
import urllib
import http.client as httplib
import time
import random
import argparse
import json

# Default values
id = '123456789012345'
server = 'localhost:5055'
period = 1
step = 0.001
device_speed = 40
driver_id = '123456'
tenant_id = None

# Common DTC codes for testing
DTC_CODES = [
    'P0301', 'P0302', 'P0303', 'P0304',  # Misfire codes
    'P0420', 'P0430',  # Catalyst efficiency
    'P0171', 'P0172',  # Fuel trim
    'P0128',  # Coolant thermostat
    'P0455',  # EVAP system leak
    'B1234', 'B1235',  # Body codes
    'C1201', 'C1202',  # Chassis codes
    'U0100', 'U0101'   # Network codes
]

waypoints = [
    (48.853780, 2.344347),
    (48.855235, 2.345852),
    (48.857238, 2.347153),
    (48.858509, 2.342563),
    (48.856066, 2.340432),
    (48.854780, 2.342230)
]

points = []

for i in range(0, len(waypoints)):
    (lat1, lon1) = waypoints[i]
    (lat2, lon2) = waypoints[(i + 1) % len(waypoints)]
    length = math.sqrt((lat2 - lat1) ** 2 + (lon2 - lon1) ** 2)
    count = int(math.ceil(length / step))
    for j in range(0, count):
        lat = lat1 + (lat2 - lat1) * j / count
        lon = lon1 + (lon2 - lon1) * j / count
        points.append((lat, lon))

def generate_obd_data(speed, ignition):
    """Generate realistic OBD data correlated with speed and ignition"""
    obd = {}
    
    if ignition and speed > 0:
        # Engine running and moving
        obd['rpm'] = max(800, int(speed * 50 + random.randint(-200, 200)))
        obd['coolantTemp'] = random.randint(85, 105)
        obd['throttle'] = min(100, int(speed / 2 + random.randint(-5, 10)))
        obd['engineLoad'] = min(95, int(speed * 1.5 + random.randint(-10, 15)))
        obd['mafAirFlow'] = round(speed * 2.5 + random.uniform(-5, 10), 2)
        obd['fuelConsumption'] = round(speed * 0.15 + random.uniform(-0.5, 1.0), 2)
        obd['obdSpeed'] = int(speed * 1.852)  # Convert knots to km/h
    elif ignition:
        # Engine running but idle
        obd['rpm'] = random.randint(750, 900)
        obd['coolantTemp'] = random.randint(80, 95)
        obd['throttle'] = random.randint(0, 5)
        obd['engineLoad'] = random.randint(15, 25)
        obd['mafAirFlow'] = round(random.uniform(5, 15), 2)
        obd['fuelConsumption'] = round(random.uniform(0.5, 2.0), 2)
        obd['obdSpeed'] = 0
    else:
        # Engine off
        obd['rpm'] = 0
        obd['coolantTemp'] = random.randint(20, 40)
        obd['throttle'] = 0
        obd['engineLoad'] = 0
        obd['mafAirFlow'] = 0
        obd['fuelConsumption'] = 0
        obd['obdSpeed'] = 0
    
    # Always available parameters
    obd['fuelLevel'] = random.randint(10, 100)
    obd['power'] = round(random.uniform(11.8, 14.2), 2)
    obd['coolantTemp'] = obd.get('coolantTemp', random.randint(20, 105))
    
    # Optional parameters (sometimes available)
    if random.random() < 0.7:  # 70% chance
        obd['oilPressure'] = round(random.uniform(30, 60), 2)
    if random.random() < 0.6:  # 60% chance
        obd['intakeAirTemp'] = random.randint(20, 50)
    if random.random() < 0.5:  # 50% chance
        obd['fuelPressure'] = round(random.uniform(300, 500), 2)
    if random.random() < 0.4:  # 40% chance
        obd['timingAdvance'] = round(random.uniform(-5, 20), 2)
    
    return obd

def send(conn, lat, lon, altitude, course, speed, battery, alarm, ignition, accuracy, rpm, fuel, driverUniqueId, obd_data=None, dtc_codes=None, headers=None):
    params = (('id', id), ('timestamp', int(time.time())), ('lat', lat), ('lon', lon), ('altitude', altitude), ('bearing', course), ('speed', speed), ('batt', battery))
    if alarm:
        params = params + (('alarm', 'sos'),)
    if ignition:
        params = params + (('ignition', 'true'),)
    else:
        params = params + (('ignition', 'false'),)
    if accuracy:
        params = params + (('accuracy', accuracy),)
    if rpm:
        params = params + (('rpm', rpm),)
    if fuel:
        params = params + (('fuel', fuel),)
    if driverUniqueId:
        params = params + (('driverUniqueId', driverUniqueId),)
    
    # Add OBD parameters
    if obd_data:
        for key, value in obd_data.items():
            if value is not None:
                params = params + ((key, str(value)),)
    
    # Add DTC codes (occasionally)
    if dtc_codes:
        params = params + (('dtcs', ','.join(dtc_codes)),)
    
    # Build request with optional headers
    request_path = '?' + urllib.parse.urlencode(params)
    conn.request('GET', request_path, headers=headers or {})
    conn.getresponse().read()

def course(lat1, lon1, lat2, lon2):
    lat1 = lat1 * math.pi / 180
    lon1 = lon1 * math.pi / 180
    lat2 = lat2 * math.pi / 180
    lon2 = lon2 * math.pi / 180
    y = math.sin(lon2 - lon1) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(lon2 - lon1)
    return (math.atan2(y, x) % (2 * math.pi)) * 180 / math.pi

def main():
    global id, server, period, device_speed, driver_id, tenant_id
    
    parser = argparse.ArgumentParser(description='Generate test GPS positions with OBD data')
    parser.add_argument('--id', default=id, help='Device ID')
    parser.add_argument('--server', default=server, help='Traccar server address')
    parser.add_argument('--period', type=int, default=period, help='Update period in seconds')
    parser.add_argument('--speed', type=int, default=device_speed, help='Vehicle speed')
    parser.add_argument('--driver', default=driver_id, help='Driver ID')
    parser.add_argument('--tenant', type=int, help='Tenant ID for multi-tenant testing')
    parser.add_argument('--obd', action='store_true', help='Include OBD data')
    parser.add_argument('--dtc', action='store_true', help='Include DTC codes (occasionally)')
    parser.add_argument('--count', type=int, help='Number of positions to send (default: infinite)')
    parser.add_argument('--dry-run', action='store_true', help='Generate data without sending to server (for testing)')
    parser.add_argument('--output', help='Output file for dry-run mode (JSON format)')
    
    args = parser.parse_args()
    
    id = args.id
    server = args.server
    period = args.period
    device_speed = args.speed
    driver_id = args.driver
    tenant_id = args.tenant
    
    # Prepare headers for tenant context
    headers = {}
    if tenant_id:
        headers['X-Tenant-ID'] = str(tenant_id)
    
    index = 0
    positions = []
    
    print(f"Starting test generator: device={id}, server={server}, obd={args.obd}, dtc={args.dtc}")
    if tenant_id:
        print(f"Using tenant ID: {tenant_id}")
    if args.dry_run:
        print("DRY-RUN mode: Generating data without sending to server")
    
    conn = None
    if not args.dry_run:
        conn = httplib.HTTPConnection(server)
    
    try:
        while True:
            if args.count and index >= args.count:
                break
                
            (lat1, lon1) = points[index % len(points)]
            (lat2, lon2) = points[(index + 1) % len(points)]
            altitude = 50
            speed = device_speed if (index % len(points)) != 0 else 0
            alarm = (index % 10) == 0
            battery = random.randint(0, 100)
            ignition = (index / 10 % 2) != 0
            accuracy = 100 if (index % 10) == 0 else 0
            rpm = random.randint(500, 4000) if ignition else 0
            fuel = random.randint(0, 80)
            driverUniqueId = driver_id if (index % len(points)) == 0 else False
            
            # Generate OBD data if requested
            obd_data = None
            if args.obd:
                obd_data = generate_obd_data(speed, ignition)
                # Use OBD RPM if available
                if 'rpm' in obd_data:
                    rpm = obd_data['rpm']
            
            # Generate DTC codes occasionally if requested
            dtc_codes = None
            if args.dtc and random.random() < 0.05:  # 5% chance
                dtc_codes = random.sample(DTC_CODES, random.randint(1, 2))
            
            # Build position data
            position_data = {
                'id': id,
                'timestamp': int(time.time()),
                'lat': lat1,
                'lon': lon1,
                'altitude': altitude,
                'bearing': course(lat1, lon1, lat2, lon2),
                'speed': speed,
                'batt': battery,
                'ignition': 'true' if ignition else 'false',
                'rpm': rpm,
                'fuel': fuel
            }
            
            if alarm:
                position_data['alarm'] = 'sos'
            if accuracy:
                position_data['accuracy'] = accuracy
            if driverUniqueId:
                position_data['driverUniqueId'] = driverUniqueId
            if obd_data:
                position_data.update(obd_data)
            if dtc_codes:
                position_data['dtcs'] = ','.join(dtc_codes)
            if tenant_id:
                position_data['tenantId'] = tenant_id
            
            if args.dry_run:
                positions.append(position_data)
            else:
                send(conn, lat1, lon1, altitude, course(lat1, lon1, lat2, lon2), 
                     speed, battery, alarm, ignition, accuracy, rpm, fuel, 
                     driverUniqueId, obd_data, dtc_codes, headers)
            
            if index % 100 == 0 and not args.dry_run:
                print(f"Sent {index} positions...")
            
            if not args.dry_run:
                time.sleep(period)
            index += 1
            
    except KeyboardInterrupt:
        print(f"\nStopped after {index} positions")
    except Exception as e:
        if not args.dry_run:
            print(f"Error: {e}")
            sys.exit(1)
        else:
            print(f"Generated {index} positions")
    
    # Save to file if dry-run mode
    if args.dry_run:
        output_file = args.output or f'test-positions-{id}.json'
        with open(output_file, 'w') as f:
            json.dump(positions, f, indent=2)
        print(f"\n✓ Generated {len(positions)} positions")
        print(f"✓ Saved to {output_file}")
        
        # Print statistics
        obd_count = sum(1 for p in positions if 'rpm' in p or 'coolantTemp' in p)
        dtc_count = sum(1 for p in positions if 'dtcs' in p)
        print(f"\nStatistics:")
        print(f"  - Positions with OBD data: {obd_count}")
        print(f"  - Positions with DTC codes: {dtc_count}")
        print(f"  - Total positions: {len(positions)}")

if __name__ == '__main__':
    main()
