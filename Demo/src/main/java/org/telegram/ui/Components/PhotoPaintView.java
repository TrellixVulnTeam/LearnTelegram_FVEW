package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Bitmaps;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Paint.PhotoFace;
import org.telegram.ui.Components.Paint.Views.EntitiesContainerView;
import org.telegram.ui.Components.Paint.Views.EntityView;
import org.telegram.ui.Components.Paint.Views.StickerView;
import org.telegram.ui.Components.Paint.Views.TextPaintView;
import org.telegram.ui.Components.Paint.UndoStore;
import org.telegram.ui.Components.Paint.Brush;
import org.telegram.ui.Components.Paint.RenderView;
import org.telegram.ui.Components.Paint.Painting;
import org.telegram.ui.Components.Paint.Swatch;
import org.telegram.ui.Components.Paint.Views.ColorPicker;
import org.telegram.ui.PhotoViewer;

import java.math.BigInteger;
import java.util.ArrayList;

@SuppressLint("NewApi")
public class PhotoPaintView extends FrameLayout implements EntityView.EntityViewDelegate {

    private Bitmap bitmapToEdit;
    private Bitmap facesBitmap;
    private UndoStore undoStore;

    int currentBrush;
    private Brush[] brushes = new Brush[]{
            new Brush.Radial(),
            new Brush.Elliptical(),
            new Brush.Neon()
    };

    private FrameLayout toolsView;
    private TextView cancelTextView;
    private TextView doneTextView;

    private FrameLayout curtainView;
    private RenderView renderView;
    private EntitiesContainerView entitiesView;
    private FrameLayout dimView;
    private FrameLayout textDimView;
    private FrameLayout selectionContainerView;
    private ColorPicker colorPicker;

    private ImageView paintButton;

    private EntityView currentEntityView;

    private boolean editingText;
    private Point editedTextPosition;
    private float editedTextRotation;
    private float editedTextScale;
    private String initialText;

    private BigInteger lcm;

    private ActionBarPopupWindow popupWindow;
    private ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout;
    private Rect popupRect;

    private Size paintingSize;

    private float baseScale;

    private int selectedTextType = 2;

    private Animator colorPickerAnimator;

    private DispatchQueue queue;
    private ArrayList<PhotoFace> faces;

    private boolean ignoreLayout;

    private Swatch brushSwatch;

    private final static int gallery_menu_done = 1;

    private int originalBitmapRotation;

