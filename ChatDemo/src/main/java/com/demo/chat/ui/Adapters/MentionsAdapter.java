package com.demo.chat.ui.Adapters;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.demo.chat.R;
import com.demo.chat.controller.LocaleController;
import com.demo.chat.controller.MediaDataController;
import com.demo.chat.controller.MessagesController;
import com.demo.chat.controller.MessagesStorage;
import com.demo.chat.controller.SendMessagesHelper;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.model.Chat;
import com.demo.chat.model.action.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.model.action.UserObject;
import com.demo.chat.model.action.ChatObject;
import com.demo.chat.model.bot.BotInfo;
import com.demo.chat.model.bot.BotInlineResult;
import com.demo.chat.model.bot.InlineBotSwitchPM;
import com.demo.chat.theme.Theme;
import com.demo.chat.ui.ActionBar.AlertDialog;
import com.demo.chat.ui.Cells.BotSwitchCell;
import com.demo.chat.ui.Cells.ContextLinkCell;
import com.demo.chat.ui.Cells.MentionCell;
import com.demo.chat.ui.ChatActivity;
import com.demo.chat.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class MentionsAdapter extends RecyclerListView.SelectionAdapter {

    public interface MentionsAdapterDelegate {
        void needChangePanelVisibility(boolean show);
        void onContextSearch(boolean searching);
        void onContextClick(BotInlineResult result);
    }

    private int currentAccount = UserConfig.selectedAccount;
    private Context mContext;
    private long dialog_id;
    private Chat info;
    private SearchAdapterHelper searchAdapterHelper;
    private ArrayList<User> searchResultUsernames;
    private SparseArray<User> searchResultUsernamesMap;
    private Runnable searchGlobalRunnable;
    private ArrayList<String> searchResultHashtags;
    private ArrayList<String> searchResultCommands;
    private ArrayList<String> searchResultCommandsHelp;
    private ArrayList<MediaDataController.KeywordResult> searchResultSuggestions;
    private String[] lastSearchKeyboardLanguage;
    private ArrayList<User> searchResultCommandsUsers;
    private ArrayList<BotInlineResult> searchResultBotContext;
    private InlineBotSwitchPM searchResultBotContextSwitch;
    private MentionsAdapter.MentionsAdapterDelegate delegate;
    private SparseArray<BotInfo> botInfo;
    private int resultStartPosition;
    private int resultLength;
    private String lastText;
    private boolean lastUsernameOnly;
    private int lastPosition;
    private ArrayList<MessageObject> messages;
    private boolean needUsernames = true;
    private boolean needBotContext = true;
    private boolean isDarkTheme;
    private int botsCount;
    private boolean inlineMediaEnabled = true;
    private int channelLastReqId;
    private int channelReqId;
    private boolean isSearchingMentions;

    private final static String punctuationsChars = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~\n";

    private Runnable cancelDelayRunnable;

    private String searchingContextUsername;
    private String searchingContextQuery;
    private String nextQueryOffset;
    private int contextUsernameReqid;
    private int contextQueryReqid;
    private boolean noUserName;
    private User foundContextBot;
    private boolean contextMedia;
    private Runnable contextQueryRunnable;
    private Location lastKnownLocation;

    private ChatActivity parentFragment;

    private SendMessagesHelper.LocationProvider locationProvider = new SendMessagesHelper.LocationProvider(new SendMessagesHelper.LocationProvider.LocationProviderDelegate() {
        @Override
        public void onLocationAcquired(Location location) {
            if (foundContextBot != null && foundContextBot.bot_inline_geo) {
                lastKnownLocation = location;
                searchForContextBotResults(true, foundContextBot, searchingContextQuery, "");
            }
        }

        @Override
        public void onUnableLocationAcquire() {
            onLocationUnavailable();
        }
    }) {
        @Override
        public void stop() {
            super.stop();
            lastKnownLocation = null;
        }
    };

    public MentionsAdapter(Context context, boolean darkTheme, long did, MentionsAdapter.MentionsAdapterDelegate mentionsAdapterDelegate) {
        mContext = context;
        delegate = mentionsAdapterDelegate;
        isDarkTheme = darkTheme;
        dialog_id = did;
        searchAdapterHelper = new SearchAdapterHelper(true);
        searchAdapterHelper.setDelegate(new SearchAdapterHelper.SearchAdapterHelperDelegate() {
            @Override
            public void onDataSetChanged(int searchId) {
                notifyDataSetChanged();
            }

            @Override
            public void onSetHashtags(ArrayList<SearchAdapterHelper.HashtagObject> arrayList, HashMap<String, SearchAdapterHelper.HashtagObject> hashMap) {
                if (lastText != null) {
                    searchUsernameOrHashtag(lastText, lastPosition, messages, lastUsernameOnly);
                }
            }
        });
    }

    public void onDestroy() {
        if (locationProvider != null) {
            locationProvider.stop();
        }
        if (contextQueryRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(contextQueryRunnable);
            contextQueryRunnable = null;
        }
//        if (contextUsernameReqid != 0) {
//            ConnectionsManager.getInstance(currentAccount).cancelRequest(contextUsernameReqid, true);
//            contextUsernameReqid = 0;
//        }
//        if (contextQueryReqid != 0) {
//            ConnectionsManager.getInstance(currentAccount).cancelRequest(contextQueryReqid, true);
//            contextQueryReqid = 0;
//        }TODO 取消请求
        foundContextBot = null;
        inlineMediaEnabled = true;
        searchingContextUsername = null;
        searchingContextQuery = null;
        noUserName = false;
    }

    public void setParentFragment(ChatActivity fragment) {
        parentFragment = fragment;
    }

    public void setChatInfo(Chat chatInfo) {
        currentAccount = UserConfig.selectedAccount;
        info = chatInfo;
        if (!inlineMediaEnabled && foundContextBot != null && parentFragment != null) {
            Chat chat = parentFragment.getCurrentChat();
            if (chat != null) {
                inlineMediaEnabled = ChatObject.canSendStickers(chat);
                if (inlineMediaEnabled) {
                    searchResultUsernames = null;
                    notifyDataSetChanged();
                    delegate.needChangePanelVisibility(false);
                    processFoundUser(foundContextBot);
                }
            }
        }
        if (lastText != null) {
            searchUsernameOrHashtag(lastText, lastPosition, messages, lastUsernameOnly);
        }
    }

    public void setNeedUsernames(boolean value) {
        needUsernames = value;
    }

    public void setNeedBotContext(boolean value) {
        needBotContext = value;
    }

    public void setBotInfo(SparseArray<BotInfo> info) {
        botInfo = info;
    }

    public void setBotsCount(int count) {
        botsCount = count;
    }

    public void clearRecentHashtags() {
        searchAdapterHelper.clearRecentHashtags();
        searchResultHashtags.clear();
        notifyDataSetChanged();
        if (delegate != null) {
            delegate.needChangePanelVisibility(false);
        }
    }

    public InlineBotSwitchPM getBotContextSwitch() {
        return searchResultBotContextSwitch;
    }

    public int getContextBotId() {
        return foundContextBot != null ? foundContextBot.id : 0;
    }

    public User getContextBotUser() {
        return foundContextBot;
    }

    public String getContextBotName() {
        return foundContextBot != null ? foundContextBot.username : "";
    }

    private void processFoundUser(User user) {
        contextUsernameReqid = 0;
        locationProvider.stop();
        if (user != null && user.bot && user.bot_inline_placeholder != null) {
            foundContextBot = user;
            if (parentFragment != null) {
                Chat chat = parentFragment.getCurrentChat();
                if (chat != null) {
                    inlineMediaEnabled = ChatObject.canSendStickers(chat);
                    if (!inlineMediaEnabled) {
                        notifyDataSetChanged();
                        delegate.needChangePanelVisibility(true);
                        return;
                    }
                }
            }
            if (foundContextBot.bot_inline_geo) {
                SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                boolean allowGeo = preferences.getBoolean("inlinegeo_" + foundContextBot.id, false);
                if (!allowGeo && parentFragment != null && parentFragment.getParentActivity() != null) {
                    final User foundContextBotFinal = foundContextBot;
                    AlertDialog.Builder builder = new AlertDialog.Builder(parentFragment.getParentActivity());
                    builder.setTitle(LocaleController.getString("ShareYouLocationTitle", R.string.ShareYouLocationTitle));
                    builder.setMessage(LocaleController.getString("ShareYouLocationInline", R.string.ShareYouLocationInline));
                    final boolean[] buttonClicked = new boolean[1];
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
                        buttonClicked[0] = true;
                        if (foundContextBotFinal != null) {
                            SharedPreferences preferences1 = MessagesController.getNotificationsSettings(currentAccount);
                            preferences1.edit().putBoolean("inlinegeo_" + foundContextBotFinal.id, true).commit();
                            checkLocationPermissionsOrStart();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialog, which) -> {
                        buttonClicked[0] = true;
                        onLocationUnavailable();
                    });
                    parentFragment.showDialog(builder.create(), dialog -> {
                        if (!buttonClicked[0]) {
                            onLocationUnavailable();
                        }
                    });
                } else {
                    checkLocationPermissionsOrStart();
                }
            }
        } else {
            foundContextBot = null;
            inlineMediaEnabled = true;
        }
        if (foundContextBot == null) {
            noUserName = true;
        } else {
            if (delegate != null) {
                delegate.onContextSearch(true);
            }
            searchForContextBotResults(true, foundContextBot, searchingContextQuery, "");
        }
    }

    private void searchForContextBot(final String username, final String query) {
        if (foundContextBot != null && foundContextBot.username != null && foundContextBot.username.equals(username) && searchingContextQuery != null && searchingContextQuery.equals(query)) {
            return;
        }
        searchResultBotContext = null;
        searchResultBotContextSwitch = null;
        notifyDataSetChanged();
        if (foundContextBot != null) {
            if (!inlineMediaEnabled && username != null && query != null) {
                return;
            }
            delegate.needChangePanelVisibility(false);
        }
        if (contextQueryRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(contextQueryRunnable);
            contextQueryRunnable = null;
        }
        if (TextUtils.isEmpty(username) || searchingContextUsername != null && !searchingContextUsername.equals(username)) {
//            if (contextUsernameReqid != 0) {
//                ConnectionsManager.getInstance(currentAccount).cancelRequest(contextUsernameReqid, true);
//                contextUsernameReqid = 0;
//            }
//            if (contextQueryReqid != 0) {
//                ConnectionsManager.getInstance(currentAccount).cancelRequest(contextQueryReqid, true);
//                contextQueryReqid = 0;
//            }TODO 取消请求
            foundContextBot = null;
            inlineMediaEnabled = true;
            searchingContextUsername = null;
            searchingContextQuery = null;
            locationProvider.stop();
            noUserName = false;
            if (delegate != null) {
                delegate.onContextSearch(false);
            }
            if (username == null || username.length() == 0) {
                return;
            }
        }
        if (query == null) {
//            if (contextQueryReqid != 0) {
//                ConnectionsManager.getInstance(currentAccount).cancelRequest(contextQueryReqid, true);
//                contextQueryReqid = 0;
//            }TODO 取消请求
            searchingContextQuery = null;
            if (delegate != null) {
                delegate.onContextSearch(false);
            }
            return;
        }
        if (delegate != null) {
            if (foundContextBot != null) {
                delegate.onContextSearch(true);
            } else if (username.equals("gif")) {
                searchingContextUsername = "gif";
                delegate.onContextSearch(false);
            }
        }
        final MessagesController messagesController = MessagesController.getInstance(currentAccount);
        final MessagesStorage messagesStorage = MessagesStorage.getInstance(currentAccount);
        searchingContextQuery = query;
        contextQueryRunnable = new Runnable() {
            @Override
            public void run() {
                if (contextQueryRunnable != this) {
                    return;
                }
                contextQueryRunnable = null;
                if (foundContextBot != null || noUserName) {
                    if (noUserName) {
                        return;
                    }
                    searchForContextBotResults(true, foundContextBot, query, "");
                } else {
                    searchingContextUsername = username;
//                    TLObject object = messagesController.getUserOrChat(searchingContextUsername);
//                    if (object instanceof User) {
//                        processFoundUser((User) object);
//                    } else {
//                        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
//                        req.username = searchingContextUsername;
//                        contextUsernameReqid = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                            if (searchingContextUsername == null || !searchingContextUsername.equals(username)) {
//                                return;
//                            }
//                            User user = null;
//                            if (error == null) {
//                                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
//                                if (!res.users.isEmpty()) {
//                                    user = res.users.get(0);
//                                    messagesController.putUser(user, false);
//                                    messagesStorage.putUsersAndChats(res.users, null, true, true);
//                                }
//                            }
//                            processFoundUser(user);
//                        }));
//                    }TODO 发起请求
                }
            }
        };
        AndroidUtilities.runOnUIThread(contextQueryRunnable, 400);
    }

    private void onLocationUnavailable() {
        if (foundContextBot != null && foundContextBot.bot_inline_geo) {
            lastKnownLocation = new Location("network");
            lastKnownLocation.setLatitude(-1000);
            lastKnownLocation.setLongitude(-1000);
            searchForContextBotResults(true, foundContextBot, searchingContextQuery, "");
        }
    }

    private void checkLocationPermissionsOrStart() {
        if (parentFragment == null || parentFragment.getParentActivity() == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && parentFragment.getParentActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            parentFragment.getParentActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            return;
        }
        if (foundContextBot != null && foundContextBot.bot_inline_geo) {
            locationProvider.start();
        }
    }

    public void setSearchingMentions(boolean value) {
        isSearchingMentions = value;
    }

    public String getBotCaption() {
        if (foundContextBot != null) {
            return foundContextBot.bot_inline_placeholder;
        } else if (searchingContextUsername != null && searchingContextUsername.equals("gif")) {
            return "Search GIFs";
        }
        return null;
    }

    public void searchForContextBotForNextOffset() {
        if (contextQueryReqid != 0 || nextQueryOffset == null || nextQueryOffset.length() == 0 || foundContextBot == null || searchingContextQuery == null) {
            return;
        }
        searchForContextBotResults(true, foundContextBot, searchingContextQuery, nextQueryOffset);
    }

    private void searchForContextBotResults(final boolean cache, final User user, final String query, final String offset) {
//        if (contextQueryReqid != 0) {
//            ConnectionsManager.getInstance(currentAccount).cancelRequest(contextQueryReqid, true);
//            contextQueryReqid = 0;
//        }TODO 取消请求
        if (!inlineMediaEnabled) {
            if (delegate != null) {
                delegate.onContextSearch(false);
            }
            return;
        }
        if (query == null || user == null) {
            searchingContextQuery = null;
            return;
        }
        if (user.bot_inline_geo && lastKnownLocation == null) {
            return;
        }
        final String key = dialog_id + "_" + query + "_" + offset + "_" + dialog_id + "_" + user.id + "_" + (user.bot_inline_geo && lastKnownLocation != null && lastKnownLocation.getLatitude() != -1000 ? lastKnownLocation.getLatitude() + lastKnownLocation.getLongitude() : "");
        final MessagesStorage messagesStorage = MessagesStorage.getInstance(currentAccount);
//        RequestDelegate requestDelegate = (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//            if (!query.equals(searchingContextQuery)) {
//                return;
//            }
//            contextQueryReqid = 0;
//            if (cache && response == null) {
//                searchForContextBotResults(false, user, query, offset);
//            } else if (delegate != null) {
//                delegate.onContextSearch(false);
//            }
//            if (response instanceof TLRPC.TL_messages_botResults) {
//                TLRPC.TL_messages_botResults res = (TLRPC.TL_messages_botResults) response;
//                if (!cache && res.cache_time != 0) {
//                    messagesStorage.saveBotCache(key, res);
//                }
//                nextQueryOffset = res.next_offset;
//                if (searchResultBotContextSwitch == null) {
//                    searchResultBotContextSwitch = res.switch_pm;
//                }
//                for (int a = 0; a < res.results.size(); a++) {
//                    BotInlineResult result = res.results.get(a);
//                    if (!(result.document instanceof TLRPC.TL_document) && !(result.photo instanceof TLRPC.TL_photo) && !"game".equals(result.type) && result.content == null && result.send_message instanceof TLRPC.TL_botInlineMessageMediaAuto) {
//                        res.results.remove(a);
//                        a--;
//                    }
//                    result.query_id = res.query_id;
//                }
//                boolean added = false;
//                if (searchResultBotContext == null || offset.length() == 0) {
//                    searchResultBotContext = res.results;
//                    contextMedia = res.gallery;
//                } else {
//                    added = true;
//                    searchResultBotContext.addAll(res.results);
//                    if (res.results.isEmpty()) {
//                        nextQueryOffset = "";
//                    }
//                }
//                if (cancelDelayRunnable != null) {
//                    AndroidUtilities.cancelRunOnUIThread(cancelDelayRunnable);
//                    cancelDelayRunnable = null;
//                }
//                searchResultHashtags = null;
//                searchResultUsernames = null;
//                searchResultUsernamesMap = null;
//                searchResultCommands = null;
//                searchResultSuggestions = null;
//                searchResultCommandsHelp = null;
//                searchResultCommandsUsers = null;
//                if (added) {
//                    boolean hasTop = searchResultBotContextSwitch != null;
//                    notifyItemChanged(searchResultBotContext.size() - res.results.size() + (hasTop ? 1 : 0) - 1);
//                    notifyItemRangeInserted(searchResultBotContext.size() - res.results.size() + (hasTop ? 1 : 0), res.results.size());
//                } else {
//                    notifyDataSetChanged();
//                }
//                delegate.needChangePanelVisibility(!searchResultBotContext.isEmpty() || searchResultBotContextSwitch != null);
//            }
//        });

//        if (cache) {
//            messagesStorage.getBotCache(key, requestDelegate);
//        } else {TODO 取消请求
//            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
//            req.bot = MessagesController.getInstance(currentAccount).getInputUser(user);
//            req.query = query;
//            req.offset = offset;
//            if (user.bot_inline_geo && lastKnownLocation != null && lastKnownLocation.getLatitude() != -1000) {
//                req.flags |= 1;
//                req.geo_point = new TLRPC.TL_inputGeoPoint();
//                req.geo_point.lat = AndroidUtilities.fixLocationCoord(lastKnownLocation.getLatitude());
//                req.geo_point._long = AndroidUtilities.fixLocationCoord(lastKnownLocation.getLongitude());
//            }
//            int lower_id = (int) dialog_id;
//            int high_id = (int) (dialog_id >> 32);
//            if (lower_id != 0) {
//                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(lower_id);
//            } else {
//                req.peer = new TLRPC.TL_inputPeerEmpty();
//            }
//            contextQueryReqid = ConnectionsManager.getInstance(currentAccount).sendRequest(req, requestDelegate, ConnectionsManager.RequestFlagFailOnServerErrors);
//        }
    }

    public void searchUsernameOrHashtag(String text, int position, ArrayList<MessageObject> messageObjects, boolean usernameOnly) {
        if (cancelDelayRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(cancelDelayRunnable);
            cancelDelayRunnable = null;
        }
//        if (channelReqId != 0) {
//            ConnectionsManager.getInstance(currentAccount).cancelRequest(channelReqId, true);
//            channelReqId = 0;
//        }TODO 取消请求
        if (searchGlobalRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(searchGlobalRunnable);
            searchGlobalRunnable = null;
        }
        if (TextUtils.isEmpty(text)) {
            searchForContextBot(null, null);
            delegate.needChangePanelVisibility(false);
            lastText = null;
            return;
        }
        int searchPostion = position;
        if (text.length() > 0) {
            searchPostion--;
        }
        lastText = null;
        lastUsernameOnly = usernameOnly;
        StringBuilder result = new StringBuilder();
        int foundType = -1;
        if (!usernameOnly && needBotContext && text.charAt(0) == '@') {
            int index = text.indexOf(' ');
            int len = text.length();
            String username = null;
            String query = null;
            if (index > 0) {
                username = text.substring(1, index);
                query = text.substring(index + 1);
            } else if (text.charAt(len - 1) == 't' && text.charAt(len - 2) == 'o' && text.charAt(len - 3) == 'b') {
                username = text.substring(1);
                query = "";
            } else {
                searchForContextBot(null, null);
            }
            if (username != null && username.length() >= 1) {
                for (int a = 1; a < username.length(); a++) {
                    char ch = username.charAt(a);
                    if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_')) {
                        username = "";
                        break;
                    }
                }
            } else {
                username = "";
            }
            searchForContextBot(username, query);
        } else {
            searchForContextBot(null, null);
        }
        if (foundContextBot != null) {
            return;
        }
        final MessagesController messagesController = MessagesController.getInstance(currentAccount);
        int dogPostion = -1;
        if (usernameOnly) {
            result.append(text.substring(1));
            resultStartPosition = 0;
            resultLength = result.length();
            foundType = 0;
        } else {
            for (int a = searchPostion; a >= 0; a--) {
                if (a >= text.length()) {
                    continue;
                }
                char ch = text.charAt(a);
                if (a == 0 || text.charAt(a - 1) == ' ' || text.charAt(a - 1) == '\n' || ch == ':') {
                    if (ch == '@') {
                        if (needUsernames || needBotContext && a == 0) {
                            if (info == null && a != 0) {
                                lastText = text;
                                lastPosition = position;
                                messages = messageObjects;
                                delegate.needChangePanelVisibility(false);
                                return;
                            }
                            dogPostion = a;
                            foundType = 0;
                            resultStartPosition = a;
                            resultLength = result.length() + 1;
                            break;
                        }
                    } else if (ch == '#') {
                        if (searchAdapterHelper.loadRecentHashtags()) {
                            foundType = 1;
                            resultStartPosition = a;
                            resultLength = result.length() + 1;
                            result.insert(0, ch);
                            break;
                        } else {
                            lastText = text;
                            lastPosition = position;
                            messages = messageObjects;
                            delegate.needChangePanelVisibility(false);
                            return;
                        }
                    } else if (a == 0 && botInfo != null && ch == '/') {
                        foundType = 2;
                        resultStartPosition = a;
                        resultLength = result.length() + 1;
                        break;
                    } else if (ch == ':' && result.length() > 0) {
                        boolean isNextPunctiationChar = punctuationsChars.indexOf(result.charAt(0)) >= 0;
                        if (!isNextPunctiationChar || result.length() > 1) {
                            foundType = 3;
                            resultStartPosition = a;
                            resultLength = result.length() + 1;
                            break;
                        }
                    }
                }
                result.insert(0, ch);
            }
        }
        if (foundType == -1) {
            delegate.needChangePanelVisibility(false);
            return;
        }
        if (foundType == 0) {
            final ArrayList<Integer> users = new ArrayList<>();
            for (int a = 0; a < Math.min(100, messageObjects.size()); a++) {
                int from_id = messageObjects.get(a).messageOwner.from_id;
                if (!users.contains(from_id)) {
                    users.add(from_id);
                }
            }
            final String usernameString = result.toString().toLowerCase();
            boolean hasSpace = usernameString.indexOf(' ') >= 0;
            ArrayList<User> newResult = new ArrayList<>();
            final SparseArray<User> newResultsHashMap = new SparseArray<>();
            final SparseArray<User> newMap = new SparseArray<>();
//            ArrayList<TLRPC.TL_topPeer> inlineBots = MediaDataController.getInstance(currentAccount).inlineBots;
//            if (!usernameOnly && needBotContext && dogPostion == 0 && !inlineBots.isEmpty()) {
//                int count = 0;
//                for (int a = 0; a < inlineBots.size(); a++) {
//                    User user = messagesController.getUser(inlineBots.get(a).peer.user_id);
//                    if (user == null) {
//                        continue;
//                    }
//                    if (user.username != null && user.username.length() > 0 && (usernameString.length() > 0 && user.username.toLowerCase().startsWith(usernameString) || usernameString.length() == 0)) {
//                        newResult.add(user);
//                        newResultsHashMap.put(user.id, user);
//                        newMap.put(user.id, user);
//                        count++;
//                    }
//                    if (count == 5) {
//                        break;
//                    }
//                }
//            }
            final Chat chat;
            if (parentFragment != null) {
                chat = parentFragment.getCurrentChat();
            } else if (info != null) {
                chat = messagesController.getChat(info.id);
            } else {
                chat = null;
            }
            if (chat != null && info != null && info.participants != null && (!ChatObject.isChannel(chat) || chat.megagroup)) {
                for (int a = 0; a < info.participants.participants.size(); a++) {
                    Chat.ChatParticipant chatParticipant = info.participants.participants.get(a);
                    User user = messagesController.getUser(chatParticipant.user_id);
                    if (user == null || !usernameOnly && UserObject.isUserSelf(user) || newResultsHashMap.indexOfKey(user.id) >= 0) {
                        continue;
                    }
                    if (usernameString.length() == 0) {
                        if (!user.deleted) {
                            newResult.add(user);
                        }
                    } else {
                        if (user.username != null && user.username.length() > 0 && user.username.toLowerCase().startsWith(usernameString)) {
                            newResult.add(user);
                            newMap.put(user.id, user);
                        } else {
                            if (user.first_name != null && user.first_name.length() > 0 && user.first_name.toLowerCase().startsWith(usernameString)) {
                                newResult.add(user);
                                newMap.put(user.id, user);
                            } else if (user.last_name != null && user.last_name.length() > 0 && user.last_name.toLowerCase().startsWith(usernameString)) {
                                newResult.add(user);
                                newMap.put(user.id, user);
                            } else if (hasSpace && UserObject.formatName(user.first_name, user.last_name).toLowerCase().startsWith(usernameString)) {
                                newResult.add(user);
                                newMap.put(user.id, user);
                            }
                        }
                    }
                }
            }
            Collections.sort(newResult, (lhs, rhs) -> {
                if (newMap.indexOfKey(lhs.id) >= 0 && newMap.indexOfKey(rhs.id) >= 0) {
                    return 0;
                } else if (newMap.indexOfKey(lhs.id) >= 0) {
                    return -1;
                } else if (newMap.indexOfKey(rhs.id) >= 0) {
                    return 1;
                }
                int lhsNum = users.indexOf(lhs.id);
                int rhsNum = users.indexOf(rhs.id);
                if (lhsNum != -1 && rhsNum != -1) {
                    return lhsNum < rhsNum ? -1 : (lhsNum == rhsNum ? 0 : 1);
                } else if (lhsNum != -1 && rhsNum == -1) {
                    return -1;
                } else if (lhsNum == -1 && rhsNum != -1) {
                    return 1;
                }
                return 0;
            });
            searchResultHashtags = null;
            searchResultCommands = null;
            searchResultCommandsHelp = null;
            searchResultCommandsUsers = null;
            searchResultSuggestions = null;
            if (chat != null && chat.megagroup && usernameString.length() > 0) {
                if (newResult.size() < 5) {
                    AndroidUtilities.runOnUIThread(cancelDelayRunnable = () -> {
                        cancelDelayRunnable = null;
                        showUsersResult(newResult, newMap, true);
                    }, 1000);
                } else {
                    showUsersResult(newResult, newMap, true);
                }

                AndroidUtilities.runOnUIThread(searchGlobalRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (searchGlobalRunnable != this) {
                            return;
                        }
                        //TODO searchGlobal
//                        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
//                        req.channel = MessagesController.getInputChannel(chat);
//                        req.limit = 20;
//                        req.offset = 0;
//                        TLRPC.TL_channelParticipantsSearch channelParticipantsSearch = new TLRPC.TL_channelParticipantsSearch();
//                        channelParticipantsSearch.q = usernameString;
//                        req.filter = channelParticipantsSearch;
//                        final int currentReqId = ++channelLastReqId;
//                        channelReqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                            if (channelReqId != 0 && currentReqId == channelLastReqId && searchResultUsernamesMap != null && searchResultUsernames != null) {
//                                showUsersResult(newResult, newMap, false);
//                                if (error == null) {
//                                    TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
//                                    messagesController.putUsers(res.users, false);
//                                    boolean hasResults = !searchResultUsernames.isEmpty();
//                                    if (!res.participants.isEmpty()) {
//                                        int currentUserId = UserConfig.getInstance(currentAccount).getClientUserId();
//                                        for (int a = 0; a < res.participants.size(); a++) {
//                                            TLRPC.ChannelParticipant participant = res.participants.get(a);
//                                            if (searchResultUsernamesMap.indexOfKey(participant.user_id) >= 0 || !isSearchingMentions && participant.user_id == currentUserId) {
//                                                continue;
//                                            }
//                                            User user = messagesController.getUser(participant.user_id);
//                                            if (user == null) {
//                                                return;
//                                            }
//                                            searchResultUsernames.add(user);
//                                        }
//                                    }
//                                }
//                                notifyDataSetChanged();
//                                delegate.needChangePanelVisibility(!searchResultUsernames.isEmpty());
//                            }
//                            channelReqId = 0;
//                        }));
                    }
                }, 200);
            } else {
                showUsersResult(newResult, newMap, true);
            }
        } else if (foundType == 1) {
            ArrayList<String> newResult = new ArrayList<>();
            String hashtagString = result.toString().toLowerCase();
            ArrayList<SearchAdapterHelper.HashtagObject> hashtags = searchAdapterHelper.getHashtags();
            for (int a = 0; a < hashtags.size(); a++) {
                SearchAdapterHelper.HashtagObject hashtagObject = hashtags.get(a);
                if (hashtagObject != null && hashtagObject.hashtag != null && hashtagObject.hashtag.startsWith(hashtagString)) {
                    newResult.add(hashtagObject.hashtag);
                }
            }
            searchResultHashtags = newResult;
            searchResultUsernames = null;
            searchResultUsernamesMap = null;
            searchResultCommands = null;
            searchResultCommandsHelp = null;
            searchResultCommandsUsers = null;
            searchResultSuggestions = null;
            notifyDataSetChanged();
            delegate.needChangePanelVisibility(!newResult.isEmpty());
        } else if (foundType == 2) {
            ArrayList<String> newResult = new ArrayList<>();
            ArrayList<String> newResultHelp = new ArrayList<>();
            ArrayList<User> newResultUsers = new ArrayList<>();
            String command = result.toString().toLowerCase();
            for (int b = 0; b < botInfo.size(); b++) {
                BotInfo info = botInfo.valueAt(b);
                for (int a = 0; a < info.commands.size(); a++) {
                    BotInfo.BotCommand botCommand = info.commands.get(a);
                    if (botCommand != null && botCommand.command != null && botCommand.command.startsWith(command)) {
                        newResult.add("/" + botCommand.command);
                        newResultHelp.add(botCommand.description);
                        newResultUsers.add(messagesController.getUser(info.user_id));
                    }
                }
            }
            searchResultHashtags = null;
            searchResultUsernames = null;
            searchResultUsernamesMap = null;
            searchResultSuggestions = null;
            searchResultCommands = newResult;
            searchResultCommandsHelp = newResultHelp;
            searchResultCommandsUsers = newResultUsers;
            notifyDataSetChanged();
            delegate.needChangePanelVisibility(!newResult.isEmpty());
        } else if (foundType == 3) {
            String[] newLanguage = AndroidUtilities.getCurrentKeyboardLanguage();
            if (!Arrays.equals(newLanguage, lastSearchKeyboardLanguage)) {
                MediaDataController.getInstance(currentAccount).fetchNewEmojiKeywords(newLanguage);
            }
            lastSearchKeyboardLanguage = newLanguage;
            MediaDataController.getInstance(currentAccount).getEmojiSuggestions(lastSearchKeyboardLanguage, result.toString(), false, (param, alias) -> {
                searchResultSuggestions = param;
                searchResultHashtags = null;
                searchResultUsernames = null;
                searchResultUsernamesMap = null;
                searchResultCommands = null;
                searchResultCommandsHelp = null;
                searchResultCommandsUsers = null;
                notifyDataSetChanged();
                delegate.needChangePanelVisibility(searchResultSuggestions != null && !searchResultSuggestions.isEmpty());
            });
        }
    }

    private void showUsersResult(ArrayList<User> newResult, SparseArray<User> newMap, boolean notify) {
        searchResultUsernames = newResult;
        searchResultUsernamesMap = newMap;
        if (cancelDelayRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(cancelDelayRunnable);
            cancelDelayRunnable = null;
        }
        if (notify) {
            notifyDataSetChanged();
            delegate.needChangePanelVisibility(!searchResultUsernames.isEmpty());
        }
    }

    public int getResultStartPosition() {
        return resultStartPosition;
    }

    public int getResultLength() {
        return resultLength;
    }

    public ArrayList<BotInlineResult> getSearchResultBotContext() {
        return searchResultBotContext;
    }

    @Override
    public int getItemCount() {
        if (foundContextBot != null && !inlineMediaEnabled) {
            return 1;
        }
        if (searchResultBotContext != null) {
            return searchResultBotContext.size() + (searchResultBotContextSwitch != null ? 1 : 0);
        } else if (searchResultUsernames != null) {
            return searchResultUsernames.size();
        } else if (searchResultHashtags != null) {
            return searchResultHashtags.size();
        } else if (searchResultCommands != null) {
            return searchResultCommands.size();
        } else if (searchResultSuggestions != null) {
            return searchResultSuggestions.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (foundContextBot != null && !inlineMediaEnabled) {
            return 3;
        } else if (searchResultBotContext != null) {
            if (position == 0 && searchResultBotContextSwitch != null) {
                return 2;
            }
            return 1;
        } else {
            return 0;
        }
    }

    public void addHashtagsFromMessage(CharSequence message) {
        searchAdapterHelper.addHashtagsFromMessage(message);
    }

    public int getItemPosition(int i) {
        if (searchResultBotContext != null && searchResultBotContextSwitch != null) {
            i--;
        }
        return i;
    }

    public Object getItem(int i) {
        if (searchResultBotContext != null) {
            if (searchResultBotContextSwitch != null) {
                if (i == 0) {
                    return searchResultBotContextSwitch;
                } else {
                    i--;
                }
            }
            if (i < 0 || i >= searchResultBotContext.size()) {
                return null;
            }
            return searchResultBotContext.get(i);
        } else if (searchResultUsernames != null) {
            if (i < 0 || i >= searchResultUsernames.size()) {
                return null;
            }
            return searchResultUsernames.get(i);
        } else if (searchResultHashtags != null) {
            if (i < 0 || i >= searchResultHashtags.size()) {
                return null;
            }
            return searchResultHashtags.get(i);
        } else if (searchResultSuggestions != null) {
            if (i < 0 || i >= searchResultSuggestions.size()) {
                return null;
            }
            return searchResultSuggestions.get(i);
        } else if (searchResultCommands != null) {
            if (i < 0 || i >= searchResultCommands.size()) {
                return null;
            }
            if (searchResultCommandsUsers != null && (botsCount != 1 || info.isChannel())) {
                if (searchResultCommandsUsers.get(i) != null) {
                    return String.format("%s@%s", searchResultCommands.get(i), searchResultCommandsUsers.get(i) != null ? searchResultCommandsUsers.get(i).username : "");
                } else {
                    return String.format("%s", searchResultCommands.get(i));
                }
            }
            return searchResultCommands.get(i);
        }
        return null;
    }

    public boolean isLongClickEnabled() {
        return searchResultHashtags != null || searchResultCommands != null;
    }

    public boolean isBotCommands() {
        return searchResultCommands != null;
    }

    public boolean isBotContext() {
        return searchResultBotContext != null;
    }

    public boolean isBannedInline() {
        return foundContextBot != null && !inlineMediaEnabled;
    }

    public boolean isMediaLayout() {
        return contextMedia;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return foundContextBot == null || inlineMediaEnabled;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = new MentionCell(mContext);
                ((MentionCell) view).setIsDarkTheme(isDarkTheme);
                break;
            case 1:
                view = new ContextLinkCell(mContext);
                ((ContextLinkCell) view).setDelegate(cell -> delegate.onContextClick(cell.getResult()));
                break;
            case 2:
                view = new BotSwitchCell(mContext);
                break;
            case 3:
            default:
                TextView textView = new TextView(mContext);
                textView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                view = textView;
                break;
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == 3) {
            TextView textView = (TextView) holder.itemView;
            Chat chat = parentFragment.getCurrentChat();
            if (chat != null) {
                if (!ChatObject.hasAdminRights(chat) && chat.default_banned_rights != null && chat.default_banned_rights.send_inline) {
                    textView.setText(LocaleController.getString("GlobalAttachInlineRestricted", R.string.GlobalAttachInlineRestricted));
                } else if (AndroidUtilities.isBannedForever(chat.banned_rights)) {
                    textView.setText(LocaleController.getString("AttachInlineRestrictedForever", R.string.AttachInlineRestrictedForever));
                } else {
                    textView.setText(LocaleController.formatString("AttachInlineRestricted", R.string.AttachInlineRestricted, LocaleController.formatDateForBan(chat.banned_rights.until_date)));
                }
            }
        } else if (searchResultBotContext != null) {
            boolean hasTop = searchResultBotContextSwitch != null;
            if (holder.getItemViewType() == 2) {
                if (hasTop) {
                    ((BotSwitchCell) holder.itemView).setText(searchResultBotContextSwitch.text);
                }
            } else {
                if (hasTop) {
                    position--;
                }
                ((ContextLinkCell) holder.itemView).setLink(searchResultBotContext.get(position), foundContextBot, contextMedia, position != searchResultBotContext.size() - 1, hasTop && position == 0, "gif".equals(searchingContextUsername));
            }
        } else {
            if (searchResultUsernames != null) {
                ((MentionCell) holder.itemView).setUser(searchResultUsernames.get(position));
            } else if (searchResultHashtags != null) {
                ((MentionCell) holder.itemView).setText(searchResultHashtags.get(position));
            } else if (searchResultSuggestions != null) {
                ((MentionCell) holder.itemView).setEmojiSuggestion(searchResultSuggestions.get(position));
            } else if (searchResultCommands != null) {
                ((MentionCell) holder.itemView).setBotCommand(searchResultCommands.get(position), searchResultCommandsHelp.get(position), searchResultCommandsUsers != null ? searchResultCommandsUsers.get(position) : null);
            }
        }
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 2) {
            if (foundContextBot != null && foundContextBot.bot_inline_geo) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationProvider.start();
                } else {
                    onLocationUnavailable();
                }
            }
        }
    }
}
