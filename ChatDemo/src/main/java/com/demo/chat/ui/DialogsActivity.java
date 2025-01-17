package com.demo.chat.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.ui.ActionBar.BaseFragment;
import com.demo.chat.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/27
 * @description null
 * @usage null
 */
public class DialogsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private DialogsActivityDelegate delegate;
    private RecyclerListView sideMenu;

    public interface DialogsActivityDelegate {
        void didSelectDialogs(DialogsActivity fragment, ArrayList<Long> dids, CharSequence message, boolean param);
    }

    public void setDelegate(DialogsActivityDelegate delegate) {
        this.delegate = delegate;
    }

    public void setSearchString(String text) {}

    public DialogsActivity(Bundle args) {
        super(args);
    }

    public void setSideMenu(RecyclerListView sideMenu) {
        this.sideMenu = sideMenu;
    }
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }
}
