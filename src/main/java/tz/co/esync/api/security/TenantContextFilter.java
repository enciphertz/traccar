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
package tz.co.esync.api.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.esync.storage.TenantAwareStorage;

import java.io.IOException;

@Provider
public class TenantContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantContextFilter.class);
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    private final TenantAwareStorage tenantAwareStorage;

    @Inject
    public TenantContextFilter(TenantAwareStorage tenantAwareStorage) {
        this.tenantAwareStorage = tenantAwareStorage;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Extract tenant ID from header
        String tenantIdHeader = requestContext.getHeaderString(TENANT_ID_HEADER);
        if (tenantIdHeader != null && !tenantIdHeader.trim().isEmpty()) {
            try {
                Long tenantId = Long.parseLong(tenantIdHeader.trim());
                tenantAwareStorage.setTenantContext(tenantId);
                LOGGER.debug("Set tenant context: {}", tenantId);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid tenant ID format in header: {}", tenantIdHeader);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        // Clear tenant context after request processing
        tenantAwareStorage.clearTenantContext();
    }
}
