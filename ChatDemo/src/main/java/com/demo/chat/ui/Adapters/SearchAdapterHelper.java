package com.demo.chat.ui.Adapters;

import android.util.SparseArray;

import com.demo.chat.PhoneFormat.PhoneFormat;
import com.demo.chat.SQLite.SQLiteCursor;
import com.demo.chat.SQLite.SQLitePreparedStatement;
import com.demo.chat.controller.MessagesStorage;
import com.demo.chat.controller.UserConfig;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.FileLog;
import com.demo.chat.model.Chat;
import com.demo.chat.model.User;
import com.demo.chat.model.UserChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class SearchAdapterHelper {

    public static class HashtagObject {
        String hashtag;
        int date;
    }

    public interface SearchAdapterHelperDelegate {
        void onDataSetChanged(int searchId);

        default void onSetHashtags(ArrayList<HashtagObject> arrayList, HashMap<String, HashtagObject> hashMap) {

        }

        default SparseArray<User> getExcludeUsers() {
            return null;
        }

        default boolean canApplySearchResults(int searchId) {
            return true;
        }
    }

    private SearchAdapterHelperDelegate delegate;

    private int reqId = 0;
    private int lastReqId;
    private String lastFoundUsername = null;
    private ArrayList<UserChat> localServerSearch = new ArrayList<>();
    private ArrayList<UserChat> globalSearch = new ArrayList<>();
    private SparseArray<UserChat> globalSearchMap = new SparseArray<>();
    private ArrayList<UserChat> groupSearch = new ArrayList<>();
    private SparseArray<UserChat> groupSearchMap = new SparseArray<>();
    private SparseArray<UserChat> phoneSearchMap = new SparseArray<>();
    private ArrayList<Object> phonesSearch = new ArrayList<>();
    private ArrayList<UserChat> localSearchResults;

    private int currentAccount = UserConfig.selectedAccount;

    private int channelReqId = 0;
    private int channelLastReqId;
    private String lastFoundChannel;

    private boolean allResultsAreGlobal;
    private boolean allowGlobalResults = true;

    private ArrayList<HashtagObject> hashtags;
    private HashMap<String, HashtagObject> hashtagsByText;
    private boolean hashtagsLoadedFromDb = false;

    protected static final class DialogSearchResult {
        public UserChat object;
        public int date;
        public CharSequence name;
    }

    public SearchAdapterHelper(boolean allAsGlobal) {
        allResultsAreGlobal = allAsGlobal;
    }

    public void setAllowGlobalResults(boolean value) {
        allowGlobalResults = value;
    }

    public boolean isSearchInProgress() {
        return reqId != 0 || channelReqId != 0;
    }

    public void queryServerSearch(String query, boolean allowUsername, boolean allowChats, boolean allowBots, boolean allowSelf, boolean canAddGroupsOnly, int channelId, boolean phoneNumbers, int type, int searchId) {
//        if (reqId != 0) {
//            ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true);
//            reqId = 0;
//        }
//        if (channelReqId != 0) {
//            ConnectionsManager.getInstance(currentAccount).cancelRequest(channelReqId, true);
//            channelReqId = 0;
//        }TODO 取消请求
        if (query == null) {
            groupSearch.clear();
            groupSearchMap.clear();
            globalSearch.clear();
            globalSearchMap.clear();
            localServerSearch.clear();
            phonesSearch.clear();
            phoneSearchMap.clear();
            lastReqId = 0;
            channelLastReqId = 0;
            delegate.onDataSetChanged(searchId);
            return;
        }
        if (query.length() > 0) {
            if (channelId != 0) {
                //TODO 发起请求
//                TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
//                if (type == ChatUsersActivity.TYPE_ADMIN) {
//                    req.filter = new TLRPC.TL_channelParticipantsAdmins();
//                } else if (type == ChatUsersActivity.TYPE_KICKED) {
//                    req.filter = new TLRPC.TL_channelParticipantsBanned();
//                } else if (type == ChatUsersActivity.TYPE_BANNED) {
//                    req.filter = new TLRPC.TL_channelParticipantsKicked();
//                } else {
//                    req.filter = new TLRPC.TL_channelParticipantsSearch();
//                }
//                req.filter.q = query;
//                req.limit = 50;
//                req.offset = 0;
//                req.channel = MessagesController.getInstance(currentAccount).getInputChannel(channelId);
//                final int currentReqId = ++channelLastReqId;
//                channelReqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                    if (currentReqId == channelLastReqId) {
//                        if (error == null) {
//                            TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
//                            lastFoundChannel = query.toLowerCase();
//                            MessagesController.getInstance(currentAccount).putUsers(res.users, false);
//                            groupSearch.clear();
//                            groupSearchMap.clear();
//                            groupSearch.addAll(res.participants);
//                            int currentUserId = UserConfig.getInstance(currentAccount).getClientUserId();
//                            for (int a = 0, N = res.participants.size(); a < N; a++) {
//                                TLRPC.ChannelParticipant participant = res.participants.get(a);
//                                if (!allowSelf && participant.user_id == currentUserId) {
//                                    groupSearch.remove(participant);
//                                    continue;
//                                }
//                                groupSearchMap.put(participant.user_id, participant);
//                            }
//                            if (localSearchResults != null) {
//                                mergeResults(localSearchResults);
//                            }
//                            delegate.onDataSetChanged(searchId);
//                        }
//                    }
//                    channelReqId = 0;
//                }), ConnectionsManager.RequestFlagFailOnServerErrors);
            } else {
                lastFoundChannel = query.toLowerCase();
            }
        } else {
            groupSearch.clear();
            groupSearchMap.clear();
            channelLastReqId = 0;
            delegate.onDataSetChanged(searchId);
        }
        if (allowUsername) {
            if (query.length() > 0) {
                //TODO 发起请求
//                TLRPC.TL_contacts_search req = new TLRPC.TL_contacts_search();
//                req.q = query;
//                req.limit = 50;
//                final int currentReqId = ++lastReqId;
//                reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
//                    if (currentReqId == lastReqId && delegate.canApplySearchResults(searchId)) {
//                        reqId = 0;
//                        if (error == null) {
//                            TLRPC.TL_contacts_found res = (TLRPC.TL_contacts_found) response;
//                            globalSearch.clear();
//                            globalSearchMap.clear();
//                            localServerSearch.clear();
//                            MessagesController.getInstance(currentAccount).putChats(res.chats, false);
//                            MessagesController.getInstance(currentAccount).putUsers(res.users, false);
//                            MessagesStorage.getInstance(currentAccount).putUsersAndChats(res.users, res.chats, true, true);
//                            SparseArray<TLRPC.Chat> chatsMap = new SparseArray<>();
//                            SparseArray<User> usersMap = new SparseArray<>();
//                            for (int a = 0; a < res.chats.size(); a++) {
//                                TLRPC.Chat chat = res.chats.get(a);
//                                chatsMap.put(chat.id, chat);
//                            }
//                            for (int a = 0; a < res.users.size(); a++) {
//                                User user = res.users.get(a);
//                                usersMap.put(user.id, user);
//                            }
//                            for (int b = 0; b < 2; b++) {
//                                ArrayList<TLRPC.Peer> arrayList;
//                                if (b == 0) {
//                                    if (!allResultsAreGlobal) {
//                                        continue;
//                                    }
//                                    arrayList = res.my_results;
//                                } else {
//                                    arrayList = res.results;
//                                }
//                                for (int a = 0; a < arrayList.size(); a++) {
//                                    TLRPC.Peer peer = arrayList.get(a);
//                                    User user = null;
//                                    TLRPC.Chat chat = null;
//                                    if (peer.user_id != 0) {
//                                        user = usersMap.get(peer.user_id);
//                                    } else if (peer.chat_id != 0) {
//                                        chat = chatsMap.get(peer.chat_id);
//                                    } else if (peer.channel_id != 0) {
//                                        chat = chatsMap.get(peer.channel_id);
//                                    }
//                                    if (chat != null) {
//                                        if (!allowChats || canAddGroupsOnly && !ChatObject.canAddBotsToChat(chat) || !allowGlobalResults && ChatObject.isNotInChat(chat)) {
//                                            continue;
//                                        }
//                                        globalSearch.add(chat);
//                                        globalSearchMap.put(-chat.id, chat);
//                                    } else if (user != null) {
//                                        if (canAddGroupsOnly || !allowBots && user.bot || !allowSelf && user.self || !allowGlobalResults && b == 1 && !user.contact) {
//                                            continue;
//                                        }
//                                        globalSearch.add(user);
//                                        globalSearchMap.put(user.id, user);
//                                    }
//                                }
//                            }
//                            if (!allResultsAreGlobal) {
//                                for (int a = 0; a < res.my_results.size(); a++) {
//                                    TLRPC.Peer peer = res.my_results.get(a);
//                                    User user = null;
//                                    TLRPC.Chat chat = null;
//                                    if (peer.user_id != 0) {
//                                        user = usersMap.get(peer.user_id);
//                                    } else if (peer.chat_id != 0) {
//                                        chat = chatsMap.get(peer.chat_id);
//                                    } else if (peer.channel_id != 0) {
//                                        chat = chatsMap.get(peer.channel_id);
//                                    }
//                                    if (chat != null) {
//                                        if (!allowChats || canAddGroupsOnly && !ChatObject.canAddBotsToChat(chat)) {
//                                            continue;
//                                        }
//                                        localServerSearch.add(chat);
//                                        globalSearchMap.put(-chat.id, chat);
//                                    } else if (user != null) {
//                                        if (canAddGroupsOnly || !allowBots && user.bot || !allowSelf && user.self) {
//                                            continue;
//                                        }
//                                        localServerSearch.add(user);
//                                        globalSearchMap.put(user.id, user);
//                                    }
//                                }
//                            }
//                            lastFoundUsername = query.toLowerCase();
//                            if (localSearchResults != null) {
//                                mergeResults(localSearchResults);
//                            }
//                            mergeExcludeResults();
//                            delegate.onDataSetChanged(searchId);
//                        }
//                    }
//                }), ConnectionsManager.RequestFlagFailOnServerErrors);
            } else {
                globalSearch.clear();
                globalSearchMap.clear();
                localServerSearch.clear();
                lastReqId = 0;
                delegate.onDataSetChanged(searchId);
            }
        }
        if (!canAddGroupsOnly && phoneNumbers && query.startsWith("+") && query.length() > 3) {
            phonesSearch.clear();
            phoneSearchMap.clear();
            String phone = PhoneFormat.stripExceptNumbers(query);
//            ArrayList<TLRPC.TL_contact> arrayList = ContactsController.getInstance(currentAccount).contacts;
//            boolean hasFullMatch = false;
//            for (int a = 0, N = arrayList.size(); a < N; a++) {
//                TLRPC.TL_contact contact = arrayList.get(a);
//                User user = MessagesController.getInstance(currentAccount).getUser(contact.user_id);
//                if (user == null) {
//                    continue;
//                }
//                if (user.phone != null && user.phone.startsWith(phone)) {
//                    if (!hasFullMatch) {
//                        hasFullMatch = user.phone.length() == phone.length();
//                    }
//                    phonesSearch.add(user);
//                    phoneSearchMap.put(user.id, user);
//                }
//            }
//            if (!hasFullMatch) {
//                phonesSearch.add("section");
//                phonesSearch.add(phone);
//            }
            delegate.onDataSetChanged(searchId);
        }
    }

    public void clear() {
        globalSearch.clear();
        globalSearchMap.clear();
        localServerSearch.clear();
    }

    public void unloadRecentHashtags() {
        hashtagsLoadedFromDb = false;
    }

    public boolean loadRecentHashtags() {
        if (hashtagsLoadedFromDb) {
            return true;
        }
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                SQLiteCursor cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT id, date FROM hashtag_recent_v2 WHERE 1");
                final ArrayList<HashtagObject> arrayList = new ArrayList<>();
                final HashMap<String, HashtagObject> hashMap = new HashMap<>();
                while (cursor.next()) {
                    HashtagObject hashtagObject = new HashtagObject();
                    hashtagObject.hashtag = cursor.stringValue(0);
                    hashtagObject.date = cursor.intValue(1);
                    arrayList.add(hashtagObject);
                    hashMap.put(hashtagObject.hashtag, hashtagObject);
                }
                cursor.dispose();
                Collections.sort(arrayList, (lhs, rhs) -> {
                    if (lhs.date < rhs.date) {
                        return 1;
                    } else if (lhs.date > rhs.date) {
                        return -1;
                    } else {
                        return 0;
                    }
                });
                AndroidUtilities.runOnUIThread(() -> setHashtags(arrayList, hashMap));
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        return false;
    }

    public void mergeResults(ArrayList<UserChat> localResults) {
        localSearchResults = localResults;
        if (globalSearchMap.size() == 0 || localResults == null) {
            return;
        }
        int count = localResults.size();
        for (int a = 0; a < count; a++) {
            UserChat obj = localResults.get(a);
            if (obj instanceof User) {
                User user = (User) obj;
                User u = (User) globalSearchMap.get(user.id);
                if (u != null) {
                    globalSearch.remove(u);
                    localServerSearch.remove(u);
                    globalSearchMap.remove(u.id);
                }
                UserChat participant = groupSearchMap.get(user.id);
                if (participant != null) {
                    groupSearch.remove(participant);
                    groupSearchMap.remove(user.id);
                }
                Object object = phoneSearchMap.get(user.id);
                if (object != null) {
                    phonesSearch.remove(object);
                    phoneSearchMap.remove(user.id);
                }
            } else if (obj instanceof Chat) {
                Chat chat = (Chat) obj;
                Chat c = (Chat) globalSearchMap.get(-chat.id);
                if (c != null) {
                    globalSearch.remove(c);
                    localServerSearch.remove(c);
                    globalSearchMap.remove(-c.id);
                }
            }
        }
    }

    public void mergeExcludeResults() {
        if (delegate == null) {
            return;
        }
        SparseArray<User> ignoreUsers = delegate.getExcludeUsers();
        if (ignoreUsers == null) {
            return;
        }
        for (int a = 0, size = ignoreUsers.size(); a < size; a++) {
            User u = (User) globalSearchMap.get(ignoreUsers.keyAt(a));
            if (u != null) {
                globalSearch.remove(u);
                localServerSearch.remove(u);
                globalSearchMap.remove(u.id);
            }
        }
    }

    public void setDelegate(SearchAdapterHelperDelegate searchAdapterHelperDelegate) {
        delegate = searchAdapterHelperDelegate;
    }

    public void addHashtagsFromMessage(CharSequence message) {
        if (message == null) {
            return;
        }
        boolean changed = false;
        Pattern pattern = Pattern.compile("(^|\\s)#[^0-9][\\w@.]+");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (message.charAt(start) != '@' && message.charAt(start) != '#') {
                start++;
            }
            String hashtag = message.subSequence(start, end).toString();
            if (hashtagsByText == null) {
                hashtagsByText = new HashMap<>();
                hashtags = new ArrayList<>();
            }
            HashtagObject hashtagObject = hashtagsByText.get(hashtag);
            if (hashtagObject == null) {
                hashtagObject = new HashtagObject();
                hashtagObject.hashtag = hashtag;
                hashtagsByText.put(hashtagObject.hashtag, hashtagObject);
            } else {
                hashtags.remove(hashtagObject);
            }
            hashtagObject.date = (int) (System.currentTimeMillis() / 1000);
            hashtags.add(0, hashtagObject);
            changed = true;
        }
        if (changed) {
            putRecentHashtags(hashtags);
        }
    }

    private void putRecentHashtags(final ArrayList<HashtagObject> arrayList) {
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                MessagesStorage.getInstance(currentAccount).getDatabase().beginTransaction();
                SQLitePreparedStatement state = MessagesStorage.getInstance(currentAccount).getDatabase().executeFast("REPLACE INTO hashtag_recent_v2 VALUES(?, ?)");
                for (int a = 0; a < arrayList.size(); a++) {
                    if (a == 100) {
                        break;
                    }
                    HashtagObject hashtagObject = arrayList.get(a);
                    state.requery();
                    state.bindString(1, hashtagObject.hashtag);
                    state.bindInteger(2, hashtagObject.date);
                    state.step();
                }
                state.dispose();
                MessagesStorage.getInstance(currentAccount).getDatabase().commitTransaction();
                if (arrayList.size() >= 100) {
                    MessagesStorage.getInstance(currentAccount).getDatabase().beginTransaction();
                    for (int a = 100; a < arrayList.size(); a++) {
                        MessagesStorage.getInstance(currentAccount).getDatabase().executeFast("DELETE FROM hashtag_recent_v2 WHERE id = '" + arrayList.get(a).hashtag + "'").stepThis().dispose();
                    }
                    MessagesStorage.getInstance(currentAccount).getDatabase().commitTransaction();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void removeUserId(int userId) {
        Object object = globalSearchMap.get(userId);
        if (object != null) {
            globalSearch.remove(object);
        }
        object = groupSearchMap.get(userId);
        if (object != null) {
            groupSearch.remove(object);
        }
    }

    public ArrayList<UserChat> getGlobalSearch() {
        return globalSearch;
    }

    public ArrayList<Object> getPhoneSearch() {
        return phonesSearch;
    }

    public ArrayList<UserChat> getLocalServerSearch() {
        return localServerSearch;
    }

    public ArrayList<UserChat> getGroupSearch() {
        return groupSearch;
    }

    public ArrayList<HashtagObject> getHashtags() {
        return hashtags;
    }

    public String getLastFoundUsername() {
        return lastFoundUsername;
    }

    public String getLastFoundChannel() {
        return lastFoundChannel;
    }

    public void clearRecentHashtags() {
        hashtags = new ArrayList<>();
        hashtagsByText = new HashMap<>();
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                MessagesStorage.getInstance(currentAccount).getDatabase().executeFast("DELETE FROM hashtag_recent_v2 WHERE 1").stepThis().dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void setHashtags(ArrayList<HashtagObject> arrayList, HashMap<String, HashtagObject> hashMap) {
        hashtags = arrayList;
        hashtagsByText = hashMap;
        hashtagsLoadedFromDb = true;
        delegate.onSetHashtags(arrayList, hashMap);
    }
}
