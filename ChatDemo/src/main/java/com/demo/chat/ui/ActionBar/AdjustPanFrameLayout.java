package com.demo.chat.ui.ActionBar;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.FrameLayout;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class AdjustPanFrameLayout extends FrameLayout {

    private AdjustPanLayoutHelper adjustPanLayoutHelper;

    public AdjustPanFrameLayout(Context context) {
        super(context);
        adjustPanLayoutHelper = new AdjustPanLayoutHelper(this) {
            @Override
            protected void onPanTranslationUpdate(int y) {
                AdjustPanFrameLayout.this.onPanTranslationUpdate(y);
            }

            @Override
            protected void onTransitionStart() {
                AdjustPanFrameLayout.this.onTransitionStart();
            }

            @Override
            protected void onTransitionEnd() {
                AdjustPanFrameLayout.this.onTransitionEnd();
            }
        };
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        adjustPanLayoutHelper.update();
        super.dispatchDraw(canvas);
    }

    protected void onPanTranslationUpdate(int y) {

    }

    protected void onTransitionStart() {

    }

    protected void onTransitionEnd() {

    }
}
