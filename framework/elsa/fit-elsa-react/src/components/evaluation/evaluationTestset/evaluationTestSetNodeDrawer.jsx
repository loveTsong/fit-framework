/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

import {jadeNodeDrawer} from "@/components/base/jadeNodeDrawer.jsx";
import TestSetIcon from "@/components/asserts/icon-evaluation-test-set.svg?react";

/**
 * 代码节点绘制器
 *
 * @override
 */
export const evaluationTestSetNodeDrawer = (shape, div, x, y) => {
    const self = jadeNodeDrawer(shape, div, x, y);
    self.type = "evaluationTestSetNodeDrawer";

    /**
     * @override
     */
    self.getHeaderIcon = () => {
        return (<>
            <TestSetIcon className="jade-node-custom-header-icon"/>
        </>);
    };

    return self;
};