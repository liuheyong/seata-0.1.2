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

import java.sql.SQLException;
import java.sql.Statement;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLVisitorFactory;

public class ExecuteTemplate {

    public static <T, S extends Statement> T execute(StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        return execute(null, statementProxy, statementCallback, args);
    }

    public static <T, S extends Statement> T execute(SQLRecognizer sqlRecognizer,
                                                     StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        //判断当前是否是全局分布式事务
        if (!RootContext.inGlobalTransaction()) {
            // 就直接使用本地事务
            return statementCallback.execute(statementProxy.getTargetStatement(), args);
        }
        if (sqlRecognizer == null) {
            sqlRecognizer = SQLVisitorFactory.get(
                    statementProxy.getTargetSQL(),
                    statementProxy.getConnectionProxy().getDbType());
        }
        Executor<T> executor;
        if (sqlRecognizer == null) {
            executor = new PlainExecutor<T, S>(statementProxy, statementCallback);
        } else {
            switch (sqlRecognizer.getSQLType()) {
                case INSERT:
                    //UpdateExecutor、DeleteExecutor、InsertExecutor：
                    //三个DML增删改执行器实现，主要在sql执行的前后对sql语句进行了解析
                    executor = new InsertExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
                    break;
                case UPDATE:
                    executor = new UpdateExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
                    break;
                case DELETE:
                    executor = new DeleteExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
                    break;
                case SELECT_FOR_UPDATE:
                    executor = new SelectForUpdateExecutor(statementProxy, statementCallback, sqlRecognizer);
                    break;
                default:
                    //PlainExecutor 原生的JDBC接口实现，未做任何处理，提供给全局事务中的普通的select查询使用
                    executor = new PlainExecutor<T, S>(statementProxy, statementCallback);
                    break;
            }
        }
        T rs = null;
        try {

            rs = executor.execute(args);

        } catch (Throwable ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                // 把一切都变成SQLException
                new SQLException(ex);
            }
        }
        return rs;
    }
}
