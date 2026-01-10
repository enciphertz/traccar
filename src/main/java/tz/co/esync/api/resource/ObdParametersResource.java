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
import tz.co.esync.model.ObdParameters;
import tz.co.esync.storage.StorageException;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Order;
import tz.co.esync.storage.query.Request;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.LinkedList;
import java.util.stream.Stream;

@Path("obd")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ObdParametersResource extends BaseResource {

    @GET
    public Stream<ObdParameters> get(
            @QueryParam("deviceId") long deviceId,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("limit") int limit) throws StorageException {
        
        var conditions = new LinkedList<Condition>();
        
        if (deviceId > 0) {
            permissionsService.checkPermission(Device.class, getUserId(), deviceId);
            conditions.add(new Condition.Equals("deviceId", deviceId));
        }
        
        if (from != null && to != null) {
            conditions.add(new Condition.Between("timestamp", from, to));
        }
        
        Request request = new Request(
            new Columns.All(),
            Condition.merge(conditions),
            new Order("timestamp", false, limit > 0 ? limit : 0)
        );
        
        return storage.getObjectsStream(ObdParameters.class, request);
    }

    @Path("{id}")
    @GET
    public Response getSingle(@PathParam("id") long id) throws StorageException {
        ObdParameters obd = storage.getObject(ObdParameters.class, new Request(
            new Columns.All(), new Condition.Equals("id", id)));
        
        if (obd == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        permissionsService.checkPermission(Device.class, getUserId(), obd.getDeviceId());
        return Response.ok(obd).build();
    }

    @Path("device/{deviceId}")
    @GET
    public Stream<ObdParameters> getByDevice(
            @PathParam("deviceId") long deviceId,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to,
            @QueryParam("limit") int limit) throws StorageException {
        
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        
        var conditions = new LinkedList<Condition>();
        conditions.add(new Condition.Equals("deviceId", deviceId));
        
        if (from != null && to != null) {
            conditions.add(new Condition.Between("timestamp", from, to));
        }
        
        Request request = new Request(
            new Columns.All(),
            Condition.merge(conditions),
            new Order("timestamp", false, limit > 0 ? limit : 0)
        );
        
        return storage.getObjectsStream(ObdParameters.class, request);
    }
}
