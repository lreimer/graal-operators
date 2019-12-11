package hands.on.operators;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Simple class to decrypt secret string values using asymmetric crypto.
 */
public class SuperSecretDecryptor {

    private final Cipher cipher;

    SuperSecretDecryptor() throws GeneralSecurityException {
        this.cipher = Cipher.getInstance("RSA");
    }

    private void initialize() throws IOException, GeneralSecurityException {
        byte[] keyBytes = Files.readAllBytes(new File("src/test/resources/publicKey").toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        this.cipher.init(Cipher.DECRYPT_MODE, publicKey);
    }

    private String decrypt(String secret) throws GeneralSecurityException {
        byte[] decode = Base64.getDecoder().decode(secret);
        return new String(cipher.doFinal(decode), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        if (args.length != 1) {
            System.err.println("Invalid program arguments. Specify secret to decrypt.");
            System.exit(-1);
        }

        SuperSecretDecryptor decryptor = new SuperSecretDecryptor();
        decryptor.initialize();

        String decrypted = decryptor.decrypt(args[0]);
        System.out.println(decrypted);
    }
}
