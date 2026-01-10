# E-Sync Telemetries Engine

## Overview

E-Sync Telemetries Engine is a comprehensive telemetry tracking system built for modern fleet and vehicle management. This repository contains the Java-based back-end service that powers E-Sync's telemetry platform. It supports more than 200 GPS protocols and more than 2000 models of GPS tracking devices. The engine can be used with any major SQL database system and provides a powerful REST API for integration.

## Features

Some of the available features include:

- Real-time GPS tracking
- OBD (On-Board Diagnostics) data processing
- DTC (Diagnostic Trouble Code) management
- Fuel consumption and economy tracking
- Driver behaviour monitoring
- Detailed and summary reports
- Geofencing functionality
- Alarms and notifications
- Account and device management
- Multi-tenancy support
- Email and SMS support

## Build

Please refer to the build documentation for instructions on building from source.

### Development Mode

For development, you can run the server in debug mode to enable hot swap and avoid multiple compilations:

#### Running in Debug Mode

```bash
# Start the server in debug mode (enables hot swap on port 5005)
./gradlew debug
```

This will:
- Start the server with JPDA debug agent enabled on port **5005**
- Enable hot swap for code changes (method body changes can be reloaded without restart)
- Use the `debug.xml` configuration file

#### Continuous Build (Auto-recompile on changes)

To automatically recompile when files change, use continuous build:

```bash
# In a separate terminal, run continuous build
./gradlew --continuous compileJava
```

This will watch for file changes and automatically recompile, allowing hot swap to pick up the changes.

#### Using with IDE (IntelliJ IDEA / Eclipse / VS Code)

1. **Run the debug task**: Execute `./gradlew debug` in a terminal
2. **In your IDE**: 
   - Enable "Build automatically" or "Compile on save"
   - Most IDEs will detect the debug port (5005) automatically
   - You can attach a debugger to `localhost:5005`
   - Make code changes and save - hot swap will reload the classes automatically

**Note**: 
- Hot swap works for method body changes and some field changes
- Structural changes (new classes, method signatures, etc.) require a server restart
- The server runs with the `debug.xml` configuration file by default

## License

    Apache License, Version 2.0

    Copyright 2025 Encipher Company Limited

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
