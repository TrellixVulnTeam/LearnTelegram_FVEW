package com.demo.chat.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.widget.Toast;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.R;
import com.demo.chat.SQLite.SQLiteCursor;
import com.demo.chat.SQLite.SQLiteDatabase;
import com.demo.chat.SQLite.SQLitePreparedStatement;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.BuildVars;
import com.demo.chat.messager.Emoji;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.ImageLoader;
import com.demo.chat.messager.ImageLocation;
import com.demo.chat.messager.NativeByteBuffer;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.SerializedData;
import com.demo.chat.messager.Utilities;
import com.demo.chat.messager.support.SparseLongArray;
import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.User;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.action.UserObject;
import com.demo.chat.model.bot.BotInfo;
import com.demo.chat.model.message.messages_Messages;
import com.demo.chat.model.small.Document;
import com.demo.chat.model.small.DraftMessage;
import com.demo.chat.model.small.FileLocation;
import com.demo.chat.model.small.MessageEntity;
import com.demo.chat.model.small.PhotoSize;
import com.demo.chat.model.sticker.InputStickerSet;
import com.demo.chat.model.sticker.MessagesStickerSet;
import com.demo.chat.model.sticker.Sticker;
import com.demo.chat.model.sticker.StickerSet;
import com.demo.chat.model.sticker.StickerSetCovered;
import com.demo.chat.receiver.OpenChatReceiver;
import com.demo.chat.ui.ActionBar.BaseFragment;
import com.demo.chat.ui.Components.AvatarDrawable;
import com.demo.chat.ui.Components.Bulletin;
import com.demo.chat.ui.Components.StickerSetBulletinLayout;
import com.demo.chat.ui.Components.TextStyleSpan;
import com.demo.chat.ui.Components.URLSpanReplacement;
import com.demo.chat.ui.Components.URLSpanUserMention;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static com.demo.chat.model.Message.MESSAGE_FLAG_MEGAGROUP;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
@SuppressWarnings("unchecked")
public class MediaDataController extends BaseController {

    private static volatile MediaDataController[] Instance = new MediaDataController[UserConfig.MAX_ACCOUNT_COUNT];
    public static MediaDataController getInstance(int num) {
        MediaDataController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MediaDataController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MediaDataController(num);
                }
            }
        }
        return localInstance;
    }

    public MediaDataController(int num) {
        super(num);

        if (currentAccount == 0) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("drafts", Activity.MODE_PRIVATE);
        } else {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("drafts" + currentAccount, Activity.MODE_PRIVATE);
        }
        Map<String, ?> values = preferences.getAll();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            try {
                String key = entry.getKey();
                long did = Utilities.parseLong(key);
                byte[] bytes = Utilities.hexToBytes((String) entry.getValue());
                SerializedData serializedData = new SerializedData(bytes);
                if (key.startsWith("r_")) {
                    Message message = Message.TLdeserialize(serializedData, serializedData.readInt32(true), true);
                    message.readAttachPath(serializedData, getUserConfig().clientUserId);
                    if (message != null) {
                        draftMessages.put(did, message);
                    }
                } else {
                    DraftMessage draftMessage = DraftMessage.TLdeserialize(serializedData, serializedData.readInt32(true), true);
                    if (draftMessage != null) {
                        drafts.put(did, draftMessage);
                    }
                }
                serializedData.cleanup();
            } catch (Exception e) {
                //igonre
            }
        }
    }


    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_MASK = 1;
    public static final int TYPE_FAVE = 2;
    public static final int TYPE_FEATURED = 3;
    public static final int TYPE_EMOJI = 4;

    //region ---------------- STICKERS ----------------
    private ArrayList<MessagesStickerSet>[] stickerSets = new ArrayList[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>(0), new ArrayList<>(), new ArrayList<>()};
    private LongSparseArray<Document>[] stickersByIds = new LongSparseArray[]{new LongSparseArray<>(), new LongSparseArray<>(), new LongSparseArray<>(), new LongSparseArray<>(), new LongSparseArray<>()};
    private LongSparseArray<MessagesStickerSet> stickerSetsById = new LongSparseArray<>();
    private LongSparseArray<MessagesStickerSet> installedStickerSetsById = new LongSparseArray<>();
    private LongSparseArray<MessagesStickerSet> groupStickerSets = new LongSparseArray<>();
    private ConcurrentHashMap<String, MessagesStickerSet> stickerSetsByName = new ConcurrentHashMap<>(100, 1.0f, 1);
    private HashMap<String, MessagesStickerSet> diceStickerSetsByEmoji = new HashMap<>();
    private LongSparseArray<String> diceEmojiStickerSetsById = new LongSparseArray<>();
    private HashSet<String> loadingDiceStickerSets = new HashSet<>();
    private LongSparseArray<Runnable> removingStickerSetsUndos = new LongSparseArray<>();
    private Runnable[] scheduledLoadStickers = new Runnable[5];
    private boolean[] loadingStickers = new boolean[5];
    private boolean[] stickersLoaded = new boolean[5];
    private int[] loadHash = new int[5];
    private int[] loadDate = new int[5];

    private HashMap<String, ArrayList<Message>> verifyingMessages = new HashMap<>();

    private int[] archivedStickersCount = new int[2];

    private LongSparseArray<String> stickersByEmoji = new LongSparseArray<>();
    private HashMap<String, ArrayList<Document>> allStickers = new HashMap<>();
    private HashMap<String, ArrayList<Document>> allStickersFeatured = new HashMap<>();

    private ArrayList<Document>[] recentStickers = new ArrayList[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    private boolean[] loadingRecentStickers = new boolean[3];
    private boolean[] recentStickersLoaded = new boolean[3];

    private ArrayList<Document> recentGifs = new ArrayList<>();
    private boolean loadingRecentGifs;
    private boolean recentGifsLoaded;

    private int loadFeaturedHash;
    private int loadFeaturedDate;
    private ArrayList<StickerSetCovered> featuredStickerSets = new ArrayList<>();
    private LongSparseArray<StickerSetCovered> featuredStickerSetsById = new LongSparseArray<>();
    private ArrayList<Long> unreadStickerSets = new ArrayList<>();
    private ArrayList<Long> readingStickerSets = new ArrayList<>();
    private boolean loadingFeaturedStickers;
    private boolean featuredStickersLoaded;

    public void cleanup() {
        for (int a = 0; a < 3; a++) {
            recentStickers[a].clear();
            loadingRecentStickers[a] = false;
            recentStickersLoaded[a] = false;
        }
        for (int a = 0; a < 4; a++) {
            loadHash[a] = 0;
            loadDate[a] = 0;
            stickerSets[a].clear();
            loadingStickers[a] = false;
            stickersLoaded[a] = false;
        }
        featuredStickerSets.clear();
        loadFeaturedDate = 0;
        loadFeaturedHash = 0;
        allStickers.clear();
        allStickersFeatured.clear();
        stickersByEmoji.clear();
        featuredStickerSetsById.clear();
        featuredStickerSets.clear();
        unreadStickerSets.clear();
        recentGifs.clear();
        stickerSetsById.clear();
        installedStickerSetsById.clear();
        stickerSetsByName.clear();
        diceStickerSetsByEmoji.clear();
        diceEmojiStickerSetsById.clear();
        loadingDiceStickerSets.clear();
        loadingFeaturedStickers = false;
        featuredStickersLoaded = false;
        loadingRecentGifs = false;
        recentGifsLoaded = false;

        currentFetchingEmoji.clear();
        if (Build.VERSION.SDK_INT >= 25) {
            Utilities.globalQueue.postRunnable(() -> {
                try {
                    ShortcutManager shortcutManager = ApplicationLoader.applicationContext.getSystemService(ShortcutManager.class);
                    shortcutManager.removeAllDynamicShortcuts();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
        verifyingMessages.clear();

        loading = false;
        loaded = false;
//        hints.clear();
//        inlineBots.clear();
        getNotificationCenter().postNotificationName(NotificationCenter.reloadHints);
        getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);

        drafts.clear();
        draftMessages.clear();
        preferences.edit().clear().commit();

        botInfos.clear();
        botKeyboards.clear();
        botKeyboardsByMids.clear();
    }

    public void checkStickers(int type) {
        if (!loadingStickers[type] && (!stickersLoaded[type] || Math.abs(System.currentTimeMillis() / 1000 - loadDate[type]) >= 60 * 60)) {
            loadStickers(type, true, false);
        }
    }

    public void checkFeaturedStickers() {
        if (!loadingFeaturedStickers && (!featuredStickersLoaded || Math.abs(System.currentTimeMillis() / 1000 - loadFeaturedDate) >= 60 * 60)) {
            loadFeaturedStickers(true, false);
        }
    }

    public ArrayList<Document> getRecentStickers(int type) {
        ArrayList<Document> arrayList = recentStickers[type];
        return new ArrayList<>(arrayList.subList(0, Math.min(arrayList.size(), 20)));
    }

    public ArrayList<Document> getRecentStickersNoCopy(int type) {
        return recentStickers[type];
    }

    public boolean isStickerInFavorites(Document document) {
        if (document == null) {
            return false;
        }
        for (int a = 0; a < recentStickers[TYPE_FAVE].size(); a++) {
            Document d = recentStickers[TYPE_FAVE].get(a);
            if (d.id == document.id && d.dc_id == document.dc_id) {
                return true;
            }
        }
        return false;
    }

    public void addRecentSticker(final int type, Object parentObject, Document document, int date, boolean remove) {
        if (!MessageObject.isStickerDocument(document) && !MessageObject.isAnimatedStickerDocument(document, true)) {
            return;
        }
        boolean found = false;
        for (int a = 0; a < recentStickers[type].size(); a++) {
            Document image = recentStickers[type].get(a);
            if (image.id == document.id) {
                recentStickers[type].remove(a);
                if (!remove) {
                    recentStickers[type].add(0, image);
                }
                found = true;
                break;
            }
        }
        if (!found && !remove) {
            recentStickers[type].add(0, document);
        }
        int maxCount;
        if (type == TYPE_FAVE) {
            if (remove) {
                Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("RemovedFromFavorites", R.string.RemovedFromFavorites), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("AddedToFavorites", R.string.AddedToFavorites), Toast.LENGTH_SHORT).show();
            }
            //TODO 发起请求
//            TL_messages_faveSticker req = new TL_messages_faveSticker();
//            req.id = new TL_inputDocument();
//            req.id.id = document.id;
//            req.id.access_hash = document.access_hash;
//            req.id.file_reference = document.file_reference;
//            if (req.id.file_reference == null) {
//                req.id.file_reference = new byte[0];
//            }
//            req.unfave = remove;
//            getConnectionsManager().sendRequest(req, (response, error) -> {
//                if (error != null && FileRefController.isFileRefError(error.text) && parentObject != null) {
//                    getFileRefController().requestReference(parentObject, req);
//                } else {
//                    AndroidUtilities.runOnUIThread(() -> getMediaDataController().loadRecents(MediaDataController.TYPE_FAVE, false, false, true));
//                }
//            });
            maxCount = getMessagesController().maxFaveStickersCount;
        } else {
            if (type == TYPE_IMAGE && remove) {
                Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("RemovedFromRecent", R.string.RemovedFromRecent), Toast.LENGTH_SHORT).show();
                //TODO 发起请求
//                TL_messages_saveRecentSticker req = new TL_messages_saveRecentSticker();
//                req.id = new TL_inputDocument();
//                req.id.id = document.id;
//                req.id.access_hash = document.access_hash;
//                req.id.file_reference = document.file_reference;
//                if (req.id.file_reference == null) {
//                    req.id.file_reference = new byte[0];
//                }
//                req.unsave = true;
//                getConnectionsManager().sendRequest(req, (response, error) -> {
//                    if (error != null && FileRefController.isFileRefError(error.text) && parentObject != null) {
//                        getFileRefController().requestReference(parentObject, req);
//                    }
//                });
            }
            maxCount = getMessagesController().maxRecentStickersCount;
        }
        if (recentStickers[type].size() > maxCount || remove) {
            final Document old = remove ? document : recentStickers[type].remove(recentStickers[type].size() - 1);
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                int cacheType;
                if (type == TYPE_IMAGE) {
                    cacheType = 3;
                } else if (type == TYPE_MASK) {
                    cacheType = 4;
                } else {
                    cacheType = 5;
                }
                try {
                    getMessagesStorage().getDatabase().executeFast("DELETE FROM web_recent_v3 WHERE id = '" + old.id + "' AND type = " + cacheType).stepThis().dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
        if (!remove) {
            ArrayList<Document> arrayList = new ArrayList<>();
            arrayList.add(document);
            processLoadedRecentDocuments(type, arrayList, false, date, false);
        }
        if (type == TYPE_FAVE || type == TYPE_IMAGE && remove) {
            getNotificationCenter().postNotificationName(NotificationCenter.recentDocumentsDidLoad, false, type);
        }
    }

    public ArrayList<Document> getRecentGifs() {
        return new ArrayList<>(recentGifs);
    }

    public void removeRecentGif(final Document document) {
        for (int i = 0, N = recentGifs.size(); i < N; i++) {
            if (recentGifs.get(i).id == document.id) {
                recentGifs.remove(i);
                break;
            }
        }
        //TODO 发起请求
//        TL_messages_saveGif req = new TL_messages_saveGif();
//        req.id = new TL_inputDocument();
//        req.id.id = document.id;
//        req.id.access_hash = document.access_hash;
//        req.id.file_reference = document.file_reference;
//        if (req.id.file_reference == null) {
//            req.id.file_reference = new byte[0];
//        }
//        req.unsave = true;
//        getConnectionsManager().sendRequest(req, (response, error) -> {
//            if (error != null && FileRefController.isFileRefError(error.text)) {
//                getFileRefController().requestReference("gif", req);
//            }
//        });
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                getMessagesStorage().getDatabase().executeFast("DELETE FROM web_recent_v3 WHERE id = '" + document.id + "' AND type = 2").stepThis().dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public boolean hasRecentGif(Document document) {
        for (int a = 0; a < recentGifs.size(); a++) {
            Document image = recentGifs.get(a);
            if (image.id == document.id) {
                recentGifs.remove(a);
                recentGifs.add(0, image);
                return true;
            }
        }
        return false;
    }

    public void addRecentGif(Document document, int date) {
        if (document == null) {
            return;
        }
        boolean found = false;
        for (int a = 0; a < recentGifs.size(); a++) {
            Document image = recentGifs.get(a);
            if (image.id == document.id) {
                recentGifs.remove(a);
                recentGifs.add(0, image);
                found = true;
                break;
            }
        }
        if (!found) {
            recentGifs.add(0, document);
        }
        if (recentGifs.size() > getMessagesController().maxRecentGifsCount) {
            final Document old = recentGifs.remove(recentGifs.size() - 1);
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    getMessagesStorage().getDatabase().executeFast("DELETE FROM web_recent_v3 WHERE id = '" + old.id + "' AND type = 2").stepThis().dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
        ArrayList<Document> arrayList = new ArrayList<>();
        arrayList.add(document);
        processLoadedRecentDocuments(0, arrayList, true, date, false);
    }

    public boolean isLoadingStickers(int type) {
        return loadingStickers[type];
    }

    public void replaceStickerSet(MessagesStickerSet set) {
        MessagesStickerSet existingSet = stickerSetsById.get(set.set.id);
        String emoji = diceEmojiStickerSetsById.get(set.set.id);
        if (emoji != null) {
            diceStickerSetsByEmoji.put(emoji, set);
            putDiceStickersToCache(emoji, set, (int) (System.currentTimeMillis() / 1000));
        }
        boolean isGroupSet = false;
        if (existingSet == null) {
            existingSet = stickerSetsByName.get(set.set.short_name);
        }
        if (existingSet == null) {
            existingSet = groupStickerSets.get(set.set.id);
            if (existingSet != null) {
                isGroupSet = true;
            }
        }
        if (existingSet == null) {
            return;
        }
        boolean changed = false;
        if ("AnimatedEmojies".equals(set.set.short_name)) {
            changed = true;
            existingSet.documents = set.documents;
            existingSet.packs = set.packs;
            existingSet.set = set.set;
            AndroidUtilities.runOnUIThread(() -> {
                LongSparseArray<Document> stickersById = getStickerByIds(TYPE_EMOJI);
                for (int b = 0; b < set.documents.size(); b++) {
                    Document document = set.documents.get(b);
                    stickersById.put(document.id, document);
                }
            });
        } else {
            LongSparseArray<Document> documents = new LongSparseArray<>();
            for (int a = 0, size = set.documents.size(); a < size; a++) {
                Document document = set.documents.get(a);
                documents.put(document.id, document);
            }
            for (int a = 0, size = existingSet.documents.size(); a < size; a++) {
                Document document = existingSet.documents.get(a);
                Document newDocument = documents.get(document.id);
                if (newDocument != null) {
                    existingSet.documents.set(a, newDocument);
                    changed = true;
                }
            }
        }
        if (changed) {
            if (isGroupSet) {
                putSetToCache(existingSet);
            } else {
                int type = set.set.masks ? TYPE_MASK : TYPE_IMAGE;
                putStickersToCache(type, stickerSets[type], loadDate[type], loadHash[type]);
                if ("AnimatedEmojies".equals(set.set.short_name)) {
                    type = TYPE_EMOJI;
                    putStickersToCache(type, stickerSets[type], loadDate[type], loadHash[type]);
                }
            }
        }
    }

    public MessagesStickerSet getStickerSetByName(String name) {
        return stickerSetsByName.get(name);
    }

    public MessagesStickerSet getDiceStickerSetByEmoji(String emoji) {
        return diceStickerSetsByEmoji.get(emoji);
    }

    public MessagesStickerSet getStickerSetById(long id) {
        return stickerSetsById.get(id);
    }

    public MessagesStickerSet getGroupStickerSetById(StickerSet stickerSet) {
        MessagesStickerSet set = stickerSetsById.get(stickerSet.id);
        if (set == null) {
            set = groupStickerSets.get(stickerSet.id);
            if (set == null || set.set == null) {
                loadGroupStickerSet(stickerSet, true);
            } else if (set.set.hash != stickerSet.hash) {
                loadGroupStickerSet(stickerSet, false);
            }
        }
        return set;
    }

    public void putGroupStickerSet(MessagesStickerSet stickerSet) {
        groupStickerSets.put(stickerSet.set.id, stickerSet);
    }

    private void loadGroupStickerSet(final StickerSet stickerSet, boolean cache) {
        if (cache) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    final MessagesStickerSet set;
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT document FROM web_recent_v3 WHERE id = 's_" + stickerSet.id + "'");
                    if (cursor.next() && !cursor.isNull(0)) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            set = MessagesStickerSet.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                        } else {
                            set = null;
                        }
                    } else {
                        set = null;
                    }
                    cursor.dispose();
                    if (set == null || set.set == null || set.set.hash != stickerSet.hash) {
                        loadGroupStickerSet(stickerSet, false);
                    }
                    if (set != null && set.set != null) {
                        AndroidUtilities.runOnUIThread(() -> {
                            groupStickerSets.put(set.set.id, set);
                            getNotificationCenter().postNotificationName(NotificationCenter.groupStickersDidLoad, set.set.id);
                        });
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
        } else {
            //TODO 发起请求
//            TL_messages_getStickerSet req = new TL_messages_getStickerSet();
//            req.stickerset = new TL_inputStickerSetID();
//            req.stickerset.id = stickerSet.id;
//            req.stickerset.access_hash = stickerSet.access_hash;
//            getConnectionsManager().sendRequest(req, (response, error) -> {
//                if (response != null) {
//                    MessagesStickerSet set = (MessagesStickerSet) response;
//                    AndroidUtilities.runOnUIThread(() -> {
//                        groupStickerSets.put(set.set.id, set);
//                        getNotificationCenter().postNotificationName(NotificationCenter.groupStickersDidLoad, set.set.id);
//                    });
//                }
//            });
        }
    }

    private void putSetToCache(MessagesStickerSet set) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                SQLiteDatabase database = getMessagesStorage().getDatabase();
                SQLitePreparedStatement state = database.executeFast("REPLACE INTO web_recent_v3 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                state.requery();
                state.bindString(1, "s_" + set.set.id);
                state.bindInteger(2, 6);
                state.bindString(3, "");
                state.bindString(4, "");
                state.bindString(5, "");
                state.bindInteger(6, 0);
                state.bindInteger(7, 0);
                state.bindInteger(8, 0);
                state.bindInteger(9, 0);
//                NativeByteBuffer data = new NativeByteBuffer(set.getObjectSize());
//                set.serializeToStream(data);
//                state.bindByteBuffer(10, data);
                state.step();
//                data.reuse();
                state.dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public HashMap<String, ArrayList<Document>> getAllStickers() {
        return allStickers;
    }

    public HashMap<String, ArrayList<Document>> getAllStickersFeatured() {
        return allStickersFeatured;
    }

    public Document getEmojiAnimatedSticker(CharSequence message) {
        String emoji = message.toString().replace("\uFE0F", "");
        ArrayList<MessagesStickerSet> arrayList = getStickerSets(MediaDataController.TYPE_EMOJI);
        for (int a = 0, N = arrayList.size(); a < N; a++) {
            MessagesStickerSet set = arrayList.get(a);
            for (int b = 0, N2 = set.packs.size(); b < N2; b++) {
                MessagesStickerSet.StickerPack pack = set.packs.get(b);
                if (!pack.documents.isEmpty() && TextUtils.equals(pack.emoticon, emoji)) {
                    LongSparseArray<Document> stickerByIds = getStickerByIds(MediaDataController.TYPE_EMOJI);
                    return stickerByIds.get(pack.documents.get(0));
                }
            }
        }
        return null;
    }

    public boolean canAddStickerToFavorites() {
        return !stickersLoaded[0] || stickerSets[0].size() >= 5 || !recentStickers[TYPE_FAVE].isEmpty();
    }

    public ArrayList<MessagesStickerSet> getStickerSets(int type) {
        if (type == TYPE_FEATURED) {
            return stickerSets[2];
        } else {
            return stickerSets[type];
        }
    }

    public LongSparseArray<Document> getStickerByIds(int type) {
        return stickersByIds[type];
    }

    public ArrayList<StickerSetCovered> getFeaturedStickerSets() {
        return featuredStickerSets;
    }

    public ArrayList<Long> getUnreadStickerSets() {
        return unreadStickerSets;
    }

    public boolean areAllTrendingStickerSetsUnread() {
        for (int a = 0, N = featuredStickerSets.size(); a < N; a++) {
            StickerSetCovered pack = featuredStickerSets.get(a);
            if (isStickerPackInstalled(pack.set.id) || pack.covers.isEmpty() && pack.cover == null) {
                continue;
            }
            if (!unreadStickerSets.contains(pack.set.id)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStickerPackInstalled(long id) {
        return installedStickerSetsById.indexOfKey(id) >= 0;
    }

    public boolean isStickerPackUnread(long id) {
        return unreadStickerSets.contains(id);
    }

    public boolean isStickerPackInstalled(String name) {
        return stickerSetsByName.containsKey(name);
    }

    public String getEmojiForSticker(long id) {
        String value = stickersByEmoji.get(id);
        return value != null ? value : "";
    }

    public static int calcDocumentsHash(ArrayList<Document> arrayList) {
        if (arrayList == null) {
            return 0;
        }
        long acc = 0;
        for (int a = 0; a < Math.min(200, arrayList.size()); a++) {
            Document document = arrayList.get(a);
            if (document == null) {
                continue;
            }
            int high_id = (int) (document.id >> 32);
            int lower_id = (int) document.id;
            acc = ((acc * 20261) + 0x80000000L + high_id) % 0x80000000L;
            acc = ((acc * 20261) + 0x80000000L + lower_id) % 0x80000000L;
        }
        return (int) acc;
    }

    public void loadRecents(final int type, final boolean gif, boolean cache, boolean force) {
        if (gif) {
            if (loadingRecentGifs) {
                return;
            }
            loadingRecentGifs = true;
            if (recentGifsLoaded) {
                cache = false;
            }
        } else {
            if (loadingRecentStickers[type]) {
                return;
            }
            loadingRecentStickers[type] = true;
            if (recentStickersLoaded[type]) {
                cache = false;
            }
        }
        if (cache) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    final int cacheType;
                    if (gif) {
                        cacheType = 2;
                    } else if (type == TYPE_IMAGE) {
                        cacheType = 3;
                    } else if (type == TYPE_MASK) {
                        cacheType = 4;
                    } else {
                        cacheType = 5;
                    }
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT document FROM web_recent_v3 WHERE type = " + cacheType + " ORDER BY date DESC");
                    final ArrayList<Document> arrayList = new ArrayList<>();
                    while (cursor.next()) {
                        if (!cursor.isNull(0)) {
                            NativeByteBuffer data = cursor.byteBufferValue(0);
                            if (data != null) {
                                Document document = Document.TLdeserialize(data, data.readInt32(false), false);
                                if (document != null) {
                                    arrayList.add(document);
                                }
                                data.reuse();
                            }
                        }
                    }
                    cursor.dispose();
                    AndroidUtilities.runOnUIThread(() -> {
                        if (gif) {
                            recentGifs = arrayList;
                            loadingRecentGifs = false;
                            recentGifsLoaded = true;
                        } else {
                            recentStickers[type] = arrayList;
                            loadingRecentStickers[type] = false;
                            recentStickersLoaded[type] = true;
                        }
                        getNotificationCenter().postNotificationName(NotificationCenter.recentDocumentsDidLoad, gif, type);
                        loadRecents(type, gif, false, false);
                    });
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
        } else {
            SharedPreferences preferences = MessagesController.getEmojiSettings(currentAccount);
            if (!force) {
                long lastLoadTime;
                if (gif) {
                    lastLoadTime = preferences.getLong("lastGifLoadTime", 0);
                } else if (type == TYPE_IMAGE) {
                    lastLoadTime = preferences.getLong("lastStickersLoadTime", 0);
                } else if (type == TYPE_MASK) {
                    lastLoadTime = preferences.getLong("lastStickersLoadTimeMask", 0);
                } else {
                    lastLoadTime = preferences.getLong("lastStickersLoadTimeFavs", 0);
                }
                if (Math.abs(System.currentTimeMillis() - lastLoadTime) < 60 * 60 * 1000) {
                    if (gif) {
                        loadingRecentGifs = false;
                    } else {
                        loadingRecentStickers[type] = false;
                    }
                    return;
                }
            }
            if (gif) {
                //TODO 发起请求
//                TL_messages_getSavedGifs req = new TL_messages_getSavedGifs();
//                req.hash = calcDocumentsHash(recentGifs);
//                getConnectionsManager().sendRequest(req, (response, error) -> {
//                    ArrayList<Document> arrayList = null;
//                    if (response instanceof TL_messages_savedGifs) {
//                        TL_messages_savedGifs res = (TL_messages_savedGifs) response;
//                        arrayList = res.gifs;
//                    }
//                    processLoadedRecentDocuments(type, arrayList, gif, 0, true);
//                });
//            } else {
//                TLObject request;
//                if (type == TYPE_FAVE) {
//                    TL_messages_getFavedStickers req = new TL_messages_getFavedStickers();
//                    req.hash = calcDocumentsHash(recentStickers[type]);
//                    request = req;
//                } else {
//                    TL_messages_getRecentStickers req = new TL_messages_getRecentStickers();
//                    req.hash = calcDocumentsHash(recentStickers[type]);
//                    req.attached = type == TYPE_MASK;
//                    request = req;
//                }
//                getConnectionsManager().sendRequest(request, (response, error) -> {
//                    ArrayList<Document> arrayList = null;
//                    if (type == TYPE_FAVE) {
//                        if (response instanceof TL_messages_favedStickers) {
//                            TL_messages_favedStickers res = (TL_messages_favedStickers) response;
//                            arrayList = res.stickers;
//                        }
//                    } else {
//                        if (response instanceof TL_messages_recentStickers) {
//                            TL_messages_recentStickers res = (TL_messages_recentStickers) response;
//                            arrayList = res.stickers;
//                        }
//                    }
//                    processLoadedRecentDocuments(type, arrayList, gif, 0, true);
//                });
            }
        }
    }

    protected void processLoadedRecentDocuments(final int type, final ArrayList<Document> documents, final boolean gif, final int date, boolean replace) {
        if (documents != null) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    SQLiteDatabase database = getMessagesStorage().getDatabase();
                    int maxCount;
                    if (gif) {
                        maxCount = getMessagesController().maxRecentGifsCount;
                    } else {
                        if (type == TYPE_FAVE) {
                            maxCount = getMessagesController().maxFaveStickersCount;
                        } else {
                            maxCount = getMessagesController().maxRecentStickersCount;
                        }
                    }
                    database.beginTransaction();

                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO web_recent_v3 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    int count = documents.size();
                    int cacheType;
                    if (gif) {
                        cacheType = 2;
                    } else if (type == TYPE_IMAGE) {
                        cacheType = 3;
                    } else if (type == TYPE_MASK) {
                        cacheType = 4;
                    } else {
                        cacheType = 5;
                    }
                    if (replace) {
                        database.executeFast("DELETE FROM web_recent_v3 WHERE type = " + cacheType).stepThis().dispose();
                    }
                    for (int a = 0; a < count; a++) {
                        if (a == maxCount) {
                            break;
                        }
                        Document document = documents.get(a);
                        state.requery();
                        state.bindString(1, "" + document.id);
                        state.bindInteger(2, cacheType);
                        state.bindString(3, "");
                        state.bindString(4, "");
                        state.bindString(5, "");
                        state.bindInteger(6, 0);
                        state.bindInteger(7, 0);
                        state.bindInteger(8, 0);
                        state.bindInteger(9, date != 0 ? date : count - a);
//                        NativeByteBuffer data = new NativeByteBuffer(document.getObjectSize());
//                        document.serializeToStream(data);
//                        state.bindByteBuffer(10, data);
                        state.step();
//                        if (data != null) {
//                            data.reuse();
//                        }
                    }
                    state.dispose();
                    database.commitTransaction();
                    if (documents.size() >= maxCount) {
                        database.beginTransaction();
                        for (int a = maxCount; a < documents.size(); a++) {
                            database.executeFast("DELETE FROM web_recent_v3 WHERE id = '" + documents.get(a).id + "' AND type = " + cacheType).stepThis().dispose();
                        }
                        database.commitTransaction();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
        if (date == 0) {
            AndroidUtilities.runOnUIThread(() -> {
                SharedPreferences.Editor editor = MessagesController.getEmojiSettings(currentAccount).edit();
                if (gif) {
                    loadingRecentGifs = false;
                    recentGifsLoaded = true;
                    editor.putLong("lastGifLoadTime", System.currentTimeMillis()).commit();
                } else {
                    loadingRecentStickers[type] = false;
                    recentStickersLoaded[type] = true;
                    if (type == TYPE_IMAGE) {
                        editor.putLong("lastStickersLoadTime", System.currentTimeMillis()).commit();
                    } else if (type == TYPE_MASK) {
                        editor.putLong("lastStickersLoadTimeMask", System.currentTimeMillis()).commit();
                    } else {
                        editor.putLong("lastStickersLoadTimeFavs", System.currentTimeMillis()).commit();
                    }
                }
                if (documents != null) {
                    if (gif) {
                        recentGifs = documents;
                    } else {
                        recentStickers[type] = documents;
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.recentDocumentsDidLoad, gif, type);
                } else {

                }
            });
        }
    }

    public void reorderStickers(int type, final ArrayList<Long> order) {
        Collections.sort(stickerSets[type], (lhs, rhs) -> {
            int index1 = order.indexOf(lhs.set.id);
            int index2 = order.indexOf(rhs.set.id);
            if (index1 > index2) {
                return 1;
            } else if (index1 < index2) {
                return -1;
            }
            return 0;
        });
        loadHash[type] = calcStickersHash(stickerSets[type]);
        getNotificationCenter().postNotificationName(NotificationCenter.stickersDidLoad, type);
        loadStickers(type, false, true);
    }

    public void calcNewHash(int type) {
        loadHash[type] = calcStickersHash(stickerSets[type]);
    }

    public void storeTempStickerSet(final MessagesStickerSet set) {
        stickerSetsById.put(set.set.id, set);
        stickerSetsByName.put(set.set.short_name, set);
    }

    public void addNewStickerSet(final MessagesStickerSet set) {
        if (stickerSetsById.indexOfKey(set.set.id) >= 0 || stickerSetsByName.containsKey(set.set.short_name)) {
            return;
        }
        int type = set.set.masks ? TYPE_MASK : TYPE_IMAGE;
        stickerSets[type].add(0, set);
        stickerSetsById.put(set.set.id, set);
        installedStickerSetsById.put(set.set.id, set);
        stickerSetsByName.put(set.set.short_name, set);
        LongSparseArray<Document> stickersById = new LongSparseArray<>();
        for (int a = 0; a < set.documents.size(); a++) {
            Document document = set.documents.get(a);
            stickersById.put(document.id, document);
        }
        for (int a = 0; a < set.packs.size(); a++) {
            MessagesStickerSet.StickerPack stickerPack = set.packs.get(a);
            stickerPack.emoticon = stickerPack.emoticon.replace("\uFE0F", "");
            ArrayList<Document> arrayList = allStickers.get(stickerPack.emoticon);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                allStickers.put(stickerPack.emoticon, arrayList);
            }
            for (int c = 0; c < stickerPack.documents.size(); c++) {
                Long id = stickerPack.documents.get(c);
                if (stickersByEmoji.indexOfKey(id) < 0) {
                    stickersByEmoji.put(id, stickerPack.emoticon);
                }
                Document sticker = stickersById.get(id);
                if (sticker != null) {
                    arrayList.add(sticker);
                }
            }
        }
        loadHash[type] = calcStickersHash(stickerSets[type]);
        getNotificationCenter().postNotificationName(NotificationCenter.stickersDidLoad, type);
        loadStickers(type, false, true);
    }

    public void loadFeaturedStickers(boolean cache, boolean force) {
        if (loadingFeaturedStickers) {
            return;
        }
        loadingFeaturedStickers = true;
        if (cache) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                ArrayList<StickerSetCovered> newStickerArray = null;
                ArrayList<Long> unread = new ArrayList<>();
                int date = 0;
                int hash = 0;
                SQLiteCursor cursor = null;
                try {
                    cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT data, unread, date, hash FROM stickers_featured WHERE 1");
                    if (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            newStickerArray = new ArrayList<>();
                            int count = data.readInt32(false);
                            for (int a = 0; a < count; a++) {
                                StickerSetCovered stickerSet = StickerSetCovered.TLdeserialize(data, data.readInt32(false), false);
                                newStickerArray.add(stickerSet);
                            }
                            data.reuse();
                        }
                        data = cursor.byteBufferValue(1);
                        if (data != null) {
                            int count = data.readInt32(false);
                            for (int a = 0; a < count; a++) {
                                unread.add(data.readInt64(false));
                            }
                            data.reuse();
                        }
                        date = cursor.intValue(2);
                        hash = calcFeaturedStickersHash(newStickerArray);
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
                processLoadedFeaturedStickers(newStickerArray, unread, true, date, hash);
            });
        } else {
            //TODO 发起请求
//            final TL_messages_getFeaturedStickers req = new TL_messages_getFeaturedStickers();
//            req.hash = force ? 0 : loadFeaturedHash;
//            getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                if (response instanceof TL_messages_featuredStickers) {
//                    TL_messages_featuredStickers res = (TL_messages_featuredStickers) response;
//                    processLoadedFeaturedStickers(res.sets, res.unread, false, (int) (System.currentTimeMillis() / 1000), res.hash);
//                } else {
//                    processLoadedFeaturedStickers(null, null, false, (int) (System.currentTimeMillis() / 1000), req.hash);
//                }
//            }));
        }
    }

    private void processLoadedFeaturedStickers(final ArrayList<StickerSetCovered> res, final ArrayList<Long> unreadStickers, final boolean cache, final int date, final int hash) {
        AndroidUtilities.runOnUIThread(() -> {
            loadingFeaturedStickers = false;
            featuredStickersLoaded = true;
        });
        Utilities.stageQueue.postRunnable(() -> {
            if (cache && (res == null || Math.abs(System.currentTimeMillis() / 1000 - date) >= 60 * 60) || !cache && res == null && hash == 0) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (res != null && hash != 0) {
                        loadFeaturedHash = hash;
                    }
                    loadFeaturedStickers(false, false);
                }, res == null && !cache ? 1000 : 0);
                if (res == null) {
                    return;
                }
            }
            if (res != null) {
                try {
                    final ArrayList<StickerSetCovered> stickerSetsNew = new ArrayList<>();
                    final LongSparseArray<StickerSetCovered> stickerSetsByIdNew = new LongSparseArray<>();

                    for (int a = 0; a < res.size(); a++) {
                        StickerSetCovered stickerSet = res.get(a);
                        stickerSetsNew.add(stickerSet);
                        stickerSetsByIdNew.put(stickerSet.set.id, stickerSet);
                    }

                    if (!cache) {
                        putFeaturedStickersToCache(stickerSetsNew, unreadStickers, date, hash);
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        unreadStickerSets = unreadStickers;
                        featuredStickerSetsById = stickerSetsByIdNew;
                        featuredStickerSets = stickerSetsNew;
                        loadFeaturedHash = hash;
                        loadFeaturedDate = date;
                        loadStickers(TYPE_FEATURED, true, false);
                        getNotificationCenter().postNotificationName(NotificationCenter.featuredStickersDidLoad);
                    });
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            } else if (!cache) {
                AndroidUtilities.runOnUIThread(() -> loadFeaturedDate = date);
                putFeaturedStickersToCache(null, null, date, 0);
            }
        });
    }

    private void putFeaturedStickersToCache(ArrayList<StickerSetCovered> stickers, final ArrayList<Long> unreadStickers, final int date, final int hash) {
        final ArrayList<StickerSetCovered> stickersFinal = stickers != null ? new ArrayList<>(stickers) : null;
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                if (stickersFinal != null) {
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO stickers_featured VALUES(?, ?, ?, ?, ?)");
                    state.requery();
                    int size = 4;
//                    for (int a = 0; a < stickersFinal.size(); a++) {
//                        size += stickersFinal.get(a).getObjectSize();
//                    }
                    NativeByteBuffer data = new NativeByteBuffer(size);
                    NativeByteBuffer data2 = new NativeByteBuffer(4 + unreadStickers.size() * 8);
                    data.writeInt32(stickersFinal.size());
//                    for (int a = 0; a < stickersFinal.size(); a++) {
//                        stickersFinal.get(a).serializeToStream(data);
//                    }
                    data2.writeInt32(unreadStickers.size());
                    for (int a = 0; a < unreadStickers.size(); a++) {
                        data2.writeInt64(unreadStickers.get(a));
                    }
                    state.bindInteger(1, 1);
                    state.bindByteBuffer(2, data);
                    state.bindByteBuffer(3, data2);
                    state.bindInteger(4, date);
                    state.bindInteger(5, hash);
                    state.step();
                    data.reuse();
                    data2.reuse();
                    state.dispose();
                } else {
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("UPDATE stickers_featured SET date = ?");
                    state.requery();
                    state.bindInteger(1, date);
                    state.step();
                    state.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private int calcFeaturedStickersHash(ArrayList<StickerSetCovered> sets) {
        long acc = 0;
        for (int a = 0; a < sets.size(); a++) {
            StickerSet set = sets.get(a).set;
            if (set.archived) {
                continue;
            }
            int high_id = (int) (set.id >> 32);
            int lower_id = (int) set.id;
            acc = ((acc * 20261) + 0x80000000L + high_id) % 0x80000000L;
            acc = ((acc * 20261) + 0x80000000L + lower_id) % 0x80000000L;
            if (unreadStickerSets.contains(set.id)) {
                acc = ((acc * 20261) + 0x80000000L + 1) % 0x80000000L;
            }
        }
        return (int) acc;
    }

    public void markFaturedStickersAsRead(boolean query) {
        if (unreadStickerSets.isEmpty()) {
            return;
        }
        unreadStickerSets.clear();
        loadFeaturedHash = calcFeaturedStickersHash(featuredStickerSets);
        getNotificationCenter().postNotificationName(NotificationCenter.featuredStickersDidLoad);
        putFeaturedStickersToCache(featuredStickerSets, unreadStickerSets, loadFeaturedDate, loadFeaturedHash);
        if (query) {
            //TODO 发起请求
//            TL_messages_readFeaturedStickers req = new TL_messages_readFeaturedStickers();
//            getConnectionsManager().sendRequest(req, (response, error) -> {
//
//            });
        }
    }

    public int getFeaturesStickersHashWithoutUnread() {
        long acc = 0;
        for (int a = 0; a < featuredStickerSets.size(); a++) {
            StickerSet set = featuredStickerSets.get(a).set;
            if (set.archived) {
                continue;
            }
            int high_id = (int) (set.id >> 32);
            int lower_id = (int) set.id;
            acc = ((acc * 20261) + 0x80000000L + high_id) % 0x80000000L;
            acc = ((acc * 20261) + 0x80000000L + lower_id) % 0x80000000L;
        }
        return (int) acc;
    }

    public void markFaturedStickersByIdAsRead(final long id) {
        if (!unreadStickerSets.contains(id) || readingStickerSets.contains(id)) {
            return;
        }
        readingStickerSets.add(id);
        //TODO 发起请求
//        TL_messages_readFeaturedStickers req = new TL_messages_readFeaturedStickers();
//        req.id.add(id);
//        getConnectionsManager().sendRequest(req, (response, error) -> {
//
//        });
        AndroidUtilities.runOnUIThread(() -> {
            unreadStickerSets.remove(id);
            readingStickerSets.remove(id);
            loadFeaturedHash = calcFeaturedStickersHash(featuredStickerSets);
            getNotificationCenter().postNotificationName(NotificationCenter.featuredStickersDidLoad);
            putFeaturedStickersToCache(featuredStickerSets, unreadStickerSets, loadFeaturedDate, loadFeaturedHash);
        }, 1000);
    }

    public int getArchivedStickersCount(int type) {
        return archivedStickersCount[type];
    }


    public void verifyAnimatedStickerMessage(Message message) {
        verifyAnimatedStickerMessage(message, false);
    }

    public void verifyAnimatedStickerMessage(Message message, boolean safe) {
        if (message == null) {
            return;
        }
        Document document = MessageObject.getDocument(message);
        String name = MessageObject.getStickerSetName(document);
        if (TextUtils.isEmpty(name)) {
            return;
        }
        MessagesStickerSet stickerSet = stickerSetsByName.get(name);
        if (stickerSet != null) {
            for (int a = 0, N = stickerSet.documents.size(); a < N; a++) {
                Document sticker = stickerSet.documents.get(a);
                if (sticker.id == document.id && sticker.dc_id == document.dc_id) {
                    message.stickerVerified = 1;
                    break;
                }
            }
            return;
        }
        if (safe) {
            AndroidUtilities.runOnUIThread(() -> verifyAnimatedStickerMessageInternal(message, name));
        } else {
            verifyAnimatedStickerMessageInternal(message, name);
        }
    }

    private void verifyAnimatedStickerMessageInternal(Message message, String name) {
        ArrayList<Message> messages = verifyingMessages.get(name);
        if (messages == null) {
            messages = new ArrayList<>();
            verifyingMessages.put(name, messages);
        }
        messages.add(message);
        //TODO 发起请求
//        TL_messages_getStickerSet req = new TL_messages_getStickerSet();
//        req.stickerset = MessageObject.getInputStickerSet(message);
//        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//            ArrayList<Message> arrayList = verifyingMessages.get(name);
//            if (response != null) {
//                MessagesStickerSet set = (MessagesStickerSet) response;
//                storeTempStickerSet(set);
//                for (int b = 0, N2 = arrayList.size(); b < N2; b++) {
//                    Message m = arrayList.get(b);
//                    Document d = MessageObject.getDocument(m);
//                    for (int a = 0, N = set.documents.size(); a < N; a++) {
//                        Document sticker = set.documents.get(a);
//                        if (sticker.id == d.id && sticker.dc_id == d.dc_id) {
//                            m.stickerVerified = 1;
//                            break;
//                        }
//                    }
//                    if (m.stickerVerified == 0) {
//                        m.stickerVerified = 2;
//                    }
//                }
//            } else {
//                for (int b = 0, N2 = arrayList.size(); b < N2; b++) {
//                    arrayList.get(b).stickerVerified = 2;
//                }
//            }
//            getNotificationCenter().postNotificationName(NotificationCenter.didVerifyMessagesStickers, arrayList);
//            getMessagesStorage().updateMessageVerifyFlags(arrayList);
//        }));
    }

    public void loadArchivedStickersCount(final int type, boolean cache) {
        if (cache) {
            SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
            int count = preferences.getInt("archivedStickersCount" + type, -1);
            if (count == -1) {
                loadArchivedStickersCount(type, false);
            } else {
                archivedStickersCount[type] = count;
                getNotificationCenter().postNotificationName(NotificationCenter.archivedStickersCountDidLoad, type);
            }
        } else {
            //TODO 发起请求
//            TL_messages_getArchivedStickers req = new TL_messages_getArchivedStickers();
//            req.limit = 0;
//            req.masks = type == TYPE_MASK;
//            int reqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                if (error == null) {
//                    TL_messages_archivedStickers res = (TL_messages_archivedStickers) response;
//                    archivedStickersCount[type] = res.count;
//                    SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
//                    preferences.edit().putInt("archivedStickersCount" + type, res.count).commit();
//                    getNotificationCenter().postNotificationName(NotificationCenter.archivedStickersCountDidLoad, type);
//                }
//            }));
        }
    }

//    private void processLoadStickersResponse(final int type, final TL_messages_allStickers res) {
//        final ArrayList<MessagesStickerSet> newStickerArray = new ArrayList<>();
//        if (res.sets.isEmpty()) {
//            processLoadedStickers(type, newStickerArray, false, (int) (System.currentTimeMillis() / 1000), res.hash);
//        } else {
//            final LongSparseArray<MessagesStickerSet> newStickerSets = new LongSparseArray<>();
//            for (int a = 0; a < res.sets.size(); a++) {
//                final StickerSet stickerSet = res.sets.get(a);
//
//                MessagesStickerSet oldSet = stickerSetsById.get(stickerSet.id);
//                if (oldSet != null && oldSet.set.hash == stickerSet.hash) {
//                    oldSet.set.archived = stickerSet.archived;
//                    oldSet.set.installed = stickerSet.installed;
//                    oldSet.set.official = stickerSet.official;
//                    newStickerSets.put(oldSet.set.id, oldSet);
//                    newStickerArray.add(oldSet);
//
//                    if (newStickerSets.size() == res.sets.size()) {
//                        processLoadedStickers(type, newStickerArray, false, (int) (System.currentTimeMillis() / 1000), res.hash);
//                    }
//                    continue;
//                }
//
//                newStickerArray.add(null);
//                final int index = a;
//
//                //TODO 发起请求
////                TL_messages_getStickerSet req = new TL_messages_getStickerSet();
////                req.stickerset = new TL_inputStickerSetID();
////                req.stickerset.id = stickerSet.id;
////                req.stickerset.access_hash = stickerSet.access_hash;
////
////                getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
////                    MessagesStickerSet res1 = (MessagesStickerSet) response;
////                    newStickerArray.set(index, res1);
////                    newStickerSets.put(stickerSet.id, res1);
////                    if (newStickerSets.size() == res.sets.size()) {
////                        for (int a1 = 0; a1 < newStickerArray.size(); a1++) {
////                            if (newStickerArray.get(a1) == null) {
////                                newStickerArray.remove(a1);
////                                a1--;
////                            }
////                        }
////                        processLoadedStickers(type, newStickerArray, false, (int) (System.currentTimeMillis() / 1000), res.hash);
////                    }
////                }));
//            }
//        }
//    }

    public void loadDiceStickers(String emoji, boolean cache) {
        if (loadingDiceStickerSets.contains(emoji) || diceStickerSetsByEmoji.get(emoji) != null) {
            return;
        }
        loadingDiceStickerSets.add(emoji);
        if (cache) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                MessagesStickerSet stickerSet = null;
                int date = 0;
                SQLiteCursor cursor = null;
                try {
                    cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT data, date FROM stickers_dice WHERE emoji = ?", emoji);
                    if (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            stickerSet = MessagesStickerSet.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                        }
                        date = cursor.intValue(1);
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
                processLoadedDiceStickers(emoji, stickerSet, true, date);
            });
        } else {
            //TODO 发送请求
//            TL_messages_getStickerSet req = new TL_messages_getStickerSet();
//            TL_inputStickerSetDice inputStickerSetDice = new TL_inputStickerSetDice();
//            inputStickerSetDice.emoticon = emoji;
//            req.stickerset = inputStickerSetDice;
//            getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                if (response instanceof MessagesStickerSet) {
//                    processLoadedDiceStickers(emoji, (MessagesStickerSet) response, false, (int) (System.currentTimeMillis() / 1000));
//                } else {
//                    processLoadedDiceStickers(emoji, null, false, (int) (System.currentTimeMillis() / 1000));
//                }
//            }));
        }
    }

    private void processLoadedDiceStickers(String emoji, MessagesStickerSet res, final boolean cache, final int date) {
        AndroidUtilities.runOnUIThread(() -> loadingDiceStickerSets.remove(emoji));
        Utilities.stageQueue.postRunnable(() -> {
            if (cache && (res == null || Math.abs(System.currentTimeMillis() / 1000 - date) >= 60 * 60 * 24) || !cache && res == null) {
                AndroidUtilities.runOnUIThread(() -> loadDiceStickers(emoji, false), res == null && !cache ? 1000 : 0);
                if (res == null) {
                    return;
                }
            }
            if (res != null) {
                if (!cache) {
                    putDiceStickersToCache(emoji, res, date);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    diceStickerSetsByEmoji.put(emoji, res);
                    diceEmojiStickerSetsById.put(res.set.id, emoji);
                    getNotificationCenter().postNotificationName(NotificationCenter.diceStickersDidLoad, emoji);
                });
            } else if (!cache) {
                putDiceStickersToCache(emoji, null, date);
            }
        });
    }

    private void putDiceStickersToCache(final String emoji, MessagesStickerSet stickers, final int date) {
        if (TextUtils.isEmpty(emoji)) {
            return;
        }
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                if (stickers != null) {
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO stickers_dice VALUES(?, ?, ?)");
                    state.requery();
//                    NativeByteBuffer data = new NativeByteBuffer(stickers.getObjectSize());
//                    stickers.serializeToStream(data);
                    state.bindString(1, emoji);
//                    state.bindByteBuffer(2, data);
                    state.bindInteger(3, date);
                    state.step();
//                    data.reuse();
                    state.dispose();
                } else {
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("UPDATE stickers_dice SET date = ?");
                    state.requery();
                    state.bindInteger(1, date);
                    state.step();
                    state.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void loadStickers(final int type, boolean cache, boolean useHash) {
        loadStickers(type, cache, useHash, false);
    }

    public void loadStickers(final int type, boolean cache, boolean force, boolean scheduleIfLoading) {
        if (loadingStickers[type]) {
            if (scheduleIfLoading) {
                scheduledLoadStickers[type] = () -> loadStickers(type, false, force, false);
            }
            return;
        }
        if (type == TYPE_FEATURED) {
            if (featuredStickerSets.isEmpty() || !getMessagesController().preloadFeaturedStickers) {
                return;
            }
        } else if (type != TYPE_EMOJI) {
            loadArchivedStickersCount(type, cache);
        }
        loadingStickers[type] = true;
        if (cache) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                ArrayList<MessagesStickerSet> newStickerArray = null;
                int date = 0;
                int hash = 0;
                SQLiteCursor cursor = null;
                try {
                    cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT data, date, hash FROM stickers_v2 WHERE id = " + (type + 1));
                    if (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            newStickerArray = new ArrayList<>();
                            int count = data.readInt32(false);
                            for (int a = 0; a < count; a++) {
                                MessagesStickerSet stickerSet = MessagesStickerSet.TLdeserialize(data, data.readInt32(false), false);
                                newStickerArray.add(stickerSet);
                            }
                            data.reuse();
                        }
                        date = cursor.intValue(1);
                        hash = calcStickersHash(newStickerArray);
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
                processLoadedStickers(type, newStickerArray, true, date, hash);
            });
        } else {
            if (type == TYPE_FEATURED) {
//                TL_messages_allStickers response = new TL_messages_allStickers();
//                response.hash = loadFeaturedHash;
//                for (int a = 0, size = featuredStickerSets.size(); a < size; a++) {
//                    response.sets.add(featuredStickerSets.get(a).set);
//                }
//                processLoadStickersResponse(type, response);
            } else if (type == TYPE_EMOJI) {
                //TODO 发起请求
//                TL_messages_getStickerSet req = new TL_messages_getStickerSet();
//                req.stickerset = new TL_inputStickerSetAnimatedEmoji();
//                getConnectionsManager().sendRequest(req, (response, error) -> {
//                    if (response instanceof MessagesStickerSet) {
//                        final ArrayList<MessagesStickerSet> newStickerArray = new ArrayList<>();
//                        newStickerArray.add((MessagesStickerSet) response);
//                        processLoadedStickers(type, newStickerArray, false, (int) (System.currentTimeMillis() / 1000), calcStickersHash(newStickerArray));
//                    } else {
//                        processLoadedStickers(type, null, false, (int) (System.currentTimeMillis() / 1000), 0);
//                    }
//                });
            } else {
                //TODO 发起请求
//                TLObject req;
//                final int hash;
//                if (type == TYPE_IMAGE) {
//                    req = new TL_messages_getAllStickers();
//                    hash = ((TL_messages_getAllStickers) req).hash = force ? 0 : loadHash[type];
//                } else {
//                    req = new TL_messages_getMaskStickers();
//                    hash = ((TL_messages_getMaskStickers) req).hash = force ? 0 : loadHash[type];
//                }
//                getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                    if (response instanceof TL_messages_allStickers) {
//                        processLoadStickersResponse(type, (TL_messages_allStickers) response);
//                    } else {
//                        processLoadedStickers(type, null, false, (int) (System.currentTimeMillis() / 1000), hash);
//                    }
//                }));
            }
        }
    }

    private void putStickersToCache(final int type, ArrayList<MessagesStickerSet> stickers, final int date, final int hash) {
        final ArrayList<MessagesStickerSet> stickersFinal = stickers != null ? new ArrayList<>(stickers) : null;
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                if (stickersFinal != null) {
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO stickers_v2 VALUES(?, ?, ?, ?)");
                    state.requery();
                    int size = 4;
//                    for (int a = 0; a < stickersFinal.size(); a++) {
//                        size += stickersFinal.get(a).getObjectSize();
//                    }
                    NativeByteBuffer data = new NativeByteBuffer(size);
                    data.writeInt32(stickersFinal.size());
//                    for (int a = 0; a < stickersFinal.size(); a++) {
//                        stickersFinal.get(a).serializeToStream(data);
//                    }
                    state.bindInteger(1, type + 1);
                    state.bindByteBuffer(2, data);
                    state.bindInteger(3, date);
                    state.bindInteger(4, hash);
                    state.step();
                    data.reuse();
                    state.dispose();
                } else {
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("UPDATE stickers_v2 SET date = ?");
                    state.requery();
                    state.bindInteger(1, date);
                    state.step();
                    state.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public String getStickerSetName(long setId) {
        MessagesStickerSet stickerSet = stickerSetsById.get(setId);
        if (stickerSet != null) {
            return stickerSet.set.short_name;
        }
        StickerSetCovered stickerSetCovered = featuredStickerSetsById.get(setId);
        if (stickerSetCovered != null) {
            return stickerSetCovered.set.short_name;
        }
        return null;
    }

    public static long getStickerSetId(Document document) {
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isSticker()) {
                if (attribute.stickerset!=null) {
                    return attribute.stickerset.id;
                }
                break;
            }
        }
        return -1;
    }

    public static InputStickerSet getInputStickerSet(Document document) {
        for (int a = 0; a < document.attributes.size(); a++) {
            Document.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute.isSticker()) {
                if (attribute.stickerset==null) {
                    return null;
                }
                return attribute.stickerset;
            }
        }
        return null;
    }

    private static int calcStickersHash(ArrayList<MessagesStickerSet> sets) {
        long acc = 0;
        for (int a = 0; a < sets.size(); a++) {
            StickerSet set = sets.get(a).set;
            if (set.archived) {
                continue;
            }
            acc = ((acc * 20261) + 0x80000000L + set.hash) % 0x80000000L;
        }
        return (int) acc;
    }

    private void processLoadedStickers(final int type, final ArrayList<MessagesStickerSet> res, final boolean cache, final int date, final int hash) {
        AndroidUtilities.runOnUIThread(() -> {
            loadingStickers[type] = false;
            stickersLoaded[type] = true;
            if (scheduledLoadStickers[type] != null) {
                scheduledLoadStickers[type].run();
                scheduledLoadStickers[type] = null;
            }
        });
        Utilities.stageQueue.postRunnable(() -> {
            if (cache && (res == null || Math.abs(System.currentTimeMillis() / 1000 - date) >= 60 * 60) || !cache && res == null && hash == 0) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (res != null && hash != 0) {
                        loadHash[type] = hash;
                    }
                    loadStickers(type, false, false);
                }, res == null && !cache ? 1000 : 0);
                if (res == null) {
                    return;
                }
            }
            if (res != null) {
                try {
                    final ArrayList<MessagesStickerSet> stickerSetsNew = new ArrayList<>();
                    final LongSparseArray<MessagesStickerSet> stickerSetsByIdNew = new LongSparseArray<>();
                    final HashMap<String, MessagesStickerSet> stickerSetsByNameNew = new HashMap<>();
                    final LongSparseArray<String> stickersByEmojiNew = new LongSparseArray<>();
                    final LongSparseArray<Document> stickersByIdNew = new LongSparseArray<>();
                    final HashMap<String, ArrayList<Document>> allStickersNew = new HashMap<>();

                    for (int a = 0; a < res.size(); a++) {
                        MessagesStickerSet stickerSet = res.get(a);
                        if (stickerSet == null || removingStickerSetsUndos.indexOfKey(stickerSet.set.id) >= 0) {
                            continue;
                        }
                        stickerSetsNew.add(stickerSet);
                        stickerSetsByIdNew.put(stickerSet.set.id, stickerSet);
                        stickerSetsByNameNew.put(stickerSet.set.short_name, stickerSet);

                        for (int b = 0; b < stickerSet.documents.size(); b++) {
                            Document document = stickerSet.documents.get(b);
                            if (document == null || document instanceof Document) {
                                continue;
                            }
                            stickersByIdNew.put(document.id, document);
                        }
                        if (!stickerSet.set.archived) {
                            for (int b = 0; b < stickerSet.packs.size(); b++) {
                                MessagesStickerSet.StickerPack stickerPack = stickerSet.packs.get(b);
                                if (stickerPack == null || stickerPack.emoticon == null) {
                                    continue;
                                }
                                stickerPack.emoticon = stickerPack.emoticon.replace("\uFE0F", "");
                                ArrayList<Document> arrayList = allStickersNew.get(stickerPack.emoticon);
                                if (arrayList == null) {
                                    arrayList = new ArrayList<>();
                                    allStickersNew.put(stickerPack.emoticon, arrayList);
                                }
                                for (int c = 0; c < stickerPack.documents.size(); c++) {
                                    Long id = stickerPack.documents.get(c);
                                    if (stickersByEmojiNew.indexOfKey(id) < 0) {
                                        stickersByEmojiNew.put(id, stickerPack.emoticon);
                                    }
                                    Document sticker = stickersByIdNew.get(id);
                                    if (sticker != null) {
                                        arrayList.add(sticker);
                                    }
                                }
                            }
                        }
                    }

                    if (!cache) {
                        putStickersToCache(type, stickerSetsNew, date, hash);
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        for (int a = 0; a < stickerSets[type].size(); a++) {
                            StickerSet set = stickerSets[type].get(a).set;
                            stickerSetsById.remove(set.id);
                            stickerSetsByName.remove(set.short_name);
                            if (type != TYPE_FEATURED && type != TYPE_EMOJI) {
                                installedStickerSetsById.remove(set.id);
                            }
                        }
                        for (int a = 0; a < stickerSetsByIdNew.size(); a++) {
                            stickerSetsById.put(stickerSetsByIdNew.keyAt(a), stickerSetsByIdNew.valueAt(a));
                            if (type != TYPE_FEATURED && type != TYPE_EMOJI) {
                                installedStickerSetsById.put(stickerSetsByIdNew.keyAt(a), stickerSetsByIdNew.valueAt(a));
                            }
                        }
                        stickerSetsByName.putAll(stickerSetsByNameNew);
                        stickerSets[type] = stickerSetsNew;
                        loadHash[type] = hash;
                        loadDate[type] = date;
                        stickersByIds[type] = stickersByIdNew;
                        if (type == TYPE_IMAGE) {
                            allStickers = allStickersNew;
                            stickersByEmoji = stickersByEmojiNew;
                        } else if (type == TYPE_FEATURED) {
                            allStickersFeatured = allStickersNew;
                        }
                        getNotificationCenter().postNotificationName(NotificationCenter.stickersDidLoad, type);
                    });
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            } else if (!cache) {
                AndroidUtilities.runOnUIThread(() -> loadDate[type] = date);
                putStickersToCache(type, null, date, 0);
            }
        });
    }

    public boolean cancelRemovingStickerSet(long id) {
        final Runnable undoAction = removingStickerSetsUndos.get(id);
        if (undoAction != null) {
            undoAction.run();
            return true;
        } else {
            return false;
        }
    }

    public void preloadStickerSetThumb(MessagesStickerSet stickerSet) {
        if (stickerSet.set.thumb instanceof PhotoSize) {
            final ArrayList<Document> documents = stickerSet.documents;
            if (documents != null && !documents.isEmpty()) {
                loadStickerSetThumbInternal(stickerSet.set.thumb, stickerSet, documents.get(0));
            }
        }
    }

    public void preloadStickerSetThumb(StickerSetCovered stickerSet) {
        if (stickerSet.set.thumb != null) {
            final Document sticker;
            if (stickerSet.cover != null) {
                sticker = stickerSet.cover;
            } else if (!stickerSet.covers.isEmpty()) {
                sticker = stickerSet.covers.get(0);
            } else {
                return;
            }
            loadStickerSetThumbInternal(stickerSet.set.thumb, stickerSet, sticker);
        }
    }

    private void loadStickerSetThumbInternal(PhotoSize thumb, Object parentObject, Document sticker) {
        final ImageLocation imageLocation = ImageLocation.getForSticker(thumb, sticker);
        if (imageLocation != null) {
            final String ext = imageLocation.imageType == FileLoader.IMAGE_TYPE_LOTTIE ? "tgs" : "webp";
            getFileLoader().loadFile(imageLocation, parentObject, ext, 2, 1);
        }
    }

    /** @param toggle 0 - remove, 1 - archive, 2 - add */
    public void toggleStickerSet(final Context context, final Sticker stickerSetObject, final int toggle, final BaseFragment baseFragment, final boolean showSettings, boolean showTooltip) {
        final StickerSet stickerSet;
        final MessagesStickerSet messages_stickerSet;

        if (stickerSetObject instanceof MessagesStickerSet) {
            messages_stickerSet = ((MessagesStickerSet) stickerSetObject);
            stickerSet = messages_stickerSet.set;
        } else if (stickerSetObject instanceof StickerSetCovered) {
            stickerSet = ((StickerSetCovered) stickerSetObject).set;
            if (toggle != 2) {
                messages_stickerSet = stickerSetsById.get(stickerSet.id);
                if (messages_stickerSet == null) {
                    return;
                }
            } else {
                messages_stickerSet = null;
            }
        } else {
            throw new IllegalArgumentException("Invalid type of the given stickerSetObject: " + stickerSetObject.getClass());
        }

        final int type = stickerSet.masks ? TYPE_MASK : TYPE_IMAGE;

        stickerSet.archived = toggle == 1;

        int currentIndex = 0;
        for (int a = 0; a < stickerSets[type].size(); a++) {
            MessagesStickerSet set = stickerSets[type].get(a);
            if (set.set.id == stickerSet.id) {
                currentIndex = a;
                stickerSets[type].remove(a);
                if (toggle == 2) {
                    stickerSets[type].add(0, set);
                } else {
                    stickerSetsById.remove(set.set.id);
                    installedStickerSetsById.remove(set.set.id);
                    stickerSetsByName.remove(set.set.short_name);
                }
                break;
            }
        }

        loadHash[type] = calcStickersHash(stickerSets[type]);
        putStickersToCache(type, stickerSets[type], loadDate[type], loadHash[type]);
        getNotificationCenter().postNotificationName(NotificationCenter.stickersDidLoad, type);

        if (toggle == 2) {
            if (!cancelRemovingStickerSet(stickerSet.id)) {
                toggleStickerSetInternal(context, toggle, baseFragment, showSettings, stickerSetObject, stickerSet, type, showTooltip);
            }
        } else if (!showTooltip || baseFragment == null) {
            toggleStickerSetInternal(context, toggle, baseFragment, showSettings, stickerSetObject, stickerSet, type, false);
        } else {
            final StickerSetBulletinLayout bulletinLayout = new StickerSetBulletinLayout(context, stickerSetObject, toggle);
            final int finalCurrentIndex = currentIndex;
            final Bulletin.UndoButton undoButton = new Bulletin.UndoButton(context).setUndoAction(() -> {
                stickerSet.archived = false;

                stickerSets[type].add(finalCurrentIndex, messages_stickerSet);
                stickerSetsById.put(stickerSet.id, messages_stickerSet);
                installedStickerSetsById.put(stickerSet.id, messages_stickerSet);
                stickerSetsByName.put(stickerSet.short_name, messages_stickerSet);
                removingStickerSetsUndos.remove(stickerSet.id);

                loadHash[type] = calcStickersHash(stickerSets[type]);
                putStickersToCache(type, stickerSets[type], loadDate[type], loadHash[type]);
                getNotificationCenter().postNotificationName(NotificationCenter.stickersDidLoad, type);
            }).setDelayedAction(() -> toggleStickerSetInternal(context, toggle, baseFragment, showSettings, stickerSetObject, stickerSet, type, false));
            bulletinLayout.setButton(undoButton);
            removingStickerSetsUndos.put(stickerSet.id, undoButton::undo);
            Bulletin.make(baseFragment, bulletinLayout, Bulletin.DURATION_LONG).show();
        }
    }

    private void toggleStickerSetInternal(Context context, int toggle, BaseFragment baseFragment, boolean showSettings, Sticker stickerSetObject, StickerSet stickerSet, int type, boolean showTooltip) {
        InputStickerSet stickerSetID = new InputStickerSet();
        stickerSetID.access_hash = stickerSet.access_hash;
        stickerSetID.id = stickerSet.id;

        //TODO 发起请求
//        if (toggle != 0) {
//            TL_messages_installStickerSet req = new TL_messages_installStickerSet();
//            req.stickerset = stickerSetID;
//            req.archived = toggle == 1;
//            getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                removingStickerSetsUndos.remove(stickerSet.id);
//                if (response instanceof TL_messages_stickerSetInstallResultArchive) {
//                    processStickerSetInstallResultArchive(baseFragment, showSettings, type, (TL_messages_stickerSetInstallResultArchive) response);
//                }
//                loadStickers(type, false, false, true);
//                if (error == null && showTooltip && baseFragment != null) {
//                    Bulletin.make(baseFragment, new StickerSetBulletinLayout(context, stickerSetObject, StickerSetBulletinLayout.TYPE_ADDED), Bulletin.DURATION_SHORT).show();
//                }
//            }));
//        } else {
//            TL_messages_uninstallStickerSet req = new TL_messages_uninstallStickerSet();
//            req.stickerset = stickerSetID;
//            getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                removingStickerSetsUndos.remove(stickerSet.id);
//                loadStickers(type, false, true);
//            }));
//        }
    }

    /** @param toggle 0 - uninstall, 1 - archive, 2 - unarchive */
    public void toggleStickerSets(final ArrayList<StickerSet> stickerSetList, final int type, final int toggle, final BaseFragment baseFragment, final boolean showSettings) {
        final int stickerSetListSize = stickerSetList.size();
        final ArrayList<InputStickerSet> inputStickerSets = new ArrayList<>(stickerSetListSize);

        for (int i = 0; i < stickerSetListSize; i++) {
            final StickerSet stickerSet = stickerSetList.get(i);
            final InputStickerSet inputStickerSet = new InputStickerSet();
            inputStickerSet.access_hash = stickerSet.access_hash;
            inputStickerSet.id = stickerSet.id;
            inputStickerSets.add(inputStickerSet);
            if (toggle != 0) {
                stickerSet.archived = toggle == 1;
            }
            for (int a = 0, size = stickerSets[type].size(); a < size; a++) {
                MessagesStickerSet set = stickerSets[type].get(a);
                if (set.set.id == inputStickerSet.id) {
                    stickerSets[type].remove(a);
                    if (toggle == 2) {
                        stickerSets[type].add(0, set);
                    } else {
                        stickerSetsById.remove(set.set.id);
                        installedStickerSetsById.remove(set.set.id);
                        stickerSetsByName.remove(set.set.short_name);
                    }
                    break;
                }
            }
        }

        loadHash[type] = calcStickersHash(this.stickerSets[type]);
        putStickersToCache(type, this.stickerSets[type], loadDate[type], loadHash[type]);
        getNotificationCenter().postNotificationName(NotificationCenter.stickersDidLoad, type);

        //TODO 发起请求
//        final TL_messages_toggleStickerSets req = new TL_messages_toggleStickerSets();
//        req.stickersets = inputStickerSets;
//        switch (toggle) {
//            case 0:
//                req.uninstall = true;
//                break;
//            case 1:
//                req.archive = true;
//                break;
//            case 2:
//                req.unarchive = true;
//                break;
//        }
//        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//            if (toggle != 0) {
//                if (response instanceof TL_messages_stickerSetInstallResultArchive) {
//                    processStickerSetInstallResultArchive(baseFragment, showSettings, type, (TL_messages_stickerSetInstallResultArchive) response);
//                }
//                loadStickers(type, false, false, true);
//            } else {
//                loadStickers(type, false, true);
//            }
//        }));
    }

