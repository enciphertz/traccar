#!/usr/bin/env python3

"""
Dataset Converter for Traccar
Converts various open source datasets (VED, OpenLKA, generic CSV/JSON) to Traccar position format
"""

import sys
import json
import csv
import argparse
import urllib.parse
import http.client as httplib
from datetime import datetime
from typing import Dict, List, Any, Optional

class DatasetConverter:
    """Convert various dataset formats to Traccar positions"""
    
    def __init__(self, server='localhost:8082', device_id='123456789012345'):
        self.server = server
        self.device_id = device_id
        self.conn = None
    
    def connect(self):
        """Establish connection to Traccar server"""
        self.conn = httplib.HTTPConnection(self.server)
    
    def close(self):
        """Close connection"""
        if self.conn:
            self.conn.close()
    
    def send_position(self, position_data: Dict[str, Any], headers: Optional[Dict] = None):
        """Send position data to Traccar server"""
        if not self.conn:
            self.connect()
        
        params = []
        for key, value in position_data.items():
            if value is not None:
                params.append((key, str(value)))
        
        request_path = '?' + urllib.parse.urlencode(params)
        self.conn.request('GET', request_path, headers=headers or {})
        response = self.conn.getresponse()
        response.read()
        return response.status == 200
    
    def convert_ved(self, ved_file: str, output_format: str = 'traccar', 
                    device_id: Optional[str] = None, batch_size: int = 100):
        """
        Convert Vehicle Energy Dataset (VED) format to Traccar positions
        
        VED format typically includes:
        - GPS coordinates (lat, lon)
        - Timestamp
        - Fuel/energy consumption
        - Speed
        - Vehicle ID
        """
        if device_id:
            self.device_id = device_id
        
        positions = []
        
        try:
            with open(ved_file, 'r') as f:
                # Try to detect format (CSV or JSON)
                first_line = f.readline().strip()
                f.seek(0)
                
                if first_line.startswith('{') or first_line.startswith('['):
                    # JSON format
                    data = json.load(f)
                    if isinstance(data, list):
                        records = data
                    else:
                        records = [data]
                else:
                    # CSV format
                    reader = csv.DictReader(f)
                    records = list(reader)
                
                print(f"Processing {len(records)} records from VED dataset...")
                
                for i, record in enumerate(records):
                    try:
                        # Map VED columns to Traccar format
                        # Common VED column names: lat, lon, latitude, longitude, timestamp, time, fuel, speed
                        position = {
                            'id': self.device_id,
                            'timestamp': self._parse_timestamp(record.get('timestamp') or record.get('time')),
                            'lat': float(record.get('lat') or record.get('latitude', 0)),
                            'lon': float(record.get('lon') or record.get('longitude', 0)),
                        }
                        
                        # Add speed if available
                        if 'speed' in record:
                            position['speed'] = float(record['speed'])
                        
                        # Add fuel data
                        if 'fuel' in record or 'fuelLevel' in record:
                            fuel_value = float(record.get('fuel') or record.get('fuelLevel', 0))
                            position['fuelLevel'] = int(fuel_value) if fuel_value <= 100 else int(fuel_value / 10)
                        
                        if 'fuelConsumption' in record or 'fuel_rate' in record:
                            position['fuelConsumption'] = float(record.get('fuelConsumption') or record.get('fuel_rate', 0))
                        
                        # Add energy data if available
                        if 'energy' in record:
                            # Convert energy to fuel consumption estimate
                            energy = float(record['energy'])
                            position['fuelConsumption'] = energy * 0.1  # Rough conversion
                        
                        # Add OBD-like data if available
                        if 'rpm' in record:
                            position['rpm'] = int(float(record['rpm']))
                        
                        if 'coolantTemp' in record or 'coolant_temp' in record:
                            position['coolantTemp'] = int(float(record.get('coolantTemp') or record.get('coolant_temp', 0)))
                        
                        positions.append(position)
                        
                        # Send in batches if output is traccar
                        if output_format == 'traccar' and len(positions) >= batch_size:
                            self._send_batch(positions)
                            positions = []
                            print(f"Sent batch, processed {i+1}/{len(records)} records...")
                    
                    except Exception as e:
                        print(f"Error processing record {i}: {e}", file=sys.stderr)
                        continue
                
                # Send remaining positions
                if positions and output_format == 'traccar':
                    self._send_batch(positions)
                
                print(f"Successfully converted {len(records)} records")
                return positions if output_format == 'json' else None
                
        except FileNotFoundError:
            print(f"Error: File {ved_file} not found", file=sys.stderr)
            sys.exit(1)
        except Exception as e:
            print(f"Error reading file: {e}", file=sys.stderr)
            sys.exit(1)
    
    def convert_openlka(self, can_bus_file: str, output_format: str = 'traccar',
                        device_id: Optional[str] = None):
        """
        Convert OpenLKA CAN bus format to Traccar positions
        
        OpenLKA format includes CAN bus streams with OBD parameters
        """
        if device_id:
            self.device_id = device_id
        
        print("OpenLKA converter - parsing CAN bus data...")
        # This is a placeholder - actual implementation would parse CAN bus format
        # which typically requires specialized libraries
        
        try:
            with open(can_bus_file, 'r') as f:
                data = json.load(f)
                
            positions = []
            for record in data.get('can_frames', []):
                position = {
                    'id': self.device_id,
                    'timestamp': self._parse_timestamp(record.get('timestamp')),
                    'lat': float(record.get('lat', 0)),
                    'lon': float(record.get('lon', 0)),
                }
                
                # Extract OBD parameters from CAN bus data
                can_data = record.get('data', {})
                if 'rpm' in can_data:
                    position['rpm'] = int(can_data['rpm'])
                if 'speed' in can_data:
                    position['speed'] = float(can_data['speed'])
                if 'throttle' in can_data:
                    position['throttle'] = int(can_data['throttle'])
                if 'coolant_temp' in can_data:
                    position['coolantTemp'] = int(can_data['coolant_temp'])
                
                positions.append(position)
            
            if output_format == 'traccar':
                self._send_batch(positions)
            else:
                return positions
                
        except Exception as e:
            print(f"Error processing OpenLKA file: {e}", file=sys.stderr)
            sys.exit(1)
    
    def convert_generic(self, csv_file: str, column_mapping: Dict[str, str],
                       output_format: str = 'traccar', device_id: Optional[str] = None):
        """
        Convert generic CSV file with custom column mapping
        
        column_mapping: Dict mapping CSV columns to Traccar position attributes
        Example: {'latitude': 'lat', 'longitude': 'lon', 'timestamp': 'timestamp'}
        """
        if device_id:
            self.device_id = device_id
        
        positions = []
        
        try:
            with open(csv_file, 'r') as f:
                reader = csv.DictReader(f)
                
                for row in reader:
                    position = {'id': self.device_id}
                    
                    # Map columns according to mapping
                    for csv_col, traccar_attr in column_mapping.items():
                        if csv_col in row and row[csv_col]:
                            value = row[csv_col]
                            
                            # Type conversion
                            if traccar_attr in ['lat', 'lon', 'speed', 'fuelLevel', 'fuelConsumption']:
                                try:
                                    value = float(value)
                                    if traccar_attr in ['lat', 'lon']:
                                        position[traccar_attr] = value
                                    elif traccar_attr == 'fuelLevel':
                                        position[traccar_attr] = int(value)
                                    else:
                                        position[traccar_attr] = value
                                except ValueError:
                                    pass
                            elif traccar_attr == 'timestamp':
                                position[traccar_attr] = self._parse_timestamp(value)
                            elif traccar_attr == 'rpm':
                                position[traccar_attr] = int(float(value))
                            else:
                                position[traccar_attr] = value
                    
                    positions.append(position)
            
            if output_format == 'traccar':
                self._send_batch(positions)
            else:
                return positions
                
        except Exception as e:
            print(f"Error processing CSV file: {e}", file=sys.stderr)
            sys.exit(1)
    
    def _parse_timestamp(self, timestamp_str: Any) -> int:
        """Parse various timestamp formats to Unix timestamp"""
        if timestamp_str is None:
            return int(datetime.now().timestamp())
        
        if isinstance(timestamp_str, (int, float)):
            return int(timestamp_str)
        
        # Try various date formats
        formats = [
            '%Y-%m-%d %H:%M:%S',
            '%Y-%m-%dT%H:%M:%S',
            '%Y-%m-%dT%H:%M:%S.%f',
            '%Y/%m/%d %H:%M:%S',
            '%d/%m/%Y %H:%M:%S',
        ]
        
        for fmt in formats:
            try:
                dt = datetime.strptime(str(timestamp_str), fmt)
                return int(dt.timestamp())
            except ValueError:
                continue
        
        # If all else fails, return current timestamp
        return int(datetime.now().timestamp())
    
    def _send_batch(self, positions: List[Dict]):
        """Send batch of positions to Traccar"""
        for position in positions:
            try:
                self.send_position(position)
            except Exception as e:
                print(f"Error sending position: {e}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description='Convert datasets to Traccar format')
    parser.add_argument('input_file', help='Input dataset file')
    parser.add_argument('--format', choices=['ved', 'openlka', 'generic'], required=True,
                       help='Dataset format')
    parser.add_argument('--output', choices=['traccar', 'json'], default='traccar',
                       help='Output format')
    parser.add_argument('--server', default='localhost:8082', help='Traccar server')
    parser.add_argument('--device-id', default='123456789012345', help='Device ID')
    parser.add_argument('--mapping', help='Column mapping JSON file (for generic format)')
    parser.add_argument('--batch-size', type=int, default=100, help='Batch size for sending')
    
    args = parser.parse_args()
    
    converter = DatasetConverter(server=args.server, device_id=args.device_id)
    
    try:
        if args.format == 'ved':
            converter.convert_ved(args.input_file, args.output, args.device_id, args.batch_size)
        elif args.format == 'openlka':
            converter.convert_openlka(args.input_file, args.output, args.device_id)
        elif args.format == 'generic':
            if not args.mapping:
                print("Error: --mapping required for generic format", file=sys.stderr)
                sys.exit(1)
            with open(args.mapping, 'r') as f:
                column_mapping = json.load(f)
            converter.convert_generic(args.input_file, column_mapping, args.output, args.device_id)
    finally:
        converter.close()


if __name__ == '__main__':
    main()
