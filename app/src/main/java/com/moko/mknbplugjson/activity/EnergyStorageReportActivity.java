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
import com.moko.mknbplugjson.databinding.ActivityEnergyStorageReportBinding;
import com.moko.mknbplugjson.entity.MokoDevice;
import com.moko.mknbplugjson.utils.SPUtils;
import com.moko.mknbplugjson.utils.ToastUtils;
import com.moko.support.json.MQTTConstants;
import com.moko.support.json.MQTTMessageAssembler;
import com.moko.support.json.MQTTSupport;
import com.moko.support.json.entity.DeviceParams;
import com.moko.support.json.entity.EnergyStorageReport;
import com.moko.support.json.entity.MQTTConfig;
import com.moko.support.json.entity.MsgCommon;
import com.moko.support.json.entity.OverloadOccur;
import com.moko.support.json.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class EnergyStorageReportActivity extends BaseActivity<ActivityEnergyStorageReportBinding> {
    private MQTTConfig appMqttConfig;
    private MokoDevice mMokoDevice;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        String mqttConfigAppStr = SPUtils.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mHandler = new Handler(Looper.getMainLooper());
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getEnergyStorageReport();
    }

    @Override
    protected ActivityEnergyStorageReportBinding getViewBinding() {
        return ActivityEnergyStorageReportBinding.inflate(getLayoutInflater());
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
        if (msgCommon.msg_id == MQTTConstants.READ_MSG_ID_ENERGY_REPORT_PARAMS) {
            if (mHandler.hasMessages(0)) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
            }
            if (msgCommon.result_code != 0)
                return;
            Type type = new TypeToken<EnergyStorageReport>() {
            }.getType();
            EnergyStorageReport energyStorageReport = new Gson().fromJson(msgCommon.data, type);
            mBind.etEnergyStorageInterval.setText(String.valueOf(energyStorageReport.storage_interval));
            mBind.etPowerChangeThreshold.setText(String.valueOf(energyStorageReport.storage_threshold));
            mBind.etEnergyReportInterval.setText(String.valueOf(energyStorageReport.report_interval));
        }
        if (msgCommon.msg_id == MQTTConstants.CONFIG_MSG_ID_ENERGY_REPORT_PARAMS) {
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
            if (overloadOccur.state == 1)
                finish();
        }
    }

    public void onBack(View view) {
        finish();
    }

    private void getEnergyStorageReport() {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        String message = MQTTMessageAssembler.assembleReadEnergyReportParams(deviceParams);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.READ_MSG_ID_ENERGY_REPORT_PARAMS, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setEnergyStorageReport(int storageInterval, int storageThreshold, int reportInterval) {
        String appTopic;
        if (TextUtils.isEmpty(appMqttConfig.topicPublish)) {
            appTopic = mMokoDevice.topicSubscribe;
        } else {
            appTopic = appMqttConfig.topicPublish;
        }
        DeviceParams deviceParams = new DeviceParams();
        deviceParams.mac = mMokoDevice.mac;
        EnergyStorageReport energyStorageReport = new EnergyStorageReport();
        energyStorageReport.storage_interval = storageInterval;
        energyStorageReport.storage_threshold = storageThreshold;
        energyStorageReport.report_interval = reportInterval;
        String message = MQTTMessageAssembler.assembleWriteEnergyReportParams(deviceParams, energyStorageReport);
        try {
            MQTTSupport.getInstance().publish(appTopic, message, MQTTConstants.CONFIG_MSG_ID_ENERGY_REPORT_PARAMS, appMqttConfig.qos);
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
        if (isValid()) {
            String energyStorageIntervalStr = mBind.etEnergyStorageInterval.getText().toString();
            String powerChangeThresholdStr = mBind.etPowerChangeThreshold.getText().toString();
            String energyReportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
            int energyStorageInterval = Integer.parseInt(energyStorageIntervalStr);
            int powerChangeThreshold = Integer.parseInt(powerChangeThresholdStr);
            int energyReportInterval = Integer.parseInt(energyReportIntervalStr);
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            setEnergyStorageReport(energyStorageInterval, powerChangeThreshold, energyReportInterval);
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isValid() {
        String energyStorageIntervalStr = mBind.etEnergyStorageInterval.getText().toString();
        String powerChangeThresholdStr = mBind.etPowerChangeThreshold.getText().toString();
        String energyReportIntervalStr = mBind.etEnergyReportInterval.getText().toString();
        if (TextUtils.isEmpty(energyStorageIntervalStr)
                || TextUtils.isEmpty(powerChangeThresholdStr)
                || TextUtils.isEmpty(energyReportIntervalStr)) {
            return false;
        }
        int energyStorageInterval = Integer.parseInt(energyStorageIntervalStr);
        if (energyStorageInterval < 1 || energyStorageInterval > 60) return false;
        int powerChangeThreshold = Integer.parseInt(powerChangeThresholdStr);
        if (powerChangeThreshold > 100) return false;
        int energyReportInterval = Integer.parseInt(energyReportIntervalStr);
        return energyReportInterval <= 1440;
    }
}
