package hcen.central.inus.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class FirebaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(FirebaseInitializer.class.getName());
    private static final String ENV_VAR_NAME = "FIREBASE_CREDENTIALS";
    private static final String SYS_PROP_NAME = "firebase.credentials";

    private FirebaseApp firebaseApp;

    @PostConstruct
    public void init() {
        Optional<InputStream> credentialsStream = resolveCredentialsStream();

        if (credentialsStream.isEmpty()) {
            LOGGER.warning(() -> "Firebase credentials not configured; push notifications will be skipped.");
            return;
        }

        try (InputStream stream = credentialsStream.get()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();

            List<FirebaseApp> apps = FirebaseApp.getApps();
            if (apps.isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(options);
                LOGGER.info("Firebase app initialized for notifications.");
            } else {
                // Reuse already bootstrapped app inside the same JVM.
                firebaseApp = apps.get(0);
                LOGGER.info("Firebase app already initialized; reusing existing instance.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Firebase app; notifications will not be sent.", e);
        }
    }

    public boolean isReady() {
        return firebaseApp != null;
    }

    public FirebaseMessaging getMessaging() {
        if (!isReady()) {
            throw new IllegalStateException("Firebase not initialized. Verify credentials configuration.");
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private Optional<InputStream> resolveCredentialsStream() {
        // First look for external path provided via env var or system property.
        String configuredPath = Optional.ofNullable(System.getenv(ENV_VAR_NAME))
                .orElse(System.getProperty(SYS_PROP_NAME));

        if (configuredPath != null && !configuredPath.isBlank()) {
            Path path = Paths.get(configuredPath);
            if (Files.exists(path)) {
                try {
                    return Optional.of(Files.newInputStream(path));
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Unable to read Firebase credentials from configured path: " + configuredPath, e);
                }
            } else {
                LOGGER.warning(() -> "Configured Firebase credentials path not found: " + configuredPath);
            }
        }

        // Fallback: try to load from classpath (for local development only).
        InputStream classpathStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("firebase-service-account.json");

        return Optional.ofNullable(classpathStream);
    }
}
