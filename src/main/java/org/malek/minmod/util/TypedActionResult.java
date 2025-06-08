package org.malek.minmod.util;

import net.minecraft.util.ActionResult;

/**
 * Temporary implementation of TypedActionResult to allow compilation.
 * This should be removed once the dependency issues are resolved.
 */
public class TypedActionResult<T> {
    private final ActionResult result;
    private final T value;

    public TypedActionResult(ActionResult result, T value) {
        this.result = result;
        this.value = value;
    }

    public ActionResult getResult() {
        return this.result;
    }

    public T getValue() {
        return this.value;
    }

    public static <T> TypedActionResult<T> success(T value) {
        return new TypedActionResult<>(ActionResult.SUCCESS, value);
    }

    public static <T> TypedActionResult<T> consume(T value) {
        return new TypedActionResult<>(ActionResult.CONSUME, value);
    }

    public static <T> TypedActionResult<T> pass(T value) {
        return new TypedActionResult<>(ActionResult.PASS, value);
    }

    public static <T> TypedActionResult<T> fail(T value) {
        return new TypedActionResult<>(ActionResult.FAIL, value);
    }
}
