/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2024 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fel.engine.operators.patterns;

import modelengine.fel.core.pattern.Pattern;
import modelengine.fel.engine.flows.AiProcessFlow;
import modelengine.fel.engine.flows.ConverseLatch;
import modelengine.fel.engine.util.AiFlowSession;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fit.waterflow.domain.context.Window;
import modelengine.fit.waterflow.domain.emitters.EmitterListener;
import modelengine.fit.waterflow.domain.emitters.FlowEmitter;
import modelengine.fit.waterflow.domain.flow.Flow;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.util.LazyLoader;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 流程委托单元。
 *
 * @author 刘信宏
 * @since 2024-06-04
 */
public abstract class AbstractFlowPattern<I, O> implements FlowPattern<I, O> {
    private final LazyLoader<AiProcessFlow<I, O>> flowSupplier;

    protected AbstractFlowPattern() {
        this.flowSupplier = LazyLoader.of(this::buildFlow);
    }

    /**
     * 构造处理流程。
     *
     * @return 表示数据处理流程的 {@code <}{@link AiProcessFlow}{@code <}{@link I}{@code , }{@link O}{@code >}。
     */
    protected abstract AiProcessFlow<I, O> buildFlow();

    @Override
    public void register(EmitterListener<O, FlowSession> handler) {
        System.out.println("[FlowPattern.register] " + this.getFlow().start().getStreamId());
        if (handler != null) {
            this.getFlow().register(handler);
        }
    }

    @Override
    public void unregister(EmitterListener<O, FlowSession> listener) {
        if (listener != null) {
            this.getFlow().unregister(listener);
        }
    }

    @Override
    public void emit(O data, FlowSession session) {
        // FlowSession flowSession = new FlowSession(session);
        System.out.println(String.format("[%s][FlowPattern.emit] data=%s, session=%s, windowId=%s", Thread.currentThread().getId(), data, session.getId(), session.getWindow().id()));
        this.getFlow().emit(data, session);
    }

    @Override
    public O invoke(I data) {
        // 这里理论上应该是监听主流session对应window的完成事件，完成子流的window
        FlowSession mainSession = AiFlowSession.require();
        FlowSession flowSession = FlowSession.newRootSession(mainSession, true);
        flowSession.setInnerState("parentSessionId", mainSession.getId());
        System.out.println(String.format("[%s][FlowPattern.invoke] data=%s, session=%s, windowId=%s, newSessionId=%s, newWindowId=%s",
                Thread.currentThread().getId(), data, AiFlowSession.require().getId(), AiFlowSession.require().getWindow().id(),
                flowSession.getId(),
                flowSession.getWindow().id()
        ));
        this.getFlow().converse(flowSession).offer(data);
        return null;
    }

    /**
     * 获取同步委托单元。
     *
     * @return 表示同步委托单元的 {@link Pattern}{@code <}{@link I}{@code , }{@link O}{@code >}。
     * @throws IllegalStateException 当流程发生异常时。
     */
    public Pattern<I, O> sync() {
        return new SimplePattern<>(data -> {
            System.out.println("sync");
            FlowSession require = AiFlowSession.require();
            FlowSession session = new FlowSession(true);
            Window window = session.begin();
            session.copySessionState(require);
            ConverseLatch<O> conversation = this.getFlow().converse(session).offer(data);
            window.complete();
            System.out.println(String.format("sync offer end. latch=%s", conversation.getId()));
            O await = conversation.await();
            System.out.println("sync offer wait end");
            return await;
        });
    }

    /**
     * 获取被装饰的流程对象。
     *
     * @return 表示被装饰流程对象的 {@link Flow}{@code <}{@link I}{@code >}。
     */
    public Flow<I> origin() {
        return this.getFlow().origin();
    }

    @Override
    public FlowEmitter<O> getEmitter(FlowContext<I> input) {
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
            session.getWindow().onDone(getOnDoneHandlerId(session), () ->  {
                System.out.println(String.format("[%s][FlowPattern.emitter] unregister. data=%s, session=%s, windowId=%s, isComplete=%s",
                        Thread.currentThread().getId(),
                        data,
                        session.getId(),
                        session.getWindow().id(),
                        session.getWindow().isComplete()));
                this.unregister(emitterListenerRef.get());
            });
            System.out.println(String.format("[%s][FlowPattern.emitter] accept. data=%s, session=%s, windowId=%s, isComplete=%s",
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

    private AiProcessFlow<I, O> getFlow() {
        return Validation.notNull(this.flowSupplier.get(), "The flow cannot be null.");
    }

    private static String getOnDoneHandlerId(FlowSession session) {
        return "AbstractFlowPattern" + session.getWindow().id();
    }
}
