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
package tz.co.esync.storage;

import jakarta.inject.Inject;
import tz.co.esync.model.BaseModel;
import tz.co.esync.model.Device;
import tz.co.esync.model.Permission;
import tz.co.esync.model.Tenant;
import tz.co.esync.session.cache.CacheManager;
import tz.co.esync.storage.query.Columns;
import tz.co.esync.storage.query.Condition;
import tz.co.esync.storage.query.Request;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

public class TenantAwareStorage extends Storage {
    
    private final Storage delegate;
    private final CacheManager cacheManager;
    private final DataSource dataSource;
    private final ThreadLocal<Long> tenantContext = new ThreadLocal<>();
    
    @Inject
    public TenantAwareStorage(Storage delegate, CacheManager cacheManager, DataSource dataSource) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
        this.dataSource = dataSource;
    }
    
    public void setTenantContext(Long tenantId) {
        tenantContext.set(tenantId);
        if (tenantId != null) {
            setSchema(tenantId);
        }
    }
    
    public void clearTenantContext() {
        Long tenantId = tenantContext.get();
        tenantContext.remove();
        if (tenantId != null) {
            resetSchema();
        }
    }
    
    public Long getTenantContext() {
        return tenantContext.get();
    }
    
    private void setSchema(Long tenantId) {
        try (Connection connection = dataSource.getConnection()) {
            Tenant tenant = cacheManager.getObject(Tenant.class, tenantId);
            if (tenant != null && tenant.getSchemaName() != null) {
                connection.createStatement().execute(
                    "SET search_path TO " + tenant.getSchemaName() + ", public");
            }
        } catch (SQLException e) {
            throw new RuntimeException(new StorageException(e));
        }
    }
    
    private void resetSchema() {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("SET search_path TO public");
        } catch (SQLException e) {
            // Ignore - schema reset failure is not critical
        }
    }
    
    @Override
    public <T> List<T> getObjects(Class<T> clazz, Request request) throws StorageException {
        Request modifiedRequest = addTenantFilter(request, clazz);
        return delegate.getObjects(clazz, modifiedRequest);
    }

    @Override
    public <T> Stream<T> getObjectsStream(Class<T> clazz, Request request) throws StorageException {
        Request modifiedRequest = addTenantFilter(request, clazz);
        return delegate.getObjectsStream(clazz, modifiedRequest);
    }

    @Override
    public <T> long addObject(T entity, Request request) throws StorageException {
        Request modifiedRequest = addTenantFilter(request, entity.getClass());
        return delegate.addObject(entity, modifiedRequest);
    }

    @Override
    public <T> void updateObject(T entity, Request request) throws StorageException {
        Request modifiedRequest = addTenantFilter(request, entity.getClass());
        delegate.updateObject(entity, modifiedRequest);
    }

    @Override
    public void removeObject(Class<?> clazz, Request request) throws StorageException {
        Request modifiedRequest = addTenantFilter(request, clazz);
        delegate.removeObject(clazz, modifiedRequest);
    }

    @Override
    public List<Permission> getPermissions(
            Class<? extends BaseModel> ownerClass, long ownerId,
            Class<? extends BaseModel> propertyClass, long propertyId) throws StorageException {
        return delegate.getPermissions(ownerClass, ownerId, propertyClass, propertyId);
    }

    @Override
    public void addPermission(Permission permission) throws StorageException {
        delegate.addPermission(permission);
    }

    @Override
    public void removePermission(Permission permission) throws StorageException {
        delegate.removePermission(permission);
    }
    
    private <T> Request addTenantFilter(Request request, Class<T> clazz) {
        Long tenantId = tenantContext.get();
        if (tenantId == null) {
            return request;
        }
        
        // Add tenant filter for Device and other tenant-scoped models
        if (clazz == Device.class) {
            Condition tenantCondition = new Condition.Equals("tenantId", tenantId);
            Condition existingCondition = request != null ? request.getCondition() : null;
            
            Condition finalCondition;
            if (existingCondition != null) {
                finalCondition = new Condition.And(existingCondition, tenantCondition);
            } else {
                finalCondition = tenantCondition;
            }
            
            Columns columns = request != null ? request.getColumns() : null;
            if (columns == null) {
                columns = new Columns.All();
            }
            
            return new Request(columns, finalCondition, request != null ? request.getOrder() : null);
        }
        
        return request;
    }
}