    public PhotoPaintView(Context context, Bitmap bitmap, Bitmap originalBitmap, int originalRotation, ArrayList<VideoEditedInfo.MediaEntity> entities, Runnable onInit) {
        super(context);

        queue = new DispatchQueue("Paint");

        originalBitmapRotation = originalRotation;

        bitmapToEdit = bitmap;
        facesBitmap = originalBitmap;
        undoStore = new UndoStore();
        undoStore.setDelegate(() -> colorPicker.setUndoEnabled(undoStore.canUndo()));

        curtainView = new FrameLayout(context);
        curtainView.setBackgroundColor(0x22000000);
        curtainView.setVisibility(INVISIBLE);
        addView(curtainView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        renderView = new RenderView(context, new Painting(getPaintingSize()), bitmap);
        renderView.setDelegate(new RenderView.RenderViewDelegate() {

            @Override
            public void onFirstDraw() {
                onInit.run();
            }

            @Override
            public void onBeganDrawing() {
                if (currentEntityView != null) {
                    selectEntity(null);
                }
            }

            @Override
            public void onFinishedDrawing(boolean moved) {
                colorPicker.setUndoEnabled(undoStore.canUndo());
            }

            @Override
            public boolean shouldDraw() {
                boolean draw = currentEntityView == null;
                if (!draw) {
                    selectEntity(null);
                }
                return draw;
            }
        });
        renderView.setUndoStore(undoStore);
        renderView.setQueue(queue);
        renderView.setVisibility(View.INVISIBLE);
        renderView.setBrush(brushes[0]);
        addView(renderView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        entitiesView = new EntitiesContainerView(context, new EntitiesContainerView.EntitiesContainerViewDelegate() {
            @Override
            public boolean shouldReceiveTouches() {
                return textDimView.getVisibility() != VISIBLE;
            }

            @Override
            public EntityView onSelectedEntityRequest() {
                return currentEntityView;
            }

            @Override
            public void onEntityDeselect() {
                selectEntity(null);
            }
        });
        addView(entitiesView);

        dimView = new FrameLayout(context);
        dimView.setAlpha(0);
        dimView.setBackgroundColor(0x66000000);
        dimView.setVisibility(GONE);
        addView(dimView);

        textDimView = new FrameLayout(context);
        textDimView.setAlpha(0);
        textDimView.setBackgroundColor(0x66000000);
        textDimView.setVisibility(GONE);
        textDimView.setOnClickListener(v -> closeTextEnter(true));

        selectionContainerView = new FrameLayout(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return false;
            }
        };
        addView(selectionContainerView);

        colorPicker = new ColorPicker(context);
        addView(colorPicker);
        colorPicker.setDelegate(new ColorPicker.ColorPickerDelegate() {
            @Override
            public void onBeganColorPicking() {
                if (!(currentEntityView instanceof TextPaintView)) {
                    setDimVisibility(true);
                }
            }

            @Override
            public void onColorValueChanged() {
                setCurrentSwatch(colorPicker.getSwatch(), false);
            }

            @Override
            public void onFinishedColorPicking() {
                setCurrentSwatch(colorPicker.getSwatch(), false);

                if (!(currentEntityView instanceof TextPaintView)) {
                    setDimVisibility(false);
                }
            }

            @Override
            public void onSettingsPressed() {
                if (currentEntityView != null) {
                    if (currentEntityView instanceof StickerView) {
                        mirrorSticker();
                    } else if (currentEntityView instanceof TextPaintView) {
                        showTextSettings();
                    }
                } else {
                    showBrushSettings();
                }
            }

            @Override
            public void onUndoPressed() {
                undoStore.undo();
            }
        });

        toolsView = new FrameLayout(context);
        toolsView.setBackgroundColor(0xff000000);
        addView(toolsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM));

        cancelTextView = new TextView(context);
        cancelTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        cancelTextView.setTextColor(0xffffffff);
        cancelTextView.setGravity(Gravity.CENTER);
        cancelTextView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR, 0));
        cancelTextView.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        cancelTextView.setText(LocaleController.getString("Cancel", R.string.Cancel).toUpperCase());
        cancelTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        toolsView.addView(cancelTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        doneTextView = new TextView(context);
        doneTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        doneTextView.setTextColor(Theme.getColor(Theme.key_dialogFloatingButton));
        doneTextView.setGravity(Gravity.CENTER);
        doneTextView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR, 0));
        doneTextView.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        doneTextView.setText(LocaleController.getString("Done", R.string.Done).toUpperCase());
        doneTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        toolsView.addView(doneTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.RIGHT));

        paintButton = new ImageView(context);
        paintButton.setScaleType(ImageView.ScaleType.CENTER);
        paintButton.setImageResource(R.drawable.photo_paint);
        paintButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR));
        toolsView.addView(paintButton, LayoutHelper.createFrame(54, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 0, 0, 56, 0));
        paintButton.setOnClickListener(v -> selectEntity(null));

        ImageView stickerButton = new ImageView(context);
        stickerButton.setScaleType(ImageView.ScaleType.CENTER);
        stickerButton.setImageResource(R.drawable.photo_sticker);
        stickerButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR));
        toolsView.addView(stickerButton, LayoutHelper.createFrame(54, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        stickerButton.setOnClickListener(v -> openStickersView());

        ImageView textButton = new ImageView(context);
        textButton.setScaleType(ImageView.ScaleType.CENTER);
        textButton.setImageResource(R.drawable.photo_paint_text);
        textButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR));
        toolsView.addView(textButton, LayoutHelper.createFrame(54, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 56, 0, 0, 0));
        textButton.setOnClickListener(v -> createText(true));

        colorPicker.setUndoEnabled(false);
        setCurrentSwatch(colorPicker.getSwatch(), false);
        updateSettingsButton();

        if (entities != null && !entities.isEmpty()) {
            for (int a = 0, N = entities.size(); a < N; a++) {
                VideoEditedInfo.MediaEntity entity = entities.get(a);
                EntityView view;
                if (entity.type == 0) {
                    StickerView stickerView = createSticker(entity.parentObject, entity.document, false);
                    if ((entity.subType & 2) != 0) {
                        stickerView.mirror();
                    }
                    view = stickerView;
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.width = entity.viewWidth;
                    layoutParams.height = entity.viewHeight;
                } else if (entity.type == 1) {
                    TextPaintView textPaintView = createText(false);
                    int type;
                    if ((entity.subType & 1) != 0) {
                        type = 0;
                    } else if ((entity.subType & 4) != 0) {
                        type = 2;
                    } else {
                        type = 1;
                    }
                    textPaintView.setType(type);
                    textPaintView.setText(entity.text);
                    Swatch swatch = textPaintView.getSwatch();
                    swatch.color = entity.color;
                    textPaintView.setSwatch(swatch);
                    view = textPaintView;
                } else {
                    continue;
                }
                view.setX(entity.x * paintingSize.width - entity.viewWidth * (1 - entity.scale) / 2);
                view.setY(entity.y * paintingSize.height - entity.viewHeight * (1 - entity.scale) / 2);
                view.setPosition(new Point(view.getX() + entity.viewWidth / 2, view.getY() + entity.viewHeight / 2));
                view.setScaleX(entity.scale);
                view.setScaleY(entity.scale);
                view.setRotation((float) (-entity.rotation / Math.PI * 180));
            }
        }
        entitiesView.setVisibility(INVISIBLE);
    }

    public void onResume() {
        renderView.redraw();
    }

    public boolean onTouch(MotionEvent ev) {
        if (currentEntityView != null) {
            if (editingText) {
                closeTextEnter(true);
            } else {
                selectEntity(null);
            }
        }

        float x = renderView.getMeasuredWidth() / 2 + (ev.getX() - renderView.getTranslationX() - getMeasuredWidth() / 2) / renderView.getScaleX();
        float y = renderView.getMeasuredHeight() / 2 + (ev.getY() - renderView.getTranslationY() - getMeasuredHeight() / 2 + AndroidUtilities.dp(32)) / renderView.getScaleY();
        MotionEvent event = MotionEvent.obtain(0, 0, ev.getActionMasked(), x, y, 0);
        renderView.onTouch(event);
        event.recycle();
        return true;
    }

    private Size getPaintingSize() {
        if (paintingSize != null) {
            return paintingSize;
        }
        float width = bitmapToEdit.getWidth();
        float height = bitmapToEdit.getHeight();

        Size size = new Size(width, height);
        size.width = 1280;
        size.height = (float) Math.floor(size.width * height / width);
        if (size.height > 1280) {
            size.height = 1280;
            size.width = (float) Math.floor(size.height * width / height);
        }
        paintingSize = size;
        return size;
    }

    private void updateSettingsButton() {
        int resource = R.drawable.photo_paint_brush;
        if (currentEntityView != null) {
            if (currentEntityView instanceof StickerView) {
                resource = R.drawable.photo_flip;
            } else if (currentEntityView instanceof TextPaintView) {
                resource = R.drawable.photo_outline;
            }
            paintButton.setImageResource(R.drawable.photo_paint);
            paintButton.setColorFilter(null);
        } else {
            if (brushSwatch != null) {
                setCurrentSwatch(brushSwatch, true);
                brushSwatch = null;
            }
            paintButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogFloatingButton), PorterDuff.Mode.MULTIPLY));
            paintButton.setImageResource(R.drawable.photo_paint);
        }

        colorPicker.setSettingsButtonImage(resource);
    }

    public void updateColors() {
        if (paintButton != null && paintButton.getColorFilter() != null) {
            paintButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogFloatingButton), PorterDuff.Mode.MULTIPLY));
        }
        if (doneTextView != null) {
            doneTextView.setTextColor(Theme.getColor(Theme.key_dialogFloatingButton));
        }
    }

    public void init() {
        entitiesView.setVisibility(VISIBLE);
        renderView.setVisibility(View.VISIBLE);
        if (facesBitmap != null) {
            detectFaces();
        }
    }

    public void shutdown() {
        renderView.shutdown();
        entitiesView.setVisibility(GONE);
        selectionContainerView.setVisibility(GONE);

        queue.postRunnable(() -> {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        });
    }

    public FrameLayout getToolsView() {
        return toolsView;
    }

    public TextView getDoneTextView() {
        return doneTextView;
    }

    public TextView getCancelTextView() {
        return cancelTextView;
    }

    public ColorPicker getColorPicker() {
        return colorPicker;
    }

    private boolean hasChanges() {
        return undoStore.canUndo();
    }

    public Bitmap getBitmap(ArrayList<VideoEditedInfo.MediaEntity> entities, Bitmap[] thumbBitmap) {
        Bitmap bitmap = renderView.getResultBitmap();
        lcm = BigInteger.ONE;
        if (bitmap != null && entitiesView.entitiesCount() > 0) {
            Canvas canvas = null;
            int count = entitiesView.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = entitiesView.getChildAt(i);
                if (!(v instanceof EntityView)) {
                    continue;
                }
                EntityView entity = (EntityView) v;
                Point position = entity.getPosition();
                if (entities != null) {
                    VideoEditedInfo.MediaEntity mediaEntity = new VideoEditedInfo.MediaEntity();
                    if (entity instanceof TextPaintView) {
                        mediaEntity.type = 1;
                        TextPaintView textPaintView = (TextPaintView) entity;
                        mediaEntity.text = textPaintView.getText();
                        int type = textPaintView.getType();
                        if (type == 0) {
                            mediaEntity.subType |= 1;
                        } else if (type == 2) {
                            mediaEntity.subType |= 4;
                        }
                        mediaEntity.color = textPaintView.getSwatch().color;
                        mediaEntity.fontSize = textPaintView.getTextSize();
                    } else if (entity instanceof StickerView) {
                        mediaEntity.type = 0;
                        StickerView stickerView = (StickerView) entity;
                        Size size = stickerView.getBaseSize();
                        mediaEntity.width = size.width;
                        mediaEntity.height = size.height;
                        mediaEntity.document = stickerView.getSticker();
                        mediaEntity.parentObject = stickerView.getParentObject();
                        TLRPC.Document document = stickerView.getSticker();
                        mediaEntity.text = FileLoader.getPathToAttach(document, true).getAbsolutePath();
                        if (MessageObject.isAnimatedStickerDocument(document, true)) {
                            mediaEntity.subType |= 1;
                            long duration = stickerView.getDuration();
                            if (duration != 0) {
                                BigInteger x = BigInteger.valueOf(duration);
                                lcm = lcm.multiply(x).divide(lcm.gcd(x));
                            }
                        }
                        if (stickerView.isMirrored()) {
                            mediaEntity.subType |= 2;
                        }
                    } else {
                        continue;
                    }
                    entities.add(mediaEntity);
                    float scaleX = v.getScaleX();
                    float scaleY = v.getScaleY();
                    float x = v.getX();
                    float y = v.getY();
                    mediaEntity.viewWidth = v.getWidth();
                    mediaEntity.viewHeight = v.getHeight();
                    mediaEntity.width = v.getWidth() * scaleX / (float) entitiesView.getMeasuredWidth();
                    mediaEntity.height = v.getHeight() * scaleY / (float) entitiesView.getMeasuredHeight();
                    mediaEntity.x = (x + v.getWidth() * (1 - scaleX) / 2) / entitiesView.getMeasuredWidth();
                    mediaEntity.y = (y + v.getHeight() * (1 - scaleY) / 2) / entitiesView.getMeasuredHeight();
                    mediaEntity.rotation = (float) (-v.getRotation() * (Math.PI / 180));

                    mediaEntity.textViewX = (x + v.getWidth() / 2) / (float) entitiesView.getMeasuredWidth();
                    mediaEntity.textViewY = (y + v.getHeight() / 2) / (float) entitiesView.getMeasuredHeight();
                    mediaEntity.textViewWidth = mediaEntity.viewWidth / (float) entitiesView.getMeasuredWidth();
                    mediaEntity.textViewHeight = mediaEntity.viewHeight / (float) entitiesView.getMeasuredHeight();
                    mediaEntity.scale = scaleX;

                    if (thumbBitmap[0] == null) {
                        thumbBitmap[0] = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                        canvas = new Canvas(thumbBitmap[0]);
                        canvas.drawBitmap(bitmap, 0, 0, null);
                    }
                }
                if (canvas == null) {
                    canvas = new Canvas(bitmap);
                }
                canvas.save();
                canvas.translate(position.x, position.y);
                canvas.scale(v.getScaleX(), v.getScaleY());
                canvas.rotate(v.getRotation());
                canvas.translate(-entity.getWidth() / 2, -entity.getHeight() / 2);
                if (v instanceof TextPaintView) {
                    Bitmap b = Bitmaps.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(b);
                    v.draw(c);
                    canvas.drawBitmap(b, null, new Rect(0, 0, b.getWidth(), b.getHeight()), null);
                    try {
                        c.setBitmap(null);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    b.recycle();
                } else {
                    v.draw(canvas);
                }
                canvas.restore();
            }
        }
        return bitmap;
    }

    public long getLcm() {
        return lcm.longValue();
    }

    public void maybeShowDismissalAlert(PhotoViewer photoViewer, Activity parentActivity, final Runnable okRunnable) {
        if (editingText) {
            closeTextEnter(false);
            return;
        }

        if (hasChanges()) {
            if (parentActivity == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
            builder.setMessage(LocaleController.getString("DiscardChanges", R.string.DiscardChanges));
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> okRunnable.run());
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            photoViewer.showAlertDialog(builder);
        } else {
            okRunnable.run();
        }
    }

    private void setCurrentSwatch(Swatch swatch, boolean updateInterface) {
        renderView.setColor(swatch.color);
        renderView.setBrushSize(swatch.brushWeight);

        if (updateInterface) {
            if (brushSwatch == null && paintButton.getColorFilter() != null) {
                brushSwatch = colorPicker.getSwatch();
            }
            colorPicker.setSwatch(swatch);
        }

        if (currentEntityView instanceof TextPaintView) {
            ((TextPaintView) currentEntityView).setSwatch(swatch);
        }
    }

    private void setDimVisibility(final boolean visible) {
        Animator animator;
        if (visible) {
            dimView.setVisibility(VISIBLE);
            animator = ObjectAnimator.ofFloat(dimView, View.ALPHA, 0.0f, 1.0f);
        } else {
            animator = ObjectAnimator.ofFloat(dimView, View.ALPHA, 1.0f, 0.0f);
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!visible) {
                    dimView.setVisibility(GONE);
                }
            }
        });
        animator.setDuration(200);
        animator.start();
    }

    private void setTextDimVisibility(final boolean visible, EntityView view) {
        Animator animator;

        if (visible && view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (textDimView.getParent() != null) {
                ((EntitiesContainerView) textDimView.getParent()).removeView(textDimView);
            }
            parent.addView(textDimView, parent.indexOfChild(view));
        }

        view.setSelectionVisibility(!visible);

        if (visible) {
            textDimView.setVisibility(VISIBLE);
            animator = ObjectAnimator.ofFloat(textDimView, View.ALPHA, 0.0f, 1.0f);
        } else {
            animator = ObjectAnimator.ofFloat(textDimView, View.ALPHA, 1.0f, 0.0f);
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!visible) {
                    textDimView.setVisibility(GONE);
                    if (textDimView.getParent() != null) {
                        ((EntitiesContainerView) textDimView.getParent()).removeView(textDimView);
                    }
                }
            }
        });
        animator.setDuration(200);
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ignoreLayout = true;
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);

        float bitmapW;
        float bitmapH;
        int fullHeight = AndroidUtilities.displaySize.y - ActionBar.getCurrentActionBarHeight();
        int maxHeight = fullHeight - AndroidUtilities.dp(48);
        if (bitmapToEdit != null) {
            bitmapW = bitmapToEdit.getWidth();
            bitmapH = bitmapToEdit.getHeight();
        } else {
            bitmapW = width;
            bitmapH = height - ActionBar.getCurrentActionBarHeight() - AndroidUtilities.dp(48);
        }

        float renderWidth = width;
        float renderHeight = (float) Math.floor(renderWidth * bitmapH / bitmapW);
        if (renderHeight > maxHeight) {
            renderHeight = maxHeight;
            renderWidth = (float) Math.floor(renderHeight * bitmapW / bitmapH);
        }

        renderView.measure(MeasureSpec.makeMeasureSpec((int) renderWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) renderHeight, MeasureSpec.EXACTLY));

        baseScale = renderWidth / paintingSize.width;
        entitiesView.setScaleX(baseScale);
        entitiesView.setScaleY(baseScale);
        entitiesView.measure(MeasureSpec.makeMeasureSpec((int) paintingSize.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) paintingSize.height, MeasureSpec.EXACTLY));
        dimView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        if (currentEntityView != null) {
            currentEntityView.updateSelectionView();
        }
        selectionContainerView.measure(MeasureSpec.makeMeasureSpec((int) renderWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) renderHeight, MeasureSpec.EXACTLY));
        colorPicker.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
        toolsView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
        curtainView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
        ignoreLayout = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        int status = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
        int actionBarHeight = ActionBar.getCurrentActionBarHeight();
        int actionBarHeight2 = ActionBar.getCurrentActionBarHeight() + status;

        int maxHeight = AndroidUtilities.displaySize.y - actionBarHeight - AndroidUtilities.dp(48);

        int x = (int) Math.ceil((width - renderView.getMeasuredWidth()) / 2);
        int y = actionBarHeight2 + (height - actionBarHeight2 - AndroidUtilities.dp(48) - renderView.getMeasuredHeight()) / 2 - ActionBar.getCurrentActionBarHeight() + AndroidUtilities.dp(8);

        renderView.layout(x, y, x + renderView.getMeasuredWidth(), y + renderView.getMeasuredHeight());
        int x2 = x + (renderView.getMeasuredWidth() - entitiesView.getMeasuredWidth()) / 2;
        int y2 = y + (renderView.getMeasuredHeight() - entitiesView.getMeasuredHeight()) / 2;
        entitiesView.layout(x2, y2, x2 + entitiesView.getMeasuredWidth(), y2 + entitiesView.getMeasuredHeight());
        dimView.layout(0, status, dimView.getMeasuredWidth(), status + dimView.getMeasuredHeight());
        selectionContainerView.layout(x, y, x + selectionContainerView.getMeasuredWidth(), y + selectionContainerView.getMeasuredHeight());
        colorPicker.layout(0, actionBarHeight2, colorPicker.getMeasuredWidth(), actionBarHeight2 + colorPicker.getMeasuredHeight());
        toolsView.layout(0, height - toolsView.getMeasuredHeight(), toolsView.getMeasuredWidth(), height);
        curtainView.layout(0, y, curtainView.getMeasuredWidth(), y + curtainView.getMeasuredHeight());
    }

    @Override
    public void requestLayout() {
        if (ignoreLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    public boolean onEntitySelected(EntityView entityView) {
        return selectEntity(entityView);
    }

    @Override
    public boolean onEntityLongClicked(EntityView entityView) {
        showMenuForEntity(entityView);
        return true;
    }

    @Override
    public boolean allowInteraction(EntityView entityView) {
        return !editingText;
    }

    private Point centerPositionForEntity() {
        Size paintingSize = getPaintingSize();
        return new Point(paintingSize.width / 2.0f, paintingSize.height / 2.0f);
    }

    private Point startPositionRelativeToEntity(EntityView entityView) {
        final float offset = 200.0f;

        if (entityView != null) {
            Point position = entityView.getPosition();
            return new Point(position.x + offset, position.y + offset);
        } else {
            final float minimalDistance = 100.0f;
            Point position = centerPositionForEntity();
            while (true) {
                boolean occupied = false;
                for (int index = 0; index < entitiesView.getChildCount(); index++) {
                    View view = entitiesView.getChildAt(index);
                    if (!(view instanceof EntityView))
                        continue;

                    Point location = ((EntityView) view).getPosition();
                    float distance = (float) Math.sqrt(Math.pow(location.x - position.x, 2) + Math.pow(location.y - position.y, 2));
                    if (distance < minimalDistance) {
                        occupied = true;
                    }
                }

                if (!occupied) {
                    break;
                } else {
                    position = new Point(position.x + offset, position.y + offset);
                }
            }
            return position;
        }
    }

    public ArrayList<TLRPC.InputDocument> getMasks() {
        ArrayList<TLRPC.InputDocument> result = null;
        int count = entitiesView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = entitiesView.getChildAt(a);
            if (child instanceof StickerView) {
                TLRPC.Document document = ((StickerView) child).getSticker();
                if (result == null) {
                    result = new ArrayList<>();
                }
                TLRPC.TL_inputDocument inputDocument = new TLRPC.TL_inputDocument();
                inputDocument.id = document.id;
                inputDocument.access_hash = document.access_hash;
                inputDocument.file_reference = document.file_reference;
                if (inputDocument.file_reference == null) {
                    inputDocument.file_reference = new byte[0];
                }
                result.add(inputDocument);
            }
        }
        return result;
    }

    public void setTransform(float scale, float trX, float trY) {
        for (int a = 0; a < 3; a++) {
            View view;
            if (a == 0) {
                view = entitiesView;
                view.setScaleX(baseScale * scale);
                view.setScaleY(baseScale * scale);
            } else {
                if (a == 1) {
                    view = selectionContainerView;
                } else {
                    view = renderView;
                }
                view.setScaleX(scale);
                view.setScaleY(scale);
            }
            view.setTranslationX(trX);
            view.setTranslationY(trY);
        }
    }

    private boolean selectEntity(EntityView entityView) {
        boolean changed = false;

        if (currentEntityView != null) {
            if (currentEntityView == entityView) {
                if (!editingText) {
                    showMenuForEntity(currentEntityView);
                }
                return true;
            } else {
                currentEntityView.deselect();
            }
            changed = true;
        }

        EntityView oldEntity = currentEntityView;
        currentEntityView = entityView;
        if (oldEntity instanceof TextPaintView) {
            TextPaintView textPaintView = (TextPaintView) oldEntity;
            if (TextUtils.isEmpty(textPaintView.getText())) {
                removeEntity(oldEntity);
            }
        }

        if (currentEntityView != null) {
            currentEntityView.select(selectionContainerView);
            entitiesView.bringViewToFront(currentEntityView);

            if (currentEntityView instanceof TextPaintView) {
                setCurrentSwatch(((TextPaintView) currentEntityView).getSwatch(), true);
            }

            changed = true;
        }

        updateSettingsButton();

        return changed;
    }

    private void removeEntity(EntityView entityView) {
        if (entityView == currentEntityView) {
            currentEntityView.deselect();
            if (editingText) {
                closeTextEnter(false);
            }
            currentEntityView = null;
            updateSettingsButton();
        }
        entitiesView.removeView(entityView);
        undoStore.unregisterUndo(entityView.getUUID());
    }

    private void duplicateSelectedEntity() {
        if (currentEntityView == null) {
            return;
        }

        EntityView entityView = null;
        Point position = startPositionRelativeToEntity(currentEntityView);

        if (currentEntityView instanceof StickerView) {
            StickerView newStickerView = new StickerView(getContext(), (StickerView) currentEntityView, position);
            newStickerView.setDelegate(this);
            entitiesView.addView(newStickerView);
            entityView = newStickerView;
        } else if (currentEntityView instanceof TextPaintView) {
            TextPaintView newTextPaintView = new TextPaintView(getContext(), (TextPaintView) currentEntityView, position);
            newTextPaintView.setDelegate(this);
            newTextPaintView.setMaxWidth((int) (getPaintingSize().width - 20));
            entitiesView.addView(newTextPaintView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            entityView = newTextPaintView;
        }

        registerRemovalUndo(entityView);
        selectEntity(entityView);

        updateSettingsButton();
    }

    private void openStickersView() {
        StickerMasksAlert stickerMasksAlert = new StickerMasksAlert(getContext(), facesBitmap == null);
        stickerMasksAlert.setDelegate((parentObject, sticker) -> createSticker(parentObject, sticker, true));
        stickerMasksAlert.setOnDismissListener(dialog -> onOpenCloseStickersAlert(false));
        stickerMasksAlert.show();
        onOpenCloseStickersAlert(true);
    }

    protected void onOpenCloseStickersAlert(boolean open) {

    }

    private Size baseStickerSize() {
        float side = (float) Math.floor(getPaintingSize().width * 0.5);
        return new Size(side, side);
    }

    private void registerRemovalUndo(final EntityView entityView) {
        undoStore.registerUndo(entityView.getUUID(), () -> removeEntity(entityView));
    }

    private StickerView createSticker(Object parentObject, TLRPC.Document sticker, boolean select) {
        StickerPosition position = calculateStickerPosition(sticker);
        StickerView view = new StickerView(getContext(), position.position, position.angle, position.scale, baseStickerSize(), sticker, parentObject);
        view.setDelegate(this);
        entitiesView.addView(view);
        if (select) {
            registerRemovalUndo(view);
            selectEntity(view);
        }
        return view;
    }

    private void mirrorSticker() {
        if (currentEntityView instanceof StickerView) {
            ((StickerView) currentEntityView).mirror();
        }
    }

    private int baseFontSize() {
        return (int) (getPaintingSize().width / 9);
    }

    private TextPaintView createText(boolean select) {
        Swatch currentSwatch = colorPicker.getSwatch();
        Swatch swatch;
        if (selectedTextType == 0) {
            swatch = new Swatch(Color.BLACK, 0.85f, currentSwatch.brushWeight);
        } else if (selectedTextType == 1) {
            swatch = new Swatch(Color.WHITE, 1.0f, currentSwatch.brushWeight);
        } else {
            swatch = new Swatch(Color.WHITE, 1.0f, currentSwatch.brushWeight);
        }

        TextPaintView view = new TextPaintView(getContext(), startPositionRelativeToEntity(null), baseFontSize(), "", swatch, selectedTextType);
        view.setDelegate(this);
        view.setMaxWidth((int) (getPaintingSize().width - 20));
        entitiesView.addView(view, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        if (select) {
            registerRemovalUndo(view);
            selectEntity(view);
            editSelectedTextEntity();
        }
        setCurrentSwatch(swatch, true);
        return view;
    }

    private void editSelectedTextEntity() {
        if (!(currentEntityView instanceof TextPaintView) || editingText) {
            return;
        }

        curtainView.setVisibility(View.VISIBLE);

        final TextPaintView textPaintView = (TextPaintView) currentEntityView;
        initialText = textPaintView.getText();
        editingText = true;

        editedTextPosition = textPaintView.getPosition();
        editedTextRotation = textPaintView.getRotation();
        editedTextScale = textPaintView.getScale();

        textPaintView.setPosition(centerPositionForEntity());
        textPaintView.setRotation(0.0f);
        textPaintView.setScale(1.0f);

        toolsView.setVisibility(GONE);

        setTextDimVisibility(true, textPaintView);
        textPaintView.beginEditing();
        View view = textPaintView.getFocusedView();
        view.requestFocus();
        AndroidUtilities.showKeyboard(view);
    }

    public void closeTextEnter(boolean apply) {
        if (!editingText || !(currentEntityView instanceof TextPaintView)) {
            return;
        }

        TextPaintView textPaintView = (TextPaintView) currentEntityView;

        toolsView.setVisibility(VISIBLE);

        AndroidUtilities.hideKeyboard(textPaintView.getFocusedView());

        textPaintView.getFocusedView().clearFocus();
        textPaintView.endEditing();

        if (!apply) {
            textPaintView.setText(initialText);
        }

        if (textPaintView.getText().trim().length() == 0) {
            entitiesView.removeView(textPaintView);
            selectEntity(null);
        } else {
            textPaintView.setPosition(editedTextPosition);
            textPaintView.setRotation(editedTextRotation);
            textPaintView.setScale(editedTextScale);

            editedTextPosition = null;
            editedTextRotation = 0.0f;
            editedTextScale = 0.0f;
        }

        setTextDimVisibility(false, textPaintView);

        editingText = false;
        initialText = null;

        curtainView.setVisibility(View.GONE);
    }

    private void setBrush(int brush) {
        renderView.setBrush(brushes[currentBrush = brush]);
    }

    private void setType(int type) {
        selectedTextType = type;
        if (currentEntityView instanceof TextPaintView) {
            Swatch currentSwatch = colorPicker.getSwatch();
            if (type == 0 && currentSwatch.color == Color.WHITE) {
                Swatch blackSwatch = new Swatch(Color.BLACK, 0.85f, currentSwatch.brushWeight);
                setCurrentSwatch(blackSwatch, true);
            } else if ((type == 1 || type == 2) && currentSwatch.color == Color.BLACK) {
                Swatch blackSwatch = new Swatch(Color.WHITE, 1.0f, currentSwatch.brushWeight);
                setCurrentSwatch(blackSwatch, true);
            }
            ((TextPaintView) currentEntityView).setType(type);
        }
    }

    private void showMenuForEntity(final EntityView entityView) {
        int x = (int) ((entityView.getPosition().x - entitiesView.getWidth() / 2) * entitiesView.getScaleX());
        int y = (int) ((entityView.getPosition().y - entityView.getHeight() * entityView.getScale() / 2 - entitiesView.getHeight() / 2) * entitiesView.getScaleY()) - AndroidUtilities.dp(32);

        showPopup(() -> {
            LinearLayout parent = new LinearLayout(getContext());
            parent.setOrientation(LinearLayout.HORIZONTAL);

            TextView deleteView = new TextView(getContext());
            deleteView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            deleteView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
            deleteView.setGravity(Gravity.CENTER_VERTICAL);
            deleteView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(14), 0);
            deleteView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            deleteView.setTag(0);
            deleteView.setText(LocaleController.getString("PaintDelete", R.string.PaintDelete));
            deleteView.setOnClickListener(v -> {
                removeEntity(entityView);

                if (popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss(true);
                }
            });
            parent.addView(deleteView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));

            if (entityView instanceof TextPaintView) {
                TextView editView = new TextView(getContext());
                editView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
                editView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
                editView.setGravity(Gravity.CENTER_VERTICAL);
                editView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
                editView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                editView.setTag(1);
                editView.setText(LocaleController.getString("PaintEdit", R.string.PaintEdit));
                editView.setOnClickListener(v -> {
                    editSelectedTextEntity();

                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss(true);
                    }
                });
                parent.addView(editView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));
            }

            TextView duplicateView = new TextView(getContext());
            duplicateView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            duplicateView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
            duplicateView.setGravity(Gravity.CENTER_VERTICAL);
            duplicateView.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(16), 0);
            duplicateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            duplicateView.setTag(2);
            duplicateView.setText(LocaleController.getString("PaintDuplicate", R.string.PaintDuplicate));
            duplicateView.setOnClickListener(v -> {
                duplicateSelectedEntity();

                if (popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss(true);
                }
            });
            parent.addView(duplicateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));

            popupLayout.addView(parent);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) parent.getLayoutParams();
            params.width = LayoutHelper.WRAP_CONTENT;
            params.height = LayoutHelper.WRAP_CONTENT;
            parent.setLayoutParams(params);
        }, entityView, Gravity.CENTER, x, y);
    }

    private FrameLayout buttonForBrush(final int brush, int resource, boolean applyColor, boolean selected) {
        FrameLayout button = new FrameLayout(getContext());
        button.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        button.setOnClickListener(v -> {
            setBrush(brush);

            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss(true);
            }
        });

        ImageView preview = new ImageView(getContext());
        preview.setImageResource(resource);
        if (applyColor) {
            preview.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), PorterDuff.Mode.SRC_IN));
        }
        button.addView(preview, LayoutHelper.createFrame(165, 44, Gravity.LEFT | Gravity.CENTER_VERTICAL, 46, 0, 8, 0));

        if (selected) {
            ImageView check = new ImageView(getContext());
            check.setImageResource(R.drawable.ic_ab_done);
            check.setScaleType(ImageView.ScaleType.CENTER);
            check.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogFloatingButton), PorterDuff.Mode.MULTIPLY));
            button.addView(check, LayoutHelper.createFrame(50, LayoutHelper.MATCH_PARENT));
        }

        return button;
    }

    private void showBrushSettings() {
        showPopup(() -> {
            View radial = buttonForBrush(0, R.drawable.paint_radial_preview, true, currentBrush == 0);
            popupLayout.addView(radial);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) radial.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = AndroidUtilities.dp(52);
            radial.setLayoutParams(layoutParams);

            View elliptical = buttonForBrush(1, R.drawable.paint_elliptical_preview, true, currentBrush == 1);
            popupLayout.addView(elliptical);

            layoutParams = (LinearLayout.LayoutParams) elliptical.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = AndroidUtilities.dp(52);
            elliptical.setLayoutParams(layoutParams);

            View neon = buttonForBrush(2, R.drawable.paint_neon_preview, false, currentBrush == 2);
            popupLayout.addView(neon);

            layoutParams = (LinearLayout.LayoutParams) neon.getLayoutParams();
            layoutParams.width = LayoutHelper.MATCH_PARENT;
            layoutParams.height = AndroidUtilities.dp(52);
            neon.setLayoutParams(layoutParams);
        }, this, Gravity.RIGHT | Gravity.BOTTOM, 0, AndroidUtilities.dp(48));
    }

    private LinearLayout buttonForText(int type, String text, int icon, boolean selected) {
        LinearLayout button = new LinearLayout(getContext()) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return true;
            }
        };
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        button.setOnClickListener(v -> {
            setType(type);

            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss(true);
            }
        });

        ImageView imageView = new ImageView(getContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(icon);
        imageView.setColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        button.addView(imageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 16, 0));

        TextView textView = new TextView(getContext());
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setText(text);
        button.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        if (selected) {
            ImageView check = new ImageView(getContext());
            check.setImageResource(R.drawable.msg_text_check);
            check.setScaleType(ImageView.ScaleType.CENTER);
            check.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_radioBackgroundChecked), PorterDuff.Mode.MULTIPLY));
            button.addView(check, LayoutHelper.createFrame(50, LayoutHelper.MATCH_PARENT));
        }

        return button;
    }

    private void showTextSettings() {
        showPopup(() -> {
            for (int a = 0; a < 3; a++) {
                String text;
                int icon;
                if (a == 0) {
                    text = LocaleController.getString("PaintOutlined", R.string.PaintOutlined);
                    icon = R.drawable.msg_text_outlined;
                } else if (a == 1) {
                    text = LocaleController.getString("PaintRegular", R.string.PaintRegular);
                    icon = R.drawable.msg_text_regular;
                } else {
                    text = LocaleController.getString("PaintFramed", R.string.PaintFramed);
                    icon = R.drawable.msg_text_framed;
                }
                popupLayout.addView(buttonForText(a, text, icon, selectedTextType == a), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            }
        }, this, Gravity.RIGHT | Gravity.BOTTOM, 0, AndroidUtilities.dp(48));
    }

    private void showPopup(Runnable setupRunnable, View parent, int gravity, int x, int y) {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }

        if (popupLayout == null) {
            popupRect = new android.graphics.Rect();
            popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext());
            popupLayout.setAnimationEnabled(false);
            popupLayout.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (popupWindow != null && popupWindow.isShowing()) {
                        v.getHitRect(popupRect);
                        if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
                            popupWindow.dismiss();
                        }
                    }
                }
                return false;
            });
            popupLayout.setDispatchKeyEventListener(keyEvent -> {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                }
            });
            popupLayout.setShowedFromBotton(true);
        }

        popupLayout.removeInnerViews();
        setupRunnable.run();

        if (popupWindow == null) {
            popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            popupWindow.setAnimationEnabled(false);
            popupWindow.setAnimationStyle(R.style.PopupAnimation);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
            popupWindow.getContentView().setFocusableInTouchMode(true);
            popupWindow.setOnDismissListener(() -> popupLayout.removeInnerViews());
        }

        popupLayout.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST));

        popupWindow.setFocusable(true);

        popupWindow.showAtLocation(parent, gravity, x, y);
        popupWindow.startAnimation();
    }

    private int getFrameRotation() {
        switch (originalBitmapRotation) {
            case 90: {
                return Frame.ROTATION_90;
            }

            case 180: {
                return Frame.ROTATION_180;
            }

            case 270: {
                return Frame.ROTATION_270;
            }

            default: {
                return Frame.ROTATION_0;
            }
        }
    }

    private boolean isSidewardOrientation() {
        return originalBitmapRotation % 360 == 90 || originalBitmapRotation % 360 == 270;
    }

    private void detectFaces() {
        queue.postRunnable(() -> {
            FaceDetector faceDetector = null;
            try {
                faceDetector = new FaceDetector.Builder(getContext())
                        .setMode(FaceDetector.ACCURATE_MODE)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setTrackingEnabled(false).build();
                if (!faceDetector.isOperational()) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.e("face detection is not operational");
                    }
                    return;
                }

                Frame frame = new Frame.Builder().setBitmap(facesBitmap).setRotation(getFrameRotation()).build();
                SparseArray<Face> faces;
                try {
                    faces = faceDetector.detect(frame);
                } catch (Throwable e) {
                    FileLog.e(e);
                    return;
                }
                ArrayList<PhotoFace> result = new ArrayList<>();
                Size targetSize = getPaintingSize();
                for (int i = 0; i < faces.size(); i++) {
                    int key = faces.keyAt(i);
                    Face f = faces.get(key);
                    PhotoFace face = new PhotoFace(f, facesBitmap, targetSize, isSidewardOrientation());
                    if (face.isSufficient()) {
                        result.add(face);
                    }
                }
                PhotoPaintView.this.faces = result;
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (faceDetector != null) {
                    faceDetector.release();
                }
            }
        });
    }

    private StickerPosition calculateStickerPosition(TLRPC.Document document) {
        TLRPC.TL_maskCoords maskCoords = null;

        for (int a = 0; a < document.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                maskCoords = attribute.mask_coords;
                break;
            }
        }

        StickerPosition defaultPosition = new StickerPosition(centerPositionForEntity(), 0.75f, 0.0f);
        if (maskCoords == null || faces == null || faces.size() == 0) {
            return defaultPosition;
        } else {
            int anchor = maskCoords.n;

            PhotoFace face = getRandomFaceWithVacantAnchor(anchor, document.id, maskCoords);
            if (face == null) {
                return defaultPosition;
            }

            Point referencePoint = face.getPointForAnchor(anchor);
            float referenceWidth = face.getWidthForAnchor(anchor);
            float angle = face.getAngle();
            Size baseSize = baseStickerSize();

            float scale = (float) (referenceWidth / baseSize.width * maskCoords.zoom);

            float radAngle = (float) Math.toRadians(angle);
            float xCompX = (float) (Math.sin(Math.PI / 2.0f - radAngle) * referenceWidth * maskCoords.x);
            float xCompY = (float) (Math.cos(Math.PI / 2.0f - radAngle) * referenceWidth * maskCoords.x);

            float yCompX = (float) (Math.cos(Math.PI / 2.0f + radAngle) * referenceWidth * maskCoords.y);
            float yCompY = (float) (Math.sin(Math.PI / 2.0f + radAngle) * referenceWidth * maskCoords.y);

            float x = referencePoint.x + xCompX + yCompX;
            float y = referencePoint.y + xCompY + yCompY;

            return new StickerPosition(new Point(x, y), scale, angle);
        }
    }

    private PhotoFace getRandomFaceWithVacantAnchor(int anchor, long documentId, TLRPC.TL_maskCoords maskCoords) {
        if (anchor < 0 || anchor > 3 || faces.isEmpty()) {
            return null;
        }

        int count = faces.size();
        int randomIndex = Utilities.random.nextInt(count);
        int remaining = count;

        PhotoFace selectedFace = null;
        for (int i = randomIndex; remaining > 0; i = (i + 1) % count, remaining--) {
            PhotoFace face = faces.get(i);
            if (!isFaceAnchorOccupied(face, anchor, documentId, maskCoords)) {
                return face;
            }
        }

        return selectedFace;
    }

    private boolean isFaceAnchorOccupied(PhotoFace face, int anchor, long documentId, TLRPC.TL_maskCoords maskCoords) {
        Point anchorPoint = face.getPointForAnchor(anchor);
        if (anchorPoint == null) {
            return true;
        }

        float minDistance = face.getWidthForAnchor(0) * 1.1f;

        for (int index = 0; index < entitiesView.getChildCount(); index++) {
            View view = entitiesView.getChildAt(index);
            if (!(view instanceof StickerView)) {
                continue;
            }

            StickerView stickerView = (StickerView) view;
            if (stickerView.getAnchor() != anchor) {
                continue;
            }

            Point location = stickerView.getPosition();
            float distance = (float)Math.hypot(location.x - anchorPoint.x, location.y - anchorPoint.y);
            if ((documentId == stickerView.getSticker().id || faces.size() > 1) && distance < minDistance) {
                return true;
            }
        }

        return false;
    }

    private static class StickerPosition {
        private Point position;
        private float scale;
        private float angle;

        StickerPosition(Point position, float scale, float angle) {
            this.position = position;
            this.scale = scale;
            this.angle = angle;
        }
    }
}