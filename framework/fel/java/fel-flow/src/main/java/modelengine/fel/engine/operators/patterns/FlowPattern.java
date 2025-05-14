/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2024 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fel.engine.operators.patterns;

import modelengine.fel.core.pattern.Pattern;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fit.waterflow.domain.emitters.Emitter;
import modelengine.fit.waterflow.domain.emitters.EmitterListener;
import modelengine.fit.waterflow.domain.emitters.FlowEmitter;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 流程委托单元。
 *
 * @param <I> 表示输入数据类型。
 * @param <O> 表示输出数据类型。
 * @author 刘信宏
 * @since 2024-04-22
 */
public interface FlowPattern<I, O> extends Pattern<I, O>, Emitter<O, FlowSession> {
    public default FlowEmitter<O> bind(FlowContext<I> input) {
        FlowEmitter<O> cachedEmitter = new FlowEmitter.AutoCompleteEmitter<>();
        AtomicReference<EmitterListener<O, FlowSession>> emitterListenerRef = new AtomicReference<>();
        EmitterListener<O, FlowSession> emitterListener = (data, session) -> {
            // 结束时取消注册
            if (!input.getSession().getId().equals(session.getInnerState("parentSessionId"))) {
                System.out.println(String.format("[%s][FlowPattern.bind] ignore. data=%s, session=%s, windowId=%s, isComplete=%s, inputSessionId=%s",
                        Thread.currentThread().getId(),
                        data,
                        session.getId(),
                        session.getWindow().id(),
                        session.getWindow().isComplete(),
                        input.getSession().getId()
                ));
                return;
            }
            if (session.isCompleted()) {
                System.out.println(String.format("[%s][FlowPattern.bind] unregister. data=%s, session=%s, windowId=%s, isComplete=%s",
                        Thread.currentThread().getId(),
                        data,
                        session.getId(),
                        session.getWindow().id(),
                        session.getWindow().isComplete()));
                this.unregister(emitterListenerRef.get());
            }
            System.out.println(String.format("[%s][FlowPattern.bind] accept. data=%s, session=%s, windowId=%s, isComplete=%s",
                    Thread.currentThread().getId(),
                    data,
                    session.getId(),
                    session.getWindow().id(),
                    session.getWindow().isComplete()));
            cachedEmitter.emit(data, session);
        };
        emitterListenerRef.set(emitterListener);
        this.register(emitterListener);
        return cachedEmitter;
    }
}
