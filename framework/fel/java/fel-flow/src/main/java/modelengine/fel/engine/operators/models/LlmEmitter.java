/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2024 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fel.engine.operators.models;

import static modelengine.fitframework.util.ObjectUtils.cast;

import modelengine.fel.core.chat.ChatMessage;
import modelengine.fel.core.chat.Prompt;
import modelengine.fel.core.chat.support.AiMessage;
import modelengine.fel.core.chat.support.HumanMessage;
import modelengine.fel.core.memory.Memory;
import modelengine.fel.engine.util.StateKey;
import modelengine.fit.waterflow.bridge.fitflow.FitBoundedEmitter;
import modelengine.fit.waterflow.domain.context.FlowContext;
import modelengine.fit.waterflow.domain.context.FlowSession;
import modelengine.fit.waterflow.domain.stream.nodes.Retryable;
import modelengine.fit.waterflow.domain.stream.reactive.Processor;
import modelengine.fitframework.flowable.Publisher;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.util.ObjectUtils;
import modelengine.fitframework.util.StringUtils;

import java.util.Collections;

/**
 * 流式模型发射器。
 *
 * @author 刘信宏
 * @since 2024-05-16
 */
public class LlmEmitter<O extends ChatMessage> extends FitBoundedEmitter<O, ChatMessage> {
    private static final StreamingConsumer<ChatMessage, ChatMessage> EMPTY_CONSUMER = (acc, chunk) -> {};

    /**
     * 初始化 {@link LlmEmitter}。
     *
     * @param publisher 表示数据发布者的 {@link Publisher}{@code <}{@link O}{@code >}。
     * @param prompt 表示模型输入的 {@link Prompt}， 用于获取默认用户问题。
     * @param session 表示流程实例运行标识的 {@link FlowSession}。
     */
    public LlmEmitter(Publisher<O> publisher, Prompt prompt, FlowSession session) {
        super(publisher, data -> data);
        Validation.notNull(session, "The session cannot be null.");
    }

    @Override
    protected void consumeAction(O source, ChatMessage target) {
        System.out.println(String.format("[%s][consumeAction] %s", Thread.currentThread().getId(), target.text()));
    }

    private ChatMessage getDefaultQuestion(Prompt prompt) {
        int size = prompt.messages().size();
        if (size == 0) {
            return new HumanMessage(StringUtils.EMPTY);
        }
        return prompt.messages().get(size - 1);
    }
}
