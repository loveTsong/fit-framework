/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2024 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.domain.utils;

import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fitframework.log.Logger;

/**
 * 流程调试工具
 *
 * @author xiafei
 * @since 1.0
 */
public class FlowDebug {
    private static final Logger LOG = Logger.get(FlowDebug.class);

    private static boolean isOpen = true;

    /**
     * 打印日志信息，包含线程ID和消息内容
     *
     * @param msg 需要打印的消息内容
     */
    public static void log(String msg) {
        if (!isOpen) {
            return;
        }
//        LOG.debug("Thread:{0}. {1}", Thread.currentThread().getId(), msg);
        System.out.println(String.format("Thread:%s. %s", Thread.currentThread().getId(), msg));
    }

    /**
     * 打印日志信息，包含线程ID、消息内容和流会话信息
     *
     * @param session 流会话信息
     * @param msg 需要打印的消息内容
     */
    public static void log(FlowSession session, String msg) {
        if (!isOpen) {
            return;
        }
//        LOG.debug("Thread:{0}. tokenCount:{1}, getTosSize={2}, isComplete={3}. msg={4}", Thread.currentThread().getId(),
//                session.getWindow().tokenCount(), session.getWindow().getTosSize(), session.getWindow().isComplete(),
//                msg);
        System.out.println(String.format("Thread:%s %s, sessionId=%s, windowId=%s, windowToken=%s, TokenStatus=%s, isComplete=%s",
                Thread.currentThread().getId(),
                msg,
                session.getId() + "|" + session.hashCode(),
                session.getWindow().id(),
                session.getWindow().tokenCount(),
                session.getWindow().debugTokens(),
                session.getWindow().isComplete()
                ));
    }
}
