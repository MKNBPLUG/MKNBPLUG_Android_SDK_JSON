package com.moko.mknbplugjson.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mknbplugjson.AppConstants;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.base.BaseActivity;
import com.moko.mknbplugjson.databinding.ActivityOverloadProtectionBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.entity.OverloadProtection;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class OverloadProtectionActivity extends BaseActivity<ActivityOverloadProtectionBinding> {
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;
    private int mDeviceType;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_DEVICE_TYPE, 0);
        if (mDeviceType == 0) {
            mBind.etPowerThreshold.setHint("10-4416");
        } else if (mDeviceType == 1) {
            mBind.etPowerThreshold.setHint("10-2160");
        } else {
            mBind.etPowerThreshold.setHint("10-3588");
        }
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getOverloadProtection();
    }

    @Override
    protected ActivityOverloadProtectionBinding getViewBinding() {
        return ActivityOverloadProtectionBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        MsgCommon<JsonObject> msgCommon;
        try {
            Type type = new TypeToken<MsgCommon<JsonObject>>() {
            }.getType();
            msgCommon = new Gson().fromJson(message, type);
        } catch (Exception e) {
            return;
        }
        if (!mMokoDevice.mac.equalsIgnoreCase(msgCommon.device_info.mac)) {
            return;
        }
        mMokoDevice.isOnline = true;
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_OVER_LOAD_PROTECTION) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) return;
            Type statusType = new TypeToken<OverloadProtection>() {
            }.getType();
            OverloadProtection overloadProtection = new Gson().fromJson(msgCommon.data, statusType);
            int enable = overloadProtection.protection_enable;
            int value = overloadProtection.protection_value;
            int judge_time = overloadProtection.judge_time;
            mBind.cbOverloadProtection.setChecked(enable == 1);
            mBind.etPowerThreshold.setText(String.valueOf(value));
            mBind.etTimeThreshold.setText(String.valueOf(judge_time));
            mBind.etPowerThreshold.setSelection(mBind.etPowerThreshold.getText().length());
            mBind.etTimeThreshold.setSelection(mBind.etTimeThreshold.getText().length());
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_OVER_LOAD_PROTECTION) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0) {
                ToastUtils.showToast(this, "Set up failed");
                return;
            }
            ToastUtils.showToast(this, "Set up succeed");
        }
        if (msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVERLOAD_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_VOLTAGE_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_UNDER_VOLTAGE_OCCUR
                || msgCommon.msg_id == MQTTConstants.NOTIFY_MSG_ID_OVER_CURRENT_OCCUR) {
            Type infoType = new TypeToken<OverloadOccur>() {
            }.getType();
            OverloadOccur overloadOccur = new Gson().fromJson(msgCommon.data, infoType);
            if (overloadOccur.state == 1) finish();
        }
    }

    public void onBack(View view) {
        finish();
    }

    private void getOverloadProtection() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadOverloadProtection(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_OVER_LOAD_PROTECTION, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        int max = 4416;
        if (mDeviceType == 1) {
            max = 2160;
        } else if (mDeviceType == 2) {
            max = 3588;
        }
        String powerThresholdStr = mBind.etPowerThreshold.getText().toString();
        if (TextUtils.isEmpty(powerThresholdStr)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int powerThreshold = Integer.parseInt(powerThresholdStr);
        if (powerThreshold < 10 || powerThreshold > max) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        String timeThresholdStr = mBind.etTimeThreshold.getText().toString();
        if (TextUtils.isEmpty(timeThresholdStr)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int timeThreshold = Integer.parseInt(timeThresholdStr);
        if (timeThreshold < 1 || timeThreshold > 30) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setOverloadProtection(powerThreshold, timeThreshold);
    }

    private void setOverloadProtection(int powerThreshold, int timeThreshold) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        OverloadProtection protection = new OverloadProtection();
        protection.protection_enable = mBind.cbOverloadProtection.isChecked() ? 1 : 0;
        protection.protection_value = powerThreshold;
        protection.judge_time = timeThreshold;
        String message = MQTTMessageAssembler.assembleWriteOverloadProtection(deviceParams, protection);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_OVER_LOAD_PROTECTION, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
