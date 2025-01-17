package com.demo.chat.ui.Components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.DispatchQueue;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.Utilities;
import com.demo.chat.theme.Theme;

import static android.graphics.Canvas.ALL_SAVE_FLAG;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class BlurBehindDrawable {

    DispatchQueue queue;

    public static final int TAG_DRAWING_AS_BACKGROUND = (1 << 26) + 3;

    public static final int STATIC_CONTENT = 0;
    public static final int ADJUST_PAN_TRANSLATION_CONTENT = 1;

    private View behindView;
    private View parentView;

    private Bitmap[] blurredBitmapTmp;
    private Bitmap[] backgroundBitmap;
    private Bitmap[] renderingBitmap;
    private Canvas[] renderingBitmapCanvas;
    private Canvas[] backgroundBitmapCanvas;
    private Canvas[] blurCanvas;

    private boolean processingNextFrame;
    private boolean invalidate = true;

    private float blurAlpha;
    private boolean show;
    private boolean error;

    private final float DOWN_SCALE = 6f;
    private int lastH;
    private int lastW;
    private int toolbarH;

    private boolean wasDraw;
    private boolean skipDraw;

    private float panTranslationY;

    BlurBackgroundTask blurBackgroundTask = new BlurBackgroundTask();

    Paint emptyPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    Paint errorBlackoutPaint = new Paint();

    public BlurBehindDrawable(View behindView, View parentView) {
        this.behindView = behindView;
        this.parentView = parentView;

        errorBlackoutPaint.setColor(Color.BLACK);
    }


    public void draw(Canvas canvas) {
        final Bitmap[] bitmap = renderingBitmap;
        if (bitmap != null || error) {
            if (show && blurAlpha != 1f) {
                blurAlpha += 0.09f;
                if (blurAlpha > 1f) {
                    blurAlpha = 1f;
                }
                parentView.invalidate();
            } else if (!show && blurAlpha != 0) {
                blurAlpha -= 0.09f;
                if (blurAlpha < 0) {
                    blurAlpha = 0f;
                }
                parentView.invalidate();
            }
        }

        if (bitmap == null && error) {
            errorBlackoutPaint.setAlpha((int) (50 * blurAlpha));
            canvas.drawPaint(errorBlackoutPaint);
            return;
        }

        canvas.saveLayerAlpha(0, 0, parentView.getMeasuredWidth(), parentView.getMeasuredHeight(), (int) (blurAlpha * 255), ALL_SAVE_FLAG);
        if (bitmap != null) {
            emptyPaint.setAlpha((int) (255 * blurAlpha));
            canvas.drawBitmap(bitmap[1], 0, 0, null);
            canvas.save();
            canvas.translate(0, panTranslationY);
            canvas.drawBitmap(bitmap[0], 0, 0, null);
            canvas.restore();
            wasDraw = true;
            canvas.drawColor(0x1a000000);
        }
        canvas.restore();

        if (show && !processingNextFrame && (renderingBitmap == null || invalidate)) {
            processingNextFrame = true;
            invalidate = false;
            if (blurredBitmapTmp == null) {
                blurredBitmapTmp = new Bitmap[2];
                blurCanvas = new Canvas[2];
            }
            for (int i = 0; i < 2; i++) {
                if (blurredBitmapTmp[i] == null || parentView.getMeasuredWidth() != lastW || parentView.getMeasuredHeight() != lastH) {
                    int lastH = parentView.getMeasuredHeight();
                    int lastW = parentView.getMeasuredWidth();
                    toolbarH = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(100);
                    try {
                        int h = i == 0 ? toolbarH : lastH;
                        blurredBitmapTmp[i] = Bitmap.createBitmap((int) (lastW / DOWN_SCALE), (int) (h / DOWN_SCALE), Bitmap.Config.ARGB_8888);
                        blurCanvas[i] = new Canvas(blurredBitmapTmp[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                        FileLog.e(e);
                        AndroidUtilities.runOnUIThread(() -> {
                            error = true;
                            parentView.invalidate();
                        });
                        return;
                    }
                }

                blurCanvas[i].save();
                blurCanvas[i].scale(1f / DOWN_SCALE, 1f / DOWN_SCALE, 0, 0);
                Drawable backDrawable = behindView.getBackground();
                if (backDrawable == null) {
                    backDrawable = Theme.getCachedWallpaperNonBlocking();
                }
                behindView.setTag(TAG_DRAWING_AS_BACKGROUND, i);
                if (i == STATIC_CONTENT) {
                    blurCanvas[i].translate(0, -panTranslationY);
                    behindView.draw(blurCanvas[i]);
                }

                if (backDrawable != null && i == ADJUST_PAN_TRANSLATION_CONTENT) {
                    android.graphics.Rect oldBounds = backDrawable.getBounds();
                    backDrawable.setBounds(0, 0, behindView.getMeasuredWidth(), behindView.getMeasuredHeight());
                    backDrawable.draw(blurCanvas[i]);
                    backDrawable.setBounds(oldBounds);
                    behindView.draw(blurCanvas[i]);
                }

                behindView.setTag(TAG_DRAWING_AS_BACKGROUND, null);
                blurCanvas[i].restore();
            }

            lastH = parentView.getMeasuredHeight();
            lastW = parentView.getMeasuredWidth();

            blurBackgroundTask.width = parentView.getMeasuredWidth();
            blurBackgroundTask.height = parentView.getMeasuredHeight();
            if (blurBackgroundTask.width == 0 || blurBackgroundTask.height == 0) {
                processingNextFrame = false;
                return;
            }
            if (queue == null) {
                queue = new DispatchQueue("blur_thread_" + this);
            }
            queue.postRunnable(blurBackgroundTask);
        }
    }

    private int getBlurRadius() {
        return Math.max(7, Math.max(lastH, lastW) / 180);
    }

    public void clear() {
        invalidate = true;
        wasDraw = false;
        error = false;
        blurAlpha = 0;
        lastW = 0;
        lastH = 0;
        if (queue != null) {
            queue.cleanupQueue();
            queue.postRunnable(() -> {
                if (renderingBitmap != null) {
                    renderingBitmap[0].recycle();
                    renderingBitmap[1].recycle();
                    renderingBitmap = null;
                }
                if (backgroundBitmap != null) {
                    backgroundBitmap[0].recycle();
                    backgroundBitmap[1].recycle();
                    backgroundBitmap = null;
                }
                renderingBitmapCanvas = null;
                skipDraw = false;
                AndroidUtilities.runOnUIThread(() -> {
                    if (queue != null) {
                        queue.recycle();
                        queue = null;
                    }
                });
            });
        }
    }

    public void invalidate() {
        invalidate = true;
        if (parentView != null) {
            parentView.invalidate();
        }
    }

    public boolean isFullyDrawing() {
        return !skipDraw && wasDraw && blurAlpha == 1f && show;
    }

    public void checkSizes() {
        final Bitmap[] bitmap = renderingBitmap;
        if (bitmap == null || parentView.getMeasuredHeight() == 0 || parentView.getMeasuredWidth() == 0) {
            return;
        }
        blurBackgroundTask.canceled = true;
        blurBackgroundTask = new BlurBackgroundTask();

        for (int i = 0; i < 2; i++) {
            int lastH = parentView.getMeasuredHeight();
            int lastW = parentView.getMeasuredWidth();
            toolbarH = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(100);
            int h = i == 0 ? toolbarH : lastH;

            if (bitmap[i].getHeight() != h || bitmap[i].getWidth() != parentView.getMeasuredWidth()) {
                if (queue != null) {
                    queue.cleanupQueue();
                }

                blurredBitmapTmp[i] = Bitmap.createBitmap((int) (lastW / DOWN_SCALE), (int) (h / DOWN_SCALE), Bitmap.Config.ARGB_8888);
                blurCanvas[i] = new Canvas(blurredBitmapTmp[i]);

                renderingBitmap[i] = Bitmap.createBitmap(lastW, i == 0 ? toolbarH : lastH, Bitmap.Config.ARGB_8888);
                renderingBitmapCanvas[i] = new Canvas(renderingBitmap[i]);
                renderingBitmapCanvas[i].scale(DOWN_SCALE, DOWN_SCALE);

                blurCanvas[i].save();
                blurCanvas[i].scale(1f / DOWN_SCALE, 1f / DOWN_SCALE, 0, 0);
                Drawable backDrawable = behindView.getBackground();
                if (backDrawable == null) {
                    backDrawable = Theme.getCachedWallpaperNonBlocking();
                }
                behindView.setTag(TAG_DRAWING_AS_BACKGROUND, i);
                if (i == STATIC_CONTENT) {
                    blurCanvas[i].translate(0, -panTranslationY);
                    behindView.draw(blurCanvas[i]);
                }

                if (i == ADJUST_PAN_TRANSLATION_CONTENT) {
                    Rect oldBounds = backDrawable.getBounds();
                    backDrawable.setBounds(0, 0, behindView.getMeasuredWidth(), behindView.getMeasuredHeight());
                    backDrawable.draw(blurCanvas[i]);
                    backDrawable.setBounds(oldBounds);
                    behindView.draw(blurCanvas[i]);
                }

                behindView.setTag(TAG_DRAWING_AS_BACKGROUND, null);
                blurCanvas[i].restore();

                Utilities.stackBlurBitmap(blurredBitmapTmp[i], getBlurRadius());
                emptyPaint.setAlpha(255);
                renderingBitmapCanvas[i].drawBitmap(blurredBitmapTmp[i], 0, 0, emptyPaint);
            }
        }

        lastH = parentView.getMeasuredHeight();
        lastW = parentView.getMeasuredWidth();
    }

    public void show(boolean show) {
        this.show = show;
    }

    public void onPanTranslationUpdate(int y) {
        panTranslationY = y;
    }

    public class BlurBackgroundTask implements Runnable {

        boolean canceled;
        int width;
        int height;

        @Override
        public void run() {
            if (backgroundBitmap == null) {
                backgroundBitmap = new Bitmap[2];
                backgroundBitmapCanvas = new Canvas[2];
            }
            for (int i = 0; i < 2; i++) {
                int h = i == 0 ? toolbarH : height;
                if (backgroundBitmap[i] != null && (backgroundBitmap[i].getHeight() != h || backgroundBitmap[i].getWidth() != width)) {
                    if (backgroundBitmap[i] != null) {
                        backgroundBitmap[i].recycle();
                        backgroundBitmap[i] = null;
                    }
                }
                long t = System.currentTimeMillis();
                if (backgroundBitmap[i] == null) {
                    int w = width;
                    backgroundBitmap[i] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    backgroundBitmapCanvas[i] = new Canvas(backgroundBitmap[i]);
                    backgroundBitmapCanvas[i].scale(DOWN_SCALE, DOWN_SCALE);
                }
                emptyPaint.setAlpha(255);
                Utilities.stackBlurBitmap(blurredBitmapTmp[i], getBlurRadius());
                backgroundBitmapCanvas[i].drawBitmap(blurredBitmapTmp[i], 0, 0, emptyPaint);

                if (canceled) {
                    return;
                }
            }
            AndroidUtilities.runOnUIThread(() -> {
                if (canceled) {
                    return;
                }
                Bitmap[] bitmap = renderingBitmap;
                Canvas[] canvas = renderingBitmapCanvas;

                renderingBitmap = backgroundBitmap;
                renderingBitmapCanvas = backgroundBitmapCanvas;

                backgroundBitmap = bitmap;
                backgroundBitmapCanvas = canvas;

                processingNextFrame = false;
                if (parentView != null) {
                    parentView.invalidate();
                }
            });
        }
    }
}
