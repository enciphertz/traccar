---
name: Fix v1 API Response Writing Error
overview: Fix the "Insufficient content written" error affecting all v1 API endpoints by correcting the CompressionHandler order and ensuring proper resource registration.
todos: []
---

# Fix v1 API Response Writing Error

## Problem Analysis

All v1 API endpoints are returning the error:

```json
{
  "message": "Insufficient content written 0 < 3811",
  "url": "http://127.0.0.1:8082/v1/server",
  "status": "500"
}
```

This error occurs when Jetty's `CompressionHandler` expects to write a certain number of bytes (3811 in this case) but actually writes 0 bytes. This is a classic symptom of:

1. **Incorrect handler order**: `CompressionHandler` is added AFTER the servlet handler in `WebServer.java`, which means it tries to compress responses that have already been committed or written.

2. **Potential missing resource registration**: `ServerResource` is not registered in the v1 ResourceConfig, so `/v1/server` requests may not be handled correctly.

## Root Cause

In [`WebServer.java`](src/main/java/tz/co/esync/web/WebServer.java) at lines 109-113:

```java
Handler.Sequence handlers = new Handler.Sequence();
initClientProxy(servletHandler);
handlers.addHandler(servletHandler);
handlers.addHandler(new CompressionHandler());  // WRONG ORDER
server.setHandler(handlers);
```

The `CompressionHandler` must be added BEFORE the servlet handler. When it comes after, it attempts to compress already-written responses, leading to Content-Length header mismatches and the "Insufficient content written" error.

## Solution

### Fix 1: Correct CompressionHandler Order

**File**: [`src/main/java/tz/co/esync/web/WebServer.java`](src/main/java/tz/co/esync/web/WebServer.java)

**Change**: Move `CompressionHandler` BEFORE `servletHandler` in the handler sequence (around line 109-113).

```java
Handler.Sequence handlers = new Handler.Sequence();
initClientProxy(servletHandler);
handlers.addHandler(new CompressionHandler());  // Compression FIRST
handlers.addHandler(servletHandler);             // Then servlets
server.setHandler(handlers);
```

This ensures compression happens on responses before they're committed to the client.

### Fix 2: Verify v1 Resource Registration (Optional Enhancement)

**File**: [`src/main/java/tz/co/esync/web/WebServer.java`](src/main/java/tz/co/esync/web/WebServer.java)

**Issue**: The v1 ResourceConfig (lines 229-246) doesn't include `ServerResource`, so `/v1/server` endpoint may not be available under the v1 API.

**Action**: If `/v1/server` is intended to work, add `ServerResource` to the v1 config:

```java
v1ResourceConfig.registerClasses(
    ObdParametersResource.class,
    DtcCodeResource.class,
    FuelConsumptionResource.class,
    OpenApiResource.class,
    ServerResource.class);  // Add if needed
```

However, this is optional - the main fix is the CompressionHandler order, which will fix all endpoints regardless of resource registration.

## Implementation Steps

1. Fix CompressionHandler order in `WebServer.java`
2. Test the `/v1/server` endpoint to verify it works
3. Optionally add `ServerResource` to v1 config if needed
4. Verify all v1 endpoints work correctly

## Expected Outcome

After fixing the handler order:

- All v1 API endpoints should return proper JSON responses
- No more "Insufficient content written" errors
- Response compression will work correctly
- Content-Length headers will match actual response body size