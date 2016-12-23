package com.example.fingerprintsample;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button encrypt, decrypt;
    private TextView tv;
    private FingerprintHelper helper;

    private static final String DIALOG_FRAGMENT_TAG = "myFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        encrypt = (Button) findViewById(R.id.encrypt);
        decrypt = (Button) findViewById(R.id.decrypt);
        tv = (TextView) findViewById(R.id.tv);
        encrypt.setOnClickListener(this);
        decrypt.setOnClickListener(this);

        helper = new FingerprintHelper(this);
        int fingerPrintCheckResult = helper.checkFingerprintAvailable(this);
        if (fingerPrintCheckResult == 1) {
            //说明正常，其他应该要禁用什么按钮
        }
    }

    private void showEnrollDialog() {
        helper.generateKey();

        // Show the fingerprint dialog. The user has the option to use the fingerprint with
        // crypto, or you can fall back to using a server-side verified password.
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        FingerprintManager.CryptoObject object = helper.getLocalAndroidKeyStore().getCryptoObject(Cipher
                .ENCRYPT_MODE, null);
        fragment.setCryptoObject(object, true);
        fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    private void showDecryptDialog() {
        // Show the fingerprint dialog. The user has the option to use the fingerprint with
        // crypto, or you can fall back to using a server-side verified password.
        FingerprintAuthenticationDialogFragment fragment
                = new FingerprintAuthenticationDialogFragment();
        String IV = helper.getIV();
        FingerprintManager.CryptoObject object = helper.getLocalAndroidKeyStore().getCryptoObject(Cipher
                .DECRYPT_MODE, Base64.decode(IV, Base64.URL_SAFE));
//        object = helper.getLocalAndroidKeyStore().getCryptoObject(Cipher.ENCRYPT_MODE, null);
        fragment.setCryptoObject(object, false);
        fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.encrypt:
                showEnrollDialog();
                tv.setText("开始验证指纹......");
                break;
            case R.id.decrypt:
                showDecryptDialog();
                tv.setText("开始验证指纹......");
                break;
        }
    }

    public void decryptPassword(FingerprintManager.AuthenticationResult result, boolean encrypt) {
        if (encrypt) {
            String encryptString = String.valueOf(System.currentTimeMillis());
            String encrypted = helper.encryptData(result, encryptString);
            tv.setText("加密密码是...." + encryptString + "，加密后：" + encrypted);

        } else {
            String truePassword = helper.decryptData(result);
            tv.setText("解密密码是......" + truePassword);
        }
    }

}
