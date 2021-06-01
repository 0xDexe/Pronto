package com.dualfie.maindirs.helpers;

import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {

    public static final String ENCRYPT_SEED = "!5K_@ARfPV_h2_s5H_KBAf9aZCqUg$M$D3sdZMbx2Kt%q&G*PgPet98JTE56#Mr-Hx&WqXHej6+mvMs=QzkM^!a%Fp4!L-rsb3GXr4H_=r3y9w4TSgVmv3UXSmf@5REt?$nnMzGe#&e-mnYhS@%GZ_*cxXAq?hvCbszJeK#$UBbjcWLd@*6nD-m%j?jGd73pS8QPRMK=yWVx4y?*M+SM$F344JfNACJ+-pmbGCnTwe2Ct_%QaxP+4PRy#wZwbx4NBs?x!kzD^r5jKmYFNJaBBqEtcurwbapLq%c5sUDRRtgQWj@cE$#?*c@=P+pcLNhN?Zuz3n^FdLE47u@5@XPxJe-h7BM-QP#KufDtfmpHC_ffVK*?mvsz@t#h8Uq!g@^Udm3hrHqthnSY?kHtBR!4b26tc*m2uJaREAx^Md=@Zd*H=Fq$j^5jmrnjHCQdU?BaD5KRr2dWvr=F#ZD7vSv^cx62SL3jPSRHaaNngH*fZuz4TW7Va&8=jEGK6pbvzXCj";


    public static String encrypt(String seed, String cleartext) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] result = encrypt(rawKey, cleartext.getBytes());
        return toHex(result);
    }

    public static String decrypt(String seed, String encrypted) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] enc = toByte(encrypted);
        byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }

    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }


    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES/ECB/NoPadding");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES/ECB/NoPadding");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private final static String HEX = "0123456789ABCDEF";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }

    public String randomKey() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String key = sb.toString();
        return key;
    }
}