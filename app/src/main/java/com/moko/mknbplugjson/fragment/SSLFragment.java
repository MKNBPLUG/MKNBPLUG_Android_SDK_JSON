package com.moko.mknbplugjson.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elvishew.xlog.XLog;
import com.moko.mknbplugjson.R;
import com.moko.mknbplugjson.databinding.FragmentSslAppBinding;
import com.moko.mknbplugjson.dialog.BottomDialog;
import com.moko.mknbplugjson.utils.FileUtils;
import com.moko.mknbplugjson.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;

public class SSLFragment extends Fragment {
    public static final int REQUEST_CODE_SELECT_CA = 0x10;
    public static final int REQUEST_CODE_SELECT_CLIENT_KEY = 0x11;
    public static final int REQUEST_CODE_SELECT_CLIENT_CERT = 0x12;
    private static final String TAG = SSLFragment.class.getSimpleName();
    private int connectMode;
    private String caPath;
    private String clientKeyPath;
    private String clientCertPath;
    private ArrayList<String> values;
    private int selected;
    private FragmentSslAppBinding mBind;

    public SSLFragment() {
    }

    public static SSLFragment newInstance() {
        return new SSLFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentSslAppBinding.inflate(inflater, container, false);
        mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(connectMode > 0);
        mBind.cbSsl.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) (buttonView, isChecked) -> {
            if (!isChecked) {
                connectMode = 0;
            } else {
                connectMode = selected + 1;
            }
            mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
            XLog.i("333333mode="+connectMode);
            if (connectMode > 0) setSSL();
        });
        values = new ArrayList<>();
        values.add("CA signed server certificate");
        values.add("CA certificate");
        values.add("Self signed certificates");
        if (connectMode > 0) {
            selected = connectMode - 1;
            mBind.tvCaFile.setText(caPath);
            mBind.tvClientKeyFile.setText(clientKeyPath);
            mBind.tvClientCertFile.setText(clientCertPath);
            mBind.tvCertification.setText(values.get(selected));
        }
        setSSL();
        return mBind.getRoot();
    }

    private void setSSL() {
        if (connectMode == 1) {
            mBind.layoutCertificate.setVisibility(View.VISIBLE);
            mBind.llCa.setVisibility(View.GONE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (connectMode == 2) {
            mBind.layoutCertificate.setVisibility(View.VISIBLE);
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (connectMode == 3) {
            mBind.layoutCertificate.setVisibility(View.VISIBLE);
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    public void setConnectMode(int connectMode) {
        this.connectMode = connectMode;
        if (null == mBind) return;
        mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(connectMode > 0);
        if (connectMode > 0) {
            selected = connectMode - 1;
            mBind.tvCertification.setText(values.get(selected));
            setSSL();
        }
    }

    public void setCAPath(String caPath) {
        this.caPath = caPath;
        if (null != mBind) mBind.tvCaFile.setText(caPath);
    }

    public void setClientKeyPath(String clientKeyPath) {
        this.clientKeyPath = clientKeyPath;
        if (null != mBind) mBind.tvClientKeyFile.setText(clientKeyPath);
    }

    public void setClientCertPath(String clientCertPath) {
        this.clientCertPath = clientCertPath;
        if (null != mBind) mBind.tvClientCertFile.setText(clientCertPath);
    }

    public void selectCertificate() {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(values, selected);
        dialog.setListener(value -> {
            selected = value;
            mBind.tvCertification.setText(values.get(selected));
            connectMode = selected + 1;
            setSSL();
        });
        if (null != getActivity()) dialog.show(getActivity().getSupportFragmentManager());
    }

    public void selectCAFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_CA);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(requireContext(), "install file manager app");
        }
    }

    public void selectKeyFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_CLIENT_KEY);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(requireContext(), "install file manager app");
        }
    }

    public void selectCertFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), REQUEST_CODE_SELECT_CLIENT_CERT);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(requireContext(), "install file manager app");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (null != getActivity()&& resultCode != getActivity().RESULT_OK) return;
        //得到uri，后面就是将uri转化成file的过程。
        Uri uri = data.getData();
        String filePath = FileUtils.getPath(requireContext(), uri);
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(requireContext(), "file path error!");
            return;
        }
        final File file = new File(filePath);
        if (file.exists()) {
            if (requestCode == REQUEST_CODE_SELECT_CA) {
                caPath = filePath;
                mBind.tvCaFile.setText(filePath);
            }
            if (requestCode == REQUEST_CODE_SELECT_CLIENT_KEY) {
                clientKeyPath = filePath;
                mBind.tvClientKeyFile.setText(filePath);
            }
            if (requestCode == REQUEST_CODE_SELECT_CLIENT_CERT) {
                clientCertPath = filePath;
                mBind.tvClientCertFile.setText(filePath);
            }
        } else {
            ToastUtils.showToast(requireContext(), "file is not exists!");
        }
    }

    public boolean isValid() {
        final String caFile = mBind.tvCaFile.getText().toString();
        final String clientKeyFile = mBind.tvClientKeyFile.getText().toString();
        final String clientCertFile = mBind.tvClientCertFile.getText().toString();
        if (connectMode == 2) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
        } else if (connectMode == 3) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
            if (TextUtils.isEmpty(clientKeyFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_client_key));
                return false;
            }
            if (TextUtils.isEmpty(clientCertFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_client_cert));
                return false;
            }
        }
        return true;
    }

    public int getConnectMode() {
        return connectMode;
    }

    public String getCaPath() {
        return caPath;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }
}
