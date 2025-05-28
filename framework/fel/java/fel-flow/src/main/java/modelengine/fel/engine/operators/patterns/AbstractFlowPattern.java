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
import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fit.waterflow.domain.context.Window;
import modelengine.fit.waterflow.domain.emitters.EmitterListener;
import modelengine.fit.waterflow.domain.emitters.FlowEmitter;
import modelengine.fit.waterflow.domain.flow.Flow;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.util.LazyLoader;
import modelengine.fitframework.util.ObjectUtils;

/**
 * 流程委托单元。
 *
 * @author 刘信宏
 * @since 2024-06-04
 */
public abstract class AbstractFlowPattern<I, O> implements FlowPattern<I, O> {
    protected static final String RESULT_ACTION_KEY = "resultAction";
    protected static final String PARENT_SESSION_ID_KEY = "parentSessionId";

    private final LazyLoader<AiProcessFlow<I, O>> flowSupplier;
    private final EmitterListener<O, FlowSession> dataDispatcher = (data, session) -> {
        Object rawResultAction = session.getInnerState(RESULT_ACTION_KEY);
        if (rawResultAction == null) {
            return;
        }
        ResultAction<O> resultAction = ObjectUtils.cast(rawResultAction);
        System.out.println(String.format("[%s][AbstractFlowPattern] sub flow data accept. data=%s, session=%s, windowId=%s, isComplete=%s",
                Thread.currentThread().getId(),
                data,
                session.getId(),
                session.getWindow().id(),
                session.getWindow().isComplete()));
        resultAction.process(data, session);
    };

    protected AbstractFlowPattern() {
        this.flowSupplier = LazyLoader.of(() -> {
            AiProcessFlow<I, O> flow = buildFlow();
            flow.register(this.dataDispatcher);
            return flow;
        });
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
        System.out.println(String.format("[%s][AbstractFlowPattern.emit] data=%s, session=%s, windowId=%s", Thread.currentThread().getId(), data, session.getId(), session.getWindow().id()));
        this.getFlow().emit(data, session);
    }

    @Override
    public FlowEmitter<O> invoke(I data) {
        FlowEmitter<O> emitter = new FlowEmitter.AutoCompleteEmitter<>();
        FlowSession flowSession = buildFlowSession(emitter);
        System.out.println(String.format("[%s][AbstractFlowPattern.invoke] data=%s, session=%s, windowId=%s, newSessionId=%s, newWindowId=%s",
                Thread.currentThread().getId(), data, AiFlowSession.require().getId(), AiFlowSession.require().getWindow().id(),
                flowSession.getId(),
                flowSession.getWindow().id()
        ));
        this.getFlow().converse(flowSession).offer(data);
        return emitter;
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

    /**
     * built the flow session for starting the conversation.
     *
     * @param emitter the {@link FlowEmitter}{@code <}{@link O}{@code >} representing output emitter.
     * @return {@link FlowSession}.
     * @param <O> the output data type.
     */
    protected static <O> FlowSession buildFlowSession(FlowEmitter<O> emitter) {
        FlowSession mainSession = AiFlowSession.require();
        FlowSession flowSession = FlowSession.newRootSession(mainSession, true);
        flowSession.setInnerState(PARENT_SESSION_ID_KEY, mainSession.getId());
        ResultAction<O> resultAction = emitter::emit;
        flowSession.setInnerState(RESULT_ACTION_KEY, resultAction);
        return flowSession;
    }

    private AiProcessFlow<I, O> getFlow() {
        return Validation.notNull(this.flowSupplier.get(), "The flow cannot be null.");
    }

    protected interface ResultAction<O> {
        /**
         * process the result.
         *
         * @param data the result of {@link O}.
         * @param flowSession the result flow session of {@link FlowSession}.
         */
        void process(O data, FlowSession flowSession);
    }
}
