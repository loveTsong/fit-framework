/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2024 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.waterflow.domain.flow;

import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fit.waterflow.domain.context.Window;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextMessenger;
import modelengine.fit.waterflow.domain.context.repo.flowcontext.FlowContextRepo;
import modelengine.fit.waterflow.domain.context.repo.flowlock.FlowLocks;
import modelengine.fit.waterflow.domain.context.repo.flowsession.FlowSessionRepo;
import modelengine.fit.waterflow.domain.emitters.Emitter;
import modelengine.fit.waterflow.domain.emitters.EmitterListener;
import modelengine.fit.waterflow.domain.stream.nodes.From;

/**
 * 处理数据Flow
 * 用于先定义流程，再不停传入不同数据驱动stream往下走
 *
 * @param <D> 初始传入数据类型
 * @since 1.0
 */
public class ProcessFlow<D> extends Flow<D> implements EmitterListener<D, FlowSession>, Emitter<Object, FlowSession> {
    /**
     * 流从起始节点开始
     *
     * @param repo 上下文持久化
     * @param messenger 上下文发送器
     * @param locks 流程锁
     */
    public ProcessFlow(FlowContextRepo repo, FlowContextMessenger messenger, FlowLocks locks) {
        this.start = new From<>(repo, messenger, locks);
    }

    @Override
    public void handle(D data, FlowSession session) {
        this.offer(data, session == null ? new FlowSession() : session);

        // Window previousWindow = session.getWindow();
        // // 这里需要汇聚数据？ 通过emitter过来的数据，这里转换，所有实际实现handle的地方都需要处理的方式。
        // FlowSession nextSession = FlowSessionRepo.getNextEmitterHandleSession(this.start.getStreamId(), session);
        // System.out.println(String.format("[%s][ProcessFlow][handle] data=%s, session=%s, windowId=%s, streamId=%s, fromWindowIsDone=%s, fromWindowId=%s",
        //         Thread.currentThread().getId(), data, nextSession.getId(), nextSession.getWindow().id(),
        //         this.start().getStreamId(),
        //         session.getWindow().isDone(),
        //         session.getWindow().id()
        // ));
        // // this.offer(data, nextSession == null ? new FlowSession() : nextSession);
        // // 这样做，会不会最后两条数据时提前结束？
        // // 另外，投递放先投递完数据然后再结束，这里怎么结束？
        // if (session.getWindow().isDone() || nextSession.getWindow().tokenCount() == 4) {
        //     System.out.println(String.format("[%s][ProcessFlow][handle] data=%s, session=%s, windowId=%s, streamId=%s, isComplete=%s, session window is done.",
        //             Thread.currentThread().getId(), data, nextSession.getId(), nextSession.getWindow().id(),
        //             this.start().getStreamId(), nextSession.getWindow().isComplete()
        //     ));
        //     // session.getWindow().tryFinish();
        //     nextSession.getWindow().complete();
        // }
        // this.offer(data, nextSession);
    }

    @Override
    public void register(EmitterListener<Object, FlowSession> handler) {
        this.end.register(handler);
    }

    public void unregister(EmitterListener<Object, FlowSession> handler) {
        if (handler != null) {
            this.end.unregister(handler);
        }
    }

    @Override
    public void emit(Object data, FlowSession token) {
        this.end.emit(data, token);
    }

    @Override
    public void complete() {
        this.defaultSession.getWindow().complete();
        this.defaultSession = new FlowSession();
        this.defaultSession.begin();
    }
}
