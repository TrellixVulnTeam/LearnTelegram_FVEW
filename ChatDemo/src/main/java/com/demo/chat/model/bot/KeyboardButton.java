package com.demo.chat.model.bot;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class KeyboardButton {
    public String text;
    public String url;
    public int flags;
    public boolean same_peer;
    public String query;
    public byte[] data;
    public int button_id;
    public String fwd_text;
    public boolean quiz;

    public void setSwitchInline(boolean isSwitchInline) {}

    public void setKeyboardButton(boolean isKeyboardButton) {}

    public void setKeyboardButtonUrl(boolean isKeyboardButtonUrl) {}

    public void setRequestGeoLocation(boolean isRequestGeoLocation) {}

    public void setButtonUrlAuth(boolean isButtonUrlAuth) {}

    public void setButtonCallback(boolean isButtonCallback) {}

    public boolean isSwitchInline() {
        return false;
    }

    public boolean isKeyboardButton() {
        return false;
    }

    public boolean isKeyboardButtonUrl() {
        return false;
    }

    public boolean isRequestGeoLocation() {
        return false;
    }

    public boolean isButtonUrlAuth() {
        return false;
    }

    public boolean isButtonCallback() {
        return false;
    }

}
