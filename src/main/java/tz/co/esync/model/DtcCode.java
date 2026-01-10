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
package tz.co.esync.model;

import tz.co.esync.storage.StorageName;
import java.util.Date;

@StorageName("tc_dtc_codes")
public class DtcCode extends BaseModel {
    
    private long deviceId;

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private long positionId;

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    private Date timestamp;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    private String code;           // e.g., "P0301"

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private String codeType;        // P, B, C, U

    public String getCodeType() {
        return codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String status;          // active, pending, permanent, cleared

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String freezeFrameData; // JSON

    public String getFreezeFrameData() {
        return freezeFrameData;
    }

    public void setFreezeFrameData(String freezeFrameData) {
        this.freezeFrameData = freezeFrameData;
    }
    
    private Date firstOccurred;

    public Date getFirstOccurred() {
        return firstOccurred;
    }

    public void setFirstOccurred(Date firstOccurred) {
        this.firstOccurred = firstOccurred;
    }

    private Date lastOccurred;

    public Date getLastOccurred() {
        return lastOccurred;
    }

    public void setLastOccurred(Date lastOccurred) {
        this.lastOccurred = lastOccurred;
    }

    private Date clearedAt;

    public Date getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(Date clearedAt) {
        this.clearedAt = clearedAt;
    }
}
