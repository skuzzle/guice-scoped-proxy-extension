package de.skuzzle.inject.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.objenesis.Objenesis;
import org.objenesis.instantiator.ObjectInstantiator;

import com.google.inject.Injector;
import com.google.inject.internal.Errors;
import com.google.inject.spi.InjectionPoint;

import net.sf.cglib.proxy.FixedValue;

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
     * Uses the injector to create the proxy object. If outside of their scope,
     * this strategy will inject unscoped objects into the constructor and
     * fields. If the scope does not allow to inject types outside their scope,
     * errors might occur.
     */
    INJECTOR_ONLY {

        @Override
        public <T> T createInstance(Class<T> proxyClass, Injector injector,
                Errors errors) {
            return injector.getInstance(proxyClass);
        }
    },

    DEEP_MOCKS {

        @Override
        public <T> T createInstance(Class<T> proxyClass, Injector injector,
                Errors errors) {
            final Constructor<T> ctor = getConstructorFor(proxyClass);
            final Object[] args = new Object[ctor.getParameterCount()];
            for (int i = 0; i < args.length; ++i) {
                final Class<?> argType = ctor.getParameterTypes()[i];
                args[i] = InstanceBuilder
                        .forType(argType)
                        .withCallback(new FixedValue() {

                    @Override
                    public Object loadObject() throws Exception {
                        return null;
                    }
                }).withConstructionStrategy(this).create(injector);
            }
            return callConstructor(ctor, errors, args);
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
            final Constructor<T> ctor = getConstructorFor(proxyClass);
            final Object[] args = new Object[ctor.getParameterCount()];
            return callConstructor(ctor, errors, args);
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
    protected <T> Constructor<T> getConstructorFor(Class<T> proxyClass) {
        return (Constructor<T>) InjectionPoint.forConstructorOf(proxyClass).getMember();
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