//    public void processStickerSetInstallResultArchive(BaseFragment baseFragment, boolean showSettings, int type, TL_messages_stickerSetInstallResultArchive response) {
//        for (int i = 0, size = response.sets.size(); i < size; i++) {
//            installedStickerSetsById.remove(response.sets.get(i).set.id);
//        }
//        loadArchivedStickersCount(type, false);
//        getNotificationCenter().postNotificationName(NotificationCenter.needAddArchivedStickers, response.sets);
//        if (baseFragment != null && baseFragment.getParentActivity() != null) {
//            final StickersArchiveAlert alert = new StickersArchiveAlert(baseFragment.getParentActivity(), showSettings ? baseFragment : null, response.sets);
//            baseFragment.showDialog(alert.create());
//        }
//    }
    //endregion ---------------- STICKERS END ----------------

    //region ---------------- MESSAGE SEARCH ----------------
    private int reqId;
    private int mergeReqId;
    private long lastMergeDialogId;
    private long lastDialogId;
    private int lastReqId;
    private int lastGuid;
    private User lastSearchUser;
    private int[] messagesSearchCount = new int[]{0, 0};
    private boolean[] messagesSearchEndReached = new boolean[]{false, false};
    private ArrayList<MessageObject> searchResultMessages = new ArrayList<>();
    private SparseArray<MessageObject>[] searchResultMessagesMap = new SparseArray[]{new SparseArray<>(), new SparseArray<>()};
    private String lastSearchQuery;
    private int lastReturnedNum;
    private boolean loadingMoreSearchMessages;

    private int getMask() {
        int mask = 0;
        if (lastReturnedNum < searchResultMessages.size() - 1 || !messagesSearchEndReached[0] || !messagesSearchEndReached[1]) {
            mask |= 1;
        }
        if (lastReturnedNum > 0) {
            mask |= 2;
        }
        return mask;
    }

    public ArrayList<MessageObject> getFoundMessageObjects() {
        return searchResultMessages;
    }

    public void clearFoundMessageObjects() {
        searchResultMessages.clear();
    }

    public boolean isMessageFound(final int messageId, boolean mergeDialog) {
        return searchResultMessagesMap[mergeDialog ? 1 : 0].indexOfKey(messageId) >= 0;
    }

    public void searchMessagesInChat(String query, final long dialogId, final long mergeDialogId, final int guid, final int direction, User user) {
        searchMessagesInChat(query, dialogId, mergeDialogId, guid, direction, false, user, true);
    }

    public void jumpToSearchedMessage(int guid, int index) {
        if (index < 0 || index >= searchResultMessages.size()) {
            return;
        }
        lastReturnedNum = index;
        MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
        getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], true);
    }

    public void loadMoreSearchMessages() {
        if (loadingMoreSearchMessages || messagesSearchEndReached[0] && lastMergeDialogId == 0 && messagesSearchEndReached[1]) {
            return;
        }
        int temp = searchResultMessages.size();
        lastReturnedNum = searchResultMessages.size();
        searchMessagesInChat(null, lastDialogId, lastMergeDialogId, lastGuid, 1, false, lastSearchUser, false);
        lastReturnedNum = temp;
        loadingMoreSearchMessages = true;
    }

    private void searchMessagesInChat(String query, final long dialogId, final long mergeDialogId, final int guid, final int direction, final boolean internal, final User user, boolean jumpToMessage) {
        int max_id = 0;
        long queryWithDialog = dialogId;
        boolean firstQuery = !internal;
//        if (reqId != 0) {
//            getConnectionsManager().cancelRequest(reqId, true);
//            reqId = 0;
//        }
//        if (mergeReqId != 0) {
//            getConnectionsManager().cancelRequest(mergeReqId, true);
//            mergeReqId = 0;
//        }TODO 取消请求
        if (query == null) {
            if (searchResultMessages.isEmpty()) {
                return;
            }
            if (direction == 1) {
                lastReturnedNum++;
                if (lastReturnedNum < searchResultMessages.size()) {
                    MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
                    getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], jumpToMessage);
                    return;
                } else {
                    if (messagesSearchEndReached[0] && mergeDialogId == 0 && messagesSearchEndReached[1]) {
                        lastReturnedNum--;
                        return;
                    }
                    firstQuery = false;
                    query = lastSearchQuery;
                    MessageObject messageObject = searchResultMessages.get(searchResultMessages.size() - 1);
                    if (messageObject.getDialogId() == dialogId && !messagesSearchEndReached[0]) {
                        max_id = messageObject.getId();
                        queryWithDialog = dialogId;
                    } else {
                        if (messageObject.getDialogId() == mergeDialogId) {
                            max_id = messageObject.getId();
                        }
                        queryWithDialog = mergeDialogId;
                        messagesSearchEndReached[1] = false;
                    }
                }
            } else if (direction == 2) {
                lastReturnedNum--;
                if (lastReturnedNum < 0) {
                    lastReturnedNum = 0;
                    return;
                }
                if (lastReturnedNum >= searchResultMessages.size()) {
                    lastReturnedNum = searchResultMessages.size() - 1;
                }
                MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
                getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], jumpToMessage);
                return;
            } else {
                return;
            }
        } else if (firstQuery) {
            messagesSearchEndReached[0] = messagesSearchEndReached[1] = false;
            messagesSearchCount[0] = messagesSearchCount[1] = 0;
            searchResultMessages.clear();
            searchResultMessagesMap[0].clear();
            searchResultMessagesMap[1].clear();
            getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsLoading, guid);
        }
        if (messagesSearchEndReached[0] && !messagesSearchEndReached[1] && mergeDialogId != 0) {
            queryWithDialog = mergeDialogId;
        }
        if (queryWithDialog == dialogId && firstQuery) {
            if (mergeDialogId != 0) {
                //TODO 发起请求
//                InputPeer inputPeer = getMessagesController().getInputPeer((int) mergeDialogId);
//                if (inputPeer == null) {
//                    return;
//                }
//                final TL_messages_search req = new TL_messages_search();
//                req.peer = inputPeer;
//                lastMergeDialogId = mergeDialogId;
//                req.limit = 1;
//                req.q = query != null ? query : "";
//                if (user != null) {
//                    req.from_id = getMessagesController().getInputUser(user);
//                    req.flags |= 1;
//                }
//                req.filter = new TL_inputMessagesFilterEmpty();
//                mergeReqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                    if (lastMergeDialogId == mergeDialogId) {
//                        mergeReqId = 0;
//                        if (response != null) {
//                            messages_Messages res = (messages_Messages) response;
//                            messagesSearchEndReached[1] = res.messages.isEmpty();
//                            messagesSearchCount[1] = res instanceof TL_messages_messagesSlice ? res.count : res.messages.size();
//                            searchMessagesInChat(req.q, dialogId, mergeDialogId, guid, direction, true, user, jumpToMessage);
//                        }
//                    }
//                }), ConnectionsManager.RequestFlagFailOnServerErrors);
                return;
            } else {
                lastMergeDialogId = 0;
                messagesSearchEndReached[1] = true;
                messagesSearchCount[1] = 0;
            }
        }
        //TODO 发起请求
