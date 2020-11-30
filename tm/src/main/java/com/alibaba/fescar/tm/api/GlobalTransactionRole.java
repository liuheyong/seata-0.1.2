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

/**
 * Role of current thread involve in a global transaction.
 */
public enum GlobalTransactionRole {

    /**
     * The Launcher.
     */
    // 开始当前全局事务的角色
    Launcher,

    /**
     * The Participant.
     */
    // 加入到当前全局事务当中的角色.
    Participant
}
