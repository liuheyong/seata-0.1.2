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

package com.alibaba.fescar.rm.datasource.exec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;

import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.rm.datasource.ParametersHolder;
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLSelectRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

public class SelectForUpdateExecutor<S extends Statement> extends BaseTransactionalExecutor<ResultSet, S> {

    public SelectForUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<ResultSet, S> statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    /**
    * @Date:  2020-12-01
    * @Param:
    * @return:
    * @Description:  fescar的AT模式在本地事务之上默认支持读未提交的隔离级别，但是通过SelectForUpdateExecutor执行器，可以支持读已提交的隔离级别
    */
    @Override
    public Object doExecute(Object... args) throws Throwable {
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) sqlRecognizer;

        Connection conn = statementProxy.getConnection();
        ResultSet rs;
        Savepoint sp = null;
        LockRetryController lockRetryController = new LockRetryController();
        boolean originalAutoCommit = conn.getAutoCommit();

        StringBuffer selectSQLAppender = new StringBuffer("SELECT ");
        selectSQLAppender.append(getColumnNameInSQL(getTableMeta().getPkName()));
        selectSQLAppender.append(" FROM " + getFromTableInSQL());
        String whereCondition = null;
        ArrayList<Object> paramAppender = new ArrayList<>();
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = recognizer.getWhereCondition((ParametersHolder) statementProxy, paramAppender);
        } else {
            whereCondition = recognizer.getWhereCondition();
        }
        if (!StringUtils.isEmpty(whereCondition)) {
            selectSQLAppender.append(" WHERE " + whereCondition);
        }
        selectSQLAppender.append(" FOR UPDATE");
        String selectPKSQL = selectSQLAppender.toString();

        try {
            if (originalAutoCommit) {
                conn.setAutoCommit(false);
            }
            sp = conn.setSavepoint();
            rs = statementCallback.execute(statementProxy.getTargetStatement(), args);

            while (true) {
                // Try to get global lock of those rows selected
                Statement stPK = null;
                PreparedStatement pstPK = null;
                ResultSet rsPK = null;
                try {
                    if (paramAppender.isEmpty()) {
                        stPK = statementProxy.getConnection().createStatement();
                        rsPK = stPK.executeQuery(selectPKSQL);
                    } else {
                        pstPK = statementProxy.getConnection().prepareStatement(selectPKSQL);
                        for (int i = 0; i < paramAppender.size(); i++) {
                            pstPK.setObject(i + 1, paramAppender.get(i));
                        }
                        rsPK = pstPK.executeQuery();
                    }
                    //通过selectPKRows表操作记录拿到lockKeys，然后到TC控制器端
                    // 查询是否被全局锁定了，如果被锁定了，就重新尝试，直到锁释放返回查询结果。
                    TableRecords selectPKRows = TableRecords.buildRecords(getTableMeta(), rsPK);
                    statementProxy.getConnectionProxy().checkLock(selectPKRows);
                    break;
                } catch (LockConflictException lce) {
                    conn.rollback(sp);
                    lockRetryController.sleep(lce);
                } finally {
                    if (rsPK != null) {
                        rsPK.close();
                    }
                    if (stPK != null) {
                        stPK.close();
                    }
                    if (pstPK != null) {
                        pstPK.close();
                    }
                }
            }

        } finally {
            if (sp != null) {
                conn.releaseSavepoint(sp);
            }
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return rs;
    }
}
