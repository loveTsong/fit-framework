/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2024 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.domain.emitters;

import modelengine.fit.waterflow.domain.common.Constants;
import modelengine.fit.waterflow.domain.context.FlowSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 流程数据发布器
 *
 * @param <D> 数据类型
 * @since 1.0
 */
public class FlowEmitter<D> implements Emitter<D, FlowSession> {
    /**
     * Emitter的监听器
     */
    protected List<EmitterListener<D, FlowSession>> listeners = new ArrayList<>();

    /**
     * 关联的 session 信息
     */
    protected FlowSession flowSession;

    /**
     * 启动发射器后才能发射数据
     */
    private boolean isStart = false;

    /**
     * 标识完成状态，完成后才能关闭窗口
     */
    private boolean isComplete = false;

    private final List<D> data = new ArrayList<>();

    /**
     * 构造单个数据的Emitter
     *
     * @param data 单个数据
     */
    protected FlowEmitter(D data) {
        this.data.add(data);
    }

    /**
     * 构造一组数据的Emitter
     *
     * @param data 一组数据
     */
    protected FlowEmitter(D... data) {
        this.data.addAll(Arrays.asList(data));
    }

    /**
     * 构造一个mono类型的发布器
     *
     * @param data 待发布的批量数据
     * @param <I> 待发布的数据类型
     * @return 构造一个数据发布器
     */
    public static <I> FlowEmitter<I> mono(I data) {
        return new FlowEmitter<>(data);
    }

    /**
     * 构造一个flux类型的发布器
     *
     * @param data 待发布的批量数据
     * @param <I> 待发布的数据类型
     * @return 构造一个批量数据发布器
     */
    public static <I> FlowEmitter<I> flux(I... data) {
        return new FlowEmitter<>(data);
    }

    /**
     * 从已有的发射器创建一个新的发射器
     *
     * @param emitter 已有的发射器
     * @param <I> 待发布的数据类型
     * @return 新的发射器
     */
    public static <I> FlowEmitter<I> from(Emitter<I, FlowSession> emitter) {
        FlowEmitter<I> cachedEmitter = new AutoCompleteEmitter<>();
        EmitterListener<I, FlowSession> emitterListener = (data, session) -> {
            System.out.println(String.format("[%s][FlowEmitter.from] data=%s, session=%s, windowId=%s, isComplete=%s",
                    Thread.currentThread().getId(),
                    data,
                    session.getId(),
                    session.getWindow().id(),
                    session.getWindow().isComplete()));
            cachedEmitter.emit(data, session);
        };
        emitter.register(emitterListener);
        return cachedEmitter;
    }

    @Override
    public synchronized void register(EmitterListener<D, FlowSession> listener) {
        this.listeners.add(listener);

        if (this.isStart) {
            this.fire();
        }
    }

    public void unregister(EmitterListener<D, FlowSession> listener) {
        if (listener != null) {
            this.listeners.remove(listener);
        }
    }

    @Override
    public synchronized void emit(D data, FlowSession trans) {
        if (!this.isStart) {
            this.data.add(data);
            return;
        }
        this.listeners.forEach(listener -> listener.handle(data, this.flowSession));
    }

    @Override
    public synchronized void start(FlowSession session) {
        if (session != null) {
            session.begin();
        }
        this.flowSession = session;
        this.isStart = true;
        this.fire();
        this.isComplete = true;
        this.tryCompleteWindow();
    }

    @Override
    public synchronized void complete() {
        this.isComplete = true;
        this.tryCompleteWindow();
    }

    /**
     * 设置开始。
     */
    protected void setStarted() {
        this.isStart = true;
    }

    /**
     * 查询是否完成。
     *
     * @return true-完成, false-未完成
     */
    protected boolean isComplete() {
        return this.isComplete;
    }

    /**
     * 设置关联的 session。
     *
     * @param flowSession 关联的session
     */
    protected void setFlowSession(FlowSession flowSession) {
        this.flowSession = flowSession;
    }

    /**
     * 发射缓存的数据。
     */
    protected void fire() {
        for (D d : this.data) {
            this.listeners.forEach(listener -> listener.handle(d,
                    (this.flowSession == null || this.flowSession.getId().equals(Constants.FROM_FLATMAP))
                            ? null
                            : this.flowSession));
        }
        this.data.clear();
    }

    /**
     * 尝试完成对应的 window。
     */
    protected void tryCompleteWindow() {
        if (this.flowSession == null) {
            return;
        }
        if (this.isStart && this.isComplete) {
            this.flowSession.getWindow().complete();
        }
    }

    /**
     * 基于发射器自适应完成的发射器实现。
     *
     * @param <D> 发射器处理的数据类型。
     */
    public static class AutoCompleteEmitter<D> extends FlowEmitter<D> {
        @Override
        public synchronized void start(FlowSession session) {
            if (session != null) {
                session.begin();
            }
            this.setFlowSession(session);
            this.setStarted();
            this.fire();
        }

        @Override
        public synchronized void emit(D data, FlowSession session) {
            // 这里需要基于目标父window判断是否全部window done. 当前这个还不行，处理不了子流中存在拆分window的场景
            // 另外基于session.isCompleted()判断时，这里如何防止并发问题，比如倒数第二条数据进来，同时整个完成时，会提前完成，可能导致少一条数据。
            // 这里也不能通过数量判断，因为前面流如果有拆分window的情况，则数量无法判断。
            if (session.getWindow().isComplete()) {
            // if (session.isCompleted() && session.getWindow().tokenCount() == this.flowSession.getWindow().tokenCount() + 1) {
                System.out.println(String.format("[%s][UnfixedEmitter.emit.session.isCompleted] data=%s, session=%s, windowId=%s, isComplete=%s",
                        Thread.currentThread().getId(),
                        data,
                        session.getId(),
                        session.getWindow().id(),
                        session.getWindow().isComplete()));
                this.complete();
            }
            this.listeners.forEach(listener -> listener.handle(data, this.flowSession));
        }
    }
}
