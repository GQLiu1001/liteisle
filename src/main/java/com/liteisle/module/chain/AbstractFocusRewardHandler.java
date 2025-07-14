package com.liteisle.module.chain;

import lombok.Setter;

@Setter
public abstract class AbstractFocusRewardHandler {

    /**
     * -- SETTER --
     *  设置链条中的下一个处理器
     */
    protected AbstractFocusRewardHandler next;

    /**
     * 模板方法：执行当前处理逻辑，如果未处理则传递给下一个
     */
    public void process(FocusRewardContext context) {
        // 如果之前的处理器已经处理完毕，则直接跳过
        if (context.isHandled()) {
            return;
        }

        // 调用具体子类的处理逻辑
        this.handle(context);

        // 如果当前处理器未处理，并且链条中还有下一个处理器，则继续传递
        if (this.next != null && !context.isHandled()) {
            this.next.process(context);
        }
    }

    /**
     * 抽象方法，由具体的处理器实现各自的奖励判断逻辑
     */
    protected abstract void handle(FocusRewardContext context);
}