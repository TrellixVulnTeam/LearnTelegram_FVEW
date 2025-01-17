package com.demo.chat.model.action;

import android.net.Uri;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.SparseArray;

import com.demo.chat.PhoneFormat.PhoneFormat;
import com.demo.chat.R;
import com.demo.chat.controller.ConnectionsManager;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.MediaDataController;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.Emoji;
import com.demo.chat.messager.EmojiData;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.SerializedData;
import com.demo.chat.messager.SharedConfig;
import com.demo.chat.messager.Utilities;
import com.demo.chat.messager.browser.Browser;
import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.User;
import com.demo.chat.model.UserChat;
import com.demo.chat.model.VideoEditedInfo;
import com.demo.chat.model.bot.BotInlineResult;
import com.demo.chat.model.bot.KeyboardButton;
import com.demo.chat.model.bot.ReactionCount;
import com.demo.chat.model.message.MessageReactions;
import com.demo.chat.model.reply.ReplyMarkup;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.Media;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.model.small.MessageMedia;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.model.small.VideoSize;
import com.demo.chat.model.small.WebDocument;
import com.demo.chat.model.small.WebFile;
import com.demo.chat.model.sticker.InputStickerSet;
import com.demo.chat.model.text.Block;
import com.demo.chat.model.text.PageBlock;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.Cells.ChatMessageCell;
import com.demo.chat.ui.Components.TextStyleSpan;
import com.demo.chat.ui.Components.URLSpanBotCommand;
import com.demo.chat.ui.Components.URLSpanBrowser;
import com.demo.chat.ui.Components.URLSpanMono;
import com.demo.chat.ui.Components.URLSpanNoUnderline;
import com.demo.chat.ui.Components.URLSpanNoUnderlineBold;
import com.demo.chat.ui.Components.URLSpanReplacement;
import com.demo.chat.ui.Components.URLSpanUserMention;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.IntDef;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description 消息
 * @usage null
 */
public class MessageObject {

    public static final int MESSAGE_SEND_STATE_SENT = 0;
    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;
    public static final int MESSAGE_SEND_STATE_EDITING = 3;

    public static final int TYPE_PHOTO = 1;
    public static final int TYPE_VIDEO = 3;
    public static final int TYPE_ROUND_VIDEO = 5;
    public static final int TYPE_STICKER = 13;
    public static final int TYPE_ANIMATED_STICKER = 15;
    public static final int TYPE_POLL = 17;

    public int localType;
    public String localName;
    public String localUserName;
    public long localGroupId;
    public long localSentGroupId;
    public boolean localChannel;
    public boolean localEdit;
    public Message messageOwner;
    public Document emojiAnimatedSticker;
    public String emojiAnimatedStickerColor;
    public CharSequence messageText;
    public CharSequence linkDescription;
    public CharSequence caption;
    public MessageObject replyMessageObject;
    /**
     * 图片 TYPE_PHOTO = 1;<br>
     *
     * 视频 TYPE_VIDEO = 3;<br>
     *
     * 圆形视频 TYPE_ROUND_VIDEO = 5;<br>
     *
     * 表情包 TYPE_STICKER = 13;<br>
     *
     * 表情包 TYPE_ANIMATED_STICKER = 15;<br>
     *
     * 投票 TYPE_POLL = 17;<br>
     */
    public int type = 1000;
    private int isRoundVideoCached;
    public long eventId;
    public int contentType;
    public String dateKey;
    public String monthKey;
    public boolean deleted;
    public float audioProgress;
    public float forceSeekTo = -1;
    public int audioProgressMs;
    public float bufferedProgress;
    public float gifState;
    public int audioProgressSec;
    public int audioPlayerDuration;
    public boolean isDateObject;
    public Media photoThumbsObject;
    public Media photoThumbsObject2;
    public ArrayList<PhotoSize> photoThumbs;
    public ArrayList<PhotoSize> photoThumbs2;
    public VideoEditedInfo videoEditedInfo;
    public boolean shouldRemoveVideoEditedInfo;
    public boolean viewsReloaded;
    public boolean pollVisibleOnScreen;
    public long pollLastCheckTime;
    public int wantedBotKeyboardWidth;
    public boolean attachPathExists;
    public boolean mediaExists;
    public boolean resendAsIs;
    public String customReplyName;
    public boolean useCustomPhoto;
    public StringBuilder botButtonsLayout;
    public boolean isRestrictedMessage;
    public long loadedFileSize;

    public int stableId;

    public boolean wasUnread;

    public boolean hadAnimationNotReadyLoading;

    public boolean cancelEditing;

    public boolean scheduled;

    public CharSequence editingMessage;
    public ArrayList<MessageEntity> editingMessageEntities;

    public String previousCaption;
    public MessageMedia previousMedia;
    public ArrayList<MessageEntity> previousCaptionEntities;
    public String previousAttachPath;

    public int currentAccount;

    public boolean forceUpdate;

    public int lastLineWidth;
    public int textWidth;
    public int textHeight;
    public boolean hasRtl;
    public float textXOffset;
    public int linesCount;

    private int emojiOnlyCount;
    private boolean layoutCreated;
    private int generatedWithMinSize;
    private float generatedWithDensity;

    public static Pattern urlPattern;
    public static Pattern instagramUrlPattern;
    public static Pattern videoTimeUrlPattern;

    public CharSequence vCardData;

    static final String[] excludeWords = new String[]{
            " vs. ",
            " vs ",
            " versus ",
            " ft. ",
            " ft ",
            " featuring ",
            " feat. ",
            " feat ",
            " presents ",
            " pres. ",
            " pres ",
            " and ",
            " & ",
            " . "
    };

    public static class VCardData {

        private String company;
        private ArrayList<String> emails = new ArrayList<>();
        private ArrayList<String> phones = new ArrayList<>();

        public static CharSequence parse(String data) {
            try {
                VCardData currentData = null;
                boolean finished = false;
                BufferedReader bufferedReader = new BufferedReader(new StringReader(data));

                String line;
                String originalLine;
                String pendingLine = null;
                while ((originalLine = line = bufferedReader.readLine()) != null) {
                    if (originalLine.startsWith("PHOTO")) {
                        continue;
                    } else {
                        if (originalLine.indexOf(':') >= 0) {
                            if (originalLine.startsWith("BEGIN:VCARD")) {
                                currentData = new VCardData();
                            } else if (originalLine.startsWith("END:VCARD")) {
                                if (currentData != null) {
                                    finished = true;
                                }
                            }
                        }
                    }
                    if (pendingLine != null) {
                        pendingLine += line;
                        line = pendingLine;
                        pendingLine = null;
                    }
                    if (line.contains("=QUOTED-PRINTABLE") && line.endsWith("=")) {
                        pendingLine = line.substring(0, line.length() - 1);
                        continue;
                    }
                    int idx = line.indexOf(":");
                    String[] args;
                    if (idx >= 0) {
                        args = new String[]{
                                line.substring(0, idx),
                                line.substring(idx + 1).trim()
                        };
                    } else {
                        args = new String[]{line.trim()};
                    }
                    if (args.length < 2 || currentData == null) {
                        continue;
                    }
                    if (args[0].startsWith("ORG")) {
                        String nameEncoding = null;
                        String nameCharset = null;
                        String[] params = args[0].split(";");
                        for (String param : params) {
                            String[] args2 = param.split("=");
                            if (args2.length != 2) {
                                continue;
                            }
                            if (args2[0].equals("CHARSET")) {
                                nameCharset = args2[1];
                            } else if (args2[0].equals("ENCODING")) {
                                nameEncoding = args2[1];
                            }
                        }
                        currentData.company = args[1];
                        if (nameEncoding != null && nameEncoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
                            byte[] bytes = AndroidUtilities.decodeQuotedPrintable(AndroidUtilities.getStringBytes(currentData.company));
                            if (bytes != null && bytes.length != 0) {
                                String decodedName = new String(bytes, nameCharset);
                                if (decodedName != null) {
                                    currentData.company = decodedName;
                                }
                            }
                        }
                        currentData.company = currentData.company.replace(';', ' ');
                    } else if (args[0].startsWith("TEL")) {
                        if (args[1].length() > 0) {
                            currentData.phones.add(args[1]);
                        }
                    } else if (args[0].startsWith("EMAIL")) {
                        String email = args[1];
                        if (email.length() > 0) {
                            currentData.emails.add(email);
                        }
                    }
                }
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (finished) {
                    StringBuilder result = new StringBuilder();
                    for (int a = 0; a < currentData.phones.size(); a++) {
                        if (result.length() > 0) {
                            result.append('\n');
                        }
                        String phone = currentData.phones.get(a);
                        if (phone.contains("#") || phone.contains("*")) {
                            result.append(phone);
                        } else {
                            result.append(PhoneFormat.getInstance().format(phone));
                        }
                    }
                    for (int a = 0; a < currentData.emails.size(); a++) {
                        if (result.length() > 0) {
                            result.append('\n');
                        }
                        result.append(PhoneFormat.getInstance().format(currentData.emails.get(a)));
                    }
                    if (!TextUtils.isEmpty(currentData.company)) {
                        if (result.length() > 0) {
                            result.append('\n');
                        }
                        result.append(currentData.company);
                    }
                    return result;
                }
            } catch (Throwable ignore) {

            }
            return null;
        }
    }

    public static class TextLayoutBlock {
        public StaticLayout textLayout;
        public float textYOffset;
        public int charactersOffset;
        public int charactersEnd;
        public int height;
        public int heightByOffset;
        public byte directionFlags;

        public boolean isRtl() {
            return (directionFlags & 1) != 0 && (directionFlags & 2) == 0;
        }
    }

    public static final int POSITION_FLAG_LEFT = 1;
    public static final int POSITION_FLAG_RIGHT = 2;
    public static final int POSITION_FLAG_TOP = 4;
    public static final int POSITION_FLAG_BOTTOM = 8;

    public static class GroupedMessagePosition {
        public byte minX;
        public byte maxX;
        public byte minY;
        public byte maxY;
        public int pw;
        public float ph;
        public float aspectRatio;
        public boolean last;
        public int spanSize;
        public int leftSpanOffset;
        public boolean edge;
        public int flags;
        public float[] siblingHeights;

        public void set(int minX, int maxX, int minY, int maxY, int w, float h, int flags) {
            this.minX = (byte) minX;
            this.maxX = (byte) maxX;
            this.minY = (byte) minY;
            this.maxY = (byte) maxY;
            this.pw = w;
            this.spanSize = w;
            this.ph = h;
            this.flags = (byte) flags;
        }
    }

    public static class GroupedMessages {
        public long groupId;
        public boolean hasSibling;
        public boolean hasCaption;
        public ArrayList<MessageObject> messages = new ArrayList<>();
        public ArrayList<GroupedMessagePosition> posArray = new ArrayList<>();
        public HashMap<MessageObject, GroupedMessagePosition> positions = new HashMap<>();

        private int maxSizeWidth = 800;

        public final TransitionParams transitionParams = new TransitionParams();

        private static class MessageGroupedLayoutAttempt {

            public int[] lineCounts;
            public float[] heights;

            public MessageGroupedLayoutAttempt(int i1, int i2, float f1, float f2) {
                lineCounts = new int[]{i1, i2};
                heights = new float[]{f1, f2};
            }

            public MessageGroupedLayoutAttempt(int i1, int i2, int i3, float f1, float f2, float f3) {
                lineCounts = new int[]{i1, i2, i3};
                heights = new float[]{f1, f2, f3};
            }

            public MessageGroupedLayoutAttempt(int i1, int i2, int i3, int i4, float f1, float f2, float f3, float f4) {
                lineCounts = new int[]{i1, i2, i3, i4};
                heights = new float[]{f1, f2, f3, f4};
            }
        }

        private float multiHeight(float[] array, int start, int end) {
            float sum = 0;
            for (int a = start; a < end; a++) {
                sum += array[a];
            }
            return maxSizeWidth / sum;
        }

        public void calculate() {
            posArray.clear();
            positions.clear();

            maxSizeWidth = 800;
            int firstSpanAdditionalSize = 200;

            int count = messages.size();
            if (count <= 1) {
                return;
            }

            float maxSizeHeight = 814.0f;
            StringBuilder proportions = new StringBuilder();
            float averageAspectRatio = 1.0f;
            boolean isOut = false;
            int maxX = 0;
            boolean forceCalc = false;
            boolean needShare = false;
            hasSibling = false;

            hasCaption = false;

            for (int a = 0; a < count; a++) {
                MessageObject messageObject = messages.get(a);
                if (a == 0) {
                    isOut = messageObject.isOutOwner();
                    needShare = !isOut && (
                            messageObject.messageOwner.fwd_from != null
                                    && messageObject.messageOwner.fwd_from.saved_from_chat_id != 0
                                    || messageObject.messageOwner.from_id > 0
                                    && (messageObject.messageOwner.to_id != 0
                                    || messageObject.messageOwner.media.isInvoice())
                    );
                }
                PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
                GroupedMessagePosition position = new GroupedMessagePosition();
                position.last = a == count - 1;
                position.aspectRatio = photoSize == null ? 1.0f : photoSize.w / (float) photoSize.h;

                if (position.aspectRatio > 1.2f) {
                    proportions.append("w");
                } else if (position.aspectRatio < 0.8f) {
                    proportions.append("n");
                } else {
                    proportions.append("q");
                }

                averageAspectRatio += position.aspectRatio;

                if (position.aspectRatio > 2.0f) {
                    forceCalc = true;
                }

                positions.put(messageObject, position);
                posArray.add(position);

                if (messageObject.caption != null) {
                    hasCaption = true;
                }
            }

            if (needShare) {
                maxSizeWidth -= 50;
                firstSpanAdditionalSize += 50;
            }

            int minHeight = AndroidUtilities.dp(120);
            int minWidth = (int) (AndroidUtilities.dp(120) / (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / (float) maxSizeWidth));
            int paddingsWidth = (int) (AndroidUtilities.dp(40) / (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / (float) maxSizeWidth));

            float maxAspectRatio = maxSizeWidth / maxSizeHeight;
            averageAspectRatio = averageAspectRatio / count;

            float minH = AndroidUtilities.dp(100) / maxSizeHeight;

            if (!forceCalc && (count == 2 || count == 3 || count == 4)) {
                if (count == 2) {
                    GroupedMessagePosition position1 = posArray.get(0);
                    GroupedMessagePosition position2 = posArray.get(1);
                    String pString = proportions.toString();
                    if (pString.equals("ww") && averageAspectRatio > 1.4 * maxAspectRatio && position1.aspectRatio - position2.aspectRatio < 0.2) {
                        float height = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, Math.min(maxSizeWidth / position2.aspectRatio, maxSizeHeight / 2.0f))) / maxSizeHeight;
                        position1.set(0, 0, 0, 0, maxSizeWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);
                        position2.set(0, 0, 1, 1, maxSizeWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                    } else if (pString.equals("ww") || pString.equals("qq")) {
                        int width = maxSizeWidth / 2;
                        float height = Math.round(Math.min(width / position1.aspectRatio, Math.min(width / position2.aspectRatio, maxSizeHeight))) / maxSizeHeight;
                        position1.set(0, 0, 0, 0, width, height, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        position2.set(1, 1, 0, 0, width, height, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        maxX = 1;
                    } else {
                        int secondWidth = (int) Math.max(0.4f * maxSizeWidth, Math.round((maxSizeWidth / position1.aspectRatio / (1.0f / position1.aspectRatio + 1.0f / position2.aspectRatio))));
                        int firstWidth = maxSizeWidth - secondWidth;
                        if (firstWidth < minWidth) {
                            int diff = minWidth - firstWidth;
                            firstWidth = minWidth;
                            secondWidth -= diff;
                        }

                        float height = Math.min(maxSizeHeight, Math.round(Math.min(firstWidth / position1.aspectRatio, secondWidth / position2.aspectRatio))) / maxSizeHeight;
                        position1.set(0, 0, 0, 0, firstWidth, height, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        position2.set(1, 1, 0, 0, secondWidth, height, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);
                        maxX = 1;
                    }
                } else if (count == 3) {
                    GroupedMessagePosition position1 = posArray.get(0);
                    GroupedMessagePosition position2 = posArray.get(1);
                    GroupedMessagePosition position3 = posArray.get(2);
                    if (proportions.charAt(0) == 'n') {
                        float thirdHeight = Math.min(maxSizeHeight * 0.5f, Math.round(position2.aspectRatio * maxSizeWidth / (position3.aspectRatio + position2.aspectRatio)));
                        float secondHeight = maxSizeHeight - thirdHeight;
                        int rightWidth = (int) Math.max(minWidth, Math.min(maxSizeWidth * 0.5f, Math.round(Math.min(thirdHeight * position3.aspectRatio, secondHeight * position2.aspectRatio))));

                        int leftWidth = Math.round(Math.min(maxSizeHeight * position1.aspectRatio + paddingsWidth, maxSizeWidth - rightWidth));
                        position1.set(0, 0, 0, 1, leftWidth, 1.0f, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM | POSITION_FLAG_TOP);

                        position2.set(1, 1, 0, 0, rightWidth, secondHeight / maxSizeHeight, POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        position3.set(0, 1, 1, 1, rightWidth, thirdHeight / maxSizeHeight, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        position3.spanSize = maxSizeWidth;

                        position1.siblingHeights = new float[]{thirdHeight / maxSizeHeight, secondHeight / maxSizeHeight};

                        if (isOut) {
                            position1.spanSize = maxSizeWidth - rightWidth;
                        } else {
                            position2.spanSize = maxSizeWidth - leftWidth;
                            position3.leftSpanOffset = leftWidth;
                        }
                        hasSibling = true;
                        maxX = 1;
                    } else {
                        float firstHeight = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, (maxSizeHeight) * 0.66f)) / maxSizeHeight;
                        position1.set(0, 1, 0, 0, maxSizeWidth, firstHeight, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        int width = maxSizeWidth / 2;
                        float secondHeight = Math.min(maxSizeHeight - firstHeight, Math.round(Math.min(width / position2.aspectRatio, width / position3.aspectRatio))) / maxSizeHeight;
                        if (secondHeight < minH) {
                            secondHeight = minH;
                        }
                        position2.set(0, 0, 1, 1, width, secondHeight, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM);
                        position3.set(1, 1, 1, 1, width, secondHeight, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        maxX = 1;
                    }
                } else if (count == 4) {
                    GroupedMessagePosition position1 = posArray.get(0);
                    GroupedMessagePosition position2 = posArray.get(1);
                    GroupedMessagePosition position3 = posArray.get(2);
                    GroupedMessagePosition position4 = posArray.get(3);
                    if (proportions.charAt(0) == 'w') {
                        float h0 = Math.round(Math.min(maxSizeWidth / position1.aspectRatio, maxSizeHeight * 0.66f)) / maxSizeHeight;
                        position1.set(0, 2, 0, 0, maxSizeWidth, h0, POSITION_FLAG_LEFT | POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        float h = Math.round(maxSizeWidth / (position2.aspectRatio + position3.aspectRatio + position4.aspectRatio));
                        int w0 = (int) Math.max(minWidth, Math.min(maxSizeWidth * 0.4f, h * position2.aspectRatio));
                        int w2 = (int) Math.max(Math.max(minWidth, maxSizeWidth * 0.33f), h * position4.aspectRatio);
                        int w1 = maxSizeWidth - w0 - w2;
                        if (w1 < AndroidUtilities.dp(58)) {
                            int diff = AndroidUtilities.dp(58) - w1;
                            w1 = AndroidUtilities.dp(58);
                            w0 -= diff / 2;
                            w2 -= (diff - diff / 2);
                        }
                        h = Math.min(maxSizeHeight - h0, h);
                        h /= maxSizeHeight;
                        if (h < minH) {
                            h = minH;
                        }
                        position2.set(0, 0, 1, 1, w0, h, POSITION_FLAG_LEFT | POSITION_FLAG_BOTTOM);
                        position3.set(1, 1, 1, 1, w1, h, POSITION_FLAG_BOTTOM);
                        position4.set(2, 2, 1, 1, w2, h, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        maxX = 2;
                    } else {
                        int w = Math.max(minWidth, Math.round(maxSizeHeight / (1.0f / position2.aspectRatio + 1.0f / position3.aspectRatio + 1.0f / position4.aspectRatio)));
                        float h0 = Math.min(0.33f, Math.max(minHeight, w / position2.aspectRatio) / maxSizeHeight);
                        float h1 = Math.min(0.33f, Math.max(minHeight, w / position3.aspectRatio) / maxSizeHeight);
                        float h2 = 1.0f - h0 - h1;
                        int w0 = Math.round(Math.min(maxSizeHeight * position1.aspectRatio + paddingsWidth, maxSizeWidth - w));

                        position1.set(0, 0, 0, 2, w0, h0 + h1 + h2, POSITION_FLAG_LEFT | POSITION_FLAG_TOP | POSITION_FLAG_BOTTOM);

                        position2.set(1, 1, 0, 0, w, h0, POSITION_FLAG_RIGHT | POSITION_FLAG_TOP);

                        position3.set(0, 1, 1, 1, w, h1, POSITION_FLAG_RIGHT);
                        position3.spanSize = maxSizeWidth;

                        position4.set(0, 1, 2, 2, w, h2, POSITION_FLAG_RIGHT | POSITION_FLAG_BOTTOM);
                        position4.spanSize = maxSizeWidth;

                        if (isOut) {
                            position1.spanSize = maxSizeWidth - w;
                        } else {
                            position2.spanSize = maxSizeWidth - w0;
                            position3.leftSpanOffset = w0;
                            position4.leftSpanOffset = w0;
                        }
                        position1.siblingHeights = new float[]{h0, h1, h2};
                        hasSibling = true;
                        maxX = 1;
                    }
                }
            } else {
                float[] croppedRatios = new float[posArray.size()];
                for (int a = 0; a < count; a++) {
                    if (averageAspectRatio > 1.1f) {
                        croppedRatios[a] = Math.max(1.0f, posArray.get(a).aspectRatio);
                    } else {
                        croppedRatios[a] = Math.min(1.0f, posArray.get(a).aspectRatio);
                    }
                    croppedRatios[a] = Math.max(0.66667f, Math.min(1.7f, croppedRatios[a]));
                }

                int firstLine;
                int secondLine;
                int thirdLine;
                int fourthLine;
                ArrayList<MessageGroupedLayoutAttempt> attempts = new ArrayList<>();
                for (firstLine = 1; firstLine < croppedRatios.length; firstLine++) {
                    secondLine = croppedRatios.length - firstLine;
                    if (firstLine > 3 || secondLine > 3) {
                        continue;
                    }
                    attempts.add(new MessageGroupedLayoutAttempt(firstLine, secondLine, multiHeight(croppedRatios, 0, firstLine), multiHeight(croppedRatios, firstLine, croppedRatios.length)));
                }

                for (firstLine = 1; firstLine < croppedRatios.length - 1; firstLine++) {
                    for (secondLine = 1; secondLine < croppedRatios.length - firstLine; secondLine++) {
                        thirdLine = croppedRatios.length - firstLine - secondLine;
                        if (firstLine > 3 || secondLine > (averageAspectRatio < 0.85f ? 4 : 3) || thirdLine > 3) {
                            continue;
                        }
                        attempts.add(new MessageGroupedLayoutAttempt(firstLine, secondLine, thirdLine, multiHeight(croppedRatios, 0, firstLine), multiHeight(croppedRatios, firstLine, firstLine + secondLine), multiHeight(croppedRatios, firstLine + secondLine, croppedRatios.length)));
                    }
                }

                for (firstLine = 1; firstLine < croppedRatios.length - 2; firstLine++) {
                    for (secondLine = 1; secondLine < croppedRatios.length - firstLine; secondLine++) {
                        for (thirdLine = 1; thirdLine < croppedRatios.length - firstLine - secondLine; thirdLine++) {
                            fourthLine = croppedRatios.length - firstLine - secondLine - thirdLine;
                            if (firstLine > 3 || secondLine > 3 || thirdLine > 3 || fourthLine > 3) {
                                continue;
                            }
                            attempts.add(new MessageGroupedLayoutAttempt(firstLine, secondLine, thirdLine, fourthLine, multiHeight(croppedRatios, 0, firstLine), multiHeight(croppedRatios, firstLine, firstLine + secondLine), multiHeight(croppedRatios, firstLine + secondLine, firstLine + secondLine + thirdLine), multiHeight(croppedRatios, firstLine + secondLine + thirdLine, croppedRatios.length)));
                        }
                    }
                }

                MessageGroupedLayoutAttempt optimal = null;
                float optimalDiff = 0.0f;
                float maxHeight = maxSizeWidth / 3 * 4;
                for (int a = 0; a < attempts.size(); a++) {
                    MessageGroupedLayoutAttempt attempt = attempts.get(a);
                    float height = 0;
                    float minLineHeight = Float.MAX_VALUE;
                    for (int b = 0; b < attempt.heights.length; b++) {
                        height += attempt.heights[b];
                        if (attempt.heights[b] < minLineHeight) {
                            minLineHeight = attempt.heights[b];
                        }
                    }

                    float diff = Math.abs(height - maxHeight);
                    if (attempt.lineCounts.length > 1) {
                        if (attempt.lineCounts[0] > attempt.lineCounts[1] || (attempt.lineCounts.length > 2 && attempt.lineCounts[1] > attempt.lineCounts[2]) || (attempt.lineCounts.length > 3 && attempt.lineCounts[2] > attempt.lineCounts[3])) {
                            diff *= 1.2f;
                        }
                    }

                    if (minLineHeight < minWidth) {
                        diff *= 1.5f;
                    }

                    if (optimal == null || diff < optimalDiff) {
                        optimal = attempt;
                        optimalDiff = diff;
                    }
                }
                if (optimal == null) {
                    return;
                }

                int index = 0;
                float y = 0.0f;

                for (int i = 0; i < optimal.lineCounts.length; i++) {
                    int c = optimal.lineCounts[i];
                    float lineHeight = optimal.heights[i];
                    int spanLeft = maxSizeWidth;
                    GroupedMessagePosition posToFix = null;
                    maxX = Math.max(maxX, c - 1);
                    for (int k = 0; k < c; k++) {
                        float ratio = croppedRatios[index];
                        int width = (int) (ratio * lineHeight);
                        spanLeft -= width;
                        GroupedMessagePosition pos = posArray.get(index);
                        int flags = 0;
                        if (i == 0) {
                            flags |= POSITION_FLAG_TOP;
                        }
                        if (i == optimal.lineCounts.length - 1) {
                            flags |= POSITION_FLAG_BOTTOM;
                        }
                        if (k == 0) {
                            flags |= POSITION_FLAG_LEFT;
                            if (isOut) {
                                posToFix = pos;
                            }
                        }
                        if (k == c - 1) {
                            flags |= POSITION_FLAG_RIGHT;
                            if (!isOut) {
                                posToFix = pos;
                            }
                        }
                        pos.set(k, k, i, i, width, Math.max(minH, lineHeight / maxSizeHeight), flags);
                        index++;
                    }
                    posToFix.pw += spanLeft;
                    posToFix.spanSize += spanLeft;
                    y += lineHeight;
                }
            }
            int avatarOffset = 108;
            for (int a = 0; a < count; a++) {
                GroupedMessagePosition pos = posArray.get(a);
                if (isOut) {
                    if (pos.minX == 0) {
                        pos.spanSize += firstSpanAdditionalSize;
                    }
                    if ((pos.flags & POSITION_FLAG_RIGHT) != 0) {
                        pos.edge = true;
                    }
                } else {
                    if (pos.maxX == maxX || (pos.flags & POSITION_FLAG_RIGHT) != 0) {
                        pos.spanSize += firstSpanAdditionalSize;
                    }
                    if ((pos.flags & POSITION_FLAG_LEFT) != 0) {
                        pos.edge = true;
                    }
                }
                MessageObject messageObject = messages.get(a);
                if (!isOut && messageObject.needDrawAvatarInternal()) {
                    if (pos.edge) {
                        if (pos.spanSize != 1000) {
                            pos.spanSize += avatarOffset;
                        }
                        pos.pw += avatarOffset;
                    } else if ((pos.flags & POSITION_FLAG_RIGHT) != 0) {
                        if (pos.spanSize != 1000) {
                            pos.spanSize -= avatarOffset;
                        } else if (pos.leftSpanOffset != 0) {
                            pos.leftSpanOffset += avatarOffset;
                        }
                    }
                }
            }
        }

        public MessageObject findPrimaryMessageObject() {
            if (!messages.isEmpty() && positions.isEmpty()) {
                calculate();
            }
            for (int i = 0; i < messages.size(); i++) {
                MessageObject object = messages.get(i);
                MessageObject.GroupedMessagePosition position = positions.get(object);
                if (position != null && (position.flags & (MessageObject.POSITION_FLAG_TOP | MessageObject.POSITION_FLAG_LEFT)) != 0) {
                    return object;
                }
            }
            return null;
        }

        public static class TransitionParams {
            public int left;
            public int top;
            public int right;
            public int bottom;

            public float offsetLeft;
            public float offsetTop;
            public float offsetRight;
            public float offsetBottom;

            public boolean drawBackgroundForDeletedItems;
            public boolean backgroundChangeBounds;

            public boolean pinnedTop;
            public boolean pinnedBotton;

            public ChatMessageCell cell;
            public float captionEnterProgress = 1f;
            public boolean drawCaptionLayout;
            public boolean isNewGroup;
        }
    }

    private static final int LINES_PER_BLOCK = 10;

    public ArrayList<TextLayoutBlock> textLayoutBlocks;

    public MessageObject(int accountNum, Message message, String formattedMessage, String name, String userName, boolean localMessage, boolean isChannel, boolean edit) {
        localType = localMessage ? 2 : 1;
        currentAccount = accountNum;
        localName = name;
        localUserName = userName;
        messageText = formattedMessage;
        messageOwner = message;
        localChannel = isChannel;
        localEdit = edit;
    }

    public MessageObject(int accountNum, Message message, AbstractMap<Integer, User> users, boolean generateLayout) {
        this(accountNum, message, users, null, generateLayout);
    }

    public MessageObject(int accountNum, Message message, SparseArray<User> users, boolean generateLayout) {
        this(accountNum, message, users, null, generateLayout);
    }

    public MessageObject(int accountNum, Message message, boolean generateLayout) {
        this(accountNum, message, null, null, null, null, null, generateLayout, 0);
    }

    public MessageObject(int accountNum, Message message, MessageObject replyToMessage, boolean generateLayout) {
        this(accountNum, message, replyToMessage, null, null, null, null, generateLayout, 0);
    }

    public MessageObject(int accountNum, Message message, AbstractMap<Integer, User> users, AbstractMap<Integer, Chat> chats, boolean generateLayout) {
        this(accountNum, message, users, chats, generateLayout, 0);
    }

    public MessageObject(int accountNum, Message message, SparseArray<User> users, SparseArray<Chat> chats, boolean generateLayout) {
        this(accountNum, message, null, null, null, users, chats, generateLayout, 0);
    }

    public MessageObject(int accountNum, Message message, AbstractMap<Integer, User> users, AbstractMap<Integer, Chat> chats, boolean generateLayout, long eid) {
        this(accountNum, message, null, users, chats, null, null, generateLayout, eid);
    }

    public MessageObject(int accountNum, Message message, MessageObject replyToMessage, AbstractMap<Integer, User> users, AbstractMap<Integer, Chat> chats, SparseArray<User> sUsers, SparseArray<Chat> sChats, boolean generateLayout, long eid) {
        Theme.createChatResources(null, true);

        currentAccount = accountNum;
        messageOwner = message;
        replyMessageObject = replyToMessage;
        eventId = eid;
        wasUnread = !messageOwner.out && messageOwner.unread;

        if (message.replyMessage != null) {
            replyMessageObject = new MessageObject(currentAccount, message.replyMessage, null, users, chats, sUsers, sChats, false, eid);
        }

        User fromUser = null;
        if (message.from_id > 0) {
            if (users != null) {
                fromUser = users.get(message.from_id);
            } else if (sUsers != null) {
                fromUser = sUsers.get(message.from_id);
            }
            if (fromUser == null) {
                fromUser = MessagesController.getInstance(currentAccount).getUser(message.from_id);
            }
        }

        updateMessageText(users, chats, sUsers, sChats);
        setType();
        measureInlineBotButtons();

        Calendar rightNow = new GregorianCalendar();
        rightNow.setTimeInMillis((long) (messageOwner.date) * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);
        int dateMonth = rightNow.get(Calendar.MONTH);
        dateKey = String.format("%d_%02d_%02d", dateYear, dateMonth, dateDay);
        monthKey = String.format("%d_%02d", dateYear, dateMonth);

        createMessageSendInfo();
        generateCaption();
        if (generateLayout) {
            TextPaint paint;
            paint = Theme.chat_msgTextPaint;
            int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
            messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), AndroidUtilities.dp(20), false, emojiOnly);
            checkEmojiOnly(emojiOnly);
            emojiAnimatedSticker = null;
            if (emojiOnlyCount == 1 && !(message.media.isWebPage()) && message.entities.isEmpty()) {
                CharSequence emoji = messageText;
                int index;
                if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFB")) >= 0) {
                    emojiAnimatedStickerColor = "_c1";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFC")) >= 0) {
                    emojiAnimatedStickerColor = "_c2";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFD")) >= 0) {
                    emojiAnimatedStickerColor = "_c3";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFE")) >= 0) {
                    emojiAnimatedStickerColor = "_c4";
                    emoji = emoji.subSequence(0, index);
                } else if ((index = TextUtils.indexOf(emoji, "\uD83C\uDFFF")) >= 0) {
                    emojiAnimatedStickerColor = "_c5";
                    emoji = emoji.subSequence(0, index);
                } else {
                    emojiAnimatedStickerColor = "";
                }
                if (TextUtils.isEmpty(emojiAnimatedStickerColor) || EmojiData.emojiColoredMap.contains(emoji.toString())) {
                    emojiAnimatedSticker = MediaDataController.getInstance(currentAccount).getEmojiAnimatedSticker(emoji);
                }
            }
            if (emojiAnimatedSticker == null) {
                generateLayout(fromUser);
            } else {
                type = 1000;
                if (isSticker()) {
                    type = TYPE_STICKER;
                } else if (isAnimatedSticker()) {
                    type = TYPE_ANIMATED_STICKER;
                }
            }
        }
        layoutCreated = generateLayout;
        generateThumbs(false);
        checkMediaExistance();
    }

    public void checkForScam() {

    }

    private void checkEmojiOnly(int[] emojiOnly) {
        if (emojiOnly != null && emojiOnly[0] >= 1 && emojiOnly[0] <= 3) {
            TextPaint emojiPaint;
            int size;
            switch (emojiOnly[0]) {
                case 1:
                    emojiPaint = Theme.chat_msgTextPaintOneEmoji;
                    size = AndroidUtilities.dp(32);
                    emojiOnlyCount = 1;
                    break;
                case 2:
                    emojiPaint = Theme.chat_msgTextPaintTwoEmoji;
                    size = AndroidUtilities.dp(28);
                    emojiOnlyCount = 2;
                    break;
                case 3:
                default:
                    emojiPaint = Theme.chat_msgTextPaintThreeEmoji;
                    size = AndroidUtilities.dp(24);
                    emojiOnlyCount = 3;
                    break;
            }
            Emoji.EmojiSpan[] spans = ((Spannable) messageText).getSpans(0, messageText.length(), Emoji.EmojiSpan.class);
            if (spans != null && spans.length > 0) {
                for (int a = 0; a < spans.length; a++) {
                    spans[a].replaceFontMetrics(emojiPaint.getFontMetricsInt(), size);
                }
            }
        }
    }

    private String getUserName(User user, ArrayList<MessageEntity> entities, int offset) {
        String name;
        if (user == null) {
            name = "";
        } else {
            name = UserObject.formatName(user.first_name, user.last_name);
        }
        if (offset >= 0) {
            MessageEntity entity = new MessageEntity();
            entity.setMentionName(true);
            entity.user_id = user.id;
            entity.offset = offset;
            entity.length = name.length();
            entities.add(entity);
        }
        if (!TextUtils.isEmpty(user.username)) {
            if (offset >= 0) {
                MessageEntity entity = new MessageEntity();
                entity.setMentionName(true);
                entity.user_id = user.id;
                entity.offset = offset + name.length() + 2;
                entity.length = user.username.length() + 1;
                entities.add(entity);
            }
            return String.format("%1$s (@%2$s)", name, user.username);
        }
        return name;
    }

    public void applyNewText() {
        if (TextUtils.isEmpty(messageOwner.message)) {
            return;
        }
        User fromUser = null;
        if (isFromUser()) {
            fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id);
        }
        messageText = messageOwner.message;
        TextPaint paint;
        paint = Theme.chat_msgTextPaint;
        int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
        messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), AndroidUtilities.dp(20), false, emojiOnly);
        checkEmojiOnly(emojiOnly);
        generateLayout(fromUser);
    }

