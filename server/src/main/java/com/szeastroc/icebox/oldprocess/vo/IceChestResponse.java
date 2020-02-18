package com.szeastroc.icebox.oldprocess.vo;

import com.szeastroc.icebox.oldprocess.entity.ClientInfo;
import com.szeastroc.icebox.oldprocess.entity.IceChestInfo;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Tulane
 * 2019/5/22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IceChestResponse {

    private IceChestInfo iceChestInfo;
    private ClientInfo clientInfo;
    private IceEventRecord iceEventRecord;
    private ClientInfo currentClientInfo;
    private IceChestPutRecord iceChestPutRecord;

    public IceChestResponse(IceChestInfo iceChestInfo, ClientInfo clientInfo, ClientInfo currentClientInfo) {
        this.iceChestInfo = iceChestInfo;
        this.clientInfo = clientInfo;
        this.currentClientInfo = currentClientInfo;
    }

    public IceChestResponse(IceChestInfo iceChestInfo, ClientInfo clientInfo) {
        this.iceChestInfo = iceChestInfo;
        this.clientInfo = clientInfo;
    }

    public IceChestResponse(IceChestInfo iceChestInfo, ClientInfo clientInfo, IceEventRecord iceEventRecord, IceChestPutRecord iceChestPutRecord) {
        this.iceChestInfo = iceChestInfo;
        this.clientInfo = clientInfo;
        this.iceEventRecord = iceEventRecord;
        this.iceChestPutRecord = iceChestPutRecord;
    }
}
