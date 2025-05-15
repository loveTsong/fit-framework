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
import modelengine.fit.waterflow.domain.emitters.FlowEmitter;

/**
 * 流程委托单元。
 *
 * @param <I> 表示输入数据类型。
 * @param <O> 表示输出数据类型。
 * @author 刘信宏
 * @since 2024-04-22
 */
public interface FlowPattern<I, O> extends Pattern<I, O>, Emitter<O, FlowSession> {
    /**
     * 生成对应数据的发射器。
     *
     * @param input 表示输入数据上下文的 {@link FlowContext}{@code <}{@link I}{@code >}。
     * @return 表示数据的发射器的 {@link FlowEmitter}{@code <}{@link O}{@code >}。
     */
    FlowEmitter<O> getEmitter(FlowContext<I> input);
}
