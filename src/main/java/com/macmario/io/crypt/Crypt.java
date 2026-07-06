/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.crypt;

import com.macmario.general.Version;
import com.macmario.io.file.WriteFile;
import com.macmario.main.Main;
import com.macmario.net.tcp.Host;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Layered AES encryption/decryption multi-tool.
 *
 * <p>This class merges the former {@code CryptHigh} engine: the low-level AES
 * primitives ({@link #cryptStr}/{@link #uncryptStr}/{@link #uncryptByte}) now
 * live here directly. Each logical "engine" (default, host, user, custom) is
 * just an AES key, represented by its derived {@code pass} string &mdash; the
 * only per-engine state, since the AES key is the sole input to the cipher.
 *
 * @author SuMario
 */
public class Crypt extends Version {
    private static final long serialVersionUID = 4709L;

    final private String secCipher = "AES";
    final private byte[] base64Alphabet;
    final private int base64Length = 255;
    final private byte PAD = (byte) '=';

    private final UUID uuid;
    private String Ukey; //Ukey="5fa4a40a-53b4-4f7a-b132-61bd19b79a8e";
    private String host = Host.getHostname();
    private String user = getUserKey();

    // AES key material per engine; passDefault is the former "ch" engine.
    private String passDefault;
    private String passHost;
    private String passUser;
    private String passCust = null;
    private boolean custEnabled = false;

    private boolean doing;
    int maxKeyLen;

    private int cryptLevel = 0;
    private String charUser;
    private String charHost;

    public Crypt() {
        Ukey = (super.readPropertyFromRessource("/com/macmario/main/main.properties")).getProperty("UKEY", "");
        uuid = UUID.fromString(Ukey);
        passDefault = getPass(uuid.toString());
        base64Alphabet = new byte[base64Length];
        initCipher();
        initBase64();
        refreshKeys();
    }

    public Crypt(Main m) {
        this();
    }

    public void setHostKey(String info) {
        if (info == null || info.isEmpty()) { return; }
        host = getUUIDCode(info).toString();
        refreshKeys();
    }

    public void setUserKey(String info) {
        if (info == null || info.isEmpty()) { return; }
        user = getUUIDCode(info).toString();
        refreshKeys();
    }

    public void setCustomKey(String info) {
        if (info == null || info.isEmpty()) { return; }
        passCust = getPass(getUUIDCode(info).toString());
        custEnabled = true;
    }

    public void setCryptLevel(int level) {
        this.cryptLevel = (level > 0) ? level : 0;
    }

    public boolean updateUKey(UUID u) { return updateUKey(u.toString()); }
    public boolean updateUKey(String u) {
        boolean b = (u != null && !u.isEmpty());
        if (b) {
            Ukey = u;
            passCust = getPass(UUID.fromString(Ukey).toString());
        }
        return b;
    }

    private void refreshKeys() {
        this.passHost = getPass(getUUIDCode(host).toString());
        this.charHost = "<"+host;
        this.passUser = getPass(getUUIDCode(user).toString());
        this.charUser = "<"+user;
    }

    private void initBase64() {
        for (int i = 0;   i < base64Length; i++) { this.base64Alphabet[i] = (byte) -1; }
        for (int i = 'Z'; i >= 'A';         i--) { this.base64Alphabet[i] = (byte) (i - 'A');      }
        for (int i = 'z'; i >= 'a';         i--) { this.base64Alphabet[i] = (byte) (i - 'a' + 26); }
        for (int i = '9'; i >= '0';         i--) { this.base64Alphabet[i] = (byte) (i - '0' + 52); }
        this.base64Alphabet[62] = (byte) '+';
        this.base64Alphabet[63] = (byte) '/';
    }

    /**
     * One-time JCE setup: lift the legacy unlimited-strength restriction on old
     * JREs and determine whether real AES is available ({@code doing}).
     */
    private void initCipher() {
        final String func = "Crypt::initCipher() - ";
        try {
            int v = getJavaMainVersion();
            if (v <= 7 || (v == 8 && getJavaMinVersion() < 152)) {
                Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
                int modify = field.getModifiers();
                if (Modifier.isFinal(modify) && Modifier.isStatic(modify) && Modifier.isPrivate(modify)) {
                    field.setAccessible(true);
                    field.setBoolean(null, java.lang.Boolean.FALSE);
                    field.setAccessible(false);
                } else {
                    throw new RuntimeException("newer JRE/JDK used");
                }
            }
        } catch (ClassNotFoundException cnf) { printf(func, 1, "strength isRestricted class set error  : " + cnf.getMessage(), cnf); }
          catch (IllegalAccessException iae) { printf(func, 1, "strength isRestricted access set error : " + iae.getMessage(), iae); }
          catch (NoSuchFieldException   nsfe) { printf(func, 1, "strength isRestricted field error : "     + nsfe.getMessage(), nsfe); }
          catch (RuntimeException       re)   { printf(func, 1, "strength isRestricted set error : "       + re.getMessage(), re); }

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(secCipher, "SunJCE");
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException ne) {
            printf(func, 1, "ERROR: " + ne.getMessage(), ne);
            cipher = null;
        }
        doing = (cipher != null);

        try {
            maxKeyLen = Cipher.getMaxAllowedKeyLength(secCipher);
            printf(func, 2, "maxKeyLen:" + maxKeyLen);
        } catch (NoSuchAlgorithmException ne) {}
        println(1, func + "max length = " + maxKeyLen + " (" + doing + ")");
    }

    private UUID getUUIDCode(String info) {
        try {
            return UUID.fromString(info);
        } catch (java.lang.IllegalArgumentException iae) {
            StringBuilder sw = new StringBuilder();

            byte[] b = getMD5(info).getBytes();  // [8]-[4]-[4]-[12]
            for (int i = 0; i < 27; i++) {
                if (i == 8 || i == 12 || i == 16 || i == 20) { sw.append("-"); }
                if (i < 27) {
                    if (i < b.length) { sw.append((char) b[i]); }
                    else { sw.append("1"); }
                }
            }

            return UUID.fromString(sw.toString());
        }
    }

    public String getMD5(String info) { return MD5.toMD5Hash(info); }

    /**
     * MD5-derived AES key material for an engine. Kept byte-identical to the
     * former {@code CryptHigh.getPass} so previously encrypted data stays
     * decryptable.
     */
    private String getPass(String info) {
        String ret = "blank" + getVersion();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((info.getBytes()));
            byte[] mdbytes = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException|NullPointerException e) {}
        return ret;
    }

    public boolean isBase64Regex(String txt) {
        try {
            return txt.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
        } catch (NullPointerException io) {
        }
        return false;
    }
    public boolean isCrypted(StringBuilder txt) {
        if (isNotNullOrEmpty(txt)) {
            return isCrypted(txt.toString());
        }
        return false;
    }
    public boolean isCrypted(String txt) {
        if (isNullOrEmpty(txt)) { return false; }
        if (isAscii(txt)) {
            //System.out.println("isAscii:" + txt);
            if ( isBase64(txt) ) {
                //System.out.println("isAscii/isBase64:" + txt);
                if ( ! isAscii(this.getUnBase64(txt)) ) {
                    String s = getUnCrypted(txt);
                    if ( ! txt.equals(s) ) {
                        //System.out.println("isAscii/isBase64/isCrypted:" + txt);
                        return true;
                    }
                }  
            }
        }
        return false;
    }

    public boolean isCrypted(byte[] b) {
        boolean br = false;
        for (int i = 0; i < b.length; i++) {
            if (!isBase64(b[i])) {
                return false;
            }
        }
        if (b[b.length - 1] == 61) { br = true; }
        return br;
    }

    public boolean isBase64(byte oct) {
        boolean ret = false;
        if      (oct == PAD)                { ret = true;  }
        else if (oct < 0)                   { ret = false; }
        else if (base64Alphabet[oct] == -1) { ret = false; }
        else                                { ret = true;  }
        return ret;
    }

    public boolean isBase64(String s) { return ( this.isBase64Regex(s) && isBase64(s.getBytes()));    }
    public boolean isBase64(byte[] b) { return getUnBase64(b).length > 1; }
    public byte[] getUnBase64(String b) { return getUnBase64(b.getBytes()); }
    public byte[] getUnBase64(byte[] b) {
        try { 
            byte[] b1=Base64.getDecoder().decode(b);
            return b1;
        } catch (IllegalArgumentException | NullPointerException io) {
        }
        return new byte[0];
    }
    public byte[] getBase64(byte[] b) {
        try {
            return Base64.getEncoder().encode(b);
        }catch (IllegalArgumentException | NullPointerException io) {
        }
        return new byte[0];
    }

    // ----- low-level AES engine (formerly CryptHigh) -----------------------

    /** Encrypt {@code txt} with the AES key {@code pass}; returns Base64 text. */
    private String cryptStr(String txt, String pass) {
        final String func = "cryptStr(String txt, String pass)";
        printf(func, 1, "doing:" + doing + ":");

        if (!doing) { return new String(getBase64(txt.getBytes())); }
        byte[] b = encrypt(txt, pass);

        if (b == null) {
            printf(func, 1, "return:: - NULL");
            return "";
        }

        printf(func, 1, "return:" + b.length + ":");
        return new String(getBase64(b));
    }

    /** Decrypt Base64 {@code info} with the AES key {@code pass}. */
    private String uncryptStr(String info, String pass) {
        final String func = "uncryptStr(String info, String pass)";
        if (!doing) {
            if (info != null) {
                byte[] b = getUnBase64(info);
                StringBuilder sw = new StringBuilder();
                for (int i = 0; i < b.length; i++) { sw.append((char) b[i]); }
                printf(func, 4, "return sw |" + sw.toString() + "|");
                return sw.toString();
            }
            printf(func, 4, "return info [!doing] |" + info + "|");
            return info;
        }
        byte[] b = null;
        try {
            b = getUnBase64(info);
        } catch (NullPointerException | IllegalArgumentException np) {
            printf(func, 0, "error base64 - " + np.getMessage());
            b = null;
        }

        if (b != null) {
            String s = new String(decrypt(b, pass));
            if (s == null) { return info; }

            StringBuilder sw = new StringBuilder();
            int c = 0;
            checkOutter:
            while (c < s.length()) {
                char ch = s.charAt(c);
                if (ch != 0) { sw.append(ch); } else { break checkOutter; }
                c++;
            }
            println(6, func + "return |" + replacePass(sw.toString()) + "|");
            return sw.toString();
        }
        printf(func, 4, "return info |" + info + "|");
        return info;
    }

    private byte[] uncryptByte(String info, String pass) {
        if (info != null && info.endsWith("=")) {
            byte[] b = getUnBase64(info);
            if (!doing) { return b; }
            b = decrypt(b, pass);
            return b;
        }
        return new byte[0];
    }

    private byte[] encrypt(String plainText, String pw) {
        final String func = "encrypt(String plainText, String pw)";
        try {
            printf(func, 2, "pw:" + pw + ":");
            SecretKeySpec skeySpec = new SecretKeySpec((pw).getBytes(), secCipher);
            printf(func, 4, "spec completed");
            Cipher ci = Cipher.getInstance(secCipher);
            printf(func, 4, "init start " + skeySpec + " " + ci);
            ci.init(Cipher.ENCRYPT_MODE, skeySpec);

            printf(func, 4, "init completed");

            byte[] original = getBase64(ci.doFinal(plainText.getBytes()));
            return original;
        } catch (NoSuchAlgorithmException
                | NullPointerException
                | InvalidKeyException
                | NoSuchPaddingException
                | BadPaddingException
                | IllegalBlockSizeException e) {
            printf(func, 1, "ERROR: message - " + e.getMessage(), e);
        }
        printf(func, 4, "return blank");
        return "".getBytes();
    }

    private byte[] decrypt(byte[] encData, String pass) {
        final String func = "decrypt(byte[] encData, String pass)";
        try {
            printf(func, 1, "init skeySpek with " + secCipher);
            SecretKeySpec skeySpec = new SecretKeySpec((pass).getBytes(), secCipher);
            printf(func, 1, "init cipher ci with " + secCipher);
            Cipher ci = Cipher.getInstance(secCipher);

            ci.init(Cipher.DECRYPT_MODE, skeySpec);

            printf(func, 1, "init cipher ci complete");

            byte[] original = ci.doFinal(getUnBase64(encData));
            printf(func, 1, "return " + ((original != null) ? original.length : "NULL"));
            return original;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            printf(func, 0, "ERROR: " + e.getMessage(), e);
        }
        return "".getBytes();
    }

    // ----- layered orchestration -------------------------------------------

    public String getCrypted(String txt) {
        String out = getUserCrypted(txt);
               out = getHostCrypted(out);
               out = getCustCrypted(out);
        return cryptStr(out, passDefault);
    }

    private String getUserCrypted(String txt) {
        if (cryptLevel > 0) {
            StringBuilder sw = new StringBuilder();
                          sw.append(cryptStr(getHostCrypted(txt), passUser));
            return "<" + user + " md5=\"" + getMD5(sw.toString()) + "\">" + sw.toString() + "</" + user + ">";
        }
        return txt;
    }

    private String getHostCrypted(String txt) {
        if (cryptLevel > 1) {
            StringBuilder sw = new StringBuilder();
                          sw.append(cryptStr(txt, passHost));
            return "<" + host + ">" + sw.toString() + "</" + host + ">";
        }
        return txt;
    }

    private String getCustCrypted(String txt) {
        if (custEnabled) {
            StringBuilder sw = new StringBuilder();
                          sw.append(cryptStr(txt, passCust));
            return sw.toString();
        }
        return txt;
    }

    public String getUnCrypted(String info) {
        String out = uncryptStr(info, passDefault);
               out = getUnCryptedCust(out);
               out = getUnCryptedHost(out);
               out = getUnCryptedUser(out);
               out = getUnCryptedHost(out);
        return out;
    }
    private String getUnCryptedHost(String info) {
        final String a = "<" + host; final String e = "</" + host + ">";
        if (info.startsWith(a) && info.endsWith(e)) {
            String[] ab = info.split(">");
            String f = info.substring(ab[0].length() + 1, info.length() - e.length());
            String md5 = getCryptedMD5(ab[0]);
            if (md5.isEmpty() || (!md5.isEmpty() && md5.matches(getMD5(f)))) {
                return uncryptStr(f, passHost);
            }
        }
        return info;
    }
    private String getUnCryptedUser(String info) {
        final String a = "<" + user; final String e = "</" + user + ">";
        if (info.startsWith(a) && info.endsWith(e)) {
            String[] ab = info.split(">");
            String f = info.substring(ab[0].length() + 1, info.length() - e.length());
            String md5 = getCryptedMD5(ab[0]);
            if (md5.isEmpty() || (!md5.isEmpty() && md5.matches(getMD5(f)))) {
                return uncryptStr(f, passUser);
            }
        }
        return info;
    }

    synchronized private String getCryptedMD5(String info) {
        String[] sp = info.split("\"");
        for (int i = 0; i < sp.length; i++) {
            if (sp[i].endsWith("md5=") && i + i < sp.length) { return sp[++i]; }
        }
        return "";
    }

    private String getUnCryptedCust(String info) {
        if (custEnabled) {
            String out = uncryptStr(info, passCust);
            return out;
        }
        return info;
    }

    public byte[] getUnCryptedByte(String info) {
        return uncryptByte(info, passDefault);
    }

    public byte[] getCryptedByte(String info) {
        return cryptStr(info, passDefault).getBytes();
    }

    public void runArgs(String[] args) {
        boolean test = false;  String cust = Host.getHostname() + "1234@456789-0";
        ArrayList<String> ar = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-d")) { debug++; } else { ar.add(args[i]); }
        }
        if (!ar.isEmpty()) {
            args = new String[ar.size()];
            for (int i = 0; i < ar.size(); i++) { args[i] = ar.get(i); }
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].matches("-max")) {
                setCryptLevel(Integer.parseInt(args[++i]));

            }
            else if (args[i].matches("-custkey"))    { cust = args[++i];    }
            else if (args[i].matches("-usecustkey")) { setCustomKey(cust);  }
            else if (args[i].matches("-test")) {
                int j = args.length;
                for (j = ++i; j < args.length; j++) {
                    String s = args[j];
                    if (s.equals("-d")) {  } else {
                        boolean b = isCrypted(s);
                        String en = getCrypted(s);
                        String des= getUnCrypted(s);
                        String de = getUnCrypted(en);
                        String ma = (s.equals(de)) ? "YES" : "NO";
                        String mas= (s.equals(de) || s.equals(en) || des.equals(de)) ? "YES" : "NO";
                        log(0, "main(String[] args) TESTING:" + s + ": (isCrypted:"+( (b)?"TRUE":"FALSE")+")\nENCODED :" + en + ":\nDECODED :" + de + ":\nDECODED :" + getUnCrypted(s) + ": (income)\nMATCHING:" + ma + "\nMATCHING(EN):"+mas+"\n");
                    }
                }
                i = j;
                test = true;
            }
            else if (args[i].matches("-version")     && !test) { System.out.println("Crypt v" + this.getVersion() + " of " + this.getFullInfo()); }
            else if (args[i].matches("-crypt") && !test) {
                WriteFile fa = new WriteFile(args[++i]);
                if (!fa.isReadableFile()) {
                    String s = getCrypted(args[i]);
                    System.out.println(s);
                } else {
                    if (!fa.isBinaryFile()) {
                        String s = fa.readOut().toString();
                               s = getCrypted(s);
                        fa.replace(s);
                    } else {
                        System.out.println("WARNING:  do not handle binary files ");
                    }
                }
            }
            else if (args[i].matches("-uncrypt") && !test) {
                WriteFile fa = new WriteFile(args[++i]);
                if (!fa.isReadableFile()) {
                    String s = getUnCrypted(args[i]);
                } else {
                    if (!fa.isBinaryFile()) {
                        String s = fa.readOut().toString();
                               s = getUnCrypted(s);
                        fa.replace(s);
                    } else {
                        System.out.println("WARNING:  do not handle binary files ");
                    }
                }
            } else {
                System.out.println("i=" + i + ": |" + args[i] + "|");
                System.out.println(usage(true));
            }
        }

    }

    public static String getRandomID() {
        String x = ("" + Math.random());
        return x.substring(2);
    }

    public static void main(String[] args) throws Exception {
        Crypt c = new Crypt();
              c.runArgs(args);
    }

    public String usage(boolean b) {
        StringBuilder sw = new StringBuilder();
        if (b) sw.append("usage() : ");
        sw.append("[-max <0..2>] [-custkey <custom key> -usecustkey] <-crypt|-uncrypt> <String|File>");
        return sw.toString();
    }
    
    
}
