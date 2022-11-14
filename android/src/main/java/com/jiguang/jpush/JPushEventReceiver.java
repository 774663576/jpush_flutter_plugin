package com.jiguang.jpush;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jpush.android.api.CustomMessage;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.JPushMessage;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;
import io.flutter.plugin.common.MethodChannel.Result;

public class JPushEventReceiver extends JPushMessageReceiver {
    private static String TAG = "| JPUSH | Flutter | Android |___________ ";

    /**
     * 收到自定义消息回调
     */
    @Override
    public void onMessage(Context context, CustomMessage customMessage) {
        Log.d(TAG, "handlingMessageReceive __:" + customMessage.toString());
        Map<String, Object> msg = new HashMap<>();
        msg.put("data", new Gson().toJson(customMessage));
        JPushPlugin.instance.channel.invokeMethod("onReceiveMessage", msg);
    }

    /**
     * 点击通知回调
     * 魅族以及其他非厂商通道手机通知跳转activity走这里
     * 极光通道
     */
    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage message) {
        Log.d(TAG, "transmitNotificationOpen __:" + message.toString());
        Map<String, Object> msg = new HashMap<>();
        msg.put("data", new Gson().toJson(message));
        JPushPlugin.instance.channel.invokeMethod("onOpenNotification", msg);
    }

    /**
     * 收到通知回调
     */
    @Override
    public void onNotifyMessageArrived(Context context, NotificationMessage message) {
        Log.d(TAG, "transmitNotificationReceive__:" + message.toString() + "\n" + Thread.currentThread());

        Map<String, Object> msg = new HashMap<>();
        msg.put("data", new Gson().toJson(message));
//        JPushPlugin.instance.channel.invokeMethod("onReceiveNotification", msg);
        JPushPlugin.instance.runMainThread(msg, null, "onReceiveNotification");

    }

    @Override
    public void onTagOperatorResult(Context context, final JPushMessage jPushMessage) {
        super.onTagOperatorResult(context, jPushMessage);

        final JSONObject resultJson = new JSONObject();

        final int sequence = jPushMessage.getSequence();
        try {
            resultJson.put("sequence", sequence);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Result callback = JPushPlugin.instance.callbackMap.get(sequence);//instance.eventCallbackMap.get(sequence);

        if (callback == null) {
            Log.i("JPushPlugin", "Unexpected error, callback is null!");
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (jPushMessage.getErrorCode() == 0) { // success
                    Set<String> tags = jPushMessage.getTags();
                    List<String> tagList = new ArrayList<>(tags);
                    Map<String, Object> res = new HashMap<>();
                    res.put("tags", tagList);
                    callback.success(res);
                } else {
                    try {
                        resultJson.put("code", jPushMessage.getErrorCode());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callback.error(Integer.toString(jPushMessage.getErrorCode()), "", "");
                }

                JPushPlugin.instance.callbackMap.remove(sequence);
            }
        });

    }


    @Override
    public void onCheckTagOperatorResult(Context context, final JPushMessage jPushMessage) {
        super.onCheckTagOperatorResult(context, jPushMessage);


        final int sequence = jPushMessage.getSequence();


        final Result callback = JPushPlugin.instance.callbackMap.get(sequence);

        if (callback == null) {
            Log.i("JPushPlugin", "Unexpected error, callback is null!");
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (jPushMessage.getErrorCode() == 0) {
                    Set<String> tags = jPushMessage.getTags();
                    List<String> tagList = new ArrayList<>(tags);
                    Map<String, Object> res = new HashMap<>();
                    res.put("tags", tagList);
                    callback.success(res);
                } else {

                    callback.error(Integer.toString(jPushMessage.getErrorCode()), "", "");
                }

                JPushPlugin.instance.callbackMap.remove(sequence);
            }
        });
    }

    @Override
    public void onAliasOperatorResult(Context context, final JPushMessage jPushMessage) {
        super.onAliasOperatorResult(context, jPushMessage);
        Log.i(TAG, "onAliasOperatorResult_" + jPushMessage.toString());
        final int sequence = jPushMessage.getSequence();

        final Result callback = JPushPlugin.instance.callbackMap.get(sequence);

        if (callback == null) {
            Log.i("JPushPlugin", "Unexpected error, callback is null!");
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (jPushMessage.getErrorCode() == 0) { // success
                    Map<String, Object> res = new HashMap<>();
                    res.put("alias", (jPushMessage.getAlias() == null) ? "" : jPushMessage.getAlias());
                    callback.success(res);

                } else {
                    callback.error(Integer.toString(jPushMessage.getErrorCode()), "", "");
                }

                JPushPlugin.instance.callbackMap.remove(sequence);
            }
        });
    }

    @Override
    public void onNotificationSettingsCheck(Context context, boolean isOn, int source) {
        super.onNotificationSettingsCheck(context, isOn, source);


        HashMap<String, Object> map = new HashMap();
        map.put("isEnabled", isOn);
        JPushPlugin.instance.runMainThread(map, null, "onReceiveNotificationAuthorization");
    }
}