//        final TL_messages_search req = new TL_messages_search();
//        req.peer = getMessagesController().getInputPeer((int) queryWithDialog);
//        if (req.peer == null) {
//            return;
//        }
//        lastGuid = guid;
//        lastDialogId = dialogId;
//        lastSearchUser = user;
//        req.limit = 21;
//        req.q = query != null ? query : "";
//        req.offset_id = max_id;
//        if (user != null) {
//            req.from_id = getMessagesController().getInputUser(user);
//            req.flags |= 1;
//        }
//        req.filter = new TL_inputMessagesFilterEmpty();
//        final int currentReqId = ++lastReqId;
//        lastSearchQuery = query;
//        final long queryWithDialogFinal = queryWithDialog;
//        reqId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//            if (currentReqId == lastReqId) {
//                reqId = 0;
//                if (!jumpToMessage) {
//                    loadingMoreSearchMessages = false;
//                }
//                if (response != null) {
//                    messages_Messages res = (messages_Messages) response;
//                    for (int a = 0; a < res.messages.size(); a++) {
//                        Message message = res.messages.get(a);
//                        if (message instanceof TL_messageEmpty || message.action instanceof TL_messageActionHistoryClear) {
//                            res.messages.remove(a);
//                            a--;
//                        }
//                    }
//                    getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
//                    getMessagesController().putUsers(res.users, false);
//                    getMessagesController().putChats(res.chats, false);
//                    if (req.offset_id == 0 && queryWithDialogFinal == dialogId) {
//                        lastReturnedNum = 0;
//                        searchResultMessages.clear();
//                        searchResultMessagesMap[0].clear();
//                        searchResultMessagesMap[1].clear();
//                        messagesSearchCount[0] = 0;
//                        getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsLoading, guid);
//                    }
//                    boolean added = false;
//                    int N = Math.min(res.messages.size(), 20);
//                    for (int a = 0; a < N; a++) {
//                        Message message = res.messages.get(a);
//                        added = true;
//                        MessageObject messageObject = new MessageObject(currentAccount, message, false);
//                        searchResultMessages.add(messageObject);
//                        searchResultMessagesMap[queryWithDialogFinal == dialogId ? 0 : 1].put(messageObject.getId(), messageObject);
//                    }
//                    messagesSearchEndReached[queryWithDialogFinal == dialogId ? 0 : 1] = res.messages.size() != 21;
//                    messagesSearchCount[queryWithDialogFinal == dialogId ? 0 : 1] = res instanceof TL_messages_messagesSlice || res instanceof TL_messages_channelMessages ? res.count : res.messages.size();
//                    if (searchResultMessages.isEmpty()) {
//                        getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, 0, getMask(), (long) 0, 0, 0, jumpToMessage);
//                    } else {
//                        if (added) {
//                            if (lastReturnedNum >= searchResultMessages.size()) {
//                                lastReturnedNum = searchResultMessages.size() - 1;
//                            }
//                            MessageObject messageObject = searchResultMessages.get(lastReturnedNum);
//                            getNotificationCenter().postNotificationName(NotificationCenter.chatSearchResultsAvailable, guid, messageObject.getId(), getMask(), messageObject.getDialogId(), lastReturnedNum, messagesSearchCount[0] + messagesSearchCount[1], jumpToMessage);
//                        }
//                    }
//                    if (queryWithDialogFinal == dialogId && messagesSearchEndReached[0] && mergeDialogId != 0 && !messagesSearchEndReached[1]) {
//                        searchMessagesInChat(lastSearchQuery, dialogId, mergeDialogId, guid, 0, true, user, jumpToMessage);
//                    }
//                }
//            }
//        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    public String getLastSearchQuery() {
        return lastSearchQuery;
    }
    //endregion ---------------- MESSAGE SEARCH END ----------------


    //region ---------------- MESSAGES ----------------
    private static Comparator<MessageEntity> entityComparator = (entity1, entity2) -> {
        if (entity1.offset > entity2.offset) {
            return 1;
        } else if (entity1.offset < entity2.offset) {
            return -1;
        }
        return 0;
    };

    public MessageObject loadPinnedMessage(final long dialogId, final int channelId, final int mid, boolean useQueue) {
        if (useQueue) {
            getMessagesStorage().getStorageQueue().postRunnable(() -> loadPinnedMessageInternal(dialogId, channelId, mid, false));
        } else {
            return loadPinnedMessageInternal(dialogId, channelId, mid, true);
        }
        return null;
    }

    private MessageObject loadPinnedMessageInternal(final long dialogId, final int channelId, final int mid, boolean returnValue) {
        try {
            long messageId;
            if (channelId != 0) {
                messageId = ((long) mid) | ((long) channelId) << 32;
            } else {
                messageId = mid;
            }

            Message result = null;
            final ArrayList<User> users = new ArrayList<>();
            final ArrayList<Chat> chats = new ArrayList<>();
            ArrayList<Integer> usersToLoad = new ArrayList<>();
            ArrayList<Integer> chatsToLoad = new ArrayList<>();

            SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid, date FROM messages WHERE mid = %d", messageId));
            if (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    result = Message.TLdeserialize(data, data.readInt32(false), false);
                    result.readAttachPath(data, getUserConfig().clientUserId);
                    data.reuse();
                    if (result.action.isHistoryClear()) {
                        result = null;
                    } else {
                        result.id = cursor.intValue(1);
                        result.date = cursor.intValue(2);
                        result.dialog_id = dialogId;
                        MessagesStorage.addUsersAndChatsFromMessage(result, usersToLoad, chatsToLoad);
                    }
                }
            }
            cursor.dispose();

            if (result == null) {
                cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data FROM chat_pinned WHERE uid = %d", dialogId));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        result = Message.TLdeserialize(data, data.readInt32(false), false);
                        result.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (result.id != mid || result.action.isHistoryClear()) {
                            result = null;
                        } else {
                            result.dialog_id = dialogId;
                            MessagesStorage.addUsersAndChatsFromMessage(result, usersToLoad, chatsToLoad);
                        }
                    }
                }
                cursor.dispose();
            }

            if (result == null) {
                //TODO 发起请求
//                if (channelId != 0) {
//                    final TL_channels_getMessages req = new TL_channels_getMessages();
//                    req.channel = getMessagesController().getInputChannel(channelId);
//                    req.id.add(mid);
//                    getConnectionsManager().sendRequest(req, (response, error) -> {
//                        boolean ok = false;
//                        if (error == null) {
//                            messages_Messages messagesRes = (messages_Messages) response;
//                            removeEmptyMessages(messagesRes.messages);
//                            if (!messagesRes.messages.isEmpty()) {
//                                ImageLoader.saveMessagesThumbs(messagesRes.messages);
//                                broadcastPinnedMessage(messagesRes.messages.get(0), messagesRes.users, messagesRes.chats, false, false);
//                                getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
//                                savePinnedMessage(messagesRes.messages.get(0));
//                                ok = true;
//                            }
//                        }
//                        if (!ok) {
//                            getMessagesStorage().updateChatPinnedMessage(channelId, 0);
//                        }
//                    });
//                } else {
//                    final TL_messages_getMessages req = new TL_messages_getMessages();
//                    req.id.add(mid);
//                    getConnectionsManager().sendRequest(req, (response, error) -> {
//                        boolean ok = false;
//                        if (error == null) {
//                            messages_Messages messagesRes = (messages_Messages) response;
//                            removeEmptyMessages(messagesRes.messages);
//                            if (!messagesRes.messages.isEmpty()) {
//                                ImageLoader.saveMessagesThumbs(messagesRes.messages);
//                                broadcastPinnedMessage(messagesRes.messages.get(0), messagesRes.users, messagesRes.chats, false, false);
//                                getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
//                                savePinnedMessage(messagesRes.messages.get(0));
//                                ok = true;
//                            }
//                        }
//                        if (!ok) {
//                            getMessagesStorage().updateChatPinnedMessage(channelId, 0);
//                        }
//                    });
//                }
            } else {
                if (returnValue) {
                    return broadcastPinnedMessage(result, users, chats, true, returnValue);
                } else {
                    if (!usersToLoad.isEmpty()) {
                        getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), users);
                    }
                    if (!chatsToLoad.isEmpty()) {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    }
                    broadcastPinnedMessage(result, users, chats, true, false);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private void savePinnedMessage(final Message result) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                long dialogId;
                if (result.to_id != 0) {
                    dialogId = -result.to_id;
                } else {
                    return;
                }
                getMessagesStorage().getDatabase().beginTransaction();
                SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO chat_pinned VALUES(?, ?, ?)");
//                NativeByteBuffer data = new NativeByteBuffer(result.getObjectSize());
//                result.serializeToStream(data);
                state.requery();
                state.bindLong(1, dialogId);
                state.bindInteger(2, result.id);
//                state.bindByteBuffer(3, data);
                state.step();
