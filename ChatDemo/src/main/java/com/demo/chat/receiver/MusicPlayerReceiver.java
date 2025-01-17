package com.demo.chat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.demo.chat.controller.MediaController;
import com.demo.chat.service.MusicPlayerService;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/28
 * @description null
 * @usage null
 */
public class MusicPlayerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            if (intent.getExtras() == null) {
                return;
            }
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null) {
                return;
            }
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;

            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (MediaController.getInstance().isMessagePaused()) {
                        MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
                    } else {
                        MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    MediaController.getInstance().playNextMessage();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    MediaController.getInstance().playPreviousMessage();
                    break;
            }
        } else {
            if (intent.getAction().equals(MusicPlayerService.NOTIFY_PLAY)) {
                MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_PAUSE) || intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_NEXT)) {
                MediaController.getInstance().playNextMessage();
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_CLOSE)) {
                MediaController.getInstance().cleanupPlayer(true, true);
            } else if (intent.getAction().equals(MusicPlayerService.NOTIFY_PREVIOUS)) {
                MediaController.getInstance().playPreviousMessage();
            }
        }
    }
}
