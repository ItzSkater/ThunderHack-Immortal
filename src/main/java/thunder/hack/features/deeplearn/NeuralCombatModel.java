package thunder.hack.features.deeplearn;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.translate.TranslateException;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Java port of LiquidBounce NextGen's {@code TwoDimensionalRegressionModel}
 * (merged with {@code ModelWrapper}).
 *
 * The architecture mirrors the LB MLP — 128/64/32 hidden units with
 * batch-norm + ReLU activations, then a 2-unit linear head producing
 * (deltaYaw, deltaPitch) — so weight files shipped by LB load straight
 * into this network.
 */
public final class NeuralCombatModel implements Closeable {

    private static final long OUTPUTS = 2;

    private final String name;
    private final Model model;
    private final Predictor<float[], float[]> predictor;
    private volatile boolean loaded = false;

    public NeuralCombatModel(@NotNull String name) {
        this.name = name;
        this.model = Model.newInstance(name);
        this.model.setBlock(buildMlpBlock(OUTPUTS));
        this.predictor = model.newPredictor(new FloatArrayTranslator());
    }

    public @NotNull String name() {
        return name;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Load weights from a directory on disk (DJL "tf" format). */
    public void loadFromDirectory(@NotNull Path dir) throws IOException {
        try {
            model.load(dir, "tf");
            loaded = true;
        } catch (Exception e) {
            throw new IOException("Failed to load model dir " + dir, e);
        }
    }

    /** Load weights from a raw {@code .params} stream — used for built-in models. */
    public void loadFromStream(@NotNull InputStream stream) throws IOException {
        try {
            model.load(stream);
            loaded = true;
        } catch (Exception e) {
            throw new IOException("Failed to load model stream", e);
        }
    }

    /**
     * Try the user's models folder first, then fall back to a built-in
     * resource at {@code /assets/thunderhack/models/<name>.params}.
     */
    public void loadAuto() throws IOException {
        File folder = new File(DeepLearningEngine.MODELS_FOLDER, name);
        if (folder.exists() && folder.isDirectory()) {
            loadFromDirectory(folder.toPath());
            return;
        }
        String resource = "/assets/thunderhack/models/" + name.toLowerCase(Locale.ENGLISH) + ".params";
        try (InputStream in = NeuralCombatModel.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("No built-in model resource at " + resource);
            }
            loadFromStream(in);
        }
    }

    public float[] predict(float[] input) throws TranslateException {
        if (!DeepLearningEngine.isInitialized()) {
            throw new IllegalStateException("DeepLearningEngine is not initialized");
        }
        if (!loaded) {
            throw new IllegalStateException("Model '" + name + "' is not loaded");
        }
        return predictor.predict(input);
    }

    @Override
    public void close() {
        try {
            predictor.close();
        } catch (Throwable t) {
            ThunderHack.LOGGER.warn("[NeuralAura] predictor close failed", t);
        }
        try {
            model.close();
        } catch (Throwable t) {
            ThunderHack.LOGGER.warn("[NeuralAura] model close failed", t);
        }
        loaded = false;
    }

    private static SequentialBlock buildMlpBlock(long outputs) {
        return new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(64).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(32).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(outputs).build());
    }
}
