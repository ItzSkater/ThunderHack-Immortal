package thunder.hack.features.deeplearn;

import ai.djl.ndarray.NDList;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/** Java port of FloatArrayInAndOutTranslator from LiquidBounce NextGen. */
public final class FloatArrayTranslator implements Translator<float[], float[]> {

    @Override
    public NDList processInput(TranslatorContext ctx, float[] input) {
        return new NDList(ctx.getNDManager().create(input));
    }

    @Override
    public float[] processOutput(TranslatorContext ctx, NDList list) {
        return list.get(0).toFloatArray();
    }
}
