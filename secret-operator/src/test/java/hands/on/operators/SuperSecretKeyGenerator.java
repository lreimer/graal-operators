package hands.on.operators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

/**
 * Simple tool to generate some RSA keys.
 */
public class SuperSecretKeyGenerator {

    private final KeyPairGenerator keyPairGenerator;

    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    SuperSecretKeyGenerator(int keysize) throws NoSuchAlgorithmException {
        this.keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        this.keyPairGenerator.initialize(keysize);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        SuperSecretKeyGenerator generator = new SuperSecretKeyGenerator(2048);
        generator.generateKeyPair();
        generator.exportPrivateKey("src/test/resources/privateKey");
        generator.exportPublicKey("src/test/resources/publicKey");
    }

    private void generateKeyPair() {
        this.keyPair = keyPairGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    private void exportPrivateKey(String filename) {
        try (FileOutputStream fos = new FileOutputStream(new File(filename))) {
            fos.write(privateKey.getEncoded());
        } catch (IOException e) {
            System.err.println("Error exporting private key: " + e.getMessage());
        }
    }

    private void exportPublicKey(String filename) {
        try (FileOutputStream fos = new FileOutputStream(new File(filename))) {
            fos.write(publicKey.getEncoded());
        } catch (IOException e) {
            System.err.println("Error exporting public key: " + e.getMessage());
        }
    }
}
