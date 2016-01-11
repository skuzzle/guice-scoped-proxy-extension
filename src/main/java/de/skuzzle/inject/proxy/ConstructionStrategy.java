package de.skuzzle.inject.proxy;

import com.google.inject.Injector;
import com.google.inject.internal.Errors;

/**
 * Strategy to customize the way in which proxy objects are created from an
 * enhanced Class object. Predefined implementations can be found in
 * {@link ConstructionStrategies}.
 *
 * @author Simon Taddiken
 */
public interface ConstructionStrategy {

    /**
     * Creates an instance of the provided class. The passed class will be an
     * enhanced class (either java reflection proxy or cglib proxy).
     *
     * @param proxyClass An enhanced proxy class.
     * @param injector The injector.
     * @param errors For collecting errors.
     * @return The created instance or null if an error occurred.
     */
    <T> T createInstance(Class<T> proxyClass, Injector injector, Errors errors);
}
