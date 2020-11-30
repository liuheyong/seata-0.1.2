/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.tm;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.TransactionManager;
import com.alibaba.fescar.core.protocol.transaction.*;
import com.alibaba.fescar.core.rpc.netty.TmRpcClient;

import java.util.concurrent.TimeoutException;

/**
 * The type Default transaction manager.
 */
public class DefaultTransactionManager implements TransactionManager {

    private static class SingletonHolder {
        private static final TransactionManager INSTANCE = new DefaultTransactionManager();
    }

    /**
     * Get transaction manager.
     *
     * @return the transaction manager
     */
    public static TransactionManager get() {
        return SingletonHolder.INSTANCE;
    }

    private DefaultTransactionManager() {

    }

    /**
    * @Date:  2020-11-30
    * @Param:  [applicationId, transactionServiceGroup, name, timeout]
    * @return:  java.lang.String
    * @Description:  开启全局事务（GlobalBeginRequest）
    */
    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName(name);
        request.setTimeout(timeout);
        GlobalBeginResponse response = (GlobalBeginResponse) syncCall(request);
        return response.getXid();
    }

    /**
    * @Date:  2020-11-30
    * @Param:  [xid]
    * @return:  com.alibaba.fescar.core.model.GlobalStatus
    * @Description:  全局事务提交（GlobalCommitRequest）
    */
    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalCommitRequest globalCommit = new GlobalCommitRequest();
        globalCommit.setTransactionId(txId);
        GlobalCommitResponse response = (GlobalCommitResponse) syncCall(globalCommit);
        return response.getGlobalStatus();
    }

    /**
     * @Date:  2020-11-30
     * @Param:  [xid]
     * @return:  com.alibaba.fescar.core.model.GlobalStatus
     * @Description:  全局事务回滚（GlobalRollbackRequest）
     */
    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setTransactionId(txId);
        GlobalRollbackResponse response = (GlobalRollbackResponse) syncCall(globalRollback);
        return response.getGlobalStatus();
    }

    /**
     * @Date:  2020-11-30
     * @Param:  [xid]
     * @return:  com.alibaba.fescar.core.model.GlobalStatus
     * @Description:  全局事务状态（GlobalStatusRequest）
     */
    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalStatusRequest queryGlobalStatus = new GlobalStatusRequest();
        queryGlobalStatus.setTransactionId(txId);
        GlobalStatusResponse response = (GlobalStatusResponse) syncCall(queryGlobalStatus);
        return response.getGlobalStatus();
    }

    // 同步调用
    private AbstractTransactionResponse syncCall(AbstractTransactionRequest request) throws TransactionException {
        try {
            return (AbstractTransactionResponse) TmRpcClient.getInstance().sendMsgWithResponse(request);
        } catch (TimeoutException toe) {
            throw new TransactionException(TransactionExceptionCode.IO, toe);
        }
    }
}
