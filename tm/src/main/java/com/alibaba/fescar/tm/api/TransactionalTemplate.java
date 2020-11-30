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

package com.alibaba.fescar.tm.api;

import com.alibaba.fescar.core.exception.TransactionException;

/**
 * 全局事务的开启，提交、回滚都被封装在TransactionalTemplate中
 */
public class TransactionalTemplate {

    /**
     * Execute object.
     *
     * @param business the business
     * @return the object
     * @throws TransactionalExecutor.ExecutionException the execution exception
     */
    public Object execute(TransactionalExecutor business) throws TransactionalExecutor.ExecutionException {

        // 1. 获取或者新建一个事务
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        // 2. 开始事务
        try {
            tx.begin(business.timeout(), business.name());
        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.BeginFailure);
        }
        Object rs;
        try {

            // 执行@GlobalTransactional标注了的业务逻辑
            rs = business.execute();

        } catch (Throwable ex) {
            // 3. 任何业务逻辑执行异常，开始回滚
            try {
                tx.rollback();
                // 3.1 回滚成功
                throw new TransactionalExecutor.ExecutionException(tx, TransactionalExecutor.Code.RollbackDone, ex);
            } catch (TransactionException txe) {
                // 3.2 回滚失败
                throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.RollbackFailure, ex);
            }
        }
        // 4. 一切都很好，提交。
        try {
            tx.commit();
        } catch (TransactionException txe) {
            // 4.1 提交失败
            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.CommitFailure);
        }
        return rs;
    }

}
