package com.demo.chat.messager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.controller.FileLoader;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.MediaController;
import com.demo.chat.controller.MessagesController;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 * 用户偏好设置
 *
 * 所有已登录的用户共用的设置
 */
public class SharedConfig {

    public static String pushString = "";
    public static String pushStringStatus = "";
    public static byte[] pushAuthKey;
    public static byte[] pushAuthKeyId;

    public static long directShareHash;

    public static boolean saveIncomingPhotos;
    public static String passcodeHash = "";
    public static long passcodeRetryInMs;
    public static long lastUptimeMillis;
    public static int badPasscodeTries;
    public static byte[] passcodeSalt = new byte[0];
    public static boolean appLocked;
    public static int passcodeType;
    public static int autoLockIn = 60 * 60;
    public static boolean allowScreenCapture;
    public static int lastPauseTime;
    public static boolean isWaitingForPasscodeEnter;
    public static boolean useFingerprint = true;
    public static String lastUpdateVersion;
    public static int suggestStickers;
    public static boolean loopStickers;
    public static int keepMedia = 2;
    public static int lastKeepMediaCheckTime;
    public static int searchMessagesAsListHintShows;
    public static int textSelectionHintShows;
    public static int scheduledOrNoSoundHintShows;
    public static int lockRecordAudioVideoHint;
    public static boolean searchMessagesAsListUsed;
    public static boolean stickersReorderingHintUsed;
    private static int lastLocalId = -210000;

    private static String passportConfigJson = "";
    private static HashMap<String, String> passportConfigMap;
    public static int passportConfigHash;

    private static boolean configLoaded;
    private static final Object sync = new Object();
    private static final Object localIdSync = new Object();

    public static boolean saveToGallery;
    public static int mapPreviewType = 2;
    public static boolean autoplayGifs = true;
    public static boolean autoplayVideo = true;
    public static boolean raiseToSpeak = true;
    public static boolean customTabs = true;
    public static boolean directShare = true;
    public static boolean inappCamera = true;
    public static boolean roundCamera16to9 = true;
    public static boolean noSoundHintShowed = false;
    public static boolean streamMedia = true;
    public static boolean streamAllVideo = false;
    public static boolean streamMkv = false;
    public static boolean saveStreamMedia = true;
    public static boolean smoothKeyboard = false;
    public static boolean pauseMusicOnRecord = true;
    public static boolean sortContactsByName;
    public static boolean sortFilesByName;
    public static boolean shuffleMusic;
    public static boolean playOrderReversed;
    public static boolean hasCameraCache;
    public static boolean showNotificationsForAllAccounts = true;
    public static int repeatMode;
    public static boolean allowBigEmoji;
    public static boolean useSystemEmoji;
    public static int fontSize = 16;
    public static int bubbleRadius = 10;
    public static int ivFontSize = 16;
    private static int devicePerformanceClass;

    public static boolean drawDialogIcons;
    public static boolean useThreeLinesLayout;
    public static boolean archiveHidden;

    public static int distanceSystemType;

    static {
        loadConfig();
    }

    public static class ProxyInfo {

        public String address;
        public int port;
        public String username;
        public String password;
        public String secret;

        public long proxyCheckPingId;
        public long ping;
        public boolean checking;
        public boolean available;
        public long availableCheckTime;

        public ProxyInfo(String a, int p, String u, String pw, String s) {
            address = a;
            port = p;
            username = u;
            password = pw;
            secret = s;
            if (address == null) {
                address = "";
            }
            if (password == null) {
                password = "";
            }
            if (username == null) {
                username = "";
            }
            if (secret == null) {
                secret = "";
            }
        }
    }

    public static ArrayList<ProxyInfo> proxyList = new ArrayList<>();
    private static boolean proxyListLoaded;
    public static ProxyInfo currentProxy;

