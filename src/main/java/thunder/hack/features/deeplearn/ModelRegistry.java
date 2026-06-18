package thunder.hack.features.deeplearn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.ThunderHack;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight stand-in for LiquidBounce's {@code ModelManager}.
 *
 * Built-in model names mirror LB so the {@code .params} we ship in
 * resources/assets/thunderhack/models can be reused verbatim. User
 * models dropped into the {@code deeplearning/models} folder are
 * picked up on load alongside the built-ins.
 */
public final class ModelRegistry {

    private ModelRegistry() {}

    public static final String[] BUILT_IN = {"21KC11KP", "19KC8KP"};

    private static final Map<String, NeuralCombatModel> MODELS = Collections.synchronizedMap(new LinkedHashMap<>());

    /** Called from {@link DeepLearningEngine#initAsync()} after DJL is ready. */
    static void loadBuiltInModels() {
        for (String name : BUILT_IN) {
            tryLoad(name);
        }
        File[] userDirs = DeepLearningEngine.MODELS_FOLDER.listFiles(File::isDirectory);
        if (userDirs != null) {
            for (File dir : userDirs) {
                if (!MODELS.containsKey(dir.getName())) {
                    tryLoad(dir.getName());
                }
            }
        }
    }

    private static void tryLoad(@NotNull String name) {
        NeuralCombatModel model = new NeuralCombatModel(name);
        try {
            long start = System.currentTimeMillis();
            model.loadAuto();
            MODELS.put(name, model);
            ThunderHack.LOGGER.info("[NeuralAura] Loaded model '{}' in {} ms.",
                    name, System.currentTimeMillis() - start);
        } catch (Throwable t) {
            ThunderHack.LOGGER.error("[NeuralAura] Failed to load model '{}'.", name, t);
            model.close();
        }
    }

    public static @Nullable NeuralCombatModel get(@NotNull String name) {
        return MODELS.get(name);
    }

    public static @NotNull String[] availableModels() {
        synchronized (MODELS) {
            return MODELS.keySet().toArray(new String[0]);
        }
    }

    public static void reload() {
        synchronized (MODELS) {
            MODELS.values().forEach(NeuralCombatModel::close);
            MODELS.clear();
        }
        loadBuiltInModels();
    }
}
