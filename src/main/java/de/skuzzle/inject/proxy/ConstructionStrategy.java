package de.skuzzle.inject.proxy;

import com.google.inject.Injector;
import com.google.inject.internal.Errors;

public interface ConstructionStrategy {

    <T> T createInstance(Class<T> proxyClass, Injector injector, Errors errors);
}