//                data.reuse();
                state.dispose();
                getMessagesStorage().getDatabase().commitTransaction();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private MessageObject broadcastPinnedMessage(final Message result, final ArrayList<User> users, final ArrayList<Chat> chats, final boolean isCache, boolean returnValue) {
        final SparseArray<User> usersDict = new SparseArray<>();
        for (int a = 0; a < users.size(); a++) {
            User user = users.get(a);
            usersDict.put(user.id, user);
        }
        final SparseArray<Chat> chatsDict = new SparseArray<>();
        for (int a = 0; a < chats.size(); a++) {
            Chat chat = chats.get(a);
            chatsDict.put(chat.id, chat);
        }
        if (returnValue) {
            return new MessageObject(currentAccount, result, usersDict, chatsDict, false);
        } else {
            AndroidUtilities.runOnUIThread(() -> {
                getMessagesController().putUsers(users, isCache);
                getMessagesController().putChats(chats, isCache);
                getNotificationCenter().postNotificationName(NotificationCenter.pinnedMessageDidLoad, new MessageObject(currentAccount, result, usersDict, chatsDict, false));
            });
        }
        return null;
    }

    private static void removeEmptyMessages(ArrayList<Message> messages) {
        for (int a = 0; a < messages.size(); a++) {
            Message message = messages.get(a);
            if (message == null || message.action.isHistoryClear()) {
                messages.remove(a);
                a--;
            }
        }
    }

    public void loadReplyMessagesForMessages(final ArrayList<MessageObject> messages, final long dialogId, boolean scheduled, Runnable callback) {
        if ((int) dialogId == 0) {
            final ArrayList<Long> replyMessages = new ArrayList<>();
            final LongSparseArray<ArrayList<MessageObject>> replyMessageRandomOwners = new LongSparseArray<>();
            for (int a = 0; a < messages.size(); a++) {
                MessageObject messageObject = messages.get(a);
                if (messageObject.isReply() && messageObject.replyMessageObject == null) {
                    long id = messageObject.messageOwner.reply_to_random_id;
                    ArrayList<MessageObject> messageObjects = replyMessageRandomOwners.get(id);
                    if (messageObjects == null) {
                        messageObjects = new ArrayList<>();
                        replyMessageRandomOwners.put(id, messageObjects);
                    }
                    messageObjects.add(messageObject);
                    if (!replyMessages.contains(id)) {
                        replyMessages.add(id);
                    }
                }
            }
            if (replyMessages.isEmpty()) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT m.data, m.mid, m.date, r.random_id FROM randoms as r INNER JOIN messages as m ON r.mid = m.mid WHERE r.random_id IN(%s)", TextUtils.join(",", replyMessages)));
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            Message message = Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            message.dialog_id = dialogId;

                            long value = cursor.longValue(3);
                            ArrayList<MessageObject> arrayList = replyMessageRandomOwners.get(value);
                            replyMessageRandomOwners.remove(value);
                            if (arrayList != null) {
                                MessageObject messageObject = new MessageObject(currentAccount, message, false);
                                for (int b = 0; b < arrayList.size(); b++) {
                                    MessageObject object = arrayList.get(b);
                                    object.replyMessageObject = messageObject;
                                    object.messageOwner.reply_to_msg_id = messageObject.getId();
                                    if (object.isMegagroup()) {
                                        object.replyMessageObject.messageOwner.flags |= MESSAGE_FLAG_MEGAGROUP;
                                    }
                                }
                            }
                        }
                    }
                    cursor.dispose();
                    if (replyMessageRandomOwners.size() != 0) {
                        for (int b = 0; b < replyMessageRandomOwners.size(); b++) {
                            ArrayList<MessageObject> arrayList = replyMessageRandomOwners.valueAt(b);
                            for (int a = 0; a < arrayList.size(); a++) {
                                arrayList.get(a).messageOwner.reply_to_random_id = 0;
                            }
                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.replyMessagesDidLoad, dialogId));
                    if (callback != null) {
                        callback.run();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        } else {
            final ArrayList<Integer> replyMessages = new ArrayList<>();
            final SparseArray<ArrayList<MessageObject>> replyMessageOwners = new SparseArray<>();
            final StringBuilder stringBuilder = new StringBuilder();
            int channelId = 0;
            for (int a = 0; a < messages.size(); a++) {
                MessageObject messageObject = messages.get(a);
                if (messageObject.getId() > 0 && messageObject.isReply() && messageObject.replyMessageObject == null) {
                    int id = messageObject.messageOwner.reply_to_msg_id;
                    long messageId = id;
                    if (messageObject.messageOwner.to_id != 0) {
                        messageId |= ((long) messageObject.messageOwner.to_id) << 32;
                        channelId = messageObject.messageOwner.to_id;
                    }
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append(messageId);
                    ArrayList<MessageObject> messageObjects = replyMessageOwners.get(id);
                    if (messageObjects == null) {
                        messageObjects = new ArrayList<>();
                        replyMessageOwners.put(id, messageObjects);
                    }
                    messageObjects.add(messageObject);
                    if (!replyMessages.contains(id)) {
                        replyMessages.add(id);
                    }
                }
            }
            if (replyMessages.isEmpty()) {
                if (callback != null) {
                    callback.run();
                }
                return;
            }

            final int channelIdFinal = channelId;
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                try {
                    final ArrayList<Message> result = new ArrayList<>();
                    final ArrayList<User> users = new ArrayList<>();
                    final ArrayList<Chat> chats = new ArrayList<>();
                    ArrayList<Integer> usersToLoad = new ArrayList<>();
                    ArrayList<Integer> chatsToLoad = new ArrayList<>();

                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid, date FROM messages WHERE mid IN(%s)", stringBuilder.toString()));
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            Message message = Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            message.dialog_id = dialogId;
                            MessagesStorage.addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad);
                            result.add(message);
                            replyMessages.remove((Integer) message.id);
                        }
                    }
                    cursor.dispose();

                    if (!usersToLoad.isEmpty()) {
                        getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), users);
                    }
                    if (!chatsToLoad.isEmpty()) {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                    }
                    broadcastReplyMessages(result, replyMessageOwners, users, chats, dialogId, true);

                    if (!replyMessages.isEmpty()) {
                        //TODO 发起请求
//                        if (channelIdFinal != 0) {
//                            final TL_channels_getMessages req = new TL_channels_getMessages();
//                            req.channel = getMessagesController().getInputChannel(channelIdFinal);
//                            req.id = replyMessages;
//                            getConnectionsManager().sendRequest(req, (response, error) -> {
//                                if (error == null) {
//                                    messages_Messages messagesRes = (messages_Messages) response;
//                                    ImageLoader.saveMessagesThumbs(messagesRes.messages);
//                                    broadcastReplyMessages(messagesRes.messages, replyMessageOwners, messagesRes.users, messagesRes.chats, dialogId, false);
//                                    getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
//                                    saveReplyMessages(replyMessageOwners, messagesRes.messages, scheduled);
//                                }
//                                if (callback != null) {
//                                    AndroidUtilities.runOnUIThread(callback);
//                                }
//                            });
//                        } else {
//                            TL_messages_getMessages req = new TL_messages_getMessages();
//                            req.id = replyMessages;
//                            getConnectionsManager().sendRequest(req, (response, error) -> {
//                                if (error == null) {
//                                    messages_Messages messagesRes = (messages_Messages) response;
//                                    ImageLoader.saveMessagesThumbs(messagesRes.messages);
//                                    broadcastReplyMessages(messagesRes.messages, replyMessageOwners, messagesRes.users, messagesRes.chats, dialogId, false);
//                                    getMessagesStorage().putUsersAndChats(messagesRes.users, messagesRes.chats, true, true);
//                                    saveReplyMessages(replyMessageOwners, messagesRes.messages, scheduled);
//                                }
//                                if (callback != null) {
//                                    AndroidUtilities.runOnUIThread(callback);
//                                }
//                            });
//                        }
                    } else {
                        if (callback != null) {
                            AndroidUtilities.runOnUIThread(callback);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
    }

    private void saveReplyMessages(final SparseArray<ArrayList<MessageObject>> replyMessageOwners, final ArrayList<Message> result, boolean scheduled) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                getMessagesStorage().getDatabase().beginTransaction();
                SQLitePreparedStatement state;
                if (scheduled) {
                    state = getMessagesStorage().getDatabase().executeFast("UPDATE scheduled_messages SET replydata = ? WHERE mid = ?");
                } else {
                    state = getMessagesStorage().getDatabase().executeFast("UPDATE messages SET replydata = ? WHERE mid = ?");
                }
                for (int a = 0; a < result.size(); a++) {
                    Message message = result.get(a);
                    ArrayList<MessageObject> messageObjects = replyMessageOwners.get(message.id);
                    if (messageObjects != null) {
//                        NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
//                        message.serializeToStream(data);
                        for (int b = 0; b < messageObjects.size(); b++) {
                            MessageObject messageObject = messageObjects.get(b);
                            state.requery();
                            long messageId = messageObject.getId();
                            if (messageObject.messageOwner.to_id != 0) {
                                messageId |= ((long) messageObject.messageOwner.to_id) << 32;
                            }
//                            state.bindByteBuffer(1, data);
                            state.bindLong(2, messageId);
                            state.step();
                        }
//                        data.reuse();
                    }
                }
                state.dispose();
                getMessagesStorage().getDatabase().commitTransaction();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void broadcastReplyMessages(final ArrayList<Message> result, final SparseArray<ArrayList<MessageObject>> replyMessageOwners, final ArrayList<User> users, final ArrayList<Chat> chats, final long dialog_id, final boolean isCache) {
        final SparseArray<User> usersDict = new SparseArray<>();
        for (int a = 0; a < users.size(); a++) {
            User user = users.get(a);
            usersDict.put(user.id, user);
        }
        final SparseArray<Chat> chatsDict = new SparseArray<>();
        for (int a = 0; a < chats.size(); a++) {
            Chat chat = chats.get(a);
            chatsDict.put(chat.id, chat);
        }
        AndroidUtilities.runOnUIThread(() -> {
            getMessagesController().putUsers(users, isCache);
            getMessagesController().putChats(chats, isCache);
            boolean changed = false;
            for (int a = 0; a < result.size(); a++) {
                Message message = result.get(a);
                ArrayList<MessageObject> arrayList = replyMessageOwners.get(message.id);
                if (arrayList != null) {
                    MessageObject messageObject = new MessageObject(currentAccount, message, usersDict, chatsDict, false);
                    for (int b = 0; b < arrayList.size(); b++) {
                        MessageObject m = arrayList.get(b);
                        m.replyMessageObject = messageObject;
                        if (m.messageOwner.action.isPinMessage()) {
                            m.generatePinMessageText(null, null);
                        }
                        if (m.isMegagroup()) {
                            m.replyMessageObject.messageOwner.flags |= MESSAGE_FLAG_MEGAGROUP;
                        }
                    }
                    changed = true;
                }
            }
            if (changed) {
                getNotificationCenter().postNotificationName(NotificationCenter.replyMessagesDidLoad, dialog_id);
            }
        });
    }

    public static void sortEntities(ArrayList<MessageEntity> entities) {
        Collections.sort(entities, entityComparator);
    }

    private static boolean checkInclusion(int index, ArrayList<MessageEntity> entities, boolean end) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        int count = entities.size();
        for (int a = 0; a < count; a++) {
            MessageEntity entity = entities.get(a);
            if ((end ? entity.offset < index : entity.offset <= index) && entity.offset + entity.length > index) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkIntersection(int start, int end, ArrayList<MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        int count = entities.size();
        for (int a = 0; a < count; a++) {
            MessageEntity entity = entities.get(a);
            if (entity.offset > start && entity.offset + entity.length <= end) {
                return true;
            }
        }
        return false;
    }

    private static void removeOffsetAfter(int start, int countToRemove, ArrayList<MessageEntity> entities) {
        int count = entities.size();
        for (int a = 0; a < count; a++) {
            MessageEntity entity = entities.get(a);
            if (entity.offset > start) {
                entity.offset -= countToRemove;
            }
        }
    }

    public CharSequence substring(CharSequence source, int start, int end) {
        if (source instanceof SpannableStringBuilder) {
            return source.subSequence(start, end);
        } else if (source instanceof SpannedString) {
            return source.subSequence(start, end);
        } else {
            return TextUtils.substring(source, start, end);
        }
    }

    private static CharacterStyle createNewSpan(CharacterStyle baseSpan, TextStyleSpan.TextStyleRun textStyleRun, TextStyleSpan.TextStyleRun newStyleRun, boolean allowIntersection) {
        TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun(textStyleRun);
        if (newStyleRun != null) {
            if (allowIntersection) {
                run.merge(newStyleRun);
            } else {
                run.replace(newStyleRun);
            }
        }
        if (baseSpan instanceof TextStyleSpan) {
            return new TextStyleSpan(run);
        } else if (baseSpan instanceof URLSpanReplacement) {
            URLSpanReplacement span = (URLSpanReplacement) baseSpan;
            return new URLSpanReplacement(span.getURL(), run);
        }
        return null;
    }

    public static void addStyleToText(TextStyleSpan span, int start, int end, Spannable editable, boolean allowIntersection) {
        try {
            CharacterStyle[] spans = editable.getSpans(start, end, CharacterStyle.class);
            if (spans != null && spans.length > 0) {
                for (int a = 0; a < spans.length; a++) {
                    CharacterStyle oldSpan = spans[a];
                    TextStyleSpan.TextStyleRun textStyleRun;
                    TextStyleSpan.TextStyleRun newStyleRun = span != null ? span.getTextStyleRun() : new TextStyleSpan.TextStyleRun();
                    if (oldSpan instanceof TextStyleSpan) {
                        TextStyleSpan textStyleSpan = (TextStyleSpan) oldSpan;
                        textStyleRun = textStyleSpan.getTextStyleRun();
                    } else if (oldSpan instanceof URLSpanReplacement) {
                        URLSpanReplacement urlSpanReplacement = (URLSpanReplacement) oldSpan;
                        textStyleRun = urlSpanReplacement.getTextStyleRun();
                        if (textStyleRun == null) {
                            textStyleRun = new TextStyleSpan.TextStyleRun();
                        }
                    } else {
                        continue;
                    }
                    if (textStyleRun == null) {
                        continue;
                    }
                    int spanStart = editable.getSpanStart(oldSpan);
                    int spanEnd = editable.getSpanEnd(oldSpan);
                    editable.removeSpan(oldSpan);
                    if (spanStart > start && end > spanEnd) {
                        editable.setSpan(createNewSpan(oldSpan, textStyleRun, newStyleRun, allowIntersection), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (span != null) {
                            editable.setSpan(new TextStyleSpan(new TextStyleSpan.TextStyleRun(newStyleRun)), spanEnd, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        end = spanStart;
                    } else {
                        int startTemp = start;
                        if (spanStart <= start) {
                            if (spanStart != start) {
                                editable.setSpan(createNewSpan(oldSpan, textStyleRun, null, allowIntersection), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (spanEnd > start) {
                                if (span != null) {
                                    editable.setSpan(createNewSpan(oldSpan, textStyleRun, newStyleRun, allowIntersection), start, Math.min(spanEnd, end), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                start = spanEnd;
                            }
                        }
                        if (spanEnd >= end) {
                            if (spanEnd != end) {
                                editable.setSpan(createNewSpan(oldSpan, textStyleRun, null, allowIntersection), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (end > spanStart && spanEnd <= startTemp) {
                                if (span != null) {
                                    editable.setSpan(createNewSpan(oldSpan, textStyleRun, newStyleRun, allowIntersection), spanStart, Math.min(spanEnd, end), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                end = spanStart;
                            }
                        }
                    }
                }
            }
            if (span != null && start < end) {
                editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static ArrayList<TextStyleSpan.TextStyleRun> getTextStyleRuns(ArrayList<MessageEntity> entities, CharSequence text) {
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
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else if (entity.isMentionName()) {
                newRun.flags = TextStyleSpan.FLAG_STYLE_MENTION;
                newRun.urlEntity = entity;
            } else {
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
        return runs;
    }

    public ArrayList<MessageEntity> getEntities(CharSequence[] message, boolean allowStrike) {
        if (message == null || message[0] == null) {
            return null;
        }
        ArrayList<MessageEntity> entities = null;
        int index;
        int start = -1;
        int lastIndex = 0;
        boolean isPre = false;
        final String mono = "`";
        final String pre = "```";
        final String bold = "**";
        final String italic = "__";
        final String strike = "~~";
        while ((index = TextUtils.indexOf(message[0], !isPre ? mono : pre, lastIndex)) != -1) {
            if (start == -1) {
                isPre = message[0].length() - index > 2 && message[0].charAt(index + 1) == '`' && message[0].charAt(index + 2) == '`';
                start = index;
                lastIndex = index + (isPre ? 3 : 1);
            } else {
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                for (int a = index + (isPre ? 3 : 1); a < message[0].length(); a++) {
                    if (message[0].charAt(a) == '`') {
                        index++;
                    } else {
                        break;
                    }
                }
                lastIndex = index + (isPre ? 3 : 1);
                if (isPre) {
                    int firstChar = start > 0 ? message[0].charAt(start - 1) : 0;
                    boolean replacedFirst = firstChar == ' ' || firstChar == '\n';
                    CharSequence startMessage = substring(message[0], 0, start - (replacedFirst ? 1 : 0));
                    CharSequence content = substring(message[0], start + 3, index);
                    firstChar = index + 3 < message[0].length() ? message[0].charAt(index + 3) : 0;
                    CharSequence endMessage = substring(message[0], index + 3 + (firstChar == ' ' || firstChar == '\n' ? 1 : 0), message[0].length());
                    if (startMessage.length() != 0) {
                        startMessage = AndroidUtilities.concat(startMessage, "\n");
                    } else {
                        replacedFirst = true;
                    }
                    if (endMessage.length() != 0) {
                        endMessage = AndroidUtilities.concat("\n", endMessage);
                    }
                    if (!TextUtils.isEmpty(content)) {
                        message[0] = AndroidUtilities.concat(startMessage, content, endMessage);
                        MessageEntity entity = new MessageEntity();
                        entity.setPre(true);
                        entity.offset = start + (replacedFirst ? 0 : 1);
                        entity.length = index - start - 3 + (replacedFirst ? 0 : 1);
                        entity.language = "";
                        entities.add(entity);
                        lastIndex -= 6;
                    }
                } else {
                    if (start + 1 != index) {
                        message[0] = AndroidUtilities.concat(substring(message[0], 0, start), substring(message[0], start + 1, index), substring(message[0], index + 1, message[0].length()));
                        MessageEntity entity = new MessageEntity();
                        entity.setCode(true);
                        entity.offset = start;
                        entity.length = index - start - 1;
                        entities.add(entity);
                        lastIndex -= 2;
                    }
                }
                start = -1;
                isPre = false;
            }
        }
        if (start != -1 && isPre) {
            message[0] = AndroidUtilities.concat(substring(message[0], 0, start), substring(message[0], start + 2, message[0].length()));
            if (entities == null) {
                entities = new ArrayList<>();
            }
            MessageEntity entity = new MessageEntity();
            entity.setCode(true);
            entity.offset = start;
            entity.length = 1;
            entities.add(entity);
        }

        if (message[0] instanceof Spanned) {
            Spanned spannable = (Spanned) message[0];
            TextStyleSpan[] spans = spannable.getSpans(0, message[0].length(), TextStyleSpan.class);
            if (spans != null && spans.length > 0) {
                for (int a = 0; a < spans.length; a++) {
                    TextStyleSpan span = spans[a];
                    int spanStart = spannable.getSpanStart(span);
                    int spanEnd = spannable.getSpanEnd(span);
                    if (checkInclusion(spanStart, entities, false) || checkInclusion(spanEnd, entities, true) || checkIntersection(spanStart, spanEnd, entities)) {
                        continue;
                    }
                    if (entities == null) {
                        entities = new ArrayList<>();
                    }
                    int flags = span.getStyleFlags();
                    if ((flags & TextStyleSpan.FLAG_STYLE_BOLD) != 0) {
                        MessageEntity entity = new MessageEntity();
                        entity.setBold(true);
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_ITALIC) != 0) {
                        MessageEntity entity = new MessageEntity();
                        entity.setItalic(true);
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_MONO) != 0) {
                        MessageEntity entity = new MessageEntity();
                        entity.setCode(true);
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_STRIKE) != 0) {
                        MessageEntity entity = new MessageEntity();
                        entity.setStrike(true);
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_UNDERLINE) != 0) {
                        MessageEntity entity = new MessageEntity();
                        entity.setUnderline(true);
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                    if ((flags & TextStyleSpan.FLAG_STYLE_QUOTE) != 0) {
                        MessageEntity entity = new MessageEntity();
                        entity.setBlockquote(true);
                        entity.offset = spanStart;
                        entity.length = spanEnd - spanStart;
                        entities.add(entity);
                    }
                }
            }

            URLSpanUserMention[] spansMentions = spannable.getSpans(0, message[0].length(), URLSpanUserMention.class);
            if (spansMentions != null && spansMentions.length > 0) {
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                for (int b = 0; b < spansMentions.length; b++) {
                    MessageEntity entity = new MessageEntity();
                    entity.setMentionName(true);
                    entity.user_id = 0;//getMessagesController().getInputUser(Utilities.parseInt(spansMentions[b].getURL()));
                    if (entity.user_id != 0) {
                        entity.offset = spannable.getSpanStart(spansMentions[b]);
                        entity.length = Math.min(spannable.getSpanEnd(spansMentions[b]), message[0].length()) - entity.offset;
                        if (message[0].charAt(entity.offset + entity.length - 1) == ' ') {
                            entity.length--;
                        }
                        entities.add(entity);
                    }
                }
            }

            URLSpanReplacement[] spansUrlReplacement = spannable.getSpans(0, message[0].length(), URLSpanReplacement.class);
            if (spansUrlReplacement != null && spansUrlReplacement.length > 0) {
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                for (int b = 0; b < spansUrlReplacement.length; b++) {
                    MessageEntity entity = new MessageEntity();
                    entity.setTextUrl(true);
                    entity.offset = spannable.getSpanStart(spansUrlReplacement[b]);
                    entity.length = Math.min(spannable.getSpanEnd(spansUrlReplacement[b]), message[0].length()) - entity.offset;
                    entity.url = spansUrlReplacement[b].getURL();
                    entities.add(entity);
                }
            }
        }

        int count = allowStrike ? 3 : 2;
        for (int c = 0; c < count; c++) {
            lastIndex = 0;
            start = -1;
            String checkString;
            char checkChar;
            switch (c) {
                case 0:
                    checkString = bold;
                    checkChar = '*';
                    break;
                case 1:
                    checkString = italic;
                    checkChar = '_';
                    break;
                case 2:
                default:
                    checkString = strike;
                    checkChar = '~';
                    break;
            }
            while ((index = TextUtils.indexOf(message[0], checkString, lastIndex)) != -1) {
                if (start == -1) {
                    char prevChar = index == 0 ? ' ' : message[0].charAt(index - 1);
                    if (!checkInclusion(index, entities, false) && (prevChar == ' ' || prevChar == '\n')) {
                        start = index;
                    }
                    lastIndex = index + 2;
                } else {
                    for (int a = index + 2; a < message[0].length(); a++) {
                        if (message[0].charAt(a) == checkChar) {
                            index++;
                        } else {
                            break;
                        }
                    }
                    lastIndex = index + 2;
                    if (checkInclusion(index, entities, false) || checkIntersection(start, index, entities)) {
                        start = -1;
                        continue;
                    }
                    if (start + 2 != index) {
                        if (entities == null) {
                            entities = new ArrayList<>();
                        }
                        try {
                            message[0] = AndroidUtilities.concat(substring(message[0], 0, start), substring(message[0], start + 2, index), substring(message[0], index + 2, message[0].length()));
                        } catch (Exception e) {
                            message[0] = substring(message[0], 0, start).toString() + substring(message[0], start + 2, index).toString() + substring(message[0], index + 2, message[0].length()).toString();
                        }

                        MessageEntity entity = new MessageEntity();
                        if (c == 0) {
                            entity.setBold(true);
                        } else if (c == 1) {
                            entity.setItalic(true);
                        } else {
                            entity.setStrike(true);
                        }
                        entity.offset = start;
                        entity.length = index - start - 2;
                        removeOffsetAfter(entity.offset + entity.length, 4, entities);
                        entities.add(entity);
                        lastIndex -= 4;
                    }
                    start = -1;
                }
            }
        }

        return entities;
    }

    //endregion ---------------- MESSAGES END ----------------


    //region ---------------- DRAFT ----------------
    private LongSparseArray<Integer> draftsFolderIds = new LongSparseArray<>();
    private LongSparseArray<DraftMessage> drafts = new LongSparseArray<>();
    private LongSparseArray<Message> draftMessages = new LongSparseArray<>();
    private boolean inTransaction;
    private SharedPreferences preferences;
    private boolean loadingDrafts;

    public void loadDraftsIfNeed() {
        if (getUserConfig().draftsLoaded || loadingDrafts) {
            return;
        }
        loadingDrafts = true;
        //TODO 发起请求
//        getConnectionsManager().sendRequest(new TL_messages_getAllDrafts(), (response, error) -> {
//            if (error != null) {
//                AndroidUtilities.runOnUIThread(() -> loadingDrafts = false);
//            } else {
//                getMessagesController().processUpdates((Updates) response, false);
//                AndroidUtilities.runOnUIThread(() -> {
//                    loadingDrafts = false;
//                    final UserConfig userConfig = getUserConfig();
//                    userConfig.draftsLoaded = true;
//                    userConfig.saveConfig(false);
//                });
//            }
//        });
    }

    public int getDraftFolderId(long did) {
        return draftsFolderIds.get(did, 0);
    }

    public void setDraftFolderId(long did, int folderId) {
        draftsFolderIds.put(did, folderId);
    }

    public void clearDraftsFolderIds() {
        draftsFolderIds.clear();
    }

    public LongSparseArray<DraftMessage> getDrafts() {
        return drafts;
    }

    public DraftMessage getDraft(long did) {
        return drafts.get(did);
    }

    public Message getDraftMessage(long did) {
        return draftMessages.get(did);
    }

    public void saveDraft(long did, CharSequence message, ArrayList<MessageEntity> entities, Message replyToMessage, boolean noWebpage) {
        saveDraft(did, message, entities, replyToMessage, noWebpage, false);
    }

    public void saveDraft(long did, CharSequence message, ArrayList<MessageEntity> entities, Message replyToMessage, boolean noWebpage, boolean clean) {
        DraftMessage draftMessage = new DraftMessage();
        draftMessage.date = (int) (System.currentTimeMillis() / 1000);
        draftMessage.message = message == null ? "" : message.toString();
        draftMessage.no_webpage = noWebpage;
        if (replyToMessage != null) {
            draftMessage.reply_to_msg_id = replyToMessage.id;
            draftMessage.flags |= 1;
        }
        if (entities != null && !entities.isEmpty()) {
            draftMessage.entities = entities;
            draftMessage.flags |= 8;
        }

        DraftMessage currentDraft = drafts.get(did);
        if (!clean) {
            if (currentDraft != null && currentDraft.message.equals(draftMessage.message) && currentDraft.reply_to_msg_id == draftMessage.reply_to_msg_id && currentDraft.no_webpage == draftMessage.no_webpage ||
                    currentDraft == null && TextUtils.isEmpty(draftMessage.message) && draftMessage.reply_to_msg_id == 0) {
                return;
            }
        }

        saveDraft(did, draftMessage, replyToMessage, false);
        int lower_id = (int) did;
        if (lower_id != 0) {
            //TODO 发起请求
//            TL_messages_saveDraft req = new TL_messages_saveDraft();
//            req.peer = getMessagesController().getInputPeer(lower_id);
//            if (req.peer == null) {
//                return;
//            }
//            req.message = draftMessage.message;
//            req.no_webpage = draftMessage.no_webpage;
//            req.reply_to_msg_id = draftMessage.reply_to_msg_id;
//            req.entities = draftMessage.entities;
//            req.flags = draftMessage.flags;
//            getConnectionsManager().sendRequest(req, (response, error) -> {
//
//            });
        }
//        getMessagesController().sortDialogs(null);
        getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
    }

    public void saveDraft(final long did, DraftMessage draft, Message replyToMessage, boolean fromServer) {
        SharedPreferences.Editor editor = preferences.edit();
        final MessagesController messagesController = getMessagesController();
        if (draft == null || draft ==null) {
            drafts.remove(did);
            draftMessages.remove(did);
            preferences.edit().remove("" + did).remove("r_" + did).commit();
//            messagesController.removeDraftDialogIfNeed(did);
        } else {
            drafts.put(did, draft);
//            messagesController.putDraftDialogIfNeed(did, draft);
            try {
//                SerializedData serializedData = new SerializedData(draft.getObjectSize());
//                draft.serializeToStream(serializedData);
//                editor.putString("" + did, Utilities.bytesToHex(serializedData.toByteArray()));
//                serializedData.cleanup();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (replyToMessage == null) {
            draftMessages.remove(did);
            editor.remove("r_" + did);
        } else {
            draftMessages.put(did, replyToMessage);
//            SerializedData serializedData = new SerializedData(replyToMessage.getObjectSize());
//            replyToMessage.serializeToStream(serializedData);
//            editor.putString("r_" + did, Utilities.bytesToHex(serializedData.toByteArray()));
//            serializedData.cleanup();
        }
        editor.commit();
        if (fromServer) {
            if (draft.reply_to_msg_id != 0 && replyToMessage == null) {
                int lower_id = (int) did;
                User user = null;
                Chat chat = null;
                if (lower_id > 0) {
                    user = getMessagesController().getUser(lower_id);
                } else {
                    chat = getMessagesController().getChat(-lower_id);
                }
                if (user != null || chat != null) {
                    long messageId = draft.reply_to_msg_id;
                    final int channelIdFinal;
                    if (ChatObject.isChannel(chat)) {
                        messageId |= ((long) chat.id) << 32;
                        channelIdFinal = chat.id;
                    } else {
                        channelIdFinal = 0;
                    }
                    final long messageIdFinal = messageId;

                    getMessagesStorage().getStorageQueue().postRunnable(() -> {
                        try {
                            Message message = null;
                            SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data FROM messages WHERE mid = %d", messageIdFinal));
                            if (cursor.next()) {
                                NativeByteBuffer data = cursor.byteBufferValue(0);
                                if (data != null) {
                                    message = Message.TLdeserialize(data, data.readInt32(false), false);
                                    message.readAttachPath(data, getUserConfig().clientUserId);
                                    data.reuse();
                                }
                            }
                            cursor.dispose();
                            if (message == null) {
                                //TODO 发起请求
//                                if (channelIdFinal != 0) {
//                                    final TL_channels_getMessages req = new TL_channels_getMessages();
//                                    req.channel = getMessagesController().getInputChannel(channelIdFinal);
//                                    req.id.add((int) messageIdFinal);
//                                    getConnectionsManager().sendRequest(req, (response, error) -> {
//                                        if (error == null) {
//                                            messages_Messages messagesRes = (messages_Messages) response;
//                                            if (!messagesRes.messages.isEmpty()) {
//                                                saveDraftReplyMessage(did, messagesRes.messages.get(0));
//                                            }
//                                        }
//                                    });
//                                } else {
//                                    TL_messages_getMessages req = new TL_messages_getMessages();
//                                    req.id.add((int) messageIdFinal);
//                                    getConnectionsManager().sendRequest(req, (response, error) -> {
//                                        if (error == null) {
//                                            messages_Messages messagesRes = (messages_Messages) response;
//                                            if (!messagesRes.messages.isEmpty()) {
//                                                saveDraftReplyMessage(did, messagesRes.messages.get(0));
//                                            }
//                                        }
//                                    });
//                                }
                            } else {
                                saveDraftReplyMessage(did, message);
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });
                }
            }
            getNotificationCenter().postNotificationName(NotificationCenter.newDraftReceived, did);
        }
    }

    private void saveDraftReplyMessage(final long did, final Message message) {
        if (message == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            DraftMessage draftMessage = drafts.get(did);
            if (draftMessage != null && draftMessage.reply_to_msg_id == message.id) {
                draftMessages.put(did, message);
//                SerializedData serializedData = new SerializedData(message.getObjectSize());
//                message.serializeToStream(serializedData);
//                preferences.edit().putString("r_" + did, Utilities.bytesToHex(serializedData.toByteArray())).commit();
                getNotificationCenter().postNotificationName(NotificationCenter.newDraftReceived, did);
//                serializedData.cleanup();
            }
        });
    }

    public void clearAllDrafts(boolean notify) {
        drafts.clear();
        draftMessages.clear();
        draftsFolderIds.clear();
        preferences.edit().clear().commit();
        if (notify) {
//            getMessagesController().sortDialogs(null);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        }
    }

    public void cleanDraft(long did, boolean replyOnly) {
        DraftMessage draftMessage = drafts.get(did);
        if (draftMessage == null) {
            return;
        }
        if (!replyOnly) {
            drafts.remove(did);
            draftMessages.remove(did);
            preferences.edit().remove("" + did).remove("r_" + did).commit();
//            getMessagesController().sortDialogs(null);
            getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        } else if (draftMessage.reply_to_msg_id != 0) {
            draftMessage.reply_to_msg_id = 0;
            draftMessage.flags &= ~1;
            saveDraft(did, draftMessage.message, draftMessage.entities, null, draftMessage.no_webpage, true);
        }
    }

    public void beginTransaction() {
        inTransaction = true;
    }

    public void endTransaction() {
        inTransaction = false;
    }

    //endregion ---------------- DRAFT END ----------------


    //region ---------------- EMOJI START ----------------

    public static class KeywordResult {
        public String emoji;
        public String keyword;
    }

    public interface KeywordResultCallback {
        void run(ArrayList<KeywordResult> param, String alias);
    }

    private HashMap<String, Boolean> currentFetchingEmoji = new HashMap<>();

    public void fetchNewEmojiKeywords(String[] langCodes) {
        if (langCodes == null) {
            return;
        }
        for (int a = 0; a < langCodes.length; a++) {
            String langCode = langCodes[a];
            if (TextUtils.isEmpty(langCode)) {
                return;
            }
            if (currentFetchingEmoji.get(langCode) != null) {
                return;
            }
            currentFetchingEmoji.put(langCode, true);
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                int version = -1;
                String alias = null;
                long date = 0;
                try {
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT alias, version, date FROM emoji_keywords_info_v2 WHERE lang = ?", langCode);
                    if (cursor.next()) {
                        alias = cursor.stringValue(0);
                        version = cursor.intValue(1);
                        date = cursor.longValue(2);
                    }
                    cursor.dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (!BuildVars.DEBUG_VERSION && Math.abs(System.currentTimeMillis() - date) < 60 * 60 * 1000) {
                    AndroidUtilities.runOnUIThread(() -> currentFetchingEmoji.remove(langCode));
                    return;
                }
                //TODO 发起请求
//                TLObject request;
//                if (version == -1) {
//                    TL_messages_getEmojiKeywords req = new TL_messages_getEmojiKeywords();
//                    req.lang_code = langCode;
//                    request = req;
//                } else {
//                    TL_messages_getEmojiKeywordsDifference req = new TL_messages_getEmojiKeywordsDifference();
//                    req.lang_code = langCode;
//                    req.from_version = version;
//                    request = req;
//                }
//                String aliasFinal = alias;
//                int versionFinal = version;
//                getConnectionsManager().sendRequest(request, (response, error) -> {
//                    if (response != null) {
//                        TL_emojiKeywordsDifference res = (TL_emojiKeywordsDifference) response;
//                        if (versionFinal != -1 && !res.lang_code.equals(aliasFinal)) {
//                            getMessagesStorage().getStorageQueue().postRunnable(() -> {
//                                try {
//                                    SQLitePreparedStatement deleteState = getMessagesStorage().getDatabase().executeFast("DELETE FROM emoji_keywords_info_v2 WHERE lang = ?");
//                                    deleteState.bindString(1, langCode);
//                                    deleteState.step();
//                                    deleteState.dispose();
//
//                                    AndroidUtilities.runOnUIThread(() -> {
//                                        currentFetchingEmoji.remove(langCode);
//                                        fetchNewEmojiKeywords(new String[]{langCode});
//                                    });
//                                } catch (Exception e) {
//                                    FileLog.e(e);
//                                }
//                            });
//                        } else {
//                            putEmojiKeywords(langCode, res);
//                        }
//                    } else {
//                        AndroidUtilities.runOnUIThread(() -> currentFetchingEmoji.remove(langCode));
//                    }
//                });
            });
        }
    }

//    private void putEmojiKeywords(String lang, TL_emojiKeywordsDifference res) {
//        if (res == null) {
//            return;
//        }
//        getMessagesStorage().getStorageQueue().postRunnable(() -> {
//            try {
//                if (!res.keywords.isEmpty()) {
//                    SQLitePreparedStatement insertState = getMessagesStorage().getDatabase().executeFast("REPLACE INTO emoji_keywords_v2 VALUES(?, ?, ?)");
//                    SQLitePreparedStatement deleteState = getMessagesStorage().getDatabase().executeFast("DELETE FROM emoji_keywords_v2 WHERE lang = ? AND keyword = ? AND emoji = ?");
//                    getMessagesStorage().getDatabase().beginTransaction();
//                    for (int a = 0, N = res.keywords.size(); a < N; a++) {
//                        EmojiKeyword keyword = res.keywords.get(a);
//                        if (keyword instanceof TL_emojiKeyword) {
//                            TL_emojiKeyword emojiKeyword = (TL_emojiKeyword) keyword;
//                            String key = emojiKeyword.keyword.toLowerCase();
//                            for (int b = 0, N2 = emojiKeyword.emoticons.size(); b < N2; b++) {
//                                insertState.requery();
//                                insertState.bindString(1, res.lang_code);
//                                insertState.bindString(2, key);
//                                insertState.bindString(3, emojiKeyword.emoticons.get(b));
//                                insertState.step();
//                            }
//                        } else if (keyword instanceof TL_emojiKeywordDeleted) {
//                            TL_emojiKeywordDeleted keywordDeleted = (TL_emojiKeywordDeleted) keyword;
//                            String key = keywordDeleted.keyword.toLowerCase();
//                            for (int b = 0, N2 = keywordDeleted.emoticons.size(); b < N2; b++) {
//                                deleteState.requery();
//                                deleteState.bindString(1, res.lang_code);
//                                deleteState.bindString(2, key);
//                                deleteState.bindString(3, keywordDeleted.emoticons.get(b));
//                                deleteState.step();
//                            }
//                        }
//                    }
//                    getMessagesStorage().getDatabase().commitTransaction();
//                    insertState.dispose();
//                    deleteState.dispose();
//                }
//
//                SQLitePreparedStatement infoState = getMessagesStorage().getDatabase().executeFast("REPLACE INTO emoji_keywords_info_v2 VALUES(?, ?, ?, ?)");
//                infoState.bindString(1, lang);
//                infoState.bindString(2, res.lang_code);
//                infoState.bindInteger(3, res.version);
//                infoState.bindLong(4, System.currentTimeMillis());
//                infoState.step();
//                infoState.dispose();
//
//                AndroidUtilities.runOnUIThread(() -> {
//                    currentFetchingEmoji.remove(lang);
//                    getNotificationCenter().postNotificationName(NotificationCenter.newEmojiSuggestionsAvailable, lang);
//                });
//            } catch (Exception e) {
//                FileLog.e(e);
//            }
//        });
//    }

    public void getEmojiSuggestions(String[] langCodes, String keyword, boolean fullMatch, KeywordResultCallback callback) {
        getEmojiSuggestions(langCodes, keyword, fullMatch, callback, null);
    }

    public void getEmojiSuggestions(String[] langCodes, String keyword, boolean fullMatch, KeywordResultCallback callback, CountDownLatch sync) {
        if (callback == null) {
            return;
        }
        if (TextUtils.isEmpty(keyword) || langCodes == null) {
            callback.run(new ArrayList<>(), null);
            return;
        }
        ArrayList<String> recentEmoji = new ArrayList<>(Emoji.recentEmoji);
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            ArrayList<KeywordResult> result = new ArrayList<>();
            HashMap<String, Boolean> resultMap = new HashMap<>();
            String alias = null;
            try {
                SQLiteCursor cursor;
                boolean hasAny = false;
                for (int a = 0; a < langCodes.length; a++) {
                    cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT alias FROM emoji_keywords_info_v2 WHERE lang = ?", langCodes[a]);
                    if (cursor.next()) {
                        alias = cursor.stringValue(0);
                    }
                    cursor.dispose();
                    if (alias != null) {
                        hasAny = true;
                    }
                }
                if (!hasAny) {
                    AndroidUtilities.runOnUIThread(() -> {
                        for (int a = 0; a < langCodes.length; a++) {
                            if (currentFetchingEmoji.get(langCodes[a]) != null) {
                                return;
                            }
                        }
                        callback.run(result, null);
                    });
                    return;
                }

                String key = keyword.toLowerCase();
                for (int a = 0; a < 2; a++) {
                    if (a == 1) {
                        String translitKey = LocaleController.getInstance().getTranslitString(key, false, false);
                        if (translitKey.equals(key)) {
                            continue;
                        }
                        key = translitKey;
                    }
                    String key2 = null;
                    StringBuilder nextKey = new StringBuilder(key);
                    int pos = nextKey.length();
                    while (pos > 0) {
                        pos--;
                        char value = nextKey.charAt(pos);
                        value++;
                        nextKey.setCharAt(pos, value);
                        if (value != 0) {
                            key2 = nextKey.toString();
                            break;
                        }
                    }

                    if (fullMatch) {
                        cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT emoji, keyword FROM emoji_keywords_v2 WHERE keyword = ?", key);
                    } else if (key2 != null) {
                        cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT emoji, keyword FROM emoji_keywords_v2 WHERE keyword >= ? AND keyword < ?", key, key2);
                    } else {
                        key += "%";
                        cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT emoji, keyword FROM emoji_keywords_v2 WHERE keyword LIKE ?", key);
                    }
                    while (cursor.next()) {
                        String value = cursor.stringValue(0).replace("\ufe0f", "");
                        if (resultMap.get(value) != null) {
                            continue;
                        }
                        resultMap.put(value, true);
                        KeywordResult keywordResult = new KeywordResult();
                        keywordResult.emoji = value;
                        keywordResult.keyword = cursor.stringValue(1);
                        result.add(keywordResult);
                    }
                    cursor.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            Collections.sort(result, (o1, o2) -> {
                int idx1 = recentEmoji.indexOf(o1.emoji);
                if (idx1 < 0) {
                    idx1 = Integer.MAX_VALUE;
                }
                int idx2 = recentEmoji.indexOf(o2.emoji);
                if (idx2 < 0) {
                    idx2 = Integer.MAX_VALUE;
                }
                if (idx1 < idx2) {
                    return -1;
                } else if (idx1 > idx2) {
                    return 1;
                } else {
                    int len1 = o1.keyword.length();
                    int len2 = o2.keyword.length();

                    if (len1 < len2) {
                        return -1;
                    } else if (len1 > len2) {
                        return 1;
                    }
                    return 0;
                }
            });
            String aliasFinal = alias;
            if (sync != null) {
                callback.run(result, aliasFinal);
                sync.countDown();
            } else {
                AndroidUtilities.runOnUIThread(() -> callback.run(result, aliasFinal));
            }
        });
        if (sync != null) {
            try {
                sync.await();
            } catch (Throwable ignore) {

            }
        }
    }

    //endregion ---------------- EMOJI END ----------------

    //region ---------------- MEDIA ----------------
    public final static int MEDIA_PHOTOVIDEO = 0;
    public final static int MEDIA_FILE = 1;
    public final static int MEDIA_AUDIO = 2;
    public final static int MEDIA_URL = 3;
    public final static int MEDIA_MUSIC = 4;
    public final static int MEDIA_GIF = 5;
    public final static int MEDIA_TYPES_COUNT = 6;

    public void loadMedia(long uid, int count, int max_id, int type, int fromCache, int classGuid) {
        final boolean isChannel = (int) uid < 0 && ChatObject.isChannel(-(int) uid, currentAccount);

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("load media did " + uid + " count = " + count + " max_id " + max_id + " type = " + type + " cache = " + fromCache + " classGuid = " + classGuid);
        }
        int lower_part = (int)uid;
        fromCache = 0;
        if (fromCache != 0 || lower_part == 0) {
            loadMediaDatabase(uid, count, max_id, type, classGuid, isChannel, fromCache);
        } else {
            //TODO 发起请求
//            TL_messages_search req = new TL_messages_search();
//            req.limit = count;
//            req.offset_id = max_id;
//            if (type == MEDIA_PHOTOVIDEO) {
//                req.filter = new TL_inputMessagesFilterPhotoVideo();
//            } else if (type == MEDIA_FILE) {
//                req.filter = new TL_inputMessagesFilterDocument();
//            } else if (type == MEDIA_AUDIO) {
//                req.filter = new TL_inputMessagesFilterRoundVoice();
//            } else if (type == MEDIA_URL) {
//                req.filter = new TL_inputMessagesFilterUrl();
//            } else if (type == MEDIA_MUSIC) {
//                req.filter = new TL_inputMessagesFilterMusic();
//            } else if (type == MEDIA_GIF) {
//                req.filter = new TL_inputMessagesFilterGif();
//            }
//            req.q = "";
//            req.peer = getMessagesController().getInputPeer(lower_part);
//            if (req.peer == null) {
//                return;
//            }
//            int reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
//                if (error == null) {
//                    final messages_Messages res = (messages_Messages) response;
//                    getMessagesController().removeDeletedMessagesFromArray(uid, res.messages);
//                    processLoadedMedia(res, uid, count, max_id, type, 0, classGuid, isChannel, res.messages.size() == 0);
//                }
//            });
//            getConnectionsManager().bindRequestToGuid(reqId, classGuid);
        }
    }

    public void getMediaCounts(final long uid, final int classGuid) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                int[] counts = new int[] {-1, -1, -1, -1, -1, -1};
                int[] countsFinal = new int[] {-1, -1, -1, -1, -1, -1};
                int[] old = new int[] {0, 0, 0, 0, 0, 0};
                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT type, count, old FROM media_counts_v2 WHERE uid = %d", uid));
                while (cursor.next()) {
                    int type = cursor.intValue(0);
                    if (type >= 0 && type < MEDIA_TYPES_COUNT) {
                        countsFinal[type] = counts[type] = cursor.intValue(1);
                        old[type] = cursor.intValue(2);
                    }
                }
                cursor.dispose();
                int lower_part = (int) uid;
                if (lower_part == 0) {
                    for (int a = 0; a < counts.length; a++) {
                        if (counts[a] == -1) {
                            cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM media_v2 WHERE uid = %d AND type = %d LIMIT 1", uid, a));
                            if (cursor.next()) {
                                counts[a] = cursor.intValue(0);
                            } else {
                                counts[a] = 0;
                            }
                            cursor.dispose();
                            putMediaCountDatabase(uid, a, counts[a]);
                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.mediaCountsDidLoad, uid, counts));
                } else {
                    boolean missing = false;
                    for (int a = 0; a < counts.length; a++) {
                        if (counts[a] == -1 || old[a] == 1) {
                            final int type = a;

                            //TODO 发起请求
//                            TL_messages_search req = new TL_messages_search();
//                            req.limit = 1;
//                            req.offset_id = 0;
//                            if (a == MEDIA_PHOTOVIDEO) {
//                                req.filter = new TL_inputMessagesFilterPhotoVideo();
//                            } else if (a == MEDIA_FILE) {
//                                req.filter = new TL_inputMessagesFilterDocument();
//                            } else if (a == MEDIA_AUDIO) {
//                                req.filter = new TL_inputMessagesFilterRoundVoice();
//                            } else if (a == MEDIA_URL) {
//                                req.filter = new TL_inputMessagesFilterUrl();
//                            } else if (a == MEDIA_MUSIC) {
//                                req.filter = new TL_inputMessagesFilterMusic();
//                            } else if (a == MEDIA_GIF) {
//                                req.filter = new TL_inputMessagesFilterGif();
//                            }
//                            req.q = "";
//                            req.peer = getMessagesController().getInputPeer(lower_part);
//                            if (req.peer == null) {
//                                counts[a] = 0;
//                                continue;
//                            }
//                            int reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
//                                if (error == null) {
//                                    final messages_Messages res = (messages_Messages) response;
//                                    if (res instanceof TL_messages_messages) {
//                                        counts[type] = res.messages.size();
//                                    } else {
//                                        counts[type] = res.count;
//                                    }
//                                    putMediaCountDatabase(uid, type, counts[type]);
//                                } else {
//                                    counts[type] = 0;
//                                }
//                                boolean finished = true;
//                                for (int b = 0; b < counts.length; b++) {
//                                    if (counts[b] == -1) {
//                                        finished = false;
//                                        break;
//                                    }
//                                }
//                                if (finished) {
//                                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.mediaCountsDidLoad, uid, counts));
//                                }
//                            });
//                            getConnectionsManager().bindRequestToGuid(reqId, classGuid);
                            if (counts[a] == -1) {
                                missing = true;
                            } else if (old[a] == 1) {
                                counts[a] = -1;
                            }
                        }
                    }
                    if (!missing) {
                        AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.mediaCountsDidLoad, uid, countsFinal));
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void getMediaCount(final long uid, final int type, final int classGuid, boolean fromCache) {
        int lower_part = (int)uid;
        if (fromCache || lower_part == 0) {
            getMediaCountDatabase(uid, type, classGuid);
        } else {
            //TODO 发起请求
//            TL_messages_search req = new TL_messages_search();
//            req.limit = 1;
//            req.offset_id = 0;
//            if (type == MEDIA_PHOTOVIDEO) {
//                req.filter = new TL_inputMessagesFilterPhotoVideo();
//            } else if (type == MEDIA_FILE) {
//                req.filter = new TL_inputMessagesFilterDocument();
//            } else if (type == MEDIA_AUDIO) {
//                req.filter = new TL_inputMessagesFilterRoundVoice();
//            } else if (type == MEDIA_URL) {
//                req.filter = new TL_inputMessagesFilterUrl();
//            } else if (type == MEDIA_MUSIC) {
//                req.filter = new TL_inputMessagesFilterMusic();
//            } else if (type == MEDIA_GIF) {
//                req.filter = new TL_inputMessagesFilterGif();
//            }
//            req.q = "";
//            req.peer = getMessagesController().getInputPeer(lower_part);
//            if (req.peer == null) {
//                return;
//            }
//            int reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
//                if (error == null) {
//                    final messages_Messages res = (messages_Messages) response;
//                    getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
//                    int count;
//                    if (res instanceof TL_messages_messages) {
//                        count = res.messages.size();
//                    } else {
//                        count = res.count;
//                    }
//                    AndroidUtilities.runOnUIThread(() -> {
//                        getMessagesController().putUsers(res.users, false);
//                        getMessagesController().putChats(res.chats, false);
//                    });
//
//                    processLoadedMediaCount(count, uid, type, classGuid, false, 0);
//                }
//            });
//            getConnectionsManager().bindRequestToGuid(reqId, classGuid);
        }
    }

    public static int getMediaType(Message message) {
        if (message == null) {
            return -1;
        }
        if (message.media.isPhoto()) {
            return MEDIA_PHOTOVIDEO;
        } else if (message.media.isDocument()) {
            if (MessageObject.isVoiceMessage(message) || MessageObject.isRoundVideoMessage(message)) {
                return MEDIA_AUDIO;
            } else if (MessageObject.isVideoMessage(message)) {
                return MEDIA_PHOTOVIDEO;
            } else if (MessageObject.isStickerMessage(message) || MessageObject.isAnimatedStickerMessage(message)) {
                return -1;
            } else if (MessageObject.isNewGifMessage(message)) {
                return MEDIA_GIF;
            } else if (MessageObject.isMusicMessage(message)) {
                return MEDIA_MUSIC;
            } else {
                return MEDIA_FILE;
            }
        } else if (!message.entities.isEmpty()) {
            for (int a = 0; a < message.entities.size(); a++) {
                MessageEntity entity = message.entities.get(a);
                if (entity.isUrl() || entity.isTextUrl() || entity.isEmail()) {
                    return MEDIA_URL;
                }
            }
        }
        return -1;
    }

    public static boolean canAddMessageToMedia(Message message) {
        if (message!=null && (message.media.isPhoto() || message.media.isDocument()) && message.media.ttl_seconds != 0) {
            return false;
        } else if (message.media.isPhoto() ||
                message.media.isDocument() && !MessageObject.isGifDocument(message.media.document)) {
            return true;
        } else if (!message.entities.isEmpty()) {
            for (int a = 0; a < message.entities.size(); a++) {
                MessageEntity entity = message.entities.get(a);
                if (entity.isUrl() || entity.isTextUrl() || entity.isEmail()) {
                    return true;
                }
            }
        }
        return MediaDataController.getMediaType(message) != -1;
    }

    private void processLoadedMedia(final messages_Messages res, final long uid, int count, int max_id, final int type, final int fromCache, final int classGuid, final boolean isChannel, final boolean topReached) {
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("process load media did " + uid + " count = " + count + " max_id " + max_id + " type = " + type + " cache = " + fromCache + " classGuid = " + classGuid);
        }
        int lower_part = (int)uid;
        if (fromCache != 0 && res.messages.isEmpty() && lower_part != 0) {
            if (fromCache == 2) {
                return;
            }
            loadMedia(uid, count, max_id, type, 0, classGuid);
        } else {
            if (fromCache == 0) {
                ImageLoader.saveMessagesThumbs(res.messages);
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                putMediaDatabase(uid, type, res.messages, max_id, topReached);
            }

            Utilities.searchQueue.postRunnable(() -> {
                final SparseArray<User> usersDict = new SparseArray<>();
                for (int a = 0; a < res.users.size(); a++) {
                    User u = res.users.get(a);
                    usersDict.put(u.id, u);
                }
                final ArrayList<MessageObject> objects = new ArrayList<>();
                for (int a = 0; a < res.messages.size(); a++) {
                    Message message = res.messages.get(a);
                    objects.add(new MessageObject(currentAccount, message, usersDict, true));
                }

                AndroidUtilities.runOnUIThread(() -> {
                    int totalCount = res.count;
                    getMessagesController().putUsers(res.users, fromCache != 0);
                    getMessagesController().putChats(res.chats, fromCache != 0);
                    getNotificationCenter().postNotificationName(NotificationCenter.mediaDidLoad, uid, totalCount, objects, classGuid, type, topReached);
                });
            });
        }
    }

    private void processLoadedMediaCount(final int count, final long uid, final int type, final int classGuid, final boolean fromCache, int old) {
        AndroidUtilities.runOnUIThread(() -> {
            int lower_part = (int) uid;
            boolean reload = fromCache && (count == -1 || count == 0 && type == 2) && lower_part != 0;
            if (reload || old == 1 && lower_part != 0) {
                getMediaCount(uid, type, classGuid, false);
            }
            if (!reload) {
                if (!fromCache) {
                    putMediaCountDatabase(uid, type, count);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mediaCountDidLoad, uid, (fromCache && count == -1 ? 0 : count), fromCache, type);
            }
        });
    }

    private void putMediaCountDatabase(final long uid, final int type, final int count) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                SQLitePreparedStatement state2 = getMessagesStorage().getDatabase().executeFast("REPLACE INTO media_counts_v2 VALUES(?, ?, ?, ?)");
                state2.requery();
                state2.bindLong(1, uid);
                state2.bindInteger(2, type);
                state2.bindInteger(3, count);
                state2.bindInteger(4, 0);
                state2.step();
                state2.dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void getMediaCountDatabase(final long uid, final int type, final int classGuid) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                int count = -1;
                int old = 0;
                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT count, old FROM media_counts_v2 WHERE uid = %d AND type = %d LIMIT 1", uid, type));
                if (cursor.next()) {
                    count = cursor.intValue(0);
                    old = cursor.intValue(1);
                }
                cursor.dispose();
                int lower_part = (int)uid;
                if (count == -1 && lower_part == 0) {
                    cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT COUNT(mid) FROM media_v2 WHERE uid = %d AND type = %d LIMIT 1", uid, type));
                    if (cursor.next()) {
                        count = cursor.intValue(0);
                    }
                    cursor.dispose();

                    if (count != -1) {
                        putMediaCountDatabase(uid, type, count);
                    }
                }
                processLoadedMediaCount(count, uid, type, classGuid, true, old);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void loadMediaDatabase(final long uid, final int count, final int max_id, final int type, final int classGuid, final boolean isChannel, final int fromCache) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean topReached = false;
                messages_Messages res = new messages_Messages();
                try {
                    ArrayList<Integer> usersToLoad = new ArrayList<>();
                    ArrayList<Integer> chatsToLoad = new ArrayList<>();
                    int countToLoad = count + 1;

                    SQLiteCursor cursor;
                    SQLiteDatabase database = getMessagesStorage().getDatabase();
                    boolean isEnd = false;
                    if ((int) uid != 0) {
                        int channelId = 0;
                        long messageMaxId = max_id;
                        if (isChannel) {
                            channelId = -(int) uid;
                        }
                        if (messageMaxId != 0 && channelId != 0) {
                            messageMaxId |= ((long) channelId) << 32;
                        }

                        cursor = database.queryFinalized(String.format(Locale.US, "SELECT start FROM media_holes_v2 WHERE uid = %d AND type = %d AND start IN (0, 1)", uid, type));
                        if (cursor.next()) {
                            isEnd = cursor.intValue(0) == 1;
                            cursor.dispose();
                        } else {
                            cursor.dispose();
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT min(mid) FROM media_v2 WHERE uid = %d AND type = %d AND mid > 0", uid, type));
                            if (cursor.next()) {
                                int mid = cursor.intValue(0);
                                if (mid != 0) {
                                    SQLitePreparedStatement state = database.executeFast("REPLACE INTO media_holes_v2 VALUES(?, ?, ?, ?)");
                                    state.requery();
                                    state.bindLong(1, uid);
                                    state.bindInteger(2, type);
                                    state.bindInteger(3, 0);
                                    state.bindInteger(4, mid);
                                    state.step();
                                    state.dispose();
                                }
                            }
                            cursor.dispose();
                        }

                        if (messageMaxId != 0) {
                            long holeMessageId = 0;
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT end FROM media_holes_v2 WHERE uid = %d AND type = %d AND end <= %d ORDER BY end DESC LIMIT 1", uid, type, max_id));
                            if (cursor.next()) {
                                holeMessageId = cursor.intValue(0);
                                if (channelId != 0) {
                                    holeMessageId |= ((long) channelId) << 32;
                                }
                            }
                            cursor.dispose();
                            if (holeMessageId > 1) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > 0 AND mid < %d AND mid >= %d AND type = %d ORDER BY date DESC, mid DESC LIMIT %d", uid, messageMaxId, holeMessageId, type, countToLoad));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > 0 AND mid < %d AND type = %d ORDER BY date DESC, mid DESC LIMIT %d", uid, messageMaxId, type, countToLoad));
                            }
                        } else {
                            long holeMessageId = 0;
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT max(end) FROM media_holes_v2 WHERE uid = %d AND type = %d", uid, type));
                            if (cursor.next()) {
                                holeMessageId = cursor.intValue(0);
                                if (channelId != 0) {
                                    holeMessageId |= ((long) channelId) << 32;
                                }
                            }
                            cursor.dispose();
                            if (holeMessageId > 1) {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid >= %d AND type = %d ORDER BY date DESC, mid DESC LIMIT %d", uid, holeMessageId, type, countToLoad));
                            } else {
                                cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > 0 AND type = %d ORDER BY date DESC, mid DESC LIMIT %d", uid, type, countToLoad));
                            }
                        }
                    } else {
                        isEnd = true;
                        if (max_id != 0) {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT m.data, m.mid, r.random_id FROM media_v2 as m LEFT JOIN randoms as r ON r.mid = m.mid WHERE m.uid = %d AND m.mid > %d AND type = %d ORDER BY m.mid ASC LIMIT %d", uid, max_id, type, countToLoad));
                        } else {
                            cursor = database.queryFinalized(String.format(Locale.US, "SELECT m.data, m.mid, r.random_id FROM media_v2 as m LEFT JOIN randoms as r ON r.mid = m.mid WHERE m.uid = %d AND type = %d ORDER BY m.mid ASC LIMIT %d", uid, type, countToLoad));
                        }
                    }

                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(0);
                        if (data != null) {
                            Message message = Message.TLdeserialize(data, data.readInt32(false), false);
                            message.readAttachPath(data, getUserConfig().clientUserId);
                            data.reuse();
                            message.id = cursor.intValue(1);
                            message.dialog_id = uid;
                            if ((int) uid == 0) {
                                message.random_id = cursor.longValue(2);
                            }
                            res.messages.add(message);
                            MessagesStorage.addUsersAndChatsFromMessage(message, usersToLoad, chatsToLoad);
                        }
                    }
                    cursor.dispose();

                    if (!usersToLoad.isEmpty()) {
                        getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), res.users);
                    }
                    if (!chatsToLoad.isEmpty()) {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), res.chats);
                    }
                    if (res.messages.size() > count) {
                        topReached = false;
                        res.messages.remove(res.messages.size() - 1);
                    } else {
                        topReached = isEnd;
                    }
                } catch (Exception e) {
                    res.messages.clear();
                    res.chats.clear();
                    res.users.clear();
                    FileLog.e(e);
                } finally {
                    Runnable task = this;
                    AndroidUtilities.runOnUIThread(() -> getMessagesStorage().completeTaskForGuid(task, classGuid));
                    processLoadedMedia(res, uid, count, max_id, type, fromCache, classGuid, isChannel, topReached);
                }
            }
        };
        MessagesStorage messagesStorage = getMessagesStorage();
        messagesStorage.getStorageQueue().postRunnable(runnable);
        messagesStorage.bindTaskToGuid(runnable, classGuid);
    }

    private void putMediaDatabase(final long uid, final int type, final ArrayList<Message> messages, final int max_id, final boolean topReached) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                if (messages.isEmpty() || topReached) {
                    getMessagesStorage().doneHolesInMedia(uid, max_id, type);
                    if (messages.isEmpty()) {
                        return;
                    }
                }
                getMessagesStorage().getDatabase().beginTransaction();
                SQLitePreparedStatement state2 = getMessagesStorage().getDatabase().executeFast("REPLACE INTO media_v2 VALUES(?, ?, ?, ?, ?)");
                for (Message message : messages) {
                    if (canAddMessageToMedia(message)) {

                        long messageId = message.id;
                        if (message.to_id != 0) {
                            messageId |= ((long) message.to_id) << 32;
                        }

                        state2.requery();
                        NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                        message.serializeToStream(data);
                        state2.bindLong(1, messageId);
                        state2.bindLong(2, uid);
                        state2.bindInteger(3, message.date);
                        state2.bindInteger(4, type);
                        state2.bindByteBuffer(5, data);
                        state2.step();
                        data.reuse();
                    }
                }
                state2.dispose();
                if (!topReached || max_id != 0) {
                    int minId = topReached ? 1 : messages.get(messages.size() - 1).id;
                    if (max_id != 0) {
                        getMessagesStorage().closeHolesInMedia(uid, minId, max_id, type);
                    } else {
                        getMessagesStorage().closeHolesInMedia(uid, minId, Integer.MAX_VALUE, type);
                    }
                }
                getMessagesStorage().getDatabase().commitTransaction();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void loadMusic(final long uid, final long max_id) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            final ArrayList<MessageObject> arrayList = new ArrayList<>();
            try {
                int lower_id = (int) uid;
                SQLiteCursor cursor;
                if (lower_id != 0) {
                    cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid < %d AND type = %d ORDER BY date DESC, mid DESC LIMIT 1000", uid, max_id, MEDIA_MUSIC));
                } else {
                    cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, mid FROM media_v2 WHERE uid = %d AND mid > %d AND type = %d ORDER BY date DESC, mid DESC LIMIT 1000", uid, max_id, MEDIA_MUSIC));
                }

                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        Message message = Message.TLdeserialize(data, data.readInt32(false), false);
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        if (MessageObject.isMusicMessage(message)) {
                            message.id = cursor.intValue(1);
                            message.dialog_id = uid;
                            arrayList.add(0, new MessageObject(currentAccount, message, false));
                        }
                    }
                }
                cursor.dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.musicDidLoad, uid, arrayList));
        });
    }
    //endregion ---------------- MEDIA END ----------------

    //region ---------------- BOT ----------------
    private SparseArray<BotInfo> botInfos = new SparseArray<>();
    private LongSparseArray<Message> botKeyboards = new LongSparseArray<>();
    private SparseLongArray botKeyboardsByMids = new SparseLongArray();

    public void clearBotKeyboard(final long did, final ArrayList<Integer> messages) {
        AndroidUtilities.runOnUIThread(() -> {
            if (messages != null) {
                for (int a = 0; a < messages.size(); a++) {
                    long did1 = botKeyboardsByMids.get(messages.get(a));
                    if (did1 != 0) {
                        botKeyboards.remove(did1);
                        botKeyboardsByMids.delete(messages.get(a));
                        getNotificationCenter().postNotificationName(NotificationCenter.botKeyboardDidLoad, null, did1);
                    }
                }
            } else {
                botKeyboards.remove(did);
                getNotificationCenter().postNotificationName(NotificationCenter.botKeyboardDidLoad, null, did);
            }
        });
    }

    public void loadBotKeyboard(final long did) {
        Message keyboard = botKeyboards.get(did);
        if (keyboard != null) {
            getNotificationCenter().postNotificationName(NotificationCenter.botKeyboardDidLoad, keyboard, did);
            return;
        }
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                Message botKeyboard = null;
                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT info FROM bot_keyboard WHERE uid = %d", did));
                if (cursor.next()) {
                    NativeByteBuffer data;

                    if (!cursor.isNull(0)) {
                        data = cursor.byteBufferValue(0);
                        if (data != null) {
                            botKeyboard = Message.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();

                if (botKeyboard != null) {
                    final Message botKeyboardFinal = botKeyboard;
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.botKeyboardDidLoad, botKeyboardFinal, did));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void loadBotInfo(final int uid, boolean cache, final int classGuid) {
        if (cache) {
            BotInfo botInfo = botInfos.get(uid);
            if (botInfo != null) {
                getNotificationCenter().postNotificationName(NotificationCenter.botInfoDidLoad, botInfo, classGuid);
                return;
            }
        }
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                BotInfo botInfo = null;
                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT info FROM bot_info WHERE uid = %d", uid));
                if (cursor.next()) {
                    NativeByteBuffer data;

                    if (!cursor.isNull(0)) {
                        data = cursor.byteBufferValue(0);
                        if (data != null) {
                            botInfo = BotInfo.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();

                if (botInfo != null) {
                    final BotInfo botInfoFinal = botInfo;
                    AndroidUtilities.runOnUIThread(() -> getNotificationCenter().postNotificationName(NotificationCenter.botInfoDidLoad, botInfoFinal, classGuid));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void putBotKeyboard(final long did, final Message message) {
        if (message == null) {
            return;
        }
        try {
            int mid = 0;
            SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT mid FROM bot_keyboard WHERE uid = %d", did));
            if (cursor.next()) {
                mid = cursor.intValue(0);
            }
            cursor.dispose();
            if (mid >= message.id) {
                return;
            }

            SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO bot_keyboard VALUES(?, ?, ?)");
            state.requery();
            NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
            message.serializeToStream(data);
            state.bindLong(1, did);
            state.bindInteger(2, message.id);
            state.bindByteBuffer(3, data);
            state.step();
            data.reuse();
            state.dispose();

            AndroidUtilities.runOnUIThread(() -> {
                Message old = botKeyboards.get(did);
                botKeyboards.put(did, message);
                if (old != null) {
                    botKeyboardsByMids.delete(old.id);
                }
                botKeyboardsByMids.put(message.id, did);
                getNotificationCenter().postNotificationName(NotificationCenter.botKeyboardDidLoad, message, did);
            });
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void putBotInfo(final BotInfo botInfo) {
        if (botInfo == null) {
            return;
        }
        botInfos.put(botInfo.user_id, botInfo);
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO bot_info(uid, info) VALUES(?, ?)");
                state.requery();
                NativeByteBuffer data = new NativeByteBuffer(botInfo.getObjectSize());
                botInfo.serializeToStream(data);
                state.bindInteger(1, botInfo.user_id);
                state.bindByteBuffer(2, data);
                state.step();
                data.reuse();
                state.dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    //endregion ---------------- BOT END ----------------


    //region ---------------- SEARCH ----------------
//    public ArrayList<TLRPC.TL_topPeer> hints = new ArrayList<>();
//    public ArrayList<TLRPC.TL_topPeer> inlineBots = new ArrayList<>();
    boolean loaded;
    boolean loading;

    private static Paint roundPaint, erasePaint;
    private static RectF bitmapRect;
    private static Path roundPath;

//    public void buildShortcuts() {
//        if (Build.VERSION.SDK_INT < 25) {
//            return;
//        }
//        final ArrayList<TLRPC.TL_topPeer> hintsFinal = new ArrayList<>();
//        if (SharedConfig.passcodeHash.length() <= 0) {
//            for (int a = 0; a < hints.size(); a++) {
//                hintsFinal.add(hints.get(a));
//                if (hintsFinal.size() == 3) {
//                    break;
//                }
//            }
//        }
//        Utilities.globalQueue.postRunnable(() -> {
//            try {
//                ShortcutManager shortcutManager = ApplicationLoader.applicationContext.getSystemService(ShortcutManager.class);
//                List<ShortcutInfo> currentShortcuts = shortcutManager.getDynamicShortcuts();
//                ArrayList<String> shortcutsToUpdate = new ArrayList<>();
//                ArrayList<String> newShortcutsIds = new ArrayList<>();
//                ArrayList<String> shortcutsToDelete = new ArrayList<>();
//
//                if (currentShortcuts != null && !currentShortcuts.isEmpty()) {
//                    newShortcutsIds.add("compose");
//                    for (int a = 0; a < hintsFinal.size(); a++) {
//                        TLRPC.TL_topPeer hint = hintsFinal.get(a);
//                        long did;
//                        if (hint.peer.user_id != 0) {
//                            did = hint.peer.user_id;
//                        } else {
//                            did = -hint.peer.chat_id;
//                            if (did == 0) {
//                                did = -hint.peer.channel_id;
//                            }
//                        }
//                        newShortcutsIds.add("did" + did);
//                    }
//                    for (int a = 0; a < currentShortcuts.size(); a++) {
//                        String id = currentShortcuts.get(a).getId();
//                        if (!newShortcutsIds.remove(id)) {
//                            shortcutsToDelete.add(id);
//                        }
//                        shortcutsToUpdate.add(id);
//                    }
//                    if (newShortcutsIds.isEmpty() && shortcutsToDelete.isEmpty()) {
//                        return;
//                    }
//                }
//
//                Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
//                intent.setAction("new_dialog");
//                ArrayList<ShortcutInfo> arrayList = new ArrayList<>();
//                arrayList.add(new ShortcutInfo.Builder(ApplicationLoader.applicationContext, "compose")
//                        .setShortLabel(LocaleController.getString("NewConversationShortcut", R.string.NewConversationShortcut))
//                        .setLongLabel(LocaleController.getString("NewConversationShortcut", R.string.NewConversationShortcut))
//                        .setIcon(Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.shortcut_compose))
//                        .setIntent(intent)
//                        .build());
//                if (shortcutsToUpdate.contains("compose")) {
//                    shortcutManager.updateShortcuts(arrayList);
//                } else {
//                    shortcutManager.addDynamicShortcuts(arrayList);
//                }
//                arrayList.clear();
//
//                if (!shortcutsToDelete.isEmpty()) {
//                    shortcutManager.removeDynamicShortcuts(shortcutsToDelete);
//                }
//
//                for (int a = 0; a < hintsFinal.size(); a++) {
//                    Intent shortcutIntent = new Intent(ApplicationLoader.applicationContext, OpenChatReceiver.class);
//                    TLRPC.TL_topPeer hint = hintsFinal.get(a);
//
//                    User user = null;
//                    Chat chat = null;
//                    long did;
//                    if (hint.peer.user_id != 0) {
//                        shortcutIntent.putExtra("userId", hint.peer.user_id);
//                        user = getMessagesController().getUser(hint.peer.user_id);
//                        did = hint.peer.user_id;
//                    } else {
//                        int chat_id = hint.peer.chat_id;
//                        if (chat_id == 0) {
//                            chat_id = hint.peer.channel_id;
//                        }
//                        chat = getMessagesController().getChat(chat_id);
//                        shortcutIntent.putExtra("chatId", chat_id);
//                        did = -chat_id;
//                    }
//                    if ((user == null || UserObject.isDeleted(user)) && chat == null) {
//                        continue;
//                    }
//
//                    String name;
//                    FileLocation photo = null;
//
//                    if (user != null) {
//                        name = UserObject.formatName(user.first_name, user.last_name);
//                        if (user.photo != null) {
//                            photo = user.photo.photo_small;
//                        }
//                    } else {
//                        name = chat.title;
//                        if (chat.photo != null) {
//                            photo = chat.photo.photo_small;
//                        }
//                    }
//
//                    shortcutIntent.putExtra("currentAccount", currentAccount);
//                    shortcutIntent.setAction("com.tmessages.openchat" + did);
//                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//                    Bitmap bitmap = null;
//                    if (photo != null) {
//                        try {
//                            File path = FileLoader.getPathToAttach(photo, true);
//                            bitmap = BitmapFactory.decodeFile(path.toString());
//                            if (bitmap != null) {
//                                int size = AndroidUtilities.dp(48);
//                                Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
//                                Canvas canvas = new Canvas(result);
//                                if (roundPaint == null) {
//                                    roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
//                                    bitmapRect = new RectF();
//                                    erasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                                    erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//                                    roundPath = new Path();
//                                    roundPath.addCircle(size / 2, size / 2, size / 2 - AndroidUtilities.dp(2), Path.Direction.CW);
//                                    roundPath.toggleInverseFillType();
//                                }
//                                bitmapRect.set(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(46), AndroidUtilities.dp(46));
//                                canvas.drawBitmap(bitmap, null, bitmapRect, roundPaint);
//                                canvas.drawPath(roundPath, erasePaint);
//                                try {
//                                    canvas.setBitmap(null);
//                                } catch (Exception ignore) {
//
//                                }
//                                bitmap = result;
//                            }
//                        } catch (Throwable e) {
//                            FileLog.e(e);
//                        }
//                    }
//
//                    String id = "did" + did;
//                    if (TextUtils.isEmpty(name)) {
//                        name = " ";
//                    }
//                    ShortcutInfo.Builder builder = new ShortcutInfo.Builder(ApplicationLoader.applicationContext, id)
//                            .setShortLabel(name)
//                            .setLongLabel(name)
//                            .setIntent(shortcutIntent);
//                    if (bitmap != null) {
//                        builder.setIcon(Icon.createWithBitmap(bitmap));
//                    } else {
//                        builder.setIcon(Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.shortcut_user));
//                    }
//                    arrayList.add(builder.build());
//                    if (shortcutsToUpdate.contains(id)) {
//                        shortcutManager.updateShortcuts(arrayList);
//                    } else {
//                        shortcutManager.addDynamicShortcuts(arrayList);
//                    }
//                    arrayList.clear();
//                }
//            } catch (Throwable ignore) {
//
//            }
//        });
//    }

//    public void loadHints(boolean cache) {
//        if (loading || !getUserConfig().suggestContacts) {
//            return;
//        }
//        if (cache) {
//            if (loaded) {
//                return;
//            }
//            loading = true;
//            getMessagesStorage().getStorageQueue().postRunnable(() -> {
//                final ArrayList<TLRPC.TL_topPeer> hintsNew = new ArrayList<>();
//                final ArrayList<TLRPC.TL_topPeer> inlineBotsNew = new ArrayList<>();
//                final ArrayList<User> users = new ArrayList<>();
//                final ArrayList<TLRPC.Chat> chats = new ArrayList<>();
//                int selfUserId = getUserConfig().getClientUserId();
//                try {
//                    ArrayList<Integer> usersToLoad = new ArrayList<>();
//                    ArrayList<Integer> chatsToLoad = new ArrayList<>();
//                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT did, type, rating FROM chat_hints WHERE 1 ORDER BY rating DESC");
//                    while (cursor.next()) {
//                        int did = cursor.intValue(0);
//                        if (did == selfUserId) {
//                            continue;
//                        }
//                        int type = cursor.intValue(1);
//                        TLRPC.TL_topPeer peer = new TLRPC.TL_topPeer();
//                        peer.rating = cursor.doubleValue(2);
//                        if (did > 0) {
//                            peer.peer = new TLRPC.TL_peerUser();
//                            peer.peer.user_id = did;
//                            usersToLoad.add(did);
//                        } else {
//                            peer.peer = new TLRPC.TL_peerChat();
//                            peer.peer.chat_id = -did;
//                            chatsToLoad.add(-did);
//                        }
//                        if (type == 0) {
//                            hintsNew.add(peer);
//                        } else if (type == 1) {
//                            inlineBotsNew.add(peer);
//                        }
//                    }
//                    cursor.dispose();
//                    if (!usersToLoad.isEmpty()) {
//                        getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), users);
//                    }
//
//                    if (!chatsToLoad.isEmpty()) {
//                        getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
//                    }
//                    AndroidUtilities.runOnUIThread(() -> {
//                        getMessagesController().putUsers(users, true);
//                        getMessagesController().putChats(chats, true);
//                        loading = false;
//                        loaded = true;
//                        hints = hintsNew;
//                        inlineBots = inlineBotsNew;
//                        buildShortcuts();
//                        getNotificationCenter().postNotificationName(NotificationCenter.reloadHints);
//                        getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);
//                        if (Math.abs(getUserConfig().lastHintsSyncTime - (int) (System.currentTimeMillis() / 1000)) >= 24 * 60 * 60) {
//                            loadHints(false);
//                        }
//                    });
//                } catch (Exception e) {
//                    FileLog.e(e);
//                }
//            });
//            loaded = true;
//        } else {
//            loading = true;
//            //TODO 发起请求
////            TLRPC.TL_contacts_getTopPeers req = new TLRPC.TL_contacts_getTopPeers();
////            req.hash = 0;
////            req.bots_pm = false;
////            req.correspondents = true;
////            req.groups = false;
////            req.channels = false;
////            req.bots_inline = true;
////            req.offset = 0;
////            req.limit = 20;
////            getConnectionsManager().sendRequest(req, (response, error) -> {
////                if (response instanceof TLRPC.TL_contacts_topPeers) {
////                    AndroidUtilities.runOnUIThread(() -> {
////                        final TLRPC.TL_contacts_topPeers topPeers = (TLRPC.TL_contacts_topPeers) response;
////                        getMessagesController().putUsers(topPeers.users, false);
////                        getMessagesController().putChats(topPeers.chats, false);
////                        for (int a = 0; a < topPeers.categories.size(); a++) {
////                            TLRPC.TL_topPeerCategoryPeers category = topPeers.categories.get(a);
////                            if (category.category instanceof TLRPC.TL_topPeerCategoryBotsInline) {
////                                inlineBots = category.peers;
////                                getUserConfig().botRatingLoadTime = (int) (System.currentTimeMillis() / 1000);
////                            } else {
////                                hints = category.peers;
////                                int selfUserId = getUserConfig().getClientUserId();
////                                for (int b = 0; b < hints.size(); b++) {
////                                    TLRPC.TL_topPeer topPeer = hints.get(b);
////                                    if (topPeer.peer.user_id == selfUserId) {
////                                        hints.remove(b);
////                                        break;
////                                    }
////                                }
////                                getUserConfig().ratingLoadTime = (int) (System.currentTimeMillis() / 1000);
////                            }
////                        }
////                        getUserConfig().saveConfig(false);
////                        buildShortcuts();
////                        getNotificationCenter().postNotificationName(NotificationCenter.reloadHints);
////                        getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);
////                        getMessagesStorage().getStorageQueue().postRunnable(() -> {
////                            try {
////                                getMessagesStorage().getDatabase().executeFast("DELETE FROM chat_hints WHERE 1").stepThis().dispose();
////                                getMessagesStorage().getDatabase().beginTransaction();
////                                getMessagesStorage().putUsersAndChats(topPeers.users, topPeers.chats, false, false);
////
////                                SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO chat_hints VALUES(?, ?, ?, ?)");
////                                for (int a = 0; a < topPeers.categories.size(); a++) {
////                                    int type;
////                                    TLRPC.TL_topPeerCategoryPeers category = topPeers.categories.get(a);
////                                    if (category.category instanceof TLRPC.TL_topPeerCategoryBotsInline) {
////                                        type = 1;
////                                    } else {
////                                        type = 0;
////                                    }
////                                    for (int b = 0; b < category.peers.size(); b++) {
////                                        TLRPC.TL_topPeer peer = category.peers.get(b);
////                                        int did;
////                                        if (peer.peer instanceof TLRPC.TL_peerUser) {
////                                            did = peer.peer.user_id;
////                                        } else if (peer.peer instanceof TLRPC.TL_peerChat) {
////                                            did = -peer.peer.chat_id;
////                                        } else {
////                                            did = -peer.peer.channel_id;
////                                        }
////                                        state.requery();
////                                        state.bindInteger(1, did);
////                                        state.bindInteger(2, type);
////                                        state.bindDouble(3, peer.rating);
////                                        state.bindInteger(4, 0);
////                                        state.step();
////                                    }
////                                }
////
////                                state.dispose();
////
////                                getMessagesStorage().getDatabase().commitTransaction();
////                                AndroidUtilities.runOnUIThread(() -> {
////                                    getUserConfig().suggestContacts = true;
////                                    getUserConfig().lastHintsSyncTime = (int) (System.currentTimeMillis() / 1000);
////                                    getUserConfig().saveConfig(false);
////                                });
////                            } catch (Exception e) {
////                                FileLog.e(e);
////                            }
////                        });
////                    });
////                } else if (response instanceof TLRPC.TL_contacts_topPeersDisabled) {
////                    AndroidUtilities.runOnUIThread(() -> {
////                        getUserConfig().suggestContacts = false;
////                        getUserConfig().lastHintsSyncTime = (int) (System.currentTimeMillis() / 1000);
////                        getUserConfig().saveConfig(false);
////                        clearTopPeers();
////                    });
////                }
////            });
//        }
//    }

//    public void clearTopPeers() {
//        hints.clear();
//        inlineBots.clear();
//        getNotificationCenter().postNotificationName(NotificationCenter.reloadHints);
//        getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);
//        getMessagesStorage().getStorageQueue().postRunnable(() -> {
//            try {
//                getMessagesStorage().getDatabase().executeFast("DELETE FROM chat_hints WHERE 1").stepThis().dispose();
//            } catch (Exception ignore) {
//
//            }
//        });
//        buildShortcuts();
//    }

    public void increaseInlineRaiting(final int uid) {
        if (!getUserConfig().suggestContacts) {
            return;
        }
        int dt;
        if (getUserConfig().botRatingLoadTime != 0) {
            dt = Math.max(1, ((int) (System.currentTimeMillis() / 1000)) - getUserConfig().botRatingLoadTime);
        } else {
            dt = 60;
        }
        getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);
    }

    public void removeInline(final int uid) {
        //TODO 发起请求
//        TLRPC.TL_topPeerCategoryPeers category = null;
//        for (int a = 0; a < inlineBots.size(); a++) {
//            if (inlineBots.get(a).peer.user_id == uid) {
//                inlineBots.remove(a);
//                TLRPC.TL_contacts_resetTopPeerRating req = new TLRPC.TL_contacts_resetTopPeerRating();
//                req.category = new TLRPC.TL_topPeerCategoryBotsInline();
//                req.peer = getMessagesController().getInputPeer(uid);
//                getConnectionsManager().sendRequest(req, (response, error) -> {
//
//                });
//                deletePeer(uid, 1);
//                getNotificationCenter().postNotificationName(NotificationCenter.reloadInlineHints);
//                return;
//            }
//        }
    }

//    public void removePeer(final int uid) {
//        for (int a = 0; a < hints.size(); a++) {
//            if (hints.get(a).peer.user_id == uid) {
//                hints.remove(a);
//                getNotificationCenter().postNotificationName(NotificationCenter.reloadHints);
//                //TODO 发起请求
////                TLRPC.TL_contacts_resetTopPeerRating req = new TLRPC.TL_contacts_resetTopPeerRating();
////                req.category = new TLRPC.TL_topPeerCategoryCorrespondents();
////                req.peer = getMessagesController().getInputPeer(uid);
////                deletePeer(uid, 0);
////                getConnectionsManager().sendRequest(req, (response, error) -> {
////
////                });
//                return;
//            }
//        }
//    }

//    public void increasePeerRaiting(final long did) {
//        if (!getUserConfig().suggestContacts) {
//            return;
//        }
//        final int lower_id = (int) did;
//        if (lower_id <= 0) {
//            return;
//        }
//        //remove chats and bots for now
//        final User user = lower_id > 0 ? getMessagesController().getUser(lower_id) : null;
//        //final TLRPC.Chat chat = lower_id < 0 ? MessagesController.getInstance().getChat(-lower_id) : null;
//        if (user == null || user.bot || user.self/*&& chat == null || ChatObject.isChannel(chat) && !chat.megagroup*/) {
//            return;
//        }
//        getMessagesStorage().getStorageQueue().postRunnable(() -> {
//            double dt = 0;
//            try {
//                int lastTime = 0;
//                int lastMid = 0;
//                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized(String.format(Locale.US, "SELECT MAX(mid), MAX(date) FROM messages WHERE uid = %d AND out = 1", did));
//                if (cursor.next()) {
//                    lastMid = cursor.intValue(0);
//                    lastTime = cursor.intValue(1);
//                }
//                cursor.dispose();
//                if (lastMid > 0 && getUserConfig().ratingLoadTime != 0) {
//                    dt = (lastTime - getUserConfig().ratingLoadTime);
//                }
//            } catch (Exception e) {
//                FileLog.e(e);
//            }
//            final double dtFinal = dt;
//            AndroidUtilities.runOnUIThread(() -> {
//                TLRPC.TL_topPeer peer = null;
//                for (int a = 0; a < hints.size(); a++) {
//                    TLRPC.TL_topPeer p = hints.get(a);
//                    if (lower_id < 0 && (p.peer.chat_id == -lower_id || p.peer.channel_id == -lower_id) || lower_id > 0 && p.peer.user_id == lower_id) {
//                        peer = p;
//                        break;
//                    }
//                }
//                if (peer == null) {
//                    peer = new TLRPC.TL_topPeer();
//                    if (lower_id > 0) {
//                        peer.peer = new TLRPC.TL_peerUser();
//                        peer.peer.user_id = lower_id;
//                    } else {
//                        peer.peer = new TLRPC.TL_peerChat();
//                        peer.peer.chat_id = -lower_id;
//                    }
//                    hints.add(peer);
//                }
//                peer.rating += Math.exp(dtFinal / getMessagesController().ratingDecay);
//                Collections.sort(hints, (lhs, rhs) -> {
//                    if (lhs.rating > rhs.rating) {
//                        return -1;
//                    } else if (lhs.rating < rhs.rating) {
//                        return 1;
//                    }
//                    return 0;
//                });
//
//                savePeer((int) did, 0, peer.rating);
//
//                getNotificationCenter().postNotificationName(NotificationCenter.reloadHints);
//            });
//        });
//    }

    private void savePeer(final int did, final int type, final double rating) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO chat_hints VALUES(?, ?, ?, ?)");
                state.requery();
                state.bindInteger(1, did);
                state.bindInteger(2, type);
                state.bindDouble(3, rating);
                state.bindInteger(4, (int) System.currentTimeMillis() / 1000);
                state.step();
                state.dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private void deletePeer(final int did, final int type) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                getMessagesStorage().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM chat_hints WHERE did = %d AND type = %d", did, type)).stepThis().dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private Intent createIntrnalShortcutIntent(long did) {
        Intent shortcutIntent = new Intent(ApplicationLoader.applicationContext, OpenChatReceiver.class);

        int lower_id = (int) did;
        int high_id = (int) (did >> 32);

        if (lower_id > 0) {
            shortcutIntent.putExtra("userId", lower_id);
        } else if (lower_id < 0) {
            shortcutIntent.putExtra("chatId", -lower_id);
        } else {
            return null;
        }
        shortcutIntent.putExtra("currentAccount", currentAccount);
        shortcutIntent.setAction("com.tmessages.openchat" + did);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return shortcutIntent;
    }

    public void installShortcut(long did) {
        try {

            Intent shortcutIntent = createIntrnalShortcutIntent(did);

            int lower_id = (int) did;
            int high_id = (int) (did >> 32);

            User user = null;
            Chat chat = null;
            if (lower_id > 0) {
                user = getMessagesController().getUser(lower_id);
            } else if (lower_id < 0) {
                chat = getMessagesController().getChat(-lower_id);
            } else {
                return;
            }
            if (user == null && chat == null) {
                return;
            }

            String name;
            FileLocation photo = null;

            boolean selfUser = false;

            if (user != null) {
                if (UserObject.isUserSelf(user)) {
                    name = LocaleController.getString("SavedMessages", R.string.SavedMessages);
                    selfUser = true;
                } else {
                    name = UserObject.formatName(user.first_name, user.last_name);
                    if (user.photo != null) {
                        photo = user.photo.photo_small;
                    }
                }
            } else {
                name = chat.title;
                if (chat.photo != null) {
                    photo = chat.photo.photo_small;
                }
            }

            Bitmap bitmap = null;
            if (selfUser || photo != null) {
                try {
                    if (!selfUser) {
                        File path = FileLoader.getPathToAttach(photo, true);
                        bitmap = BitmapFactory.decodeFile(path.toString());
                    }
                    if (selfUser || bitmap != null) {
                        int size = AndroidUtilities.dp(58);
                        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        result.eraseColor(Color.TRANSPARENT);
                        Canvas canvas = new Canvas(result);
                        if (selfUser) {
                            AvatarDrawable avatarDrawable = new AvatarDrawable(user);
                            avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                            avatarDrawable.setBounds(0, 0, size, size);
                            avatarDrawable.draw(canvas);
                        } else {
                            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                            if (roundPaint == null) {
                                roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                bitmapRect = new RectF();
                            }
                            float scale = size / (float) bitmap.getWidth();
                            canvas.save();
                            canvas.scale(scale, scale);
                            roundPaint.setShader(shader);
                            bitmapRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                            canvas.drawRoundRect(bitmapRect, bitmap.getWidth(), bitmap.getHeight(), roundPaint);
                            canvas.restore();
                        }
                        Drawable drawable = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.book_logo);
                        int w = AndroidUtilities.dp(15);
                        int left = size - w - AndroidUtilities.dp(2);
                        int top = size - w - AndroidUtilities.dp(2);
                        drawable.setBounds(left, top, left + w, top + w);
                        drawable.draw(canvas);
                        try {
                            canvas.setBitmap(null);
                        } catch (Exception e) {
                            //don't promt, this will crash on 2.x
                        }
                        bitmap = result;
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            if (Build.VERSION.SDK_INT >= 26) {
                ShortcutInfo.Builder pinShortcutInfo =
                        new ShortcutInfo.Builder(ApplicationLoader.applicationContext, "sdid_" + did)
                                .setShortLabel(name)
                                .setIntent(shortcutIntent);

                if (bitmap != null) {
                    pinShortcutInfo.setIcon(Icon.createWithBitmap(bitmap));
                } else {
                    if (user != null) {
                        if (user.bot) {
                            pinShortcutInfo.setIcon(Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.book_bot));
                        } else {
                            pinShortcutInfo.setIcon(Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.book_user));
                        }
                    } else if (chat != null) {
                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                            pinShortcutInfo.setIcon(Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.book_channel));
                        } else {
                            pinShortcutInfo.setIcon(Icon.createWithResource(ApplicationLoader.applicationContext, R.drawable.book_group));
                        }
                    }
                }

                ShortcutManager shortcutManager = ApplicationLoader.applicationContext.getSystemService(ShortcutManager.class);
                shortcutManager.requestPinShortcut(pinShortcutInfo.build(), null);
            } else {
                Intent addIntent = new Intent();
                if (bitmap != null) {
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
                } else {
                    if (user != null) {
                        if (user.bot) {
                            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_bot));
                        } else {
                            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_user));
                        }
                    } else if (chat != null) {
                        if (ChatObject.isChannel(chat) && !chat.megagroup) {
                            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_channel));
                        } else {
                            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_group));
                        }
                    }
                }

                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                addIntent.putExtra("duplicate", false);

                addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                ApplicationLoader.applicationContext.sendBroadcast(addIntent);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

//    public void uninstallShortcut(long did) {
//        try {
//            if (Build.VERSION.SDK_INT >= 26) {
//                ShortcutManager shortcutManager = ApplicationLoader.applicationContext.getSystemService(ShortcutManager.class);
//                ArrayList<String> arrayList = new ArrayList<>();
//                arrayList.add("sdid_" + did);
//                shortcutManager.removeDynamicShortcuts(arrayList);
//            } else {
//                int lower_id = (int) did;
//                int high_id = (int) (did >> 32);
//
//                User user = null;
//                TLRPC.Chat chat = null;
//                if (lower_id == 0) {
//                    TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(high_id);
//                    if (encryptedChat == null) {
//                        return;
//                    }
//                    user = getMessagesController().getUser(encryptedChat.user_id);
//                } else if (lower_id > 0) {
//                    user = getMessagesController().getUser(lower_id);
//                } else if (lower_id < 0) {
//                    chat = getMessagesController().getChat(-lower_id);
//                } else {
//                    return;
//                }
//                if (user == null && chat == null) {
//                    return;
//                }
//                String name;
//
//                if (user != null) {
//                    name = ContactsController.formatName(user.first_name, user.last_name);
//                } else {
//                    name = chat.title;
//                }
//
//                Intent addIntent = new Intent();
//                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, createIntrnalShortcutIntent(did));
//                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
//                addIntent.putExtra("duplicate", false);
//
//                addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
//                ApplicationLoader.applicationContext.sendBroadcast(addIntent);
//            }
//        } catch (Exception e) {
//            FileLog.e(e);
//        }
//    }
    //endregion ---------------- SEARCH END ----------------
}
