package org.mitalex.kurs;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class CryptoUtils {
    private static final String AES = "AES";

    public static String encrypt(String value, File keyFile)
            throws GeneralSecurityException, IOException
    {
        if (!keyFile.exists()) {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES);
            keyGen.init(128);
            SecretKey sk = keyGen.generateKey();
            FileWriter fw = new FileWriter(keyFile);
            fw.write( DatatypeConverter.printHexBinary(sk.getEncoded()) );
            fw.flush();
            fw.close();
        }

        SecretKeySpec sks = getSecretKeySpec(keyFile);
        Cipher cipher = Cipher.getInstance(CryptoUtils.AES);
        cipher.init(Cipher.ENCRYPT_MODE, sks, cipher.getParameters());
        byte[] encrypted = cipher.doFinal(value.getBytes());
        return DatatypeConverter.printHexBinary(encrypted);
    }

    /**
     * decrypt a value
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static String decrypt(String message, File keyFile)
    {
        try {
            SecretKeySpec sks = getSecretKeySpec(keyFile);
            Cipher cipher = Cipher.getInstance(CryptoUtils.AES);
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] decrypted = cipher.doFinal(DatatypeConverter.parseHexBinary(message));
            return new String(decrypted);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    /**
     * @param keyFile
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private static SecretKeySpec getSecretKeySpec(File keyFile)
            throws NoSuchAlgorithmException, IOException {
        byte [] key = Files.readAllBytes(keyFile.toPath());
        SecretKeySpec sks = new SecretKeySpec(key, CryptoUtils.AES);
        return sks;
    }


}
