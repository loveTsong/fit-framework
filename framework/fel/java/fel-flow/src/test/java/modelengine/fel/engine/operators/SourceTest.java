// /*---------------------------------------------------------------------------------------------
//  *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
//  *  This file is a part of the ModelEngine Project.
//  *  Licensed under the MIT License. See License.txt in the project root for license information.
//  *--------------------------------------------------------------------------------------------*/
//
// package modelengine.fel.engine.operators;
//
// import static org.assertj.core.api.Assertions.assertThat;
//
// import modelengine.fel.core.chat.Prompt;
// import modelengine.fel.core.util.Tip;
// import modelengine.fel.engine.flows.AiFlows;
// import modelengine.fel.engine.flows.AiProcessFlow;
// import modelengine.fel.engine.flows.ConverseLatch;
// import modelengine.fel.engine.operators.prompts.Prompts;
// import modelengine.fel.engine.operators.sources.Source;
// import modelengine.fit.waterflow.domain.utils.SleepUtil;
//
// import org.junit.jupiter.api.Test;
//
// import java.util.concurrent.atomic.AtomicInteger;
//
// /**
//  * {@link Source} 的测试。
//  *
//  * @author 刘信宏
//  * @since 2024-05-09
//  */
// public class SourceTest {
//     @Test
//     void shouldOkWhenFlowOfferSource() {
//         AtomicInteger cnt = new AtomicInteger(0);
//         StringBuilder answer = new StringBuilder();
//         AiProcessFlow<Tip, Prompt> flow = AiFlows.<Tip>create()
//                 .prompt(Prompts.human("{{question}}"))
//                 .close(data -> cnt.incrementAndGet());
//
//         Source<Tip> tipSource = new Source<>();
//         ConverseLatch<Prompt> converseLatch = flow.converse()
//                 .doOnConsume(data -> answer.append(data.text()))
//                 // .doOnSuccess(data -> answer.append(data.text()))
//                 .offer(tipSource);
//         tipSource.emit(Tip.from("question", "my question"));
//
//         converseLatch.await();
//         assertThat(answer.toString()).isEqualTo("my question");
//
//         // 第二次会话
//         StringBuilder answer2 = new StringBuilder();
//         ConverseLatch<Prompt> converseLatch2 =
//                 flow.converse().doOnSuccess(data -> answer2.append(data.text())).offer(tipSource);
//         tipSource.emit(Tip.from("question", "my question2"));
//
//         converseLatch2.await();
//         assertThat(answer2.toString()).isEqualTo("my question2");
//
//         // 验证会话结束后注销热源的订阅
//         SleepUtil.sleep(10);
//         assertThat(cnt.get()).isEqualTo(2);
//     }
// }
