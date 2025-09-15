// Simple test to verify PasswordEncoder functionality
// This is a temporary test file to verify the implementation

import com.jooyeon.app.common.encryption.PasswordEncoder;

public class PasswordEncoderTest {
    public static void main(String[] args) {
        PasswordEncoder encoder = new PasswordEncoder();

        // Test encoding
        String password = "TestPassword123!";
        String encoded = encoder.encode(password);
        System.out.println("Original: " + password);
        System.out.println("Encoded: " + encoded);

        // Test matching
        boolean matches = encoder.matches(password, encoded);
        System.out.println("Matches: " + matches);

        // Test wrong password
        boolean wrongMatches = encoder.matches("WrongPassword", encoded);
        System.out.println("Wrong password matches: " + wrongMatches);

        // Test password strength
        System.out.println("Strong password test:");
        System.out.println("'TestPassword123!' is strong: " + encoder.isStrongPassword("TestPassword123!"));
        System.out.println("'weak' is strong: " + encoder.isStrongPassword("weak"));
        System.out.println("'Password123' is strong: " + encoder.isStrongPassword("Password123"));
    }
}