package timecapsule.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for encryption and decryption operations.
 * Uses standard Java cryptography primitives:
 * - PBKDF2WithHmacSHA256 for key derivation from passphrase
 * - AES/GCM/NoPadding for authenticated encryption
 */
public class CryptoUtils {

    // ========================
    // Constants
    // ========================
    
    // AES key size in bits (256-bit for strong security)
    private static final int AES_KEY_SIZE = 256;
    
    // GCM authentication tag length in bits
    private static final int GCM_TAG_LENGTH = 128;
    
    // IV size for GCM mode (12 bytes is recommended)
    private static final int GCM_IV_LENGTH = 12;
    
    // Salt size for PBKDF2 (16 bytes)
    private static final int SALT_LENGTH = 16;
    
    // PBKDF2 iteration count (reasonable for intro-level, balance security and speed)
    private static final int PBKDF2_ITERATIONS = 100000;
    
    // Secure random generator for cryptographic operations
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypt a plaintext message using a passphrase.
     * 
     * @param plaintext The message to encrypt
     * @param passphrase The user's passphrase
     * @param associatedData Additional authenticated data (e.g., ownerEmail + unlockTime)
     * @return EncryptionResult containing ciphertext, IV, and salt (all Base64-encoded)
     */
    public static EncryptionResult encrypt(String plaintext, String passphrase, String associatedData) 
            throws Exception {
        
        // Generate random salt for key derivation
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        
        // Generate random IV for AES-GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        // Derive AES key from passphrase using PBKDF2
        SecretKey key = deriveKey(passphrase, salt);
        
        // Initialize AES-GCM cipher for encryption
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        
        // Add associated data (authenticated but not encrypted)
        // This binds the ciphertext to the owner and unlock time
        if (associatedData != null && !associatedData.isEmpty()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        
        // Encrypt the plaintext
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // Return all components Base64-encoded
        return new EncryptionResult(
            Base64.getEncoder().encodeToString(ciphertext),
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(salt)
        );
    }

    /**
     * Decrypt a ciphertext using a passphrase.
     * 
     * @param ciphertextBase64 Base64-encoded ciphertext
     * @param ivBase64 Base64-encoded initialization vector
     * @param saltBase64 Base64-encoded salt
     * @param passphrase The user's passphrase
     * @param associatedData Additional authenticated data (must match what was used during encryption)
     * @return The decrypted plaintext message
     */
    public static String decrypt(String ciphertextBase64, String ivBase64, String saltBase64,
                                  String passphrase, String associatedData) throws Exception {
        
        // Decode Base64 values
        byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        
        // Derive the same key using the same salt
        SecretKey key = deriveKey(passphrase, salt);
        
        // Initialize AES-GCM cipher for decryption
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        
        // Add the same associated data
        if (associatedData != null && !associatedData.isEmpty()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        
        // Decrypt and return the plaintext
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * Derive an AES key from a passphrase using PBKDF2.
     * 
     * @param passphrase The user's passphrase
     * @param salt Random salt bytes
     * @return A 256-bit AES key
     */
    private static SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        // Create PBKDF2 key specification
        PBEKeySpec spec = new PBEKeySpec(
            passphrase.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            AES_KEY_SIZE
        );
        
        // Generate the derived key bytes
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        
        // Clear the passphrase from memory
        spec.clearPassword();
        
        // Wrap as an AES key
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Container for encryption results.
     * All fields are Base64-encoded strings ready for JSON transmission.
     */
    public static class EncryptionResult {
        public final String ciphertextBase64;
        public final String ivBase64;
        public final String saltBase64;

        public EncryptionResult(String ciphertextBase64, String ivBase64, String saltBase64) {
            this.ciphertextBase64 = ciphertextBase64;
            this.ivBase64 = ivBase64;
            this.saltBase64 = saltBase64;
        }
    }
}
