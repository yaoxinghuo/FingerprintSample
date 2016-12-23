package com.example.fingerprintsample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;


/**
 * Created by hzlinxuanxuan on 2016/9/12.
 */
public class FingerprintHelper {

    private FingerprintManager manager;
    private LocalSharedPreference mLocalSharedPreference;
    private LocalAndroidKeyStore mLocalAndroidKeyStore;

    public FingerprintHelper(Context context) {
        manager = context.getSystemService(FingerprintManager.class);
        mLocalSharedPreference = new LocalSharedPreference(context);
        mLocalAndroidKeyStore = new LocalAndroidKeyStore();
    }

    public LocalAndroidKeyStore getLocalAndroidKeyStore() {
        return mLocalAndroidKeyStore;
    }

    public String getIV() {
        return mLocalSharedPreference.getData(mLocalSharedPreference.IVKeyName);
    }

    public void generateKey() {
        //在keystore中生成加密密钥
        mLocalAndroidKeyStore.generateKey(LocalAndroidKeyStore.keyName);
    }

    public boolean isKeyProtectedEnforcedBySecureHardware() {
        return mLocalAndroidKeyStore.isKeyProtectedEnforcedBySecureHardware();
    }

    /**
     * @param ctx
     * @return 0 支持指纹但是没有录入指纹； 1：有可用指纹； -1，手机不支持指纹
     */
    public int checkFingerprintAvailable(Context ctx) {
        if (!isKeyProtectedEnforcedBySecureHardware()) {
            return -1;
        } else if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.USE_FINGERPRINT) != PackageManager
                .PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return -1;
        }
        if (!manager.isHardwareDetected()) {
            Toast.makeText(ctx, "该设备尚未检测到指纹硬件", Toast.LENGTH_SHORT).show();
            return -1;
        } else if (!manager.hasEnrolledFingerprints()) {
            Toast.makeText(ctx, "该设备未录入指纹，请去系统->设置中添加指纹", Toast.LENGTH_SHORT).show();
            return 0;
        }
        return 1;
    }

    public boolean containsToken() {
        return mLocalSharedPreference.containsKey(mLocalSharedPreference.dataKeyName);
    }

    public String decryptData(FingerprintManager.AuthenticationResult result) {
        final Cipher cipher = result.getCryptoObject().getCipher();
        //取出secret key并返回
        String data = mLocalSharedPreference.getData(mLocalSharedPreference.dataKeyName);
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        try {
            byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.URL_SAFE));
            return new String(decrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encryptData(FingerprintManager.AuthenticationResult result, String data) {
        final Cipher cipher = result.getCryptoObject().getCipher();
        //将前面生成的data包装成secret key，存入沙盒
        try {
            byte[] encrypted = cipher.doFinal(data.getBytes());
            byte[] IV = cipher.getIV();
            String se = Base64.encodeToString(encrypted, Base64.URL_SAFE);
            String siv = Base64.encodeToString(IV, Base64.URL_SAFE);
            if (mLocalSharedPreference.storeData(mLocalSharedPreference.dataKeyName, se) &&
                    mLocalSharedPreference.storeData(mLocalSharedPreference.IVKeyName, siv)) {
                System.out.println("siv:"+siv);
                return se;
            } else {
                //几乎不可能到这里
                System.out.println("auth fail");
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            System.out.println("auth fail");
        }
        return null;
    }


}