    public static void saveConfig() {
        synchronized (sync) {
            try {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("saveIncomingPhotos", saveIncomingPhotos);
                editor.putString("passcodeHash1", passcodeHash);
                editor.putString("passcodeSalt", passcodeSalt.length > 0 ? Base64.encodeToString(passcodeSalt, Base64.DEFAULT) : "");
                editor.putBoolean("appLocked", appLocked);
                editor.putInt("passcodeType", passcodeType);
                editor.putLong("passcodeRetryInMs", passcodeRetryInMs);
                editor.putLong("lastUptimeMillis", lastUptimeMillis);
                editor.putInt("badPasscodeTries", badPasscodeTries);
                editor.putInt("autoLockIn", autoLockIn);
                editor.putInt("lastPauseTime", lastPauseTime);
                editor.putString("lastUpdateVersion2", lastUpdateVersion);
                editor.putBoolean("useFingerprint", useFingerprint);
                editor.putBoolean("allowScreenCapture", allowScreenCapture);
                editor.putString("pushString2", pushString);
                editor.putString("pushAuthKey", pushAuthKey != null ? Base64.encodeToString(pushAuthKey, Base64.DEFAULT) : "");
                editor.putInt("lastLocalId", lastLocalId);
                editor.putString("passportConfigJson", passportConfigJson);
                editor.putInt("passportConfigHash", passportConfigHash);
                editor.putBoolean("sortContactsByName", sortContactsByName);
                editor.putBoolean("sortFilesByName", sortFilesByName);
                editor.putInt("textSelectionHintShows", textSelectionHintShows);
                editor.putInt("scheduledOrNoSoundHintShows", scheduledOrNoSoundHintShows);
                editor.putInt("lockRecordAudioVideoHint", lockRecordAudioVideoHint);
                editor.commit();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public static int getLastLocalId() {
        int value;
        synchronized (localIdSync) {
            value = lastLocalId--;
        }
        return value;
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("userconfing", Context.MODE_PRIVATE);
            saveIncomingPhotos = preferences.getBoolean("saveIncomingPhotos", false);
            passcodeHash = preferences.getString("passcodeHash1", "");
            appLocked = preferences.getBoolean("appLocked", false);
            passcodeType = preferences.getInt("passcodeType", 0);
            passcodeRetryInMs = preferences.getLong("passcodeRetryInMs", 0);
            lastUptimeMillis = preferences.getLong("lastUptimeMillis", 0);
            badPasscodeTries = preferences.getInt("badPasscodeTries", 0);
            autoLockIn = preferences.getInt("autoLockIn", 60 * 60);
            lastPauseTime = preferences.getInt("lastPauseTime", 0);
            useFingerprint = preferences.getBoolean("useFingerprint", true);
            lastUpdateVersion = preferences.getString("lastUpdateVersion2", "3.5");
            allowScreenCapture = preferences.getBoolean("allowScreenCapture", false);
            lastLocalId = preferences.getInt("lastLocalId", -210000);
            pushString = preferences.getString("pushString2", "");
            passportConfigJson = preferences.getString("passportConfigJson", "");
            passportConfigHash = preferences.getInt("passportConfigHash", 0);
            String authKeyString = preferences.getString("pushAuthKey", null);
            if (!TextUtils.isEmpty(authKeyString)) {
                pushAuthKey = Base64.decode(authKeyString, Base64.DEFAULT);
            }

            if (passcodeHash.length() > 0 && lastPauseTime == 0) {
                lastPauseTime = (int) (SystemClock.elapsedRealtime() / 1000 - 60 * 10);
            }

            String passcodeSaltString = preferences.getString("passcodeSalt", "");
            if (passcodeSaltString.length() > 0) {
                passcodeSalt = Base64.decode(passcodeSaltString, Base64.DEFAULT);
            } else {
                passcodeSalt = new byte[0];
            }

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            saveToGallery = preferences.getBoolean("save_gallery", false);
            autoplayGifs = preferences.getBoolean("autoplay_gif", true);
            autoplayVideo = preferences.getBoolean("autoplay_video", true);
            mapPreviewType = preferences.getInt("mapPreviewType", 2);
            raiseToSpeak = preferences.getBoolean("raise_to_speak", true);
            customTabs = preferences.getBoolean("custom_tabs", true);
            directShare = preferences.getBoolean("direct_share", true);
            shuffleMusic = preferences.getBoolean("shuffleMusic", false);
            playOrderReversed = preferences.getBoolean("playOrderReversed", false);
            inappCamera = preferences.getBoolean("inappCamera", true);
            hasCameraCache = preferences.contains("cameraCache");
            roundCamera16to9 = true;//preferences.getBoolean("roundCamera16to9", false);
            repeatMode = preferences.getInt("repeatMode", 0);
            fontSize = preferences.getInt("fons_size", AndroidUtilities.isTablet() ? 18 : 16);
            bubbleRadius = preferences.getInt("bubbleRadius", 10);
            ivFontSize = preferences.getInt("iv_font_size", fontSize);
            allowBigEmoji = preferences.getBoolean("allowBigEmoji", true);
            useSystemEmoji = preferences.getBoolean("useSystemEmoji", false);
            streamMedia = preferences.getBoolean("streamMedia", true);
            saveStreamMedia = preferences.getBoolean("saveStreamMedia", true);
            smoothKeyboard = preferences.getBoolean("smoothKeyboard", false);
            pauseMusicOnRecord = preferences.getBoolean("pauseMusicOnRecord", true);
            streamAllVideo = preferences.getBoolean("streamAllVideo", BuildVars.DEBUG_VERSION);
            streamMkv = preferences.getBoolean("streamMkv", false);
            suggestStickers = preferences.getInt("suggestStickers", 0);
            sortContactsByName = preferences.getBoolean("sortContactsByName", false);
            sortFilesByName = preferences.getBoolean("sortFilesByName", false);
            noSoundHintShowed = preferences.getBoolean("noSoundHintShowed", false);
            directShareHash = preferences.getLong("directShareHash", 0);
            useThreeLinesLayout = preferences.getBoolean("useThreeLinesLayout", false);
            archiveHidden = preferences.getBoolean("archiveHidden", false);
            distanceSystemType = preferences.getInt("distanceSystemType", 0);
            devicePerformanceClass = preferences.getInt("devicePerformanceClass", -1);
            loopStickers = preferences.getBoolean("loopStickers", true);
            keepMedia = preferences.getInt("keep_media", 2);
            lastKeepMediaCheckTime = preferences.getInt("lastKeepMediaCheckTime", 0);
            searchMessagesAsListHintShows = preferences.getInt("searchMessagesAsListHintShows", 0);
            searchMessagesAsListUsed = preferences.getBoolean("searchMessagesAsListUsed", false);
            stickersReorderingHintUsed = preferences.getBoolean("stickersReorderingHintUsed", false);
            textSelectionHintShows = preferences.getInt("textSelectionHintShows", 0);
            scheduledOrNoSoundHintShows = preferences.getInt("scheduledOrNoSoundHintShows", 0);
            lockRecordAudioVideoHint = preferences.getInt("lockRecordAudioVideoHint", 0);
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            showNotificationsForAllAccounts = preferences.getBoolean("AllAccounts", true);

            configLoaded = true;
        }
    }

    public static void increaseBadPasscodeTries() {
        SharedConfig.badPasscodeTries++;
        if (badPasscodeTries >= 3) {
            switch (SharedConfig.badPasscodeTries) {
                case 3:
                    passcodeRetryInMs = 5000;
                    break;
                case 4:
                    passcodeRetryInMs = 10000;
                    break;
                case 5:
                    passcodeRetryInMs = 15000;
                    break;
                case 6:
                    passcodeRetryInMs = 20000;
                    break;
                case 7:
                    passcodeRetryInMs = 25000;
                    break;
                default:
                    passcodeRetryInMs = 30000;
                    break;
            }
            SharedConfig.lastUptimeMillis = SystemClock.elapsedRealtime();
        }
        saveConfig();
    }

    public static boolean isPassportConfigLoaded() {
        return passportConfigMap != null;
    }

    public static void setPassportConfig(String json, int hash) {
        passportConfigMap = null;
        passportConfigJson = json;
        passportConfigHash = hash;
        saveConfig();
        getCountryLangs();
    }

    public static HashMap<String, String> getCountryLangs() {
        if (passportConfigMap == null) {
            passportConfigMap = new HashMap<>();
            try {
                JSONObject object = new JSONObject(passportConfigJson);
                Iterator<String> iter = object.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    passportConfigMap.put(key.toUpperCase(), object.getString(key).toUpperCase());
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return passportConfigMap;
    }

    public static boolean checkPasscode(String passcode) {
        if (passcodeSalt.length == 0) {
            boolean result = Utilities.MD5(passcode).equals(passcodeHash);
            if (result) {
                try {
                    passcodeSalt = new byte[16];
                    Utilities.random.nextBytes(passcodeSalt);
                    byte[] passcodeBytes = passcode.getBytes("UTF-8");
                    byte[] bytes = new byte[32 + passcodeBytes.length];
                    System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
                    System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                    System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                    passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
                    saveConfig();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return result;
        } else {
            try {
                byte[] passcodeBytes = passcode.getBytes("UTF-8");
                byte[] bytes = new byte[32 + passcodeBytes.length];
                System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
                System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
                System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
                String hash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
                return passcodeHash.equals(hash);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return false;
    }

    public static void clearConfig() {
        saveIncomingPhotos = false;
        appLocked = false;
        passcodeType = 0;
        passcodeRetryInMs = 0;
        lastUptimeMillis = 0;
        badPasscodeTries = 0;
        passcodeHash = "";
        passcodeSalt = new byte[0];
        autoLockIn = 60 * 60;
        lastPauseTime = 0;
        useFingerprint = true;
        isWaitingForPasscodeEnter = false;
        allowScreenCapture = false;
        lastUpdateVersion = BuildVars.BUILD_VERSION_STRING;
        textSelectionHintShows = 0;
        scheduledOrNoSoundHintShows = 0;
        lockRecordAudioVideoHint = 0;
        saveConfig();
    }

    public static void setSuggestStickers(int type) {
        suggestStickers = type;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("suggestStickers", suggestStickers);
        editor.commit();
    }

    public static void setSearchMessagesAsListUsed(boolean searchMessagesAsListUsed) {
        SharedConfig.searchMessagesAsListUsed = searchMessagesAsListUsed;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("searchMessagesAsListUsed", searchMessagesAsListUsed);
        editor.commit();
    }

    public static void setStickersReorderingHintUsed(boolean stickersReorderingHintUsed) {
        SharedConfig.stickersReorderingHintUsed = stickersReorderingHintUsed;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("stickersReorderingHintUsed", stickersReorderingHintUsed);
        editor.commit();
    }

    public static void increaseTextSelectionHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("textSelectionHintShows", ++textSelectionHintShows);
        editor.commit();
    }

    public static void removeTextSelectionHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("textSelectionHintShows", 3);
        editor.commit();
    }

    public static void increaseScheduledOrNoSuoundHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("scheduledOrNoSoundHintShows", ++scheduledOrNoSoundHintShows);
        editor.commit();
    }

    public static void removeScheduledOrNoSuoundHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("scheduledOrNoSoundHintShows", 3);
        editor.commit();
    }

    public static void increaseLockRecordAudioVideoHintShowed() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("lockRecordAudioVideoHint", ++lockRecordAudioVideoHint);
        editor.commit();
    }

    public static void removeLockRecordAudioVideoHint() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("lockRecordAudioVideoHint", 3);
        editor.commit();
    }

    public static void increaseSearchAsListHintShows() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("searchMessagesAsListHintShows", ++searchMessagesAsListHintShows);
        editor.commit();
    }

    public static void setKeepMedia(int value) {
        keepMedia = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("keep_media", keepMedia);
        editor.commit();
    }

    public static void checkKeepMedia() {
        int time = (int) (System.currentTimeMillis() / 1000);
        if (Math.abs(time - lastKeepMediaCheckTime) < 60 * 60) {
            return;
        }
        lastKeepMediaCheckTime = time;
        File cacheDir = FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE);
        Utilities.globalQueue.postRunnable(() -> {
            if (keepMedia != 2) {
                int days;
                if (keepMedia == 0) {
                    days = 7;
                } else if (keepMedia == 1) {
                    days = 30;
                } else {
                    days = 3;
                }
                long currentTime = time - 60 * 60 * 24 * days;
                final SparseArray<File> paths = ImageLoader.getInstance().createMediaPaths();
                for (int a = 0; a < paths.size(); a++) {
                    if (paths.keyAt(a) == FileLoader.MEDIA_DIR_CACHE) {
                        continue;
                    }
                    try {
                        Utilities.clearDir(paths.valueAt(a).getAbsolutePath(), 0, currentTime, false);
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }
            File stickersPath = new File(cacheDir, "acache");
            if (stickersPath.exists()) {
                long currentTime = time - 60 * 60 * 24;
                try {
                    Utilities.clearDir(stickersPath.getAbsolutePath(), 0, currentTime, false);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("lastKeepMediaCheckTime", lastKeepMediaCheckTime);
            editor.commit();
        });
    }

    public static void toggleLoopStickers() {
        loopStickers = !loopStickers;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("loopStickers", loopStickers);
        editor.commit();
    }

    public static void toggleBigEmoji() {
        allowBigEmoji = !allowBigEmoji;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("allowBigEmoji", allowBigEmoji);
        editor.commit();
    }

    public static void toggleShuffleMusic(int type) {
        if (type == 2) {
            shuffleMusic = !shuffleMusic;
        } else {
            playOrderReversed = !playOrderReversed;
        }
        MediaController.getInstance().checkIsNextMediaFileDownloaded();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("shuffleMusic", shuffleMusic);
        editor.putBoolean("playOrderReversed", playOrderReversed);
        editor.commit();
    }

    public static void toggleRepeatMode() {
        repeatMode++;
        if (repeatMode > 2) {
            repeatMode = 0;
        }
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("repeatMode", repeatMode);
        editor.commit();
    }

    public static void toggleSaveToGallery() {
        saveToGallery = !saveToGallery;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("save_gallery", saveToGallery);
        editor.commit();
        checkSaveToGalleryFiles();
    }

    public static void toggleAutoplayGifs() {
        autoplayGifs = !autoplayGifs;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("autoplay_gif", autoplayGifs);
        editor.commit();
    }

    public static void setUseThreeLinesLayout(boolean value) {
        useThreeLinesLayout = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("useThreeLinesLayout", useThreeLinesLayout);
        editor.commit();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload, true);
    }

    public static void toggleArchiveHidden() {
        archiveHidden = !archiveHidden;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("archiveHidden", archiveHidden);
        editor.commit();
    }

    public static void toggleAutoplayVideo() {
        autoplayVideo = !autoplayVideo;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("autoplay_video", autoplayVideo);
        editor.commit();
    }

    public static boolean isSecretMapPreviewSet() {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        return preferences.contains("mapPreviewType");
    }

    public static void setSecretMapPreviewType(int value) {
        mapPreviewType = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("mapPreviewType", mapPreviewType);
        editor.commit();
    }

    public static void setNoSoundHintShowed(boolean value) {
        if (noSoundHintShowed == value) {
            return;
        }
        noSoundHintShowed = value;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("noSoundHintShowed", noSoundHintShowed);
        editor.commit();
    }

    public static void toogleRaiseToSpeak() {
        raiseToSpeak = !raiseToSpeak;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("raise_to_speak", raiseToSpeak);
        editor.commit();
    }

    public static void toggleCustomTabs() {
        customTabs = !customTabs;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("custom_tabs", customTabs);
        editor.commit();
    }

    public static void toggleDirectShare() {
        directShare = !directShare;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("direct_share", directShare);
        editor.commit();
    }

    public static void toggleStreamMedia() {
        streamMedia = !streamMedia;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("streamMedia", streamMedia);
        editor.commit();
    }

    public static void toggleSortContactsByName() {
        sortContactsByName = !sortContactsByName;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("sortContactsByName", sortContactsByName);
        editor.commit();
    }

    public static void toggleSortFilesByName() {
        sortFilesByName = !sortFilesByName;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("sortFilesByName", sortFilesByName);
        editor.commit();
    }

    public static void toggleStreamAllVideo() {
        streamAllVideo = !streamAllVideo;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("streamAllVideo", streamAllVideo);
        editor.commit();
    }

    public static void toggleStreamMkv() {
        streamMkv = !streamMkv;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("streamMkv", streamMkv);
        editor.commit();
    }

    public static void toggleSaveStreamMedia() {
        saveStreamMedia = !saveStreamMedia;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("saveStreamMedia", saveStreamMedia);
        editor.commit();
    }

    public static void toggleSmoothKeyboard() {
        smoothKeyboard = !smoothKeyboard;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("smoothKeyboard", smoothKeyboard);
        editor.commit();
    }

    public static void togglePauseMusicOnRecord() {
        pauseMusicOnRecord = !pauseMusicOnRecord;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("pauseMusicOnRecord", pauseMusicOnRecord);
        editor.commit();
    }

    public static void toggleInappCamera() {
        inappCamera = !inappCamera;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("inappCamera", inappCamera);
        editor.commit();
    }

    public static void toggleRoundCamera16to9() {
        roundCamera16to9 = !roundCamera16to9;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("roundCamera16to9", roundCamera16to9);
        editor.commit();
    }

    public static void setDistanceSystemType(int type) {
        distanceSystemType = type;
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("distanceSystemType", distanceSystemType);
        editor.commit();
        LocaleController.resetImperialSystemType();
    }

    public static void checkSaveToGalleryFiles() {
        try {
            File telegramPath = new File(Environment.getExternalStorageDirectory(), "Telegram");
            File imagePath = new File(telegramPath, "Telegram Images");
            imagePath.mkdir();
            File videoPath = new File(telegramPath, "Telegram Video");
            videoPath.mkdir();

            if (saveToGallery) {
                if (imagePath.isDirectory()) {
                    new File(imagePath, ".nomedia").delete();
                }
                if (videoPath.isDirectory()) {
                    new File(videoPath, ".nomedia").delete();
                }
            } else {
                if (imagePath.isDirectory()) {
                    AndroidUtilities.createEmptyFile(new File(imagePath, ".nomedia"));
                }
                if (videoPath.isDirectory()) {
                    AndroidUtilities.createEmptyFile(new File(videoPath, ".nomedia"));
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public final static int PERFORMANCE_CLASS_LOW = 0;
    public final static int PERFORMANCE_CLASS_AVERAGE = 1;
    public final static int PERFORMANCE_CLASS_HIGH = 2;

    public static int getDevicePerfomanceClass() {
        if (devicePerformanceClass == -1) {
            int maxCpuFreq = -1;
            try {
                RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
                String line = reader.readLine();
                if (line != null) {
                    maxCpuFreq = Utilities.parseInt(line) / 1000;
                }
                reader.close();
            } catch (Throwable ignore) {

            }
            int androidVersion = Build.VERSION.SDK_INT;
            int cpuCount = CPU_COUNT;
            int memoryClass = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
            if (androidVersion < 21 || cpuCount <= 2 || memoryClass <= 100 || cpuCount <= 4 && maxCpuFreq != -1 && maxCpuFreq <= 1250 || cpuCount <= 4 && maxCpuFreq <= 1600 && memoryClass <= 128 && androidVersion <= 21 || cpuCount <= 4 && maxCpuFreq <= 1300 && memoryClass <= 128 && androidVersion <= 24) {
                devicePerformanceClass = PERFORMANCE_CLASS_LOW;
            } else if (cpuCount < 8 || memoryClass <= 160 || maxCpuFreq != -1 && maxCpuFreq <= 1650 || maxCpuFreq == -1 && cpuCount == 8 && androidVersion <= 23) {
                devicePerformanceClass = PERFORMANCE_CLASS_AVERAGE;
            } else {
                devicePerformanceClass = PERFORMANCE_CLASS_HIGH;
            }
            if (BuildVars.DEBUG_VERSION) {
                FileLog.d("device performance info (cpu_count = " + cpuCount + ", freq = " + maxCpuFreq + ", memoryClass = " + memoryClass + ", android version " + androidVersion + ")");
            }
        }

        return devicePerformanceClass;
    }
    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

}

