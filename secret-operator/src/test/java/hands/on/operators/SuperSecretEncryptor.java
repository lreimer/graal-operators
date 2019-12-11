package hands.on.operators;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Simple class to encrypt string values using asymmetric crypto.
 */
public class SuperSecretEncryptor {

    private final Cipher cipher;

    SuperSecretEncryptor() throws GeneralSecurityException {
        this.cipher = Cipher.getInstance("RSA");
    }

    private void initialize() throws IOException, GeneralSecurityException {
        byte[] keyBytes = Files.readAllBytes(new File("src/test/resources/privateKey").toPath());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(spec);

        this.cipher.init(Cipher.ENCRYPT_MODE, privateKey);
    }

    private String encrypt(String text) throws GeneralSecurityException {
        byte[] bytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        if (args.length != 1) {
            System.err.println("Invalid program arguments. Specify text to encrypt.");
            System.exit(-1);
        }

        SuperSecretEncryptor encryptor = new SuperSecretEncryptor();
        encryptor.initialize();

        String encrypted = encryptor.encrypt(args[0]);
        System.out.println(encrypted);
    }
}