    private boolean allowsBigEmoji() {
        if (!SharedConfig.allowBigEmoji) {
            return false;
        }
        if (messageOwner == null || messageOwner.to_id == 0 && messageOwner.to_id == 0) {
            return true;
        }
        Chat chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.to_id != 0 ? messageOwner.to_id : messageOwner.to_id);
        return !ChatObject.isActionBanned(chat, ChatObject.ACTION_SEND_STICKERS);
    }

    public boolean hasValidReplyMessageObject() {
        return !(replyMessageObject == null
                || replyMessageObject.messageOwner == null
                || replyMessageObject.messageOwner.action.isHistoryClear());
    }

    public void generatePinMessageText(User fromUser, Chat chat) {
        if (fromUser == null && chat == null) {
            if (messageOwner.from_id > 0) {
                fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id);
            }
            if (fromUser == null) {
                chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.to_id);
            }
        }
        if (replyMessageObject == null || replyMessageObject.messageOwner == null || replyMessageObject.messageOwner.action.isHistoryClear()) {
            messageText = replaceWithLink(LocaleController.getString("ActionPinnedNoText", R.string.ActionPinnedNoText), "un1", fromUser != null ? fromUser : chat);
        } else {
            if (replyMessageObject.isMusic()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedMusic", R.string.ActionPinnedMusic), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isVideo()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedVideo", R.string.ActionPinnedVideo), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isGif()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedGif", R.string.ActionPinnedGif), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isVoice()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedVoice", R.string.ActionPinnedVoice), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isRoundVideo()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedRound", R.string.ActionPinnedRound), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.isSticker() || replyMessageObject.isAnimatedSticker()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedSticker", R.string.ActionPinnedSticker), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.messageOwner.media.isDocument()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedFile", R.string.ActionPinnedFile), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.messageOwner.media.isGeo()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedGeo", R.string.ActionPinnedGeo), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.messageOwner.media.isGeoLive()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedGeoLive", R.string.ActionPinnedGeoLive), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.messageOwner.media.isContact()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedContact", R.string.ActionPinnedContact), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.messageOwner.media.isPhoto()) {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedPhoto", R.string.ActionPinnedPhoto), "un1", fromUser != null ? fromUser : chat);
            } else if (replyMessageObject.messageText != null && replyMessageObject.messageText.length() > 0) {
                CharSequence mess = replyMessageObject.messageText;
                if (mess.length() > 20) {
                    mess = mess.subSequence(0, 20) + "...";
                }
                mess = Emoji.replaceEmoji(mess, Theme.chat_msgTextPaint.getFontMetricsInt(), AndroidUtilities.dp(20), false);
                messageText = replaceWithLink(LocaleController.formatString("ActionPinnedText", R.string.ActionPinnedText, mess), "un1", fromUser != null ? fromUser : chat);
            } else {
                messageText = replaceWithLink(LocaleController.getString("ActionPinnedNoText", R.string.ActionPinnedNoText), "un1", fromUser != null ? fromUser : chat);
            }
        }
    }

    public static void updateReactions(Message message, MessageReactions reactions) {
        if (message == null || reactions == null) {
            return;
        }
        if (reactions.min && message.reactions != null) {
            for (int a = 0, N = message.reactions.results.size(); a < N; a++) {
                ReactionCount reaction = message.reactions.results.get(a);
                if (reaction.chosen) {
                    for (int b = 0, N2 = reactions.results.size(); b < N2; b++) {
                        ReactionCount newReaction = reactions.results.get(b);
                        if (reaction.reaction.equals(newReaction.reaction)) {
                            newReaction.chosen = true;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        message.reactions = reactions;
        message.flags |= 1048576;
    }

    public boolean hasReactions() {
        return messageOwner.reactions != null && !messageOwner.reactions.results.isEmpty();
    }

    public boolean isPollClosed() {
        return false;
    }

    public boolean isQuiz() {
        return false;
    }

    public boolean isPublicPoll() {
        return false;
    }

    public boolean isPoll() {
        return type == TYPE_POLL;
    }

    public boolean canUnvote() {
        return false;
    }

    public boolean isVoted() {
        return false;
    }

    public long getPollId() {
        return 0;
    }

    private MessageMedia.Photo getPhotoWithId(MessageMedia.WebPage webPage, long id) {
        if (webPage == null || webPage.cached_page == null) {
            return null;
        }
        if (webPage.photo != null && webPage.photo.id == id) {
            return webPage.photo;
        }
        for (int a = 0; a < webPage.cached_page.photos.size(); a++) {
            MessageMedia.Photo photo = webPage.cached_page.photos.get(a);
            if (photo.id == id) {
                return photo;
            }
        }
        return null;
    }

    private Document getDocumentWithId(MessageMedia.WebPage webPage, long id) {
        if (webPage == null || webPage.cached_page == null) {
            return null;
        }
        if (webPage.document != null && webPage.document.id == id) {
            return webPage.document;
        }
        for (int a = 0; a < webPage.cached_page.documents.size(); a++) {
            Document document = webPage.cached_page.documents.get(a);
            if (document.id == id) {
                return document;
            }
        }
        return null;
    }

    private MessageObject getMessageObjectForBlock(MessageMedia.WebPage webPage, PageBlock pageBlock) {
        Message message = null;
        if (pageBlock instanceof Block.TL_pageBlockPhoto) {
            Block.TL_pageBlockPhoto pageBlockPhoto = (Block.TL_pageBlockPhoto) pageBlock;
            MessageMedia.Photo photo = getPhotoWithId(webPage, pageBlockPhoto.photo_id);
            if (photo == webPage.photo) {
                return this;
            }
            message = new Message();
            message.media = new MessageMedia().setPhoto(true);
            message.media.photo = photo;
        } else if (pageBlock instanceof Block.TL_pageBlockVideo) {
            Block.TL_pageBlockVideo pageBlockVideo = (Block.TL_pageBlockVideo) pageBlock;
            Document document = getDocumentWithId(webPage, pageBlockVideo.video_id);
            if (document == webPage.document) {
                return this;
            }
            message = new Message();
            message.media = new MessageMedia().setDocument(true);
            message.media.document = getDocumentWithId(webPage, pageBlockVideo.video_id);
        }
        message.message = "";
        message.realId = getId();
        message.id = Utilities.random.nextInt();
        message.date = messageOwner.date;
        message.to_id = messageOwner.to_id;
        message.out = messageOwner.out;
        message.from_id = messageOwner.from_id;
        return new MessageObject(currentAccount, message, false);
    }

    public ArrayList<MessageObject> getWebPagePhotos(ArrayList<MessageObject> array, ArrayList<PageBlock> blocksToSearch) {
        ArrayList<MessageObject> messageObjects = array == null ? new ArrayList<>() : array;
        if (messageOwner.media == null || messageOwner.media.webpage == null) {
            return messageObjects;
        }
        MessageMedia.WebPage webPage = messageOwner.media.webpage;
        if (webPage.cached_page == null) {
            return messageObjects;
        }
        ArrayList<PageBlock> blocks = blocksToSearch == null ? webPage.cached_page.blocks : blocksToSearch;
        for (int a = 0; a < blocks.size(); a++) {
            PageBlock block = blocks.get(a);
            if (block instanceof Block.TL_pageBlockSlideshow) {
                Block.TL_pageBlockSlideshow slideshow = (Block.TL_pageBlockSlideshow) block;
                for (int b = 0; b < slideshow.items.size(); b++) {
                    messageObjects.add(getMessageObjectForBlock(webPage, slideshow.items.get(b)));
                }
            } else if (block instanceof Block.TL_pageBlockCollage) {
                Block.TL_pageBlockCollage slideshow = (Block.TL_pageBlockCollage) block;
                for (int b = 0; b < slideshow.items.size(); b++) {
                    messageObjects.add(getMessageObjectForBlock(webPage, slideshow.items.get(b)));
                }
            }
        }
        return messageObjects;
    }

    public void createMessageSendInfo() {
        if (messageOwner.message != null && (messageOwner.id < 0 || isEditing()) && messageOwner.params != null) {
            String param;
            if ((param = messageOwner.params.get("ve")) != null && (isVideo() || isNewGif() || isRoundVideo())) {
                videoEditedInfo = new VideoEditedInfo();
                if (!videoEditedInfo.parseString(param)) {
                    videoEditedInfo = null;
                } else {
                    videoEditedInfo.roundVideo = isRoundVideo();
                }
            }
            if (messageOwner.send_state == MESSAGE_SEND_STATE_EDITING && (param = messageOwner.params.get("prevMedia")) != null) {
                SerializedData serializedData = new SerializedData(Base64.decode(param, Base64.DEFAULT));
                int constructor = serializedData.readInt32(false);
                previousMedia = MessageMedia.TLdeserialize(serializedData, constructor, false);
                previousCaption = serializedData.readString(false);
                previousAttachPath = serializedData.readString(false);
                int count = serializedData.readInt32(false);
                previousCaptionEntities = new ArrayList<>(count);
                for (int a = 0; a < count; a++) {
                    constructor = serializedData.readInt32(false);
                    MessageEntity entity = MessageEntity.TLdeserialize(serializedData, constructor, false);
                    previousCaptionEntities.add(entity);
                }
                serializedData.cleanup();
            }
        }
    }

    public void measureInlineBotButtons() {
        wantedBotKeyboardWidth = 0;
        if (messageOwner.reply_markup.isInlineMarkup() || messageOwner.reactions != null && !messageOwner.reactions.results.isEmpty()) {
            Theme.createChatResources(null, true);
            if (botButtonsLayout == null) {
                botButtonsLayout = new StringBuilder();
            } else {
                botButtonsLayout.setLength(0);
            }
        }

        if (messageOwner.reply_markup.isInlineMarkup()) {
            for (int a = 0; a < messageOwner.reply_markup.rows.size(); a++) {
                ReplyMarkup.KeyboardButtonRow row = messageOwner.reply_markup.rows.get(a);
                int maxButtonSize = 0;
                int size = row.buttons.size();
                for (int b = 0; b < size; b++) {
                    KeyboardButton button = row.buttons.get(b);
                    botButtonsLayout.append(a).append(b);
                    CharSequence text;
                    if ((messageOwner.media.flags & 4) != 0) {
                        text = LocaleController.getString("PaymentReceipt", R.string.PaymentReceipt);
                    } else {
                        text = Emoji.replaceEmoji(button.text, Theme.chat_msgBotButtonPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false);
                    }
                    StaticLayout staticLayout = new StaticLayout(text, Theme.chat_msgBotButtonPaint, AndroidUtilities.dp(2000), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    if (staticLayout.getLineCount() > 0) {
                        float width = staticLayout.getLineWidth(0);
                        float left = staticLayout.getLineLeft(0);
                        if (left < width) {
                            width -= left;
                        }
                        maxButtonSize = Math.max(maxButtonSize, (int) Math.ceil(width) + AndroidUtilities.dp(4));
                    }
                }
                wantedBotKeyboardWidth = Math.max(wantedBotKeyboardWidth, (maxButtonSize + AndroidUtilities.dp(12)) * size + AndroidUtilities.dp(5) * (size - 1));
            }
        } else if (messageOwner.reactions != null) {
            int size = messageOwner.reactions.results.size();
            for (int a = 0; a < size; a++) {
                ReactionCount reactionCount = messageOwner.reactions.results.get(a);
                int maxButtonSize = 0;
                botButtonsLayout.append(0).append(a);
                CharSequence text = Emoji.replaceEmoji(String.format("%d %s", reactionCount.count, reactionCount.reaction), Theme.chat_msgBotButtonPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false);
                StaticLayout staticLayout = new StaticLayout(text, Theme.chat_msgBotButtonPaint, AndroidUtilities.dp(2000), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (staticLayout.getLineCount() > 0) {
                    float width = staticLayout.getLineWidth(0);
                    float left = staticLayout.getLineLeft(0);
                    if (left < width) {
                        width -= left;
                    }
                    maxButtonSize = Math.max(maxButtonSize, (int) Math.ceil(width) + AndroidUtilities.dp(4));
                }
                wantedBotKeyboardWidth = Math.max(wantedBotKeyboardWidth, (maxButtonSize + AndroidUtilities.dp(12)) * size + AndroidUtilities.dp(5) * (size - 1));
            }
        }
    }

    public boolean isFcmMessage() {
        return localType != 0;
    }

    private void updateMessageText(AbstractMap<Integer, User> users, AbstractMap<Integer, Chat> chats, SparseArray<User> sUsers, SparseArray<Chat> sChats) {
        User fromUser = null;
        if (messageOwner.from_id > 0) {
            if (users != null) {
                fromUser = users.get(messageOwner.from_id);
            } else if (sUsers != null) {
                fromUser = sUsers.get(messageOwner.from_id);
            }
            if (fromUser == null) {
                fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id);
            }
        }

        isRestrictedMessage = false;
        String restrictionReason = "";//MessagesController.getRestrictionReason(messageOwner.restriction_reason);
        if (!TextUtils.isEmpty(restrictionReason)) {
                messageText = restrictionReason;
                isRestrictedMessage = true;
            } else if (!isMediaEmpty()) {
                if (messageOwner.media.isDice()) {
                    messageText = getDiceEmoji();
                } else if (messageOwner.media.isPhoto()) {
                    if (messageOwner.media.ttl_seconds != 0) {
                        messageText = LocaleController.getString("AttachDestructingPhoto", R.string.AttachDestructingPhoto);
                    } else {
                        messageText = LocaleController.getString("AttachPhoto", R.string.AttachPhoto);
                    }
                } else if (isVideo() || messageOwner.media.isDocument() && getDocument() !=null && messageOwner.media.ttl_seconds != 0) {
                    if (messageOwner.media.ttl_seconds != 0) {
                        messageText = LocaleController.getString("AttachDestructingVideo", R.string.AttachDestructingVideo);
                    } else {
                        messageText = LocaleController.getString("AttachVideo", R.string.AttachVideo);
                    }
                } else if (isVoice()) {
                    messageText = LocaleController.getString("AttachAudio", R.string.AttachAudio);
                } else if (isRoundVideo()) {
                    messageText = LocaleController.getString("AttachRound", R.string.AttachRound);
                } else if (messageOwner.media.isGeo() || messageOwner.media.isVenue()) {
                    messageText = LocaleController.getString("AttachLocation", R.string.AttachLocation);
                } else if (messageOwner.media.isGeoLive()) {
                    messageText = LocaleController.getString("AttachLiveLocation", R.string.AttachLiveLocation);
                } else if (messageOwner.media.isContact()) {
                    messageText = LocaleController.getString("AttachContact", R.string.AttachContact);
                    if (!TextUtils.isEmpty(messageOwner.media.vcard)) {
                        vCardData = VCardData.parse(messageOwner.media.vcard);
                    }
                } else if (messageOwner.media.isInvoice()) {
                    messageText = messageOwner.media.description;
                } else if (messageOwner.media.isUnsupported()) {
                    messageText = LocaleController.getString("UnsupportedMedia", R.string.UnsupportedMedia);
                } else if (messageOwner.media.isDocument()) {
                    if (isSticker() || isAnimatedStickerDocument(getDocument(), true)) {
                        String sch = getStickerChar();
                        if (sch != null && sch.length() > 0) {
                            messageText = String.format("%s %s", sch, LocaleController.getString("AttachSticker", R.string.AttachSticker));
                        } else {
                            messageText = LocaleController.getString("AttachSticker", R.string.AttachSticker);
                        }
                    } else if (isMusic()) {
                        messageText = LocaleController.getString("AttachMusic", R.string.AttachMusic);
                    } else if (isGif()) {
                        messageText = LocaleController.getString("AttachGif", R.string.AttachGif);
                    } else {
                        String name = FileLoader.getDocumentFileName(getDocument());
                        if (name != null && name.length() > 0) {
                            messageText = name;
                        } else {
                            messageText = LocaleController.getString("AttachDocument", R.string.AttachDocument);
                        }
                    }
                }
            } else {
                messageText = messageOwner.message;
            }

        if (messageText == null) {
            messageText = "";
        }
    }

    /**
     * 计算展示的类型
     */
    public void setType() {
        int oldType = type;
        type = 1000;
        isRoundVideoCached = 0;
        if (messageOwner instanceof Message) {
            if (isRestrictedMessage) {
                type = 0;
            } else if (emojiAnimatedSticker != null) {
                if (isSticker()) {
                    type = TYPE_STICKER;
                } else {
                    type = TYPE_ANIMATED_STICKER;
                }
            } else if (isMediaEmpty()) {
                type = 0;
                if (TextUtils.isEmpty(messageText) && eventId == 0) {
                    messageText = "Empty message";
                }
            } else if (messageOwner.media.ttl_seconds != 0 && (messageOwner.media.photo==null || getDocument() ==null)) {
                contentType = 1;
                type = 10;
            } else if (messageOwner.media.isDice()) {
                type = TYPE_ANIMATED_STICKER;
                if (messageOwner.media.document == null) {
                    messageOwner.media.document = new Document();
                    messageOwner.media.document.file_reference = new byte[0];
                    messageOwner.media.document.mime_type = "application/x-tgsdice";
                    messageOwner.media.document.dc_id = Integer.MIN_VALUE;
                    messageOwner.media.document.id = Integer.MIN_VALUE;
                    Document.DocumentAttribute attributeImageSize = new Document.DocumentAttribute();
                    attributeImageSize.setImageSize(true);
                    attributeImageSize.w = 512;
                    attributeImageSize.h = 512;
                    messageOwner.media.document.attributes.add(attributeImageSize);
                }
            } else if (messageOwner.media.isPhoto()) {
                type = TYPE_PHOTO;
            } else if (messageOwner.media.isGeo() || messageOwner.media.isVenue() || messageOwner.media.isGeoLive()) {
                type = 4;
            } else if (isRoundVideo()) {
                type = TYPE_ROUND_VIDEO;
            } else if (isVideo()) {
                type = TYPE_VIDEO;
            } else if (isVoice()) {
                type = 2;
            } else if (isMusic()) {
                type = 14;
            } else if (messageOwner.media.isContact()) {
                type = 12;
            } else if (messageOwner.media.isUnsupported()) {
                type = 0;
            } else if (messageOwner.media.isDocument()) {
                Document document = getDocument();
                if (document != null && document.mime_type != null) {
                    if (isGifDocument(document)) {
                        type = 8;
                    } else if (isSticker()) {
                        type = TYPE_STICKER;
                    } else if (isAnimatedSticker()) {
                        type = TYPE_ANIMATED_STICKER;
                    } else {
                        type = 9;
                    }
                } else {
                    type = 9;
                }
            } else if (messageOwner.media.isInvoice()) {
                type = 0;
            }
        } else if (messageOwner!=null) {
            if (messageOwner.action.isLoginUnknownLocation()) {
                type = 0;
            } else if (messageOwner.action.isChatEditPhoto() || messageOwner.action.isUserUpdatedPhoto()) {
                contentType = 1;
                type = 11;
            } else if (messageOwner.action.isHistoryClear()) {
                contentType = -1;
                type = -1;
            } else {
                contentType = 1;
                type = 10;
            }
        }
        if (oldType != 1000 && oldType != type) {
            updateMessageText(MessagesController.getInstance(currentAccount).getUsers(), MessagesController.getInstance(currentAccount).getChats(), null, null);
            generateThumbs(false);
        }
    }

    public boolean checkLayout() {
        if (type != 0 || messageOwner.to_id == 0 || messageText == null || messageText.length() == 0) {
            return false;
        }
        if (layoutCreated) {
            int newMinSize = AndroidUtilities.isTablet() ? AndroidUtilities.getMinTabletSide() : AndroidUtilities.displaySize.x;
            if (Math.abs(generatedWithMinSize - newMinSize) > AndroidUtilities.dp(52) || generatedWithDensity != AndroidUtilities.density) {
                layoutCreated = false;
            }
        }
        if (!layoutCreated) {
            layoutCreated = true;
            User fromUser = null;
            if (isFromUser()) {
                fromUser = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id);
            }
            TextPaint paint;
            paint = Theme.chat_msgTextPaint;
            int[] emojiOnly = allowsBigEmoji() ? new int[1] : null;
            messageText = Emoji.replaceEmoji(messageText, paint.getFontMetricsInt(), AndroidUtilities.dp(20), false, emojiOnly);
            checkEmojiOnly(emojiOnly);
            generateLayout(fromUser);
            return true;
        }
        return false;
    }

    public void resetLayout() {
        layoutCreated = false;
    }

    public String getMimeType() {
        Document document = getDocument();
        if (document != null) {
            return document.mime_type;
        } else if (messageOwner.media.isInvoice()) {
//            WebDocument photo = messageOwner.media.document;
//            if (photo != null) {
//                return photo.mime_type;
//            }
        } else if (messageOwner.media.isPhoto()) {
            return "image/jpeg";
        } else if (messageOwner.media.isWebPage()) {
            if (messageOwner.media.webpage.photo != null) {
                return "image/jpeg";
            }
        }
        return "";
    }

    public boolean canPreviewDocument() {
        return canPreviewDocument(getDocument());
    }

    public static boolean isGifDocument(WebFile document) {
        return document != null && (document.mime_type.equals("image/gif") || isNewGifDocument(document));
    }

    public static boolean isGifDocument(Document document) {
        return document != null /*&& !document.thumbs.isEmpty()*/ && document.mime_type != null && (document.mime_type.equals("image/gif") || isNewGifDocument(document));
    }

    public static boolean isDocumentHasThumb(Document document) {
        if (document == null || document.thumbs.isEmpty()) {
            return false;
        }
        for (int a = 0, N = document.thumbs.size(); a < N; a++) {
            PhotoSize photoSize = document.thumbs.get(a);
            if (photoSize != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean canPreviewDocument(Document document) {
        if (document != null && document.mime_type != null) {
            String mime = document.mime_type.toLowerCase();
            if (isDocumentHasThumb(document) && (mime.equals("image/png") || mime.equals("image/jpg") || mime.equals("image/jpeg"))) {
                for (int a = 0; a < document.attributes.size(); a++) {
                    Document.DocumentAttribute attribute = document.attributes.get(a);
                    if (attribute.isImageSize()) {
                        return attribute.w < 6000 && attribute.h < 6000;
                    }
                }
            } else if (BuildVars.DEBUG_PRIVATE_VERSION) {
                String fileName = FileLoader.getDocumentFileName(document);
                if (fileName.startsWith("tg_secret_sticker") && fileName.endsWith("json")) {
                    return true;
                } else if (fileName.endsWith(".svg")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isRoundVideoDocument(Document document) {
        if (document != null && "video/mp4".equals(document.mime_type)) {
            int width = 0;
            int height = 0;
            boolean round = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isVideo()) {
                    width = attribute.w;
                    height = attribute.w;
                    round = attribute.round_message;
                }
            }
            if (round && width <= 1280 && height <= 1280) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNewGifDocument(WebFile document) {
        if (document != null && "video/mp4".equals(document.mime_type)) {
            int width = 0;
            int height = 0;
            boolean animated = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isVideo()) {
                    width = attribute.w;
                    height = attribute.h;
                }
            }
            if (width <= 1280 && height <= 1280) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNewGifDocument(Document document) {
        if (document != null && "video/mp4".equals(document.mime_type)) {
            int width = 0;
            int height = 0;
            boolean animated = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isAnimated()) {
                    animated = true;
                } else if (attribute.isVideo()) {
                    width = attribute.w;
                    height = attribute.h;
                }
            }
            if (animated && width <= 1280 && height <= 1280) {
                return true;
            }
        }
        return false;
    }

    public void generateThumbs(boolean update) {
        if (emojiAnimatedSticker != null) {
            if (TextUtils.isEmpty(emojiAnimatedStickerColor) && isDocumentHasThumb(emojiAnimatedSticker)) {
                if (!update || photoThumbs == null) {
                    photoThumbs = new ArrayList<>();
                    photoThumbs.addAll(emojiAnimatedSticker.thumbs);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    updatePhotoSizeLocations(photoThumbs, emojiAnimatedSticker.thumbs);
                }
                photoThumbsObject = emojiAnimatedSticker;
            }
        } else if (messageOwner.media != null) {
            if (messageOwner.media.isPhoto()) {
                MessageMedia.Photo photo = messageOwner.media.photo;
                if (!update || photoThumbs != null && photoThumbs.size() != photo.sizes.size()) {
                    photoThumbs = new ArrayList<>(photo.sizes);
                } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                    for (int a = 0; a < photoThumbs.size(); a++) {
                        PhotoSize photoObject = photoThumbs.get(a);
                        if (photoObject == null) {
                            continue;
                        }
                        for (int b = 0; b < photo.sizes.size(); b++) {
                            PhotoSize size = photo.sizes.get(b);
                            if (size == null) {
                                continue;
                            }
                            if (size.type.equals(photoObject.type)) {
                                photoObject.location = size.location;
                                break;
                            }
                        }
                    }
                }
                photoThumbsObject = messageOwner.media.photo;
            } else if (messageOwner.media.isDocument()) {
                Document document = getDocument();
                if (isDocumentHasThumb(document)) {
                    if (!update || photoThumbs == null) {
                        photoThumbs = new ArrayList<>();
                        photoThumbs.addAll(document.thumbs);
                    } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                        updatePhotoSizeLocations(photoThumbs, document.thumbs);
                    }
                    photoThumbsObject = document;
                }
            } else if (messageOwner.media.isWebPage()) {
                MessageMedia.Photo photo = messageOwner.media.webpage.photo;
                Document document = messageOwner.media.webpage.document;
                if (photo != null) {
                    if (!update || photoThumbs == null) {
                        photoThumbs = new ArrayList<>(photo.sizes);
                    } else if (!photoThumbs.isEmpty()) {
                        updatePhotoSizeLocations(photoThumbs, photo.sizes);
                    }
                    photoThumbsObject = photo;
                } else if (document != null) {
                    if (isDocumentHasThumb(document)) {
                        if (!update) {
                            photoThumbs = new ArrayList<>();
                            photoThumbs.addAll(document.thumbs);
                        } else if (photoThumbs != null && !photoThumbs.isEmpty()) {
                            updatePhotoSizeLocations(photoThumbs, document.thumbs);
                        }
                        photoThumbsObject = document;
                    }
                }
            }
        }
    }

    private static void updatePhotoSizeLocations(ArrayList<PhotoSize> o, ArrayList<PhotoSize> n) {
        for (int a = 0, N = o.size(); a < N; a++) {
            PhotoSize photoObject = o.get(a);
            if (photoObject == null) {
                continue;
            }
            for (int b = 0, N2 = n.size(); b < N2; b++) {
                PhotoSize size = n.get(b);
                if (size ==null) {
                    continue;
                }
                if (size.type.equals(photoObject.type)) {
                    photoObject.location = size.location;
                    break;
                }
            }
        }
    }

    public CharSequence replaceWithLink(CharSequence source, String param, ArrayList<Integer> uids, AbstractMap<Integer, User> usersDict, SparseArray<User> sUsersDict) {
        int start = TextUtils.indexOf(source, param);
        if (start >= 0) {
            SpannableStringBuilder names = new SpannableStringBuilder("");
            for (int a = 0; a < uids.size(); a++) {
                User user = null;
                if (usersDict != null) {
                    user = usersDict.get(uids.get(a));
                } else if (sUsersDict != null) {
                    user = sUsersDict.get(uids.get(a));
                }
                if (user == null) {
                    user = MessagesController.getInstance(currentAccount).getUser(uids.get(a));
                }
                if (user != null) {
                    String name = UserObject.getUserName(user);
                    start = names.length();
                    if (names.length() != 0) {
                        names.append(", ");
                    }
                    names.append(name);
                    names.setSpan(new URLSpanNoUnderlineBold("" + user.id), start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return TextUtils.replace(source, new String[]{param}, new CharSequence[]{names});
        }
        return source;
    }

    public CharSequence replaceWithLink(CharSequence source, String param, UserChat object) {
        int start = TextUtils.indexOf(source, param);
        if (start >= 0) {
            String name;
            String id;
            if (object instanceof User) {
                name = UserObject.getUserName((User) object);
                id = "" + ((User) object).id;
            } else if (object instanceof Chat) {
                name = ((Chat) object).title;
                id = "" + -((Chat) object).id;
            } else {
                name = "";
                id = "0";
            }
            name = name.replace('\n', ' ');
            SpannableStringBuilder builder = new SpannableStringBuilder(TextUtils.replace(source, new String[]{param}, new String[]{name}));
            builder.setSpan(new URLSpanNoUnderlineBold("" + id), start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return source;
    }

    public String getExtension() {
        String fileName = getFileName();
        int idx = fileName.lastIndexOf('.');
        String ext = null;
        if (idx != -1) {
            ext = fileName.substring(idx + 1);
        }
        if (ext == null || ext.length() == 0) {
            ext = getDocument().mime_type;
        }
        if (ext == null) {
            ext = "";
        }
        ext = ext.toUpperCase();
        return ext;
    }

    public String getFileName() {
        if (messageOwner.media.isDocument()) {
            return FileLoader.getAttachFileName(getDocument());
        } else if (messageOwner.media.isPhoto()) {
            ArrayList<PhotoSize> sizes = messageOwner.media.photo.sizes;
            if (sizes.size() > 0) {
                PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(sizes, AndroidUtilities.getPhotoSize());
                if (sizeFull != null) {
                    return FileLoader.getAttachFileName(sizeFull);
                }
            }
        } else if (messageOwner.media.isWebPage()) {
            return FileLoader.getAttachFileName(messageOwner.media.webpage.document);
        }
        return "";
    }

    public int getMediaType() {
        if (isVideo()) {
            return FileLoader.MEDIA_DIR_VIDEO;
        } else if (isVoice()) {
            return FileLoader.MEDIA_DIR_AUDIO;
        } else if (messageOwner.media.isDocument()) {
            return FileLoader.MEDIA_DIR_DOCUMENT;
        } else if (messageOwner.media.isPhoto()) {
            return FileLoader.MEDIA_DIR_IMAGE;
        }
        return FileLoader.MEDIA_DIR_CACHE;
    }

    private static boolean containsUrls(CharSequence message) {
        if (message == null || message.length() < 2 || message.length() > 1024 * 20) {
            return false;
        }

        int length = message.length();

        int digitsInRow = 0;
        int schemeSequence = 0;
        int dotSequence = 0;

        char lastChar = 0;

        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);

            if (c >= '0' && c <= '9') {
                digitsInRow++;
                if (digitsInRow >= 6) {
                    return true;
                }
                schemeSequence = 0;
                dotSequence = 0;
            } else if (!(c != ' ' && digitsInRow > 0)) {
                digitsInRow = 0;
            }
            if ((c == '@' || c == '#' || c == '/' || c == '$') && i == 0 || i != 0 && (message.charAt(i - 1) == ' ' || message.charAt(i - 1) == '\n')) {
                return true;
            }
            if (c == ':') {
                if (schemeSequence == 0) {
                    schemeSequence = 1;
                } else {
                    schemeSequence = 0;
                }
            } else if (c == '/') {
                if (schemeSequence == 2) {
                    return true;
                }
                if (schemeSequence == 1) {
                    schemeSequence++;
                } else {
                    schemeSequence = 0;
                }
            } else if (c == '.') {
                if (dotSequence == 0 && lastChar != ' ') {
                    dotSequence++;
                } else {
                    dotSequence = 0;
                }
            } else if (c != ' ' && lastChar == '.' && dotSequence == 1) {
                return true;
            } else {
                dotSequence = 0;
            }
            lastChar = c;
        }
        return false;
    }

    public void generateLinkDescription() {
        if (linkDescription != null) {
            return;
        }
        int hashtagsType = 0;
        if (messageOwner.media.isWebPage() && messageOwner.media.webpage instanceof MessageMedia.WebPage && messageOwner.media.webpage.description != null) {
            linkDescription = Spannable.Factory.getInstance().newSpannable(messageOwner.media.webpage.description);
            String siteName = messageOwner.media.webpage.site_name;
            if (siteName != null) {
                siteName = siteName.toLowerCase();
            }
            if ("instagram".equals(siteName)) {
                hashtagsType = 1;
            } else if ("twitter".equals(siteName)) {
                hashtagsType = 2;
            }
        } else if (messageOwner.media.isInvoice() && messageOwner.media.description != null) {
            linkDescription = Spannable.Factory.getInstance().newSpannable(messageOwner.media.description);
        }
        if (!TextUtils.isEmpty(linkDescription)) {
            if (containsUrls(linkDescription)) {
                try {
                    AndroidUtilities.addLinks((Spannable) linkDescription, Linkify.WEB_URLS);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            linkDescription = Emoji.replaceEmoji(linkDescription, Theme.chat_msgTextPaint.getFontMetricsInt(), AndroidUtilities.dp(20), false);
            if (hashtagsType != 0) {
                if (!(linkDescription instanceof Spannable)) {
                    linkDescription = new SpannableStringBuilder(linkDescription);
                }
                addUrlsByPattern(isOutOwner(), linkDescription, false, hashtagsType, 0, false);
            }
        }
    }

    public void generateCaption() {
        if (caption != null || isRoundVideo()) {
            return;
        }
        if (!isMediaEmpty() && !TextUtils.isEmpty(messageOwner.message)) {
            caption = Emoji.replaceEmoji(messageOwner.message, Theme.chat_msgTextPaint.getFontMetricsInt(), AndroidUtilities.dp(20), false);

            boolean hasEntities;
            if (messageOwner.send_state != MESSAGE_SEND_STATE_SENT) {
                hasEntities = false;
                for (int a = 0; a < messageOwner.entities.size(); a++) {
                    if (!(messageOwner.entities.get(a).isMentionName())) {
                        hasEntities = true;
                        break;
                    }
                }
            } else {
                hasEntities = !messageOwner.entities.isEmpty();
            }

            boolean useManualParse = !hasEntities && (eventId != 0
                    || isOut()
                    && messageOwner.send_state != MESSAGE_SEND_STATE_SENT
                    || messageOwner.id < 0);

            if (useManualParse) {
                if (containsUrls(caption)) {
                    try {
                        AndroidUtilities.addLinks((Spannable) caption, Linkify.WEB_URLS | Linkify.PHONE_NUMBERS);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                addUrlsByPattern(isOutOwner(), caption, true, 0, 0, true);
            }

            addEntitiesToText(caption, useManualParse);
            if (isVideo()) {
                addUrlsByPattern(isOutOwner(), caption, true, 3, getDuration(), false);
            } else if (isMusic() || isVoice()) {
                addUrlsByPattern(isOutOwner(), caption, true, 4, getDuration(), false);
            }
        }
    }

    public static void addUrlsByPattern(boolean isOut, CharSequence charSequence, boolean botCommands, int patternType, int duration, boolean check) {
        try {
            Matcher matcher;
            if (patternType == 3 || patternType == 4) {
                if (videoTimeUrlPattern == null) {
                    videoTimeUrlPattern = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,3}):([0-5][0-9])\\b");
                }
                matcher = videoTimeUrlPattern.matcher(charSequence);
            } else if (patternType == 1) {
                if (instagramUrlPattern == null) {
                    instagramUrlPattern = Pattern.compile("(^|\\s|\\()@[a-zA-Z\\d_.]{1,32}|(^|\\s|\\()#[\\w.]+");
                }
                matcher = instagramUrlPattern.matcher(charSequence);
            } else {
                if (urlPattern == null) {
                    urlPattern = Pattern.compile("(^|\\s)/[a-zA-Z@\\d_]{1,255}|(^|\\s|\\()@[a-zA-Z\\d_]{1,32}|(^|\\s|\\()#[^0-9][\\w.]+|(^|\\s)\\$[A-Z]{3,8}([ ,.]|$)");
                }
                matcher = urlPattern.matcher(charSequence);
            }
            Spannable spannable = (Spannable) charSequence;
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                URLSpanNoUnderline url = null;
                if (patternType == 3 || patternType == 4) {
                    URLSpan[] spans = spannable.getSpans(start, end, URLSpan.class);
                    if (spans != null && spans.length > 0) {
                        continue;
                    }
                    int count = matcher.groupCount();
                    int s1 = matcher.start(1);
                    int e1 = matcher.end(1);
                    int s2 = matcher.start(2);
                    int e2 = matcher.end(2);
                    int s3 = matcher.start(3);
                    int e3 = matcher.end(3);
                    int minutes = Utilities.parseInt(charSequence.subSequence(s2, e2));
                    int seconds = Utilities.parseInt(charSequence.subSequence(s3, e3));
                    int hours = s1 >= 0 && e1 >= 0 ? Utilities.parseInt(charSequence.subSequence(s1, e1)) : -1;
                    seconds += minutes * 60;
                    if (hours > 0) {
                        seconds += hours * 60 * 60;
                    }
                    if (seconds > duration) {
                        continue;
                    }
                    if (patternType == 3) {
                        url = new URLSpanNoUnderline("video?" + seconds);
                    } else {
                        url = new URLSpanNoUnderline("audio?" + seconds);
                    }
                } else {
                    char ch = charSequence.charAt(start);
                    if (patternType != 0) {
                        if (ch != '@' && ch != '#') {
                            start++;
                        }
                        ch = charSequence.charAt(start);
                        if (ch != '@' && ch != '#') {
                            continue;
                        }
                    } else {
                        if (ch != '@' && ch != '#' && ch != '/' && ch != '$') {
                            start++;
                        }
                    }
                    if (patternType == 1) {
                        if (ch == '@') {
                            url = new URLSpanNoUnderline("https://instagram.com/" + charSequence.subSequence(start + 1, end).toString());
                        } else if (ch == '#') {
                            url = new URLSpanNoUnderline("https://www.instagram.com/explore/tags/" + charSequence.subSequence(start + 1, end).toString());
                        }
                    } else if (patternType == 2) {
                        if (ch == '@') {
                            url = new URLSpanNoUnderline("https://twitter.com/" + charSequence.subSequence(start + 1, end).toString());
                        } else if (ch == '#') {
                            url = new URLSpanNoUnderline("https://twitter.com/hashtag/" + charSequence.subSequence(start + 1, end).toString());
                        }
                    } else {
                        if (charSequence.charAt(start) == '/') {
                            if (botCommands) {
                                url = new URLSpanBotCommand(charSequence.subSequence(start, end).toString(), isOut ? 1 : 0);
                            }
                        } else {
                            url = new URLSpanNoUnderline(charSequence.subSequence(start, end).toString());
                        }
                    }
                }
                if (url != null) {
                    if (check) {
                        ClickableSpan[] spans = spannable.getSpans(start, end, ClickableSpan.class);
                        if (spans != null && spans.length > 0) {
                            spannable.removeSpan(spans[0]);
                        }
                    }
                    spannable.setSpan(url, start, end, 0);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static int[] getWebDocumentWidthAndHeight(WebDocument document) {
        if (document == null) {
            return null;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isImageSize()) {
                return new int[]{attribute.w, attribute.h};
            } else if (attribute.isVideo()) {
                return new int[]{attribute.w, attribute.h};
            }
        }
        return null;
    }

    public static int getWebDocumentDuration(WebDocument document) {
        if (document == null) {
            return 0;
        }
        for (int a = 0, size = document.attributes.size(); a < size; a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isVideo()) {
                return attribute.duration;
            } else if (attribute.isAudio()) {
                return attribute.duration;
            }
        }
        return 0;
    }

    public static int[] getInlineResultWidthAndHeight(BotInlineResult inlineResult) {
        int[] result = getWebDocumentWidthAndHeight(inlineResult.content);
        if (result == null) {
            result = getWebDocumentWidthAndHeight(inlineResult.thumb);
            if (result == null) {
                result = new int[]{0, 0};
            }
        }
        return result;
    }

    public static int getInlineResultDuration(BotInlineResult inlineResult) {
        int result = getWebDocumentDuration(inlineResult.content);
        if (result == 0) {
            result = getWebDocumentDuration(inlineResult.thumb);
        }
        return result;
    }

    public boolean hasValidGroupId() {
        return getGroupId() != 0 && photoThumbs != null && !photoThumbs.isEmpty();
    }

    public long getGroupIdForUse() {
        return localSentGroupId != 0 ? localSentGroupId : messageOwner.grouped_id;
    }

    public long getGroupId() {
        return localGroupId != 0 ? localGroupId : getGroupIdForUse();
    }

    public static void addLinks(boolean isOut, CharSequence messageText) {
        addLinks(isOut, messageText, true, false);
    }

    public static void addLinks(boolean isOut, CharSequence messageText, boolean botCommands, boolean check) {
        if (messageText instanceof Spannable && containsUrls(messageText)) {
            if (messageText.length() < 1000) {
                try {
                    AndroidUtilities.addLinks((Spannable) messageText, Linkify.WEB_URLS | Linkify.PHONE_NUMBERS);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                try {
                    AndroidUtilities.addLinks((Spannable) messageText, Linkify.WEB_URLS);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            addUrlsByPattern(isOut, messageText, botCommands, 0, 0, check);
        }
    }

    public void resetPlayingProgress() {
        audioProgress = 0.0f;
        audioProgressSec = 0;
        bufferedProgress = 0.0f;
    }

    private boolean addEntitiesToText(CharSequence text, boolean useManualParse) {
        return addEntitiesToText(text, false, useManualParse);
    }

    public boolean addEntitiesToText(CharSequence text, boolean photoViewer, boolean useManualParse) {
        if (isRestrictedMessage) {
            ArrayList<MessageEntity> entities = new ArrayList<>();
            MessageEntity entityItalic = new MessageEntity();
            entityItalic.setItalic(true);
            entityItalic.offset = 0;
            entityItalic.length = text.length();
            entities.add(entityItalic);
            return addEntitiesToText(text, entities, isOutOwner(), true, photoViewer, useManualParse);
        } else {
            return addEntitiesToText(text, messageOwner.entities, isOutOwner(), true, photoViewer, useManualParse);
        }
    }

    public static boolean addEntitiesToText(CharSequence text, ArrayList<MessageEntity> entities, boolean out,
            boolean usernames, boolean photoViewer, boolean useManualParse) {
        if (!(text instanceof Spannable)) {
            return false;
        }
        Spannable spannable = (Spannable) text;
        URLSpan[] spans = spannable.getSpans(0, text.length(), URLSpan.class);
        boolean hasUrls = spans != null && spans.length > 0;
        if (entities.isEmpty()) {
            return hasUrls;
        }

        byte t;
        if (photoViewer) {
            t = 2;
        } else if (out) {
            t = 1;
        } else {
            t = 0;
        }

        ArrayList<TextStyleSpan.TextStyleRun> runs = new ArrayList<>();
        ArrayList<MessageEntity> entitiesCopy = new ArrayList<>(entities);

        Collections.sort(entitiesCopy, (o1, o2) -> {
            if (o1.offset > o2.offset) {
                return 1;
            } else if (o1.offset < o2.offset) {
                return -1;
            }
            return 0;
        });
        for (int a = 0, N = entitiesCopy.size(); a < N; a++) {
            MessageEntity entity = entitiesCopy.get(a);
            if (entity.length <= 0 || entity.offset < 0 || entity.offset >= text.length()) {
                continue;
            } else if (entity.offset + entity.length > text.length()) {
                entity.length = text.length() - entity.offset;
            }

            if (!useManualParse || entity.isBold() ||
                    entity.isItalic() ||
                    entity.isStrike() ||
                    entity.isUnderline() ||
                    entity.isBlockquote() ||
                    entity.isCode() ||
                    entity.isPre() ||
                    entity.isMentionName() ||
                    entity.isTextUrl()) {
                if (spans != null && spans.length > 0) {
                    for (int b = 0; b < spans.length; b++) {
                        if (spans[b] == null) {
                            continue;
                        }
                        int start = spannable.getSpanStart(spans[b]);
                        int end = spannable.getSpanEnd(spans[b]);
                        if (entity.offset <= start && entity.offset + entity.length >= start || entity.offset <= end && entity.offset + entity.length >= end) {
                            spannable.removeSpan(spans[b]);
                            spans[b] = null;
                        }
                    }
                }
            }

            TextStyleSpan.TextStyleRun newRun = new TextStyleSpan.TextStyleRun();
            newRun.start = entity.offset;
            newRun.end = newRun.start + entity.length;
            MessageEntity urlEntity = null;
            if (entity.isStrike()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_STRIKE;
            } else if (entity.isUnderline()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_UNDERLINE;
            } else if (entity.isBlockquote()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_QUOTE;
            } else if (entity.isBold()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_BOLD;
            } else if (entity.isItalic()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_ITALIC;
            } else if (entity.isCode() || entity.isPre()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_MONO;
            } else if (entity.isMentionName()) {
                if (!usernames) {
                    continue;
                }
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else if (entity.isMentionName()) {
                if (!usernames) {
                    continue;
                }
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else {
                if (useManualParse && !(entity.isTextUrl())) {
                    continue;
                }
                if ((entity.isUrl() || entity.isTextUrl()) && Browser.isPassportUrl(entity.url)) {
                    continue;
                }
                if (entity.isMentionName() && !usernames) {
                    continue;
                }
                newRun.flags = TextStyleSpan.FLAG_STYLE_URL;
                newRun.urlEntity = entity;
            }

            for (int b = 0, N2 = runs.size(); b < N2; b++) {
                TextStyleSpan.TextStyleRun run = runs.get(b);

                if (newRun.start > run.start) {
                    if (newRun.start >= run.end) {
                        continue;
                    }

                    if (newRun.end < run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.merge(run);
                        b++;
                        N2++;
                        runs.add(b, r);

                        r = new TextStyleSpan.TextStyleRun(run);
                        r.start = newRun.end;
                        b++;
                        N2++;
                        runs.add(b, r);
                    } else if (newRun.end >= run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.merge(run);
                        r.end = run.end;
                        b++;
                        N2++;
                        runs.add(b, r);
                    }

                    int temp = newRun.start;
                    newRun.start = run.end;
                    run.end = temp;
                } else {
                    if (run.start >= newRun.end) {
                        continue;
                    }
                    int temp = run.start;
                    if (newRun.end == run.end) {
                        run.merge(newRun);
                    } else if (newRun.end < run.end) {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(run);
                        r.merge(newRun);
                        r.end = newRun.end;
                        b++;
                        N2++;
                        runs.add(b, r);

                        run.start = newRun.end;
                    } else {
                        TextStyleSpan.TextStyleRun r = new TextStyleSpan.TextStyleRun(newRun);
                        r.start = run.end;
                        b++;
                        N2++;
                        runs.add(b, r);

                        run.merge(newRun);
                    }
                    newRun.end = temp;
                }
            }
            if (newRun.start < newRun.end) {
                runs.add(newRun);
            }
        }

        int count = runs.size();
        for (int a = 0; a < count; a++) {
            TextStyleSpan.TextStyleRun run = runs.get(a);

            String url = run.urlEntity != null ? TextUtils.substring(text, run.urlEntity.offset, run.urlEntity.offset + run.urlEntity.length) : null;
            if (run.urlEntity.isBotCommand()) {
                spannable.setSpan(new URLSpanBotCommand(url, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isHashtag() || run.urlEntity.isMention() || run.urlEntity.isCashtag()) {
                spannable.setSpan(new URLSpanNoUnderline(url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isEmail()) {
                spannable.setSpan(new URLSpanReplacement("mailto:" + url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isUrl()) {
                hasUrls = true;
                String lowerCase = url.toLowerCase();
                if (!lowerCase.contains("://")) {
                    spannable.setSpan(new URLSpanBrowser("http://" + url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    spannable.setSpan(new URLSpanBrowser(url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (run.urlEntity.isBankCard()) {
                hasUrls = true;
                spannable.setSpan(new URLSpanNoUnderline("card:" + url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isPhone()) {
                hasUrls = true;
                String tel = PhoneFormat.stripExceptNumbers(url);
                if (url.startsWith("+")) {
                    tel = "+" + tel;
                }
                spannable.setSpan(new URLSpanBrowser("tel:" + tel, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isTextUrl()) {
                spannable.setSpan(new URLSpanReplacement(run.urlEntity.url, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isMentionName()) {
                spannable.setSpan(new URLSpanUserMention("" + (run.urlEntity).user_id, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (run.urlEntity.isMentionName()) {
                spannable.setSpan(new URLSpanUserMention("" + (run.urlEntity).user_id, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if ((run.flags & TextStyleSpan.FLAG_STYLE_MONO) != 0) {
                spannable.setSpan(new URLSpanMono(spannable, run.start, run.end, t, run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                spannable.setSpan(new TextStyleSpan(run), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return hasUrls;
    }

    public boolean needDrawShareButton() {
        if (scheduled) {
            return false;
        } else if (eventId != 0) {
            return false;
        } else if (messageOwner.fwd_from != null && !isOutOwner() && messageOwner.fwd_from.saved_from_chat_id != 0 && getDialogId() == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return true;
        } else if (type == TYPE_STICKER || type == TYPE_ANIMATED_STICKER) {
            return false;
        } else if (messageOwner.fwd_from != null && messageOwner.fwd_from.channel_id != 0 && !isOutOwner()) {
            return true;
        } else if (isFromUser()) {
            if (messageOwner.media == null
                    || messageOwner.media == null
                    || messageOwner.media.isWebPage() && !(messageOwner.media.webpage != null)) {
                return false;
            }
            User user = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id);
            if (user != null && user.bot) {
                return true;
            }
            if (!isOut()) {
                if (messageOwner.media.isInvoice()) {
                    return true;
                }
                if (isMegagroup()) {
                    Chat chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.to_id);
                    return chat != null && chat.username != null && chat.username.length() > 0 && !(messageOwner.media.isContact()) && !(messageOwner.media.isGeo());
                }
            }
        } else if (messageOwner.from_id < 0 || messageOwner.post) {
            if (messageOwner.to_id != 0 && (messageOwner.via_bot_id == 0 && messageOwner.reply_to_msg_id == 0 || type != TYPE_STICKER && type != TYPE_ANIMATED_STICKER)) {
                return true;
            }
        }
        return false;
    }

    public boolean isYouTubeVideo() {
        return messageOwner.media.isWebPage() && messageOwner.media.webpage != null && !TextUtils.isEmpty(messageOwner.media.webpage.embed_url) && "YouTube".equals(messageOwner.media.webpage.site_name);
    }

    public int getMaxMessageTextWidth() {
        int maxWidth = 0;
        if (AndroidUtilities.isTablet() && eventId != 0) {
            generatedWithMinSize = AndroidUtilities.dp(530);
        } else {
            generatedWithMinSize = AndroidUtilities.isTablet() ? AndroidUtilities.getMinTabletSide() : AndroidUtilities.displaySize.x;
        }
        generatedWithDensity = AndroidUtilities.density;
        if (messageOwner.media.isWebPage() && messageOwner.media.webpage != null && "telegram_background".equals(messageOwner.media.webpage.type)) {
            try {
                Uri uri = Uri.parse(messageOwner.media.webpage.url);
                String segment = uri.getLastPathSegment();
                if (uri.getQueryParameter("bg_color") != null) {
                    maxWidth = AndroidUtilities.dp(220);
                } else if (segment.length() == 6 || segment.length() == 13 && segment.charAt(6) == '-') {
                    maxWidth = AndroidUtilities.dp(200);
                }
            } catch (Exception ignore) {

            }
        } else if (isAndroidTheme()) {
            maxWidth = AndroidUtilities.dp(200);
        }
        if (maxWidth == 0) {
            maxWidth = generatedWithMinSize - AndroidUtilities.dp(needDrawAvatarInternal() && !isOutOwner() ? 132 : 80);
            if (needDrawShareButton() && !isOutOwner()) {
                maxWidth -= AndroidUtilities.dp(10);
            }
        }
        return maxWidth;
    }

    public void generateLayout(User fromUser) {
        if (type != 0 || messageOwner.to_id == 0 || TextUtils.isEmpty(messageText)) {
            return;
        }

        generateLinkDescription();
        textLayoutBlocks = new ArrayList<>();
        textWidth = 0;

        boolean hasEntities;
        if (messageOwner.send_state != MESSAGE_SEND_STATE_SENT) {
            hasEntities = false;
            /*for (int a = 0; a < messageOwner.entities.size(); a++) {
                if (!(messageOwner.entities.get(a) instanceof TLRPC.TL_inputMessageEntityMentionName)) {
                    hasEntities = true;
                    break;
                }
            }*/
        } else {
            hasEntities = !messageOwner.entities.isEmpty();
        }

        boolean useManualParse = !hasEntities && (
                eventId != 0
                        || messageOwner.media.isInvoice()
                        || isOut()
                        && messageOwner.send_state != MESSAGE_SEND_STATE_SENT
                        || messageOwner.id < 0
                        || messageOwner.media.isUnsupported());

        if (useManualParse) {
            addLinks(isOutOwner(), messageText, true, true);
        } else {
            if (messageText instanceof Spannable && messageText.length() < 1000) {
                try {
                    AndroidUtilities.addLinks((Spannable) messageText, Linkify.PHONE_NUMBERS);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }
        if (isYouTubeVideo() || replyMessageObject != null && replyMessageObject.isYouTubeVideo()) {
            addUrlsByPattern(isOutOwner(), messageText, false, 3, Integer.MAX_VALUE, false);
        } else if (replyMessageObject != null) {
            if (replyMessageObject.isVideo()) {
                addUrlsByPattern(isOutOwner(), messageText, false, 3, replyMessageObject.getDuration(), false);
            } else if (replyMessageObject.isMusic() || replyMessageObject.isVoice()) {
                addUrlsByPattern(isOutOwner(), messageText, false, 4, replyMessageObject.getDuration(), false);
            }
        }

        boolean hasUrls = addEntitiesToText(messageText, useManualParse);

        int maxWidth = getMaxMessageTextWidth();

        StaticLayout textLayout;

        TextPaint paint;
        paint = Theme.chat_msgTextPaint;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textLayout = StaticLayout.Builder.obtain(messageText, 0, messageText.length(), paint, maxWidth)
                                                 .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                                                 .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                                                 .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                                 .build();
            } else {
                textLayout = new StaticLayout(messageText, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
        } catch (Exception e) {
            FileLog.e(e);
            return;
        }

        textHeight = textLayout.getHeight();
        linesCount = textLayout.getLineCount();

        int blocksCount;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            blocksCount = 1;
        } else {
            blocksCount = (int) Math.ceil((float) linesCount / LINES_PER_BLOCK);
        }
        int linesOffset = 0;
        float prevOffset = 0;

        for (int a = 0; a < blocksCount; a++) {
            int currentBlockLinesCount;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentBlockLinesCount = linesCount;
            } else {
                currentBlockLinesCount = Math.min(LINES_PER_BLOCK, linesCount - linesOffset);
            }
            TextLayoutBlock block = new TextLayoutBlock();

            if (blocksCount == 1) {
                block.textLayout = textLayout;
                block.textYOffset = 0;
                block.charactersOffset = 0;
                block.charactersEnd = textLayout.getText().length();
                if (emojiOnlyCount != 0) {
                    switch (emojiOnlyCount) {
                        case 1:
                            textHeight -= AndroidUtilities.dp(5.3f);
                            block.textYOffset -= AndroidUtilities.dp(5.3f);
                            break;
                        case 2:
                            textHeight -= AndroidUtilities.dp(4.5f);
                            block.textYOffset -= AndroidUtilities.dp(4.5f);
                            break;
                        case 3:
                            textHeight -= AndroidUtilities.dp(4.2f);
                            block.textYOffset -= AndroidUtilities.dp(4.2f);
                            break;
                    }

                }
                block.height = textHeight;
            } else {
                int startCharacter = textLayout.getLineStart(linesOffset);
                int endCharacter = textLayout.getLineEnd(linesOffset + currentBlockLinesCount - 1);
                if (endCharacter < startCharacter) {
                    continue;
                }
                block.charactersOffset = startCharacter;
                block.charactersEnd = endCharacter;
                try {
                    if (hasUrls && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        block.textLayout = StaticLayout.Builder.obtain(messageText, startCharacter, endCharacter, paint, maxWidth + AndroidUtilities.dp(2))
                                                               .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                                                               .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                                                               .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                                               .build();
                    } else {
                        block.textLayout = new StaticLayout(messageText, startCharacter, endCharacter, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    }
                    block.textYOffset = textLayout.getLineTop(linesOffset);
                    if (a != 0) {
                        block.height = (int) (block.textYOffset - prevOffset);
                    }
                    block.height = Math.max(block.height, block.textLayout.getLineBottom(block.textLayout.getLineCount() - 1));
                    prevOffset = block.textYOffset;
                } catch (Exception e) {
                    FileLog.e(e);
                    continue;
                }
                if (a == blocksCount - 1) {
                    currentBlockLinesCount = Math.max(currentBlockLinesCount, block.textLayout.getLineCount());
                    try {
                        textHeight = Math.max(textHeight, (int) (block.textYOffset + block.textLayout.getHeight()));
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            }

            textLayoutBlocks.add(block);

            float lastLeft;
            try {
                lastLeft = block.textLayout.getLineLeft(currentBlockLinesCount - 1);
                if (a == 0 && lastLeft >= 0) {
                    textXOffset = lastLeft;
                }
            } catch (Exception e) {
                lastLeft = 0;
                if (a == 0) {
                    textXOffset = 0;
                }
                FileLog.e(e);
            }

            float lastLine;
            try {
                lastLine = block.textLayout.getLineWidth(currentBlockLinesCount - 1);
            } catch (Exception e) {
                lastLine = 0;
                FileLog.e(e);
            }

            int linesMaxWidth = (int) Math.ceil(lastLine);
            if (linesMaxWidth > maxWidth + 80) {
                linesMaxWidth = maxWidth;
            }
            int lastLineWidthWithLeft;
            int linesMaxWidthWithLeft;

            if (a == blocksCount - 1) {
                lastLineWidth = linesMaxWidth;
            }

            linesMaxWidthWithLeft = lastLineWidthWithLeft = (int) Math.ceil(lastLine + lastLeft);

            if (currentBlockLinesCount > 1) {
                boolean hasNonRTL = false;
                float textRealMaxWidth = 0, textRealMaxWidthWithLeft = 0, lineWidth, lineLeft;
                for (int n = 0; n < currentBlockLinesCount; n++) {
                    try {
                        lineWidth = block.textLayout.getLineWidth(n);
                    } catch (Exception e) {
                        FileLog.e(e);
                        lineWidth = 0;
                    }

                    if (lineWidth > maxWidth + 20) {
                        lineWidth = maxWidth;
                    }

                    try {
                        lineLeft = block.textLayout.getLineLeft(n);
                    } catch (Exception e) {
                        FileLog.e(e);
                        lineLeft = 0;
                    }

                    if (lineLeft > 0) {
                        textXOffset = Math.min(textXOffset, lineLeft);
                        block.directionFlags |= 1;
                        hasRtl = true;
                    } else {
                        block.directionFlags |= 2;
                    }

                    try {
                        if (!hasNonRTL && lineLeft == 0 && block.textLayout.getParagraphDirection(n) == Layout.DIR_LEFT_TO_RIGHT) {
                            hasNonRTL = true;
                        }
                    } catch (Exception ignore) {
                        hasNonRTL = true;
                    }

                    textRealMaxWidth = Math.max(textRealMaxWidth, lineWidth);
                    textRealMaxWidthWithLeft = Math.max(textRealMaxWidthWithLeft, lineWidth + lineLeft);
                    linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
                    linesMaxWidthWithLeft = Math.max(linesMaxWidthWithLeft, (int) Math.ceil(lineWidth + lineLeft));
                }
                if (hasNonRTL) {
                    textRealMaxWidth = textRealMaxWidthWithLeft;
                    if (a == blocksCount - 1) {
                        lastLineWidth = lastLineWidthWithLeft;
                    }
                } else if (a == blocksCount - 1) {
                    lastLineWidth = linesMaxWidth;
                }
                textWidth = Math.max(textWidth, (int) Math.ceil(textRealMaxWidth));
            } else {
                if (lastLeft > 0) {
                    textXOffset = Math.min(textXOffset, lastLeft);
                    if (textXOffset == 0) {
                        linesMaxWidth += lastLeft;
                    }
                    hasRtl = blocksCount != 1;
                    block.directionFlags |= 1;
                } else {
                    block.directionFlags |= 2;
                }

                textWidth = Math.max(textWidth, Math.min(maxWidth, linesMaxWidth));
            }

            linesOffset += currentBlockLinesCount;
        }
    }

    public boolean isOut() {
        return messageOwner.out;
    }

    public boolean isOutOwner() {
        if (!messageOwner.out || messageOwner.from_id <= 0 || messageOwner.post) {
            return false;
        }
        if (messageOwner.fwd_from == null) {
            return true;
        }
        int selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (getDialogId() == selfUserId) {
            return messageOwner.fwd_from.from_id == selfUserId && (messageOwner.fwd_from.saved_from_chat_id == 0 || messageOwner.fwd_from.saved_from_chat_id == selfUserId) || messageOwner.fwd_from.saved_from_chat_id != 0 && messageOwner.fwd_from.saved_from_chat_id == selfUserId;
        }
        return messageOwner.fwd_from.saved_from_chat_id ==0 || messageOwner.fwd_from.saved_from_chat_id == selfUserId;
    }

    public boolean needDrawAvatar() {
        return isFromUser() || eventId != 0
                || messageOwner.fwd_from != null && messageOwner.fwd_from.saved_from_chat_id != 0;
    }

    private boolean needDrawAvatarInternal() {
        return isFromChat() && isFromUser() || eventId != 0 || messageOwner.fwd_from != null && messageOwner.fwd_from.saved_from_chat_id != 0;
    }

    public boolean isFromChat() {
        if (getDialogId() == UserConfig.getInstance(currentAccount).clientUserId) {
            return true;
        }
        if (isMegagroup() || messageOwner.to_id != 0) {
            return true;
        }
        if (messageOwner.to_id != 0) {
            Chat chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.to_id);
            return chat != null && chat.megagroup;
        }
        return false;
    }

    public boolean isFromUser() {
        return messageOwner.from_id > 0 && !messageOwner.post;
    }

    public boolean isForwardedChannelPost() {
        return messageOwner.from_id <= 0 && messageOwner.fwd_from != null && messageOwner.fwd_from.channel_post != 0;
    }

    public boolean isUnread() {
        return messageOwner.unread;
    }

    public boolean isContentUnread() {
        return messageOwner.media_unread;
    }

    public void setIsRead() {
        messageOwner.unread = false;
    }

    public int getUnradFlags() {
        return getUnreadFlags(messageOwner);
    }

    public static int getUnreadFlags(Message message) {
        int flags = 0;
        if (!message.unread) {
            flags |= 1;
        }
        if (!message.media_unread) {
            flags |= 2;
        }
        return flags;
    }

    public void setContentIsRead() {
        messageOwner.media_unread = false;
    }

    public int getId() {
        return messageOwner.id;
    }

    public int getRealId() {
        return messageOwner.realId != 0 ? messageOwner.realId : messageOwner.id;
    }

    public static int getMessageSize(Message message) {
        Document document;
        if (message.media.isWebPage()) {
            document = message.media.webpage.document;
        } else {
            document = message.media != null ? message.media.document : null;
        }
        if (document != null) {
            return document.size;
        }
        return 0;
    }

    public int getSize() {
        return getMessageSize(messageOwner);
    }

    public long getIdWithChannel() {
        long id = messageOwner.id;
        if (messageOwner.to_id != 0 && messageOwner.to_id != 0) {
            id |= ((long) messageOwner.to_id) << 32;
        }
        return id;
    }

    public int getChannelId() {
        if (messageOwner.to_id != 0) {
            return messageOwner.to_id;
        }
        return 0;
    }

    public static boolean shouldEncryptPhotoOrVideo(Message message) {
        return (message.media.isPhoto() || message.media.isDocument()) && message.media.ttl_seconds != 0;
    }

    public boolean shouldEncryptPhotoOrVideo() {
        return shouldEncryptPhotoOrVideo(messageOwner);
    }

    public static boolean isSecretMedia(Message message) {
        if (message instanceof Message) {
            return (message.media.isPhoto() || message.media.isDocument()) && message.media.ttl_seconds != 0;
        }
        return false;
    }

    public boolean needDrawBluredPreview() {
        if (messageOwner instanceof Message) {
            return (messageOwner.media.isPhoto() || messageOwner.media.isDocument()) && messageOwner.media.ttl_seconds != 0;
        }
        return false;
    }

    public boolean isSecretMedia() {
        if (messageOwner instanceof Message) {
            return (messageOwner.media.isPhoto() || messageOwner.media.isDocument()) && messageOwner.media.ttl_seconds != 0;
        }
        return false;
    }

    public static void setUnreadFlags(Message message, int flag) {
        message.unread = (flag & 1) == 0;
        message.media_unread = (flag & 2) == 0;
    }

    public static boolean isUnread(Message message) {
        return message.unread;
    }

    public static boolean isContentUnread(Message message) {
        return message.media_unread;
    }

    public boolean isMegagroup() {
        return isMegagroup(messageOwner);
    }

    public boolean isSavedFromMegagroup() {
        if (messageOwner.fwd_from != null && messageOwner.fwd_from.saved_from_chat_id != 0) {
            Chat chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.fwd_from.saved_from_chat_id);
            return ChatObject.isMegagroup(chat);
        }
        return false;
    }

    public static boolean isMegagroup(Message message) {
        return (message.flags & Message.MESSAGE_FLAG_MEGAGROUP) != 0;
    }

    public static boolean isOut(Message message) {
        return message.out;
    }

    public long getDialogId() {
        return getDialogId(messageOwner);
    }

    public boolean canStreamVideo() {
        Document document = getDocument();
        if (document == null) {
            return false;
        }
        if (SharedConfig.streamAllVideo) {
            return true;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isVideo()) {
                return attribute.supports_streaming;
            }
        }
        if (SharedConfig.streamMkv && "video/x-matroska".equals(document.mime_type)) {
            return true;
        }
        return false;
    }

    public static long getDialogId(Message message) {
        if (message.dialog_id == 0 && message.to_id != 0) {
            if (message.to_id != 0) {
                message.dialog_id = -message.to_id;
            } else if (message.to_id != 0) {
                message.dialog_id = -message.to_id;
            } else if (isOut(message)) {
                message.dialog_id = message.to_id;
            } else {
                message.dialog_id = message.from_id;
            }
        }
        return message.dialog_id;
    }

    public boolean isSending() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_SENDING && messageOwner.id < 0;
    }

    public boolean isEditing() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_EDITING && messageOwner.id > 0;
    }

    public boolean isSendError() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_SEND_ERROR && messageOwner.id < 0 || scheduled && messageOwner.id > 0 && messageOwner.date < ConnectionsManager.getInstance(currentAccount).getCurrentTime() - 60;
    }

    public boolean isSent() {
        return messageOwner.send_state == MESSAGE_SEND_STATE_SENT || messageOwner.id > 0;
    }

    public int getSecretTimeLeft() {
        int secondsLeft = messageOwner.ttl;
        if (messageOwner.destroyTime != 0) {
            secondsLeft = Math.max(0, messageOwner.destroyTime - ConnectionsManager.getInstance(currentAccount).getCurrentTime());
        }
        return secondsLeft;
    }

    public String getSecretTimeString() {
        if (!isSecretMedia()) {
            return null;
        }
        int secondsLeft = getSecretTimeLeft();
        String str;
        if (secondsLeft < 60) {
            str = secondsLeft + "s";
        } else {
            str = secondsLeft / 60 + "m";
        }
        return str;
    }

    public String getDocumentName() {
        return FileLoader.getDocumentFileName(getDocument());
    }

    public static boolean isStickerDocument(Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isSticker()) {
                    return "image/webp".equals(document.mime_type);
                }
            }
        }
        return false;
    }

    public static boolean isStickerHasSet(Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isSticker() && attribute.stickerset != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAnimatedStickerDocument(Document document, boolean allowWithoutSet) {
        if (document != null && ("application/x-tgsticker".equals(document.mime_type) && !document.thumbs.isEmpty() || "application/x-tgsdice".equals(document.mime_type))) {
            if (allowWithoutSet) {
                return true;
            }
            for (int a = 0, N = document.attributes.size(); a < N; a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isSticker()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canAutoplayAnimatedSticker(Document document) {
        return isAnimatedStickerDocument(document, true) && SharedConfig.getDevicePerfomanceClass() != SharedConfig.PERFORMANCE_CLASS_LOW;
    }

    public static boolean isMaskDocument(Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isSticker() && attribute.mask) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isVoiceDocument(Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isAudio()) {
                    return attribute.voice;
                }
            }
        }
        return false;
    }

    public static boolean isVoiceWebDocument(WebFile webDocument) {
        return webDocument != null && webDocument.mime_type.equals("audio/ogg");
    }

    public static boolean isImageWebDocument(WebFile webDocument) {
        return webDocument != null && !isGifDocument(webDocument) && webDocument.mime_type.startsWith("image/");
    }

    public static boolean isVideoWebDocument(WebFile webDocument) {
        return webDocument != null && webDocument.mime_type.startsWith("video/");
    }

    public static boolean isMusicDocument(Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isAudio()) {
                    return !attribute.voice;
                }
            }
            if (!TextUtils.isEmpty(document.mime_type)) {
                String mime = document.mime_type.toLowerCase();
                if (mime.equals("audio/flac") || mime.equals("audio/ogg") || mime.equals("audio/opus") || mime.equals("audio/x-opus+ogg")) {
                    return true;
                } else if (mime.equals("application/octet-stream") && FileLoader.getDocumentFileName(document).endsWith(".opus")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static VideoSize getDocumentVideoThumb(Document document) {
        if (document == null || document.video_thumbs.isEmpty()) {
            return null;
        }
        return document.video_thumbs.get(0);
    }

    public static boolean isVideoDocument(Document document) {
        if (document == null) {
            return false;
        }
        boolean isAnimated = false;
        boolean isVideo = false;
        int width = 0;
        int height = 0;
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isVideo()) {
                if (attribute.round_message) {
                    return false;
                }
                isVideo = true;
                width = attribute.w;
                height = attribute.h;
            } else if (attribute.isAnimated()) {
                isAnimated = true;
            }
        }
        if (isAnimated && (width > 1280 || height > 1280)) {
            isAnimated = false;
        }
        if (SharedConfig.streamMkv && !isVideo && "video/x-matroska".equals(document.mime_type)) {
            isVideo = true;
        }
        return isVideo && !isAnimated;
    }

    public Document getDocument() {
        if (emojiAnimatedSticker != null) {
            return emojiAnimatedSticker;
        }
        return getDocument(messageOwner);
    }

    public static Document getDocument(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return message.media.webpage.document;
        }
        return message.media != null ? message.media.document : null;
    }

    public static MessageMedia.Photo getPhoto(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return message.media.webpage.photo;
        }
        return message.media != null ? message.media.photo : null;
    }

    public static boolean isStickerMessage(Message message) {
        return message.media != null && isStickerDocument(message.media.document);
    }

    public static boolean isAnimatedStickerMessage(Message message) {
        if (message.stickerVerified != 1) {
            return false;
        }
        return message.media != null && isAnimatedStickerDocument(message.media.document, message.out);
    }

    public static boolean isLocationMessage(Message message) {
        return message.media != null && (message.media.isGeo() || message.media.isGeoLive() || message.media.isVenue());
    }

    public static boolean isMaskMessage(Message message) {
        return message.media != null && isMaskDocument(message.media.document);
    }

    public static boolean isMusicMessage(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return isMusicDocument(message.media.webpage.document);
        }
        return message.media != null && isMusicDocument(message.media.document);
    }

    public static boolean isGifMessage(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return isGifDocument(message.media.webpage.document);
        }
        return message.media != null && isGifDocument(message.media.document);
    }

    public static boolean isRoundVideoMessage(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return isRoundVideoDocument(message.media.webpage.document);
        }
        return message.media != null && isRoundVideoDocument(message.media.document);
    }

    public static boolean isPhoto(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return message.media.webpage.photo!=null && !(message.media.webpage.document !=null);
        }
        return message.media != null && message.media.isPhoto();
    }

    public static boolean isVoiceMessage(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return isVoiceDocument(message.media.webpage.document);
        }
        return message.media != null && isVoiceDocument(message.media.document);
    }

    public static boolean isNewGifMessage(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return isNewGifDocument(message.media.webpage.document);
        }
        return message.media != null && isNewGifDocument(message.media.document);
    }

    public static boolean isLiveLocationMessage(Message message) {
        return message.media != null && message.media.isGeoLive();
    }

    public static boolean isVideoMessage(Message message) {
        if (message.media != null && message.media.isWebPage()) {
            return isVideoDocument(message.media.webpage.document);
        }
        return message.media != null && isVideoDocument(message.media.document);
    }

    public static boolean isGameMessage(Message message) {
        return false;
    }

    public static boolean isInvoiceMessage(Message message) {
        return message.media != null && message.media.isInvoice();
    }

    public static InputStickerSet getInputStickerSet(Message message) {
        if (message.media != null && message.media.document != null) {
            return getInputStickerSet(message.media.document);
        }
        return null;
    }

    public static InputStickerSet getInputStickerSet(Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0, N = document.attributes.size(); a < N; a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isSticker()) {
                return attribute.stickerset;
            }
        }
        return null;
    }

    public static long getStickerSetId(Document document) {
        if (document == null) {
            return -1;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isSticker()) {
                return attribute.stickerset.id;
            }
        }
        return -1;
    }

    public static String getStickerSetName(Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isSticker()) {
                return attribute.stickerset.short_name;
            }
        }
        return null;
    }

    public String getStickerChar() {
        Document document = getDocument();
        if (document != null) {
            for (Document.DocumentAttribute attribute : document.attributes) {
                if (attribute.isSticker()) {
                    return attribute.alt;
                }
            }
        }
        return null;
    }

    public int getApproximateHeight() {
        if (type == 0) {
            int height = textHeight + (messageOwner.media.isWebPage() && messageOwner.media.webpage instanceof MessageMedia.WebPage ? AndroidUtilities.dp(100) : 0);
            if (isReply()) {
                height += AndroidUtilities.dp(42);
            }
            return height;
        } else if (type == 2) {
            return AndroidUtilities.dp(72);
        } else if (type == 12) {
            return AndroidUtilities.dp(71);
        } else if (type == 9) {
            return AndroidUtilities.dp(100);
        } else if (type == 4) {
            return AndroidUtilities.dp(114);
        } else if (type == 14) {
            return AndroidUtilities.dp(82);
        } else if (type == 10) {
            return AndroidUtilities.dp(30);
        } else if (type == 11) {
            return AndroidUtilities.dp(50);
        } else if (type == TYPE_ROUND_VIDEO) {
            return AndroidUtilities.roundMessageSize;
        } else if (type == TYPE_STICKER || type == TYPE_ANIMATED_STICKER) {
            float maxHeight = AndroidUtilities.displaySize.y * 0.4f;
            float maxWidth;
            if (AndroidUtilities.isTablet()) {
                maxWidth = AndroidUtilities.getMinTabletSide() * 0.5f;
            } else {
                maxWidth = AndroidUtilities.displaySize.x * 0.5f;
            }
            int photoHeight = 0;
            int photoWidth = 0;
            Document document = getDocument();
            for (int a = 0, N = document.attributes.size(); a < N; a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isImageSize()) {
                    photoWidth = attribute.w;
                    photoHeight = attribute.h;
                    break;
                }
            }
            if (photoWidth == 0) {
                photoHeight = (int) maxHeight;
                photoWidth = photoHeight + AndroidUtilities.dp(100);
            }
            if (photoHeight > maxHeight) {
                photoWidth *= maxHeight / photoHeight;
                photoHeight = (int) maxHeight;
            }
            if (photoWidth > maxWidth) {
                photoHeight *= maxWidth / photoWidth;
            }
            return photoHeight + AndroidUtilities.dp(14);
        } else {
            int photoHeight;
            int photoWidth;

            if (AndroidUtilities.isTablet()) {
                photoWidth = (int) (AndroidUtilities.getMinTabletSide() * 0.7f);
            } else {
                photoWidth = (int) (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) * 0.7f);
            }
            photoHeight = photoWidth + AndroidUtilities.dp(100);
            if (photoWidth > AndroidUtilities.getPhotoSize()) {
                photoWidth = AndroidUtilities.getPhotoSize();
            }
            if (photoHeight > AndroidUtilities.getPhotoSize()) {
                photoHeight = AndroidUtilities.getPhotoSize();
            }
            PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());

            if (currentPhotoObject != null) {
                float scale = (float) currentPhotoObject.w / (float) photoWidth;
                int h = (int) (currentPhotoObject.h / scale);
                if (h == 0) {
                    h = AndroidUtilities.dp(100);
                }
                if (h > photoHeight) {
                    h = photoHeight;
                } else if (h < AndroidUtilities.dp(120)) {
                    h = AndroidUtilities.dp(120);
                }
                if (needDrawBluredPreview()) {
                    if (AndroidUtilities.isTablet()) {
                        h = (int) (AndroidUtilities.getMinTabletSide() * 0.5f);
                    } else {
                        h = (int) (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) * 0.5f);
                    }
                }
                photoHeight = h;
            }
            return photoHeight + AndroidUtilities.dp(14);
        }
    }

    public String getStickerEmoji() {
        Document document = getDocument();
        if (document == null) {
            return null;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isSticker()) {
                return attribute.alt != null && attribute.alt.length() > 0 ? attribute.alt : null;
            }
        }
        return null;
    }

    public boolean isAnimatedEmoji() {
        return emojiAnimatedSticker != null;
    }

    public boolean isDice() {
        return messageOwner.media.isDice();
    }

    public String getDiceEmoji() {
        return "";
//        if (!isDice()) {
//            return null;
//        }
//        MessageMedia messageMediaDice = messageOwner.media;
//        if (TextUtils.isEmpty(messageMediaDice.emoticon)) {
//            return "\uD83C\uDFB2";
//        }
//        return messageMediaDice.emoticon;
    }

    public int getDiceValue() {
//        if (messageOwner.media.isDice()) {
//            return messageOwner.media.value;
//        }
        return -1;
    }

    public boolean isSticker() {
        if (type != 1000) {
            return type == TYPE_STICKER;
        }
        return isStickerDocument(getDocument());
    }

    public boolean isAnimatedSticker() {
        if (type != 1000) {
            return type == TYPE_ANIMATED_STICKER;
        }
        if (messageOwner.stickerVerified != 1) {
            return false;
        }
        return isAnimatedStickerDocument(getDocument(), emojiAnimatedSticker != null || isOut());
    }

    public boolean isAnyKindOfSticker() {
        return type == TYPE_STICKER || type == TYPE_ANIMATED_STICKER;
    }

    public boolean shouldDrawWithoutBackground() {
        return type == TYPE_STICKER || type == TYPE_ANIMATED_STICKER || type == TYPE_ROUND_VIDEO;
    }

    public boolean isLocation() {
        return isLocationMessage(messageOwner);
    }

    public boolean isMask() {
        return isMaskMessage(messageOwner);
    }

    public boolean isMusic() {
        return isMusicMessage(messageOwner);
    }

    public boolean isVoice() {
        return isVoiceMessage(messageOwner);
    }

    public boolean isVideo() {
        return isVideoMessage(messageOwner);
    }

    public boolean isPhoto() {
        return isPhoto(messageOwner);
    }

    public boolean isLiveLocation() {
        return isLiveLocationMessage(messageOwner);
    }

    public boolean isGame() {
        return isGameMessage(messageOwner);
    }

    public boolean isInvoice() {
        return isInvoiceMessage(messageOwner);
    }

    public boolean isRoundVideo() {
        if (isRoundVideoCached == 0) {
            isRoundVideoCached = type == TYPE_ROUND_VIDEO || isRoundVideoMessage(messageOwner) ? 1 : 2;
        }
        return isRoundVideoCached == 1;
    }

    public boolean hasAttachedStickers() {
        if (messageOwner.media.isPhoto()) {
            return messageOwner.media.photo != null && messageOwner.media.photo.has_stickers;
        } else if (messageOwner.media.isDocument()) {
            return isDocumentHasAttachedStickers(messageOwner.media.document);
        }
        return false;
    }

    public static boolean isDocumentHasAttachedStickers(Document document) {
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isSticker()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isGif() {
        return isGifMessage(messageOwner);
    }

    public boolean isWebpageDocument() {
        return messageOwner.media.isWebPage() && messageOwner.media.webpage.document != null && !isGifDocument(messageOwner.media.webpage.document);
    }

    public boolean isWebpage() {
        return messageOwner.media.isWebPage();
    }

    public boolean isNewGif() {
        return messageOwner.media != null && isNewGifDocument(getDocument());
    }

    public boolean isAndroidTheme() {
        return false;
    }

    public String getMusicTitle() {
        return getMusicTitle(true);
    }

    public String getMusicTitle(boolean unknown) {
        Document document = getDocument();
        if (document != null) {
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isAudio()) {
                    if (attribute.voice) {
                        if (!unknown) {
                            return null;
                        }
                        return LocaleController.formatDateAudio(messageOwner.date, true);
                    }
                    String title = attribute.title;
                    if (title == null || title.length() == 0) {
                        title = FileLoader.getDocumentFileName(document);
                        if (TextUtils.isEmpty(title) && unknown) {
                            title = LocaleController.getString("AudioUnknownTitle", R.string.AudioUnknownTitle);
                        }
                    }
                    return title;
                } else if (attribute.isVideo()) {
                    if (attribute.round_message) {
                        return LocaleController.formatDateAudio(messageOwner.date, true);
                    }
                }
            }
            String fileName = FileLoader.getDocumentFileName(document);
            if (!TextUtils.isEmpty(fileName)) {
                return fileName;
            }
        }
        return LocaleController.getString("AudioUnknownTitle", R.string.AudioUnknownTitle);
    }

    public int getDuration() {
        Document document = getDocument();
        if (document == null) {
            return 0;
        }
        if (audioPlayerDuration > 0) {
            return audioPlayerDuration;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isAudio()) {
                return attribute.duration;
            } else if (attribute.isVideo()) {
                return attribute.duration;
            }
        }
        return audioPlayerDuration;
    }

    public String getArtworkUrl(boolean small) {
        Document document = getDocument();
        if (document != null) {
            for (int i = 0, N = document.attributes.size(); i < N; i++) {
                Document.DocumentAttribute attribute = document.attributes.get(i);
                if (attribute.isAudio()) {
                    if (attribute.voice) {
                        return null;
                    } else {
                        String performer = attribute.performer;
                        String title = attribute.title;
                        if (!TextUtils.isEmpty(performer)) {
                            for (int a = 0; a < excludeWords.length; a++) {
                                performer = performer.replace(excludeWords[a], " ");
                            }
                        }
                        if (TextUtils.isEmpty(performer) && TextUtils.isEmpty(title)) {
                            return null;
                        }
                        try {
                            return "athumb://itunes.apple.com/search?term=" + URLEncoder.encode(performer + " - " + title, "UTF-8") + "&entity=song&limit=4" + (small ? "&s=1" : "");
                        } catch (Exception ignore) {

                        }
                    }
                }
            }
        }
        return null;
    }

    public String getMusicAuthor() {
        return getMusicAuthor(true);
    }

    public String getMusicAuthor(boolean unknown) {
        Document document = getDocument();
        if (document != null) {
            boolean isVoice = false;
            for (int a = 0; a < document.attributes.size(); a++) {
                Document.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute.isAudio()) {
                    if (attribute.voice) {
                        isVoice = true;
                    } else {
                        String performer = attribute.performer;
                        if (TextUtils.isEmpty(performer) && unknown) {
                            performer = LocaleController.getString("AudioUnknownArtist", R.string.AudioUnknownArtist);
                        }
                        return performer;
                    }
                } else if (attribute.isVideo()) {
                    if (attribute.round_message) {
                        isVoice = true;
                    }
                }
                if (isVoice) {
                    if (!unknown) {
                        return null;
                    }
                    if (isOutOwner() || messageOwner.fwd_from != null && messageOwner.fwd_from.from_id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                        return LocaleController.getString("FromYou", R.string.FromYou);
                    }
                    User user = null;
                    Chat chat = null;
                    if (messageOwner.fwd_from != null && messageOwner.fwd_from.channel_id != 0) {
                        chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.fwd_from.channel_id);
                    } else if (messageOwner.fwd_from != null && messageOwner.fwd_from.from_id != 0) {
                        user = MessagesController.getInstance(currentAccount).getUser(messageOwner.fwd_from.from_id);
                    } else if (messageOwner.fwd_from != null && messageOwner.fwd_from.from_name != null) {
                        return messageOwner.fwd_from.from_name;
                    } else if (messageOwner.from_id < 0) {
                        chat = MessagesController.getInstance(currentAccount).getChat(-messageOwner.from_id);
                    } else if (messageOwner.from_id == 0 && messageOwner.to_id != 0) {
                        chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.to_id);
                    } else {
                        user = MessagesController.getInstance(currentAccount).getUser(messageOwner.from_id);
                    }
                    if (user != null) {
                        return UserObject.getUserName(user);
                    } else if (chat != null) {
                        return chat.title;
                    }
                }
            }
        }
        return LocaleController.getString("AudioUnknownArtist", R.string.AudioUnknownArtist);
    }

    public InputStickerSet getInputStickerSet() {
        return getInputStickerSet(messageOwner);
    }

    public boolean isForwarded() {
        return isForwardedMessage(messageOwner);
    }

    public boolean needDrawForwarded() {
        return (messageOwner.flags & Message.MESSAGE_FLAG_FWD) != 0 && messageOwner.fwd_from != null && (messageOwner.fwd_from.saved_from_chat_id != 0 || messageOwner.fwd_from.saved_from_chat_id != messageOwner.fwd_from.channel_id) && UserConfig.getInstance(currentAccount).getClientUserId() != getDialogId();
    }

    public static boolean isForwardedMessage(Message message) {
        return (message.flags & Message.MESSAGE_FLAG_FWD) != 0 && message.fwd_from != null;
    }

    public boolean isReply() {
        return !(replyMessageObject != null && replyMessageObject.messageOwner == null) && (messageOwner.reply_to_msg_id != 0 || messageOwner.reply_to_random_id != 0) && (messageOwner.flags & Message.MESSAGE_FLAG_REPLY) != 0;
    }

    public boolean isMediaEmpty() {
        return isMediaEmpty(messageOwner);
    }

    public boolean isMediaEmptyWebpage() {
        return isMediaEmptyWebpage(messageOwner);
    }

    public static boolean isMediaEmpty(Message message) {
        return message == null || message.media == null || message.media.isWebPage();
    }

    public static boolean isMediaEmptyWebpage(Message message) {
        return message == null || message.media == null;
    }

    public boolean canEditMessage(Chat chat) {
        return canEditMessage(currentAccount, messageOwner, chat, scheduled);
    }

    public boolean canEditMessageScheduleTime(Chat chat) {
        return canEditMessageScheduleTime(currentAccount, messageOwner, chat);
    }

    public boolean canForwardMessage() {
        return !needDrawBluredPreview() && !isLiveLocation() && type != 16;
    }

    public boolean canEditMedia() {
        if (isSecretMedia()) {
            return false;
        } else if (messageOwner.media.isPhoto()) {
            return true;
        } else if (messageOwner.media.isDocument()) {
            return !isVoice() && !isSticker() && !isAnimatedSticker() && !isRoundVideo();
        }
        return false;
    }

    public boolean canEditMessageAnytime(Chat chat) {
        return canEditMessageAnytime(currentAccount, messageOwner, chat);
    }

    public static boolean canEditMessageAnytime(int currentAccount, Message message, Chat chat) {
        if (message == null || message.to_id == 0 || message.media != null && (isRoundVideoDocument(message.media.document)
                || isStickerDocument(message.media.document) || isAnimatedStickerDocument(message.media.document, true))
                || message.action != null || isForwardedMessage(message) || message.via_bot_id != 0 || message.id < 0) {
            return false;
        }
        if (message.from_id == message.to_id && message.from_id == UserConfig.getInstance(currentAccount).getClientUserId() && !isLiveLocationMessage(message)) {
            return true;
        }
        if (chat == null && message.to_id != 0) {
            chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(message.to_id);
            if (chat == null) {
                return false;
            }
        }
        if (ChatObject.isChannel(chat) && !chat.megagroup && (chat.creator || chat.admin_rights != null && chat.admin_rights.edit_messages)) {
            return true;
        }
        if (message.out && chat != null && chat.megagroup && (chat.creator || chat.admin_rights != null && chat.admin_rights.pin_messages)) {
            return true;
        }
        //
        return false;
    }

    public static boolean canEditMessageScheduleTime(int currentAccount, Message message, Chat chat) {
        if (chat == null && message.to_id != 0) {
            chat = MessagesController.getInstance(currentAccount).getChat(message.to_id);
            if (chat == null) {
                return false;
            }
        }
        if (!ChatObject.isChannel(chat) || chat.megagroup || chat.creator) {
            return true;
        }
        if (chat.admin_rights != null && (chat.admin_rights.edit_messages || message.out)) {
            return true;
        }
        return false;
    }

    public static boolean canEditMessage(int currentAccount, Message message, Chat chat, boolean scheduled) {
        if (scheduled && message.date < ConnectionsManager.getInstance(currentAccount).getCurrentTime() - 60) {
            return false;
        }
        if (chat != null && (chat.left || chat.kicked)) {
            return false;
        }
        if (message == null || message.to_id == 0 || message.media != null && (isRoundVideoDocument(message.media.document) || isStickerDocument(message.media.document) || isAnimatedStickerDocument(message.media.document, true)
                || isLocationMessage(message)) || message.action != null
                || isForwardedMessage(message) || message.via_bot_id != 0 || message.id < 0) {
            return false;
        }
        if (message.from_id == message.to_id && message.from_id == UserConfig.getInstance(currentAccount).getClientUserId() && !isLiveLocationMessage(message) && !(message.media.isContact())) {
            return true;
        }
        if (chat == null && message.to_id != 0) {
            chat = MessagesController.getInstance(currentAccount).getChat(message.to_id);
            if (chat == null) {
                return false;
            }
        }
        if (message.media != null && !(message.media.isPhoto()) && !(message.media.isDocument()) && !(message.media.isWebPage())) {
            return false;
        }
        if (ChatObject.isChannel(chat) && !chat.megagroup && (chat.creator || chat.admin_rights != null && chat.admin_rights.edit_messages)) {
            return true;
        }
        if (message.out && chat != null && chat.megagroup && (chat.creator || chat.admin_rights != null && chat.admin_rights.pin_messages)) {
            return true;
        }
        if (!scheduled && Math.abs(message.date - ConnectionsManager.getInstance(currentAccount).getCurrentTime()) > MessagesController.getInstance(currentAccount).maxEditTime) {
            return false;
        }
        if (message.to_id == 0) {
            return (message.out || message.from_id == UserConfig.getInstance(currentAccount).getClientUserId()) && (message.media.isPhoto() ||
                    message.media.isDocument() && !isStickerMessage(message) && !isAnimatedStickerMessage(message) ||
                    message.media ==null ||
                    message.media.isWebPage() ||
                    message.media == null);
        }
        if (chat.megagroup && message.out || !chat.megagroup && (chat.creator || chat.admin_rights != null && (chat.admin_rights.edit_messages || message.out && chat.admin_rights.post_messages)) && message.post) {
            if (message.media.isPhoto() ||
                    message.media.isDocument() && !isStickerMessage(message) && !isAnimatedStickerMessage(message) ||
                    message.media.isWebPage() ||
                    message.media == null) {
                return true;
            }
        }
        return false;
    }

    public boolean canDeleteMessage(boolean inScheduleMode, Chat chat) {
        return eventId == 0 && canDeleteMessage(currentAccount, inScheduleMode, messageOwner, chat);
    }

    public static boolean canDeleteMessage(int currentAccount, boolean inScheduleMode, Message message, Chat chat) {
        if (message.id < 0) {
            return true;
        }
        if (chat == null && message.to_id != 0) {
            chat = MessagesController.getInstance(currentAccount).getChat(message.to_id);
        }
        if (ChatObject.isChannel(chat)) {
            if (inScheduleMode && !chat.megagroup) {
                return chat.creator || chat.admin_rights != null && (chat.admin_rights.delete_messages || message.out);
            }
            return inScheduleMode || message.id != 1 && (chat.creator || chat.admin_rights != null && (chat.admin_rights.delete_messages || message.out && (chat.megagroup || chat.admin_rights.post_messages)) || chat.megagroup && message.out && message.from_id > 0);
        }
        return inScheduleMode || isOut(message) || !ChatObject.isChannel(chat);
    }

    public String getForwardedName() {
        if (messageOwner.fwd_from != null) {
            if (messageOwner.fwd_from.channel_id != 0) {
                Chat chat = MessagesController.getInstance(currentAccount).getChat(messageOwner.fwd_from.channel_id);
                if (chat != null) {
                    return chat.title;
                }
            } else if (messageOwner.fwd_from.from_id != 0) {
                User user = MessagesController.getInstance(currentAccount).getUser(messageOwner.fwd_from.from_id);
                if (user != null) {
                    return UserObject.getUserName(user);
                }
            } else if (messageOwner.fwd_from.from_name != null) {
                return messageOwner.fwd_from.from_name;
            }
        }
        return null;
    }

    public int getFromId() {
        if (messageOwner.fwd_from != null && messageOwner.fwd_from.saved_from_chat_id != 0) {
            if (messageOwner.fwd_from.from_id != 0) {
                return messageOwner.fwd_from.from_id;
            } else {
                return messageOwner.fwd_from.saved_from_chat_id;
            }
        } else if (messageOwner.from_id != 0) {
            return messageOwner.from_id;
        } else if (messageOwner.post) {
            return messageOwner.to_id;
        }
        return 0;
    }

    public boolean isWallpaper() {
        return messageOwner.media.isWebPage() && messageOwner.media.webpage != null && "telegram_background".equals(messageOwner.media.webpage.type);
    }

    public boolean isTheme() {
        return messageOwner.media.isWebPage() && messageOwner.media.webpage != null && "telegram_theme".equals(messageOwner.media.webpage.type);
    }

    public int getMediaExistanceFlags() {
        int flags = 0;
        if (attachPathExists) {
            flags |= 1;
        }
        if (mediaExists) {
            flags |= 2;
        }
        return flags;
    }

    public void applyMediaExistanceFlags(int flags) {
        if (flags == -1) {
            checkMediaExistance();
        } else {
            attachPathExists = (flags & 1) != 0;
            mediaExists = (flags & 2) != 0;
        }
    }

    public void checkMediaExistance() {
        File cacheFile = null;
        attachPathExists = false;
        mediaExists = false;
        if (type == TYPE_PHOTO) {
            PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());
            if (currentPhotoObject != null) {
                File file = FileLoader.getPathToMessage(messageOwner);
                if (needDrawBluredPreview()) {
                    mediaExists = new File(file.getAbsolutePath() + ".enc").exists();
                }
                if (!mediaExists) {
                    mediaExists = file.exists();
                }
            }
        }
        if (!mediaExists && type == 8 || type == 3 || type == 9 || type == 2 || type == 14 || type == TYPE_ROUND_VIDEO) {
            if (messageOwner.attachPath != null && messageOwner.attachPath.length() > 0) {
                File f = new File(messageOwner.attachPath);
                attachPathExists = f.exists();
            }
            if (!attachPathExists) {
                File file = FileLoader.getPathToMessage(messageOwner);
                if (type == 3 && needDrawBluredPreview()) {
                    mediaExists = new File(file.getAbsolutePath() + ".enc").exists();
                }
                if (!mediaExists) {
                    mediaExists = file.exists();
                }
            }
        }
        if (!mediaExists) {
            Document document = getDocument();
            if (document != null) {
                if (isWallpaper()) {
                    mediaExists = FileLoader.getPathToAttach(document, true).exists();
                } else {
                    mediaExists = FileLoader.getPathToAttach(document).exists();
                }
            } else if (type == 0) {
                PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, AndroidUtilities.getPhotoSize());
                if (currentPhotoObject == null) {
                    return;
                }
                if (currentPhotoObject != null) {
                    mediaExists = FileLoader.getPathToAttach(currentPhotoObject, true).exists();
                }
            }
        }
    }

    public boolean equals(MessageObject obj) {
        return getId() == obj.getId() && getDialogId() == obj.getDialogId();
    }
}
