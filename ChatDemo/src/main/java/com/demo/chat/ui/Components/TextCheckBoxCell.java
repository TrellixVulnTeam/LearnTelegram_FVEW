package com.demo.chat.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.theme.Theme;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class TextCheckBoxCell extends FrameLayout {

    private TextView textView;
    private CheckBoxSquare checkBox;
    private boolean needDivider;

    public TextCheckBoxCell(Context context) {
        this(context, false);
    }

    public TextCheckBoxCell(Context context, boolean dialog) {
        super(context);

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(dialog ? Theme.key_dialogTextBlack : Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 66 : 21, 0, LocaleController.isRTL ? 21 : 66, 0));

        checkBox = new CheckBoxSquare(context, dialog);
        checkBox.setDuplicateParentStateEnabled(false);
        checkBox.setFocusable(false);
        checkBox.setFocusableInTouchMode(false);
        checkBox.setClickable(false);
        addView(checkBox, LayoutHelper.createFrame(18, 18, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 21, 0, 21, 0));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        checkBox.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setTextAndCheck(String text, boolean checked, boolean divider) {
        textView.setText(text);
        checkBox.setChecked(checked, false);
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.CheckBox");
        info.setCheckable(true);
        info.setChecked(isChecked());
    }
}