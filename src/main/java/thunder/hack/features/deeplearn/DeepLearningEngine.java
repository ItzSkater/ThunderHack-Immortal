package thunder.hack.features.deeplearn;

import ai.djl.engine.Engine;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ConfigManager;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java port of LiquidBounce NextGen's DeepLearningEngine.
 *
 * Owns the DJL/PyTorch lifecycle: configures cache dirs under the
 * shared ThunderHackRecode config tree (so Recode users get the same
 * layout) and lazily initialises the engine off the main thread.
 */
public final class DeepLearningEngine {

    private DeepLearningEngine() {}

    public static final File ROOT_FOLDER = new File(ConfigManager.MAIN_FOLDER, "deeplearning");
    public static final File DJL_CACHE_FOLDER = new File(ROOT_FOLDER, "djl");
    public static final File ENGINES_CACHE_FOLDER = new File(ROOT_FOLDER, "engines");
    public static final File MODELS_FOLDER = new File(ROOT_FOLDER, "models");

    private static final AtomicBoolean initStarted = new AtomicBoolean(false);
    private static volatile boolean initialized = false;
    private static volatile String engineDescription = "uninitialized";

    static {
        ROOT_FOLDER.mkdirs();
        DJL_CACHE_FOLDER.mkdirs();
        ENGINES_CACHE_FOLDER.mkdirs();
        MODELS_FOLDER.mkdirs();

        System.setProperty("DJL_CACHE_DIR", DJL_CACHE_FOLDER.getAbsolutePath());
        System.setProperty("ENGINE_CACHE_DIR", ENGINES_CACHE_FOLDER.getAbsolutePath());
        System.setProperty("OPT_OUT_TRACKING", "true");
        System.setProperty("DJL_DEFAULT_ENGINE", "PyTorch");
        System.setProperty("PYTORCH_FLAVOR", "cpu");
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static @NotNull String describeEngine() {
        return engineDescription;
    }

    /**
     * Kicks off engine initialisation on a background thread. Repeated
     * calls are cheap — only the first one actually loads DJL.
     */
    public static CompletableFuture<Void> initAsync() {
        if (!initStarted.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                ThunderHack.LOGGER.info("[NeuralAura] Initializing DJL engine...");
                Engine engine = Engine.getInstance();
                engineDescription = engine.getEngineName() + " " + engine.getVersion()
                        + " on " + engine.defaultDevice().getDeviceType().toUpperCase(Locale.ENGLISH);
                ThunderHack.LOGGER.info("[NeuralAura] Using deep learning engine {}.", engineDescription);

                ModelRegistry.loadBuiltInModels();
                initialized = true;
            } catch (Throwable t) {
                ThunderHack.LOGGER.error("[NeuralAura] Failed to initialize DJL engine.", t);
                initStarted.set(false);
            }
        });
    }
}
