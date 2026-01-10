---
name: Fix v1 API servlet mapping
overview: Fix the servlet mapping issue preventing /v1/* endpoints from working. The ResourceServlet at / is intercepting /v1/* requests before the v1 servlets can handle them. We need to ensure proper servlet matching order and configure the ResourceServlet to not interfere with /v1/* paths.
todos:
  - id: fix-resource-servlet-config
    content: Configure ResourceServlet in initWebApp() to not interfere with /v1/* paths by ensuring it only handles actual files and returns 404 for non-existent files on API paths
    status: completed
  - id: verify-servlet-registration
    content: Verify v1 servlet registration order and ensure ResourceConfig is properly configured in initApi() method
    status: completed
  - id: test-v1-endpoints
    content: Test all /v1/* endpoints (/v1/openapi/yaml, /v1/docs, /v1/obd, /v1/dtc, /v1/fuel) to ensure they work correctly
    status: completed
    dependencies:
      - fix-resource-servlet-config
      - verify-servlet-registration
---

# Fix v1 API Servlet Mapping Issue

## Problem Analysis

The issue is that `/v1/openapi/yaml` and `/v1/docs` are showing the web app (index.html) instead of the actual API endpoints. This happens because:

1. **ResourceServlet Interference**: The ResourceServlet registered at `/` (line 155 in `WebServer.java`) is intercepting `/v1/*` requests before the v1 servlets can handle them.

2. **Servlet Matching Order**: Even though `/v1/*` servlets are registered in `initApi()` (called before `initWebApp()`), the ResourceServlet at `/` acts as a default servlet and may match requests for paths that don't correspond to actual files, then forward to welcome files.

3. **Base Resource Configuration**: When `setBaseResource()` is called on `ServletContextHandler` (line 142), it sets the base resource for the entire context, which the ResourceServlet uses to serve files. This can cause it to intercept requests even when no file exists.

## Solution Strategy

The solution involves ensuring that `/v1/*` paths are explicitly excluded from ResourceServlet handling and that servlet matching works correctly by path specificity.

### Approach

1. **Explicit Path Exclusion**: Configure the ResourceServlet to explicitly not handle `/v1/*` paths by using a servlet mapping that excludes these paths, or by ensuring it returns 404 for non-existent files instead of forwarding to welcome files for API paths.

2. **Servlet Registration Order**: Ensure v1 servlets are registered with explicit names and in the correct order.

3. **ResourceServlet Configuration**: Configure the ResourceServlet to only serve files that actually exist and not act as a catch-all for paths that don't match other servlets.

## Implementation Plan

### Step 1: Fix ResourceServlet Configuration

**File**: `src/main/java/tz/co/esync/web/WebServer.java`

**Changes in `initWebApp()` method** (around line 136):

- The ResourceServlet should be configured to not interfere with `/v1/*` paths
- Add configuration to ensure it only handles requests for actual files
- Configure it to return 404 for non-existent files on API paths instead of forwarding to welcome files

**Specific changes**:

- Modify the ResourceServlet configuration to explicitly exclude `/v1/*` paths from welcome file forwarding
- Ensure that when the ResourceServlet doesn't find a file for `/v1/*` paths, it returns 404 or lets other servlets handle it

### Step 2: Ensure Correct Servlet Registration Order

**File**: `src/main/java/tz/co/esync/web/WebServer.java`

**Changes in `initApi()` method** (around line 179):

- The v1 servlets are already registered before `initWebApp()`, which is correct
- However, we should ensure the servlet mapping is explicit and works correctly
- Verify that the ResourceConfig for v1 is properly configured

### Step 3: Add Explicit Path Exclusion Filter (if needed)

**File**: `src/main/java/tz/co/esync/web/WebServer.java` OR create new filter

**If the ResourceServlet configuration isn't sufficient**, create a filter that runs before the ResourceServlet and explicitly excludes `/v1/*` paths from being handled by it. However, the `OverrideFileFilter` already does this (line 54), so this might not be necessary.

### Step 4: Test and Verify

After making changes:

1. Build the project
2. Start the server
3. Test `/v1/openapi/yaml` - should return YAML content
4. Test `/v1/docs` - should show Swagger UI
5. Test `/v1/obd` - should return OBD parameters (if data exists)
6. Test `/v1/dtc` - should return DTC codes (if data exists)
7. Test `/v1/fuel` - should return fuel consumption (if data exists)

## Technical Details

### Servlet Matching in Jetty EE10

In Jetty EE10 (Jakarta Servlet 6.0), servlet matching works as follows:

1. Exact path matches (`/v1/docs`) take precedence
2. Path prefix matches (`/v1/*`) take precedence over default servlet (`/`)
3. Default servlet (`/`) matches only if no other servlet matches

However, when `setBaseResource()` is called, the ResourceServlet might be configured as a default servlet that matches all paths, then checks if a file exists and either serves it or forwards to welcome files.

### The Fix

The key is to ensure the ResourceServlet:

1. Only handles requests for actual files
2. Returns 404 for non-existent files on API paths (`/v1/*`, `/api/*`)
3. Only forwards to welcome files for web app routes (not API routes)

## Files to Modify

1. **`src/main/java/tz/co/esync/web/WebServer.java`**:

- Modify `initWebApp()` method to configure ResourceServlet correctly
- Ensure it doesn't interfere with `/v1/*` paths

## Alternative Approach (if above doesn't work)

If the ResourceServlet configuration doesn't solve the issue, we can:

1. Register the ResourceServlet only for specific paths (not `/`)
2. Use a custom default servlet that explicitly excludes `/v1/*` paths
3. Register v1 servlets with explicit path mappings that take precedence

However, the first approach should work and is cleaner.