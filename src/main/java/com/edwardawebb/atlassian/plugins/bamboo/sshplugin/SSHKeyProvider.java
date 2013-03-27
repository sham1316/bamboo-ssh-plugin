package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import com.atlassian.bamboo.utils.error.ErrorCollection;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SSHKeyProvider implements net.schmizz.sshj.userauth.keyprovider.KeyProvider
{

    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    
    public SSHKeyProvider(String privateKeyString, String passphrase)
    {
        super();
        
        FileKeyProvider keyProvider = new PKCS8KeyFile.Factory().create();

        keyProvider.init(privateKeyString, null, PasswordUtils.createOneOff(passphrase.toCharArray()));
        
        try
        {
            this.publicKey = keyProvider.getPublic();
            this.privateKey = keyProvider.getPrivate();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PrivateKey getPrivate() throws IOException
    {
        return privateKey;
    }

    @Override
    public PublicKey getPublic() throws IOException
    {
        return publicKey;
    }

    @Override
    public KeyType getType() throws IOException
    {
        return KeyType.fromKey(publicKey);
    }

    /**
     * Validates that if the key is supplied, it is usable
     * @param privateKey
     * @param errorCollection an error message is added if the return value is false.
     * @return false if the key is supplied but invalid, true if it is usable or absent.
     */
    public static boolean validatePrivateKey(String privateKey, String passphrase, @NotNull final ErrorCollection errorCollection)
    {
        if (!StringUtils.isEmpty(privateKey))
        {
            new SSHClient(); // initialize 
            try {
                new SSHKeyProvider(privateKey, passphrase);
            }
            catch (RuntimeException e)
            {
                errorCollection.addError("private_key", "There is something wrong with your private key: " + e.getMessage());
                return false;
            }
        }
        return true;
    }
}
