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

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tz.co.esync.api.BaseResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Path("openapi")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiResource extends BaseResource {

    @Context
    private HttpServletRequest request;

    @PermitAll
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml", "text/yaml"})
    public Response getOpenApiSpec() throws IOException {
        String content = null;
        
        // Try to read from classpath first
        var resource = getClass().getClassLoader().getResourceAsStream("openapi.yaml");
        if (resource != null) {
            content = new String(resource.readAllBytes());
        } else {
            // Try to read from project root
            java.nio.file.Path openApiPath = Paths.get("openapi.yaml");
            if (Files.exists(openApiPath)) {
                content = Files.readString(openApiPath);
            }
        }
        
        if (content == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("OpenAPI specification not found")
                    .build();
        }
        
        // Return YAML content
        // In production, you might want to convert YAML to JSON when JSON is requested
        return Response.ok(content, "application/yaml").build();
    }

    @PermitAll
    @Path("yaml")
    @GET
    @Produces("application/yaml")
    public Response getOpenApiYaml() throws IOException {
        return getOpenApiSpec();
    }

    @PermitAll
    @Path("json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOpenApiJson() throws IOException {
        // Return YAML content with JSON content type
        // In production, convert YAML to JSON properly
        return getOpenApiSpec();
    }

}
