package work.art1st.proxiedproxy.config;

import lombok.Getter;
import work.art1st.proxiedproxy.forwarding.VerificationType;
import work.art1st.proxiedproxy.util.RSAUtil;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class TrustedEntry {
    @Getter
    private String id;
    private String publicKey;
    @Getter
    private String key;
    private PublicKey _publicKey;

    public TrustedEntry(String id, String content, VerificationType type) {
        this.id = id;
        switch (type) {
            case KEY:
                this.key = content;
                break;
            case RSA:
                this.publicKey = content;
                break;
        }
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (_publicKey == null) {
            _publicKey = RSAUtil.convertStringToPublicKey(publicKey);
        }
        return _publicKey;
    }

    public VerificationType getType() {
        if (publicKey != null) {
            return VerificationType.RSA;
        }
        if (key != null) {
            return VerificationType.KEY;
        }
        return null;
    }
}
