package com.demo.chat.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.receiver.ImageReceiver;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.ActionBar.SimpleTextView;
import com.demo.chat.ui.Cells.ChatMessageCell;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
@SuppressWarnings("FieldCanBeLocal")
public class HintView extends FrameLayout {

    public static final int TYPE_SEARCH_AS_LIST = 3;
    public static final int TYPE_POLL_VOTE = 5;

    private TextView textView;
    private ImageView imageView;
    private ImageView arrowImageView;
    private ChatMessageCell messageCell;
    private View currentView;
    private AnimatorSet animatorSet;
    private Runnable hideRunnable;
    private int currentType;
    private boolean isTopArrow;
    private String overrideText;
    private int shownY;

    private long showingDuration = 2000;

    public HintView(Context context, int type) {
        this(context, type, false);
    }

    public HintView(Context context, int type, boolean topArrow) {
        super(context);

        currentType = type;
        isTopArrow = topArrow;

        textView = new CorrectlyMeasuringTextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_chat_gifSaveHintText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setMaxLines(2);
        textView.setMaxWidth(AndroidUtilities.dp(type == 4 ? 280 : 250));
        if (currentType == TYPE_SEARCH_AS_LIST) {
            textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(5), Theme.getColor(Theme.key_chat_gifSaveHintBackground)));
            textView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 30, Gravity.LEFT | Gravity.TOP, 0, topArrow ? 6 : 0, 0, topArrow ? 0 : 6));
        } else {
            textView.setGravity(Gravity.LEFT | Gravity.TOP);
            textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(3), Theme.getColor(Theme.key_chat_gifSaveHintBackground)));
            if (currentType == TYPE_POLL_VOTE || currentType == 4) {
                textView.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(6), AndroidUtilities.dp(9), AndroidUtilities.dp(7));
            } else if (currentType == 2) {
                textView.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(6), AndroidUtilities.dp(7), AndroidUtilities.dp(7));
            } else {
                textView.setPadding(AndroidUtilities.dp(currentType == 0 ? 54 : 5), AndroidUtilities.dp(6), AndroidUtilities.dp(5), AndroidUtilities.dp(7));
            }
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, topArrow ? 6 : 0, 0, topArrow ? 0 : 6));
        }

        if (type == 0) {
            textView.setText(LocaleController.getString("AutoplayVideoInfo", R.string.AutoplayVideoInfo));

            imageView = new ImageView(context);
            imageView.setImageResource(R.drawable.tooltip_sound);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_gifSaveHintText), PorterDuff.Mode.MULTIPLY));
            addView(imageView, LayoutHelper.createFrame(38, 34, Gravity.LEFT | Gravity.TOP, 7, 7, 0, 0));
        }

        arrowImageView = new ImageView(context);
        arrowImageView.setImageResource(topArrow ? R.drawable.tooltip_arrow_up : R.drawable.tooltip_arrow);
        arrowImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_gifSaveHintBackground), PorterDuff.Mode.MULTIPLY));
        addView(arrowImageView, LayoutHelper.createFrame(14, 6, Gravity.LEFT | (topArrow ? Gravity.TOP : Gravity.BOTTOM), 0, 0, 0, 0));
    }

    public void setOverrideText(String text) {
        overrideText = text;
        textView.setText(text);
        if (messageCell != null) {
            ChatMessageCell cell = messageCell;
            messageCell = null;
            showForMessageCell(cell, false);
        }
    }

    public boolean showForMessageCell(ChatMessageCell cell, boolean animated) {
        return showForMessageCell(cell, null, 0, 0, animated);
    }

    public boolean showForMessageCell(ChatMessageCell cell, Object object, int x, int y, boolean animated) {
        if (currentType == TYPE_POLL_VOTE && y == shownY && messageCell == cell || currentType != TYPE_POLL_VOTE && (currentType == 0 && getTag() != null || messageCell == cell)) {
            return false;
        }
        if (hideRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(hideRunnable);
            hideRunnable = null;
        }
        int[] position = new int[2];
        cell.getLocationInWindow(position);
        int top = position[1];
        View p = (View) getParent();
        p.getLocationInWindow(position);
        top -= position[1];

        View parentView = (View) cell.getParent();
        int centerX;
        if (currentType == 0) {
            ImageReceiver imageReceiver = cell.getPhotoImage();
            top += imageReceiver.getImageY();
            int height = (int) imageReceiver.getImageHeight();
            int bottom = top + height;
            int parentHeight = parentView.getMeasuredHeight();
            if (top <= getMeasuredHeight() + AndroidUtilities.dp(10) || bottom > parentHeight + height / 4) {
                return false;
            }
            centerX = cell.getNoSoundIconCenterX();
        } else if (currentType == TYPE_POLL_VOTE) {
            Integer count = (Integer) object;
            centerX = x;
            top += y;
            shownY = y;
            if (count == -1) {
                textView.setText(LocaleController.getString("PollSelectOption", R.string.PollSelectOption));
            } else {
                if (cell.getMessageObject().isQuiz()) {
                    if (count == 0) {
                        textView.setText(LocaleController.getString("NoVotesQuiz", R.string.NoVotesQuiz));
                    } else {
                        textView.setText(LocaleController.formatPluralString("Answer", count));
                    }
                } else {
                    if (count == 0) {
                        textView.setText(LocaleController.getString("NoVotes", R.string.NoVotes));
                    } else {
                        textView.setText(LocaleController.formatPluralString("Vote", count));
                    }
                }
            }
            measure(MeasureSpec.makeMeasureSpec(1000, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(1000, MeasureSpec.AT_MOST));
        } else {
            MessageObject messageObject = cell.getMessageObject();
            if (overrideText == null) {
                textView.setText(LocaleController.getString("HidAccount", R.string.HidAccount));
            } else {
                textView.setText(overrideText);
            }
            measure(MeasureSpec.makeMeasureSpec(1000, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(1000, MeasureSpec.AT_MOST));

            User user = cell.getCurrentUser();
            if (user != null && user.id == 0) {
                top += (cell.getMeasuredHeight() - Math.max(0, cell.getBottom() - parentView.getMeasuredHeight()) - AndroidUtilities.dp(50));
            } else {
                top += AndroidUtilities.dp(22);
                if (!messageObject.isOutOwner() && cell.isDrawNameLayout()) {
                    top += AndroidUtilities.dp(20);
                }
            }
            if (!isTopArrow && top <= getMeasuredHeight() + AndroidUtilities.dp(10)) {
                return false;
            }
            centerX = cell.getForwardNameCenterX();
        }

        int parentWidth = parentView.getMeasuredWidth();
        if (isTopArrow) {
            setTranslationY(AndroidUtilities.dp(44));
        } else {
            setTranslationY(top - getMeasuredHeight());
        }
        int iconX = cell.getLeft() + centerX;
        int left = AndroidUtilities.dp(19);
        if (currentType == TYPE_POLL_VOTE) {
            int offset = Math.max(0, centerX - getMeasuredWidth() / 2 - AndroidUtilities.dp(19.1f));
            setTranslationX(offset);
            left += offset;
        } else if (iconX > parentView.getMeasuredWidth() / 2) {
            int offset = parentWidth - getMeasuredWidth() - AndroidUtilities.dp(38);
            setTranslationX(offset);
            left += offset;
        } else {
            setTranslationX(0);
        }
        float arrowX = cell.getLeft() + centerX - left - arrowImageView.getMeasuredWidth() / 2;
        arrowImageView.setTranslationX(arrowX);
        if (iconX > parentView.getMeasuredWidth() / 2) {
            if (arrowX < AndroidUtilities.dp(10)) {
                float diff = arrowX - AndroidUtilities.dp(10);
                setTranslationX(getTranslationX() + diff);
                arrowImageView.setTranslationX(arrowX - diff);
            }
        } else {
            if (arrowX > getMeasuredWidth() - AndroidUtilities.dp(14 + 10)) {
                float diff = arrowX - getMeasuredWidth() + AndroidUtilities.dp(14 + 10);
                setTranslationX(diff);
                arrowImageView.setTranslationX(arrowX - diff);
            } else if (arrowX < AndroidUtilities.dp(10)) {
                float diff = arrowX - AndroidUtilities.dp(10);
                setTranslationX(getTranslationX() + diff);
                arrowImageView.setTranslationX(arrowX - diff);
            }
        }

        messageCell = cell;
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }

        setTag(1);
        setVisibility(VISIBLE);
        if (animated) {
            animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f, 1.0f)
            );
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animatorSet = null;
                    AndroidUtilities.runOnUIThread(hideRunnable = () -> hide(), currentType == 0 ? 10000 : 2000);
                }
            });
            animatorSet.setDuration(300);
            animatorSet.start();
        } else {
            setAlpha(1.0f);
        }

        return true;
    }

    public boolean showForView(View view, boolean animated) {
        if (currentView == view || getTag() != null) {
            return false;
        }
        if (hideRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(hideRunnable);
            hideRunnable = null;
        }
        measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.displaySize.x, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.displaySize.x, MeasureSpec.AT_MOST));

        int[] position = new int[2];
        view.getLocationInWindow(position);

        int top = position[1] - AndroidUtilities.dp(4);

        if (currentType == 4) {
            top += AndroidUtilities.dp(4);
        }

        int centerX;
        if (currentType == TYPE_SEARCH_AS_LIST) {
            if (view instanceof SimpleTextView) {
                centerX = position[0] + ((SimpleTextView) view).getTextWidth() / 2;
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            centerX = position[0] + view.getMeasuredWidth() / 2;
        }

        View parentView = (View) getParent();
        parentView.getLocationInWindow(position);
        centerX -= position[0];
        top -= position[1];

        int parentWidth = parentView.getMeasuredWidth();
        if (isTopArrow) {
            setTranslationY(AndroidUtilities.dp(44));
        } else {
            setTranslationY(top - getMeasuredHeight());
        }
        final int offset;

        int leftMargin = 0;
        int rightMargin = 0;
        if (getLayoutParams() instanceof MarginLayoutParams) {
            leftMargin = ((MarginLayoutParams) getLayoutParams()).leftMargin;
            rightMargin = ((MarginLayoutParams) getLayoutParams()).rightMargin;
        }
        if (centerX > parentView.getMeasuredWidth() / 2) {
            if (currentType == TYPE_SEARCH_AS_LIST) {
                offset = (int) (parentWidth - getMeasuredWidth() * 1.5f);
            } else {
                offset = parentWidth - getMeasuredWidth() - (leftMargin + rightMargin);
            }
        } else {
            if (currentType == TYPE_SEARCH_AS_LIST) {
                offset = centerX - getMeasuredWidth() / 2 - arrowImageView.getMeasuredWidth();
            } else {
                offset = 0;
            }
        }
        setTranslationX(offset);
        float arrowX = centerX - (leftMargin + offset) - arrowImageView.getMeasuredWidth() / 2;
        arrowImageView.setTranslationX(arrowX);
        if (centerX > parentView.getMeasuredWidth() / 2) {
            if (arrowX < AndroidUtilities.dp(10)) {
                float diff = arrowX - AndroidUtilities.dp(10);
                setTranslationX(getTranslationX() + diff);
                arrowImageView.setTranslationX(arrowX - diff);
            }
        } else {
            if (arrowX > getMeasuredWidth() - AndroidUtilities.dp(14 + 10)) {
                float diff = arrowX - getMeasuredWidth() + AndroidUtilities.dp(14 + 10);
                setTranslationX(diff);
                arrowImageView.setTranslationX(arrowX - diff);
            } else if (arrowX < AndroidUtilities.dp(10)) {
                float diff = arrowX - AndroidUtilities.dp(10);
                setTranslationX(getTranslationX() + diff);
                arrowImageView.setTranslationX(arrowX - diff);
            }
        }

        currentView = view;
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }

        setTag(1);
        setVisibility(VISIBLE);
        if (animated) {
            animatorSet = new AnimatorSet();
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f, 1.0f)
            );
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animatorSet = null;
                    AndroidUtilities.runOnUIThread(hideRunnable = () -> hide(), showingDuration);
                }
            });
            animatorSet.setDuration(300);
            animatorSet.start();
        } else {
            setAlpha(1.0f);
        }

        return true;
    }

    public void hide() {
        if (getTag() == null) {
            return;
        }
        setTag(null);
        if (hideRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(hideRunnable);
            hideRunnable = null;
        }
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f)
        );
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.INVISIBLE);
                currentView = null;
                messageCell = null;
                animatorSet = null;
            }
        });
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public ChatMessageCell getMessageCell() {
        return messageCell;
    }

    public void setShowingDuration(long showingDuration) {
        this.showingDuration = showingDuration;
    }
}
