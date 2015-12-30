package de.skuzzle.inject.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.objenesis.Objenesis;
import org.objenesis.instantiator.ObjectInstantiator;

import com.google.inject.Injector;
import com.google.inject.internal.Errors;

/**
 * Holds some useful default {@link ConstructionStrategy construction
 * strategies} for building scoped proxy objects for concrete classes.
 *
 * @author Simon Taddiken
 */
public enum ConstructionStrategies implements ConstructionStrategy {
    /**
     * Uses Objenesis to bypass all constructor restrictions.
     */
    OBJENESIS {

        @Override
        public <T> T createInstance(Class<T> proxyClass, Injector injector,
                Errors errors) {
            final Objenesis objenesis = ObjenesisHolder.getInstance();
            final ObjectInstantiator<T> instantiator = objenesis.getInstantiatorOf(
                    proxyClass);

            return instantiator.newInstance();
        }
    },
    /**
     * Calls the injectable constructor of the scoped proxy type by passing
     * <code>null</code> as value for each parameter. This strategy can not be
     * used if the type bound as proxy performs any actions on the parameters
     * passed to its constructor.
     */
    NULL_VALUES {

        @Override
        public <T> T createInstance(Class<T> proxyClass, Injector injector,
                Errors errors) {
            try {
                final Constructor<T> ctor= getConstructorFor(proxyClass);
                final Object[] args = new Object[ctor.getParameterCount()];
                return callConstructor(ctor, errors, args);
            } catch (final NoSuchMethodException e) {
                errors.addMessage(e.getMessage());
                return null;
            }

        }
    },

    /**
     * Completely forbids constructor injection on types bound as scoped proxy.
     * Using this strategy, only types that have a public no-argument
     * constructor can be bound as scoped proxy.
     */
    FAIL_ON_CONSTRUCTOR {

        @Override
        public <T> T createInstance(Class<T> proxyClass, Injector injector,
                Errors errors) {
            try {
                final Constructor<T> ctor = proxyClass.getConstructor();
                return callConstructor(ctor, errors, new Object[0]);
            } catch (NoSuchMethodException | SecurityException e) {
                errors.addMessage("scoped proxy '%s' has no no-argument constructor. " +
                    "Use a different ConstructionStrategy to create proxies of that " +
                    "object.", proxyClass.getName());
                return null;
            }
        }
    };

    @SuppressWarnings("unchecked")
    protected <T> Constructor<T> getConstructorFor(Class<T> proxyClass)
            throws NoSuchMethodException {
        final Collection<Constructor<?>> ctors = Arrays
                .stream(proxyClass.getConstructors())
                .filter(member -> !Modifier.isPrivate(member.getModifiers()))
                .collect(Collectors.toList());

        if (ctors.isEmpty()) {
            throw new NoSuchMethodException(String.format(
                    "No accessible constructor can be found on type %s",
                    proxyClass.getName()));
        } else if (ctors.size() > 1) {
            throw new NoSuchMethodException(String.format(
                    "Type %s has multiple accessible constructors",
                    proxyClass.getName()));
        }

        return (Constructor<T>) ctors.iterator().next();
    }

    protected <T> T callConstructor(Constructor<T> ctor, Errors errors, Object[] args) {
        try {
            return ctor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            errors.addMessage("Error calling constructor");
            return null;
        }
    }

}
