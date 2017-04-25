package com.example.apple.myapplication;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Encryption {

    public byte[] cryptByRSA(byte[] data, Key key, String algorithm, int mode) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);

            // Initiate the cipher.
            if (mode == Cipher.ENCRYPT_MODE)
                cipher.init(Cipher.ENCRYPT_MODE, (RSAPublicKey) key);
            else
                cipher.init(Cipher.DECRYPT_MODE, (RSAPrivateKey) key);

            // Encrypt/Decrypt the data.
            return cipher.doFinal(data);

        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
