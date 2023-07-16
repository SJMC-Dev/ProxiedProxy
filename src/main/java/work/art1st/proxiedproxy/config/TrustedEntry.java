package work.art1st.proxiedproxy.config;

import lombok.Getter;
import work.art1st.proxiedproxy.util.RSAUtil;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class TrustedEntry {
    @Getter
    private final String id;
    private String publicKeyString;
    @Getter
    private String key;
    private PublicKey publicKey;

    public TrustedEntry(String id, String content, VerificationType type) {
        this.id = id;
        switch (type) {
            case KEY:
                this.key = content;
                break;
            case RSA:
                this.publicKeyString = content;
                break;
        }
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (publicKey == null) {
            publicKey = RSAUtil.convertStringToPublicKey(publicKeyString);
        }
        return publicKey;
    }

    public VerificationType getType() {
        if (publicKeyString != null) {
            return VerificationType.RSA;
        }
        if (key != null) {
            return VerificationType.KEY;
        }
        return null;
    }
}
