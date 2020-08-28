package com.demo.chat.model.message;

import com.demo.chat.model.User;
import com.demo.chat.model.small.MessageMedia;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class MessageAction {
    public String title;
    public String address;
    public String message;
    public ArrayList<Integer> users = new ArrayList<>();
    public int channel_id;
    public MessageMedia.Photo photo;
    public int chat_id;
    public int user_id;
    public User.UserProfilePhoto newUserPhoto;
    public int inviter_id;
    public int ttl;
    public int flags;
    public long call_id;
    public int duration;
    public String currency;
    public long total_amount;
    public long game_id;
    public int score;
    public boolean video;
//    public DecryptedMessageAction encryptedAction;
//    public TL_inputGroupCall call;
//    public PhoneCallDiscardReason reason;

    public boolean isChatEditPhoto(){return false;}
    public boolean isSecureValuesSent(){return false;}
    public boolean isEmpty(){return false;}
    public boolean isChatCreate(){return false;}
    public boolean isChatDeleteUser(){return false;}
    public boolean isChatAddUser(){return false;}
    public boolean isPinMessage(){return false;}
    public boolean isUserUpdatedPhoto(){return false;}
}