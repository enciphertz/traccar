/*
 * Copyright 2025 Encipher Company Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tz.co.esync.api.resource;

import tz.co.esync.api.BaseResource;
import tz.co.esync.model.Device;
import tz.co.esync.model.DtcCode;
import tz.co.esync.storage.StorageException;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Order;
import tz.co.esync.storage.query.Request;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.LinkedList;
import java.util.stream.Stream;

@Path("dtc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DtcCodeResource extends BaseResource {

    @GET
    public Stream<DtcCode> get(
            @QueryParam("deviceId") long deviceId,
            @QueryParam("status") String status,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {
        
        var conditions = new LinkedList<Condition>();
        
        if (deviceId > 0) {
            permissionsService.checkPermission(Device.class, getUserId(), deviceId);
            conditions.add(new Condition.Equals("deviceId", deviceId));
        }
        
        if (status != null && !status.isEmpty()) {
            conditions.add(new Condition.Equals("status", status));
        }
        
        if (from != null && to != null) {
            conditions.add(new Condition.Between("timestamp", from, to));
        }
        
        Request request = new Request(
            new Columns.All(),
            Condition.merge(conditions),
            new Order("timestamp", false, 0)
        );
        
        return storage.getObjectsStream(DtcCode.class, request);
    }

    @Path("{id}")
    @GET
    public Response getSingle(@PathParam("id") long id) throws StorageException {
        DtcCode dtc = storage.getObject(DtcCode.class, new Request(
            new Columns.All(), new Condition.Equals("id", id)));
        
        if (dtc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        permissionsService.checkPermission(Device.class, getUserId(), dtc.getDeviceId());
        return Response.ok(dtc).build();
    }

    @Path("device/{deviceId}")
    @GET
    public Stream<DtcCode> getByDevice(
            @PathParam("deviceId") long deviceId,
            @QueryParam("status") String status) throws StorageException {
        
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        
        var conditions = new LinkedList<Condition>();
        conditions.add(new Condition.Equals("deviceId", deviceId));
        
        if (status != null && !status.isEmpty()) {
            conditions.add(new Condition.Equals("status", status));
        }
        
        Request request = new Request(
            new Columns.All(),
            Condition.merge(conditions),
            new Order("timestamp", false, 0)
        );
        
        return storage.getObjectsStream(DtcCode.class, request);
    }

    @Path("device/{deviceId}/active")
    @GET
    public Stream<DtcCode> getActiveByDevice(@PathParam("deviceId") long deviceId) throws StorageException {
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        
        var conditions = new LinkedList<Condition>();
        conditions.add(new Condition.Equals("deviceId", deviceId));
        conditions.add(new Condition.Equals("status", "active"));
        
        Request request = new Request(
            new Columns.All(),
            Condition.merge(conditions),
            new Order("timestamp", false, 0)
        );
        
        return storage.getObjectsStream(DtcCode.class, request);
    }

    @Path("{id}/clear")
    @PUT
    public Response clearDtc(@PathParam("id") long id) throws StorageException {
        DtcCode dtc = storage.getObject(DtcCode.class, new Request(
            new Columns.All(), new Condition.Equals("id", id)));
        
        if (dtc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        permissionsService.checkPermission(Device.class, getUserId(), dtc.getDeviceId());
        
        dtc.setStatus("cleared");
        dtc.setClearedAt(new Date());
        
        storage.updateObject(dtc, new Request(new Columns.Include("status", "clearedAt")));
        
        return Response.ok(dtc).build();
    }
}
