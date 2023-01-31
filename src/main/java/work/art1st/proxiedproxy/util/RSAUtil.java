package work.art1st.proxiedproxy.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtil {
    private static final int KEY_SIZE = 3072;

    public static KeyPair genKeyPair() {
        try {
            KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
            kpGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = kpGen.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveKeyPair(KeyPair keyPair, File publicKeyFile, File privateKeyFile) throws IOException {
        FileWriter publicFileWriter = new FileWriter(publicKeyFile, false);
        String publicKeyString = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        publicFileWriter.write("-----BEGIN PUBLIC KEY-----\n");
        int start = 0;
        int len = publicKeyString.length();
        while (start < len) {
            publicFileWriter.write(publicKeyString.substring(start, Math.min(start + 64, len)) + '\n');
            start += 64;
        }
        publicFileWriter.write("-----END PUBLIC KEY-----\n");
        publicFileWriter.flush();
        publicFileWriter.close();
        FileWriter privateFileWriter = new FileWriter(privateKeyFile, false);
        String privateKeyString = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        privateFileWriter.write("-----BEGIN PRIVATE KEY-----\n");
        start = 0;
        len = privateKeyString.length();
        while (start < len) {
            privateFileWriter.write(privateKeyString.substring(start, Math.min(start + 64, len)) + '\n');
            start += 64;
        }
        privateFileWriter.write("-----END PRIVATE KEY-----\n");
        privateFileWriter.flush();
        privateFileWriter.close();
    }

    public static KeyPair readKeyPair(File publicKeyFile, File privateKeyFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKey = new String(Files.readAllBytes(publicKeyFile.toPath()));
        String privateKey = new String(Files.readAllBytes(privateKeyFile.toPath()));
        return new KeyPair(convertStringToPublicKey(publicKey), convertStringToPrivateKey(privateKey));
    }

    public static PublicKey convertStringToPublicKey(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKeyPEM = input
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replace("-----END PUBLIC KEY-----", "");

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPEM));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(keySpec);
    }

    public static PrivateKey convertStringToPrivateKey(String input) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyPEM = input
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("\n", "")
                .replace("-----END PRIVATE KEY-----", "");

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPEM));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(keySpec);
    }

    public static String sign(String content, PrivateKey privateKey) throws InvalidKeyException {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(content.getBytes());
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean validate(String content, String signature, PublicKey publicKey) throws InvalidKeyException {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initVerify(publicKey);
            signer.update(content.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return signer.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
}
