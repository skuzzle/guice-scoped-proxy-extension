package de.skuzzle.inject.proxy;

import java.lang.reflect.Method;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.internal.Errors;

import net.sf.cglib.core.DefaultNamingPolicy;
import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.InvocationHandler;

/**
 * Builds a proxy instance which is backed by a scoped provider.
 *
 * @author Simon Taddiken
 * @param <T> Type of the proxy to create.
 */
final class InstanceBuilder<T> {

    /**
     * The unique callback index to which each method in the proxy object is
     * mapped.
     */
    private static final int CALLBACK_INDEX = 0;

    /** Maps all methods to index {@link #CALLBACK_INDEX}. */
    private static final CallbackFilter ZERO_CALLBACK_FILTER = new CallbackFilter() {
        @Override
        public int accept(Method method) {
            return CALLBACK_INDEX;
        }
    };

    /** Naming strategy for our enhancer */
    private static final NamingPolicy ENHANCER_NAMING = new DefaultNamingPolicy() {

        @Override
        protected String getTag() {
            return "ByGuice";
        };

        @Override
        public String getClassName(String prefix, String source, Object key,
                net.sf.cglib.core.Predicate names) {
            return super.getClassName(prefix, "ScopedProxy", key, names);
        };

    };

    private final Enhancer enhancer;
    private Callback dispatcher;
    private ConstructionStrategy constructionStrategy =
            ConstructionStrategies.NULL_VALUES;

    private InstanceBuilder(Class<T> superType) {
        this.enhancer = new Enhancer();
        this.enhancer.setSuperclass(superType);
        this.enhancer.setUseFactory(true);
        this.enhancer.setCallbackFilter(ZERO_CALLBACK_FILTER);
        this.enhancer.setNamingPolicy(ENHANCER_NAMING);
    }

    /**
     * Builds a scoped proxy instance which is a subtype of the given class.
     *
     * @param type The class.
     * @return Builder object for further configuration.
     */
    public static <E> InstanceBuilder<E> forType(Class<E> type) {
        Preconditions.checkNotNull(type, "type");
        return new InstanceBuilder<E>(type);
    }

    /**
     * Specifies the scoped provider. Every method call on the object created by
     * this builder will be delegated to the object returned by the given
     * provider.
     * <p>
     * This method overrides the callback set by {@link #withCallback(Callback)}.
     *
     * @param provider The scoped provider.
     * @return Builder object for further configuration.
     * @see #withCallback(Callback)
     */
    public InstanceBuilder<T> dispatchTo(Provider<T> provider) {
        Preconditions.checkNotNull(provider, "provider");
        this.dispatcher = new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                return method.invoke(provider.get(), args);
            }
        };
        return this;
    }

    /**
     * Sets the single callback to which all method calls of the created object
     * are delegated.
     * <p>
     * This method overrides the provider based callback set by
     * {@link #dispatchTo(Provider)}.
     *
     * @param callback The callback to use for the created object.
     * @return Builder object for further configuration.
     * @see #dispatchTo(Provider)
     */
    public InstanceBuilder<T> withCallback(Callback callback) {
        Preconditions.checkNotNull(callback, "callback");
        this.dispatcher = callback;
        return this;
    }

    /**
     * Sets the strategy that is used to instantiate proxies of concrete classes
     * that require constructor arguments.
     *
     * @param strategy The strategy.
     * @return Builder object for further configuration.
     */
    public InstanceBuilder<T> withConstructionStrategy(
            ConstructionStrategy strategy) {
        Preconditions.checkNotNull(strategy);
        this.constructionStrategy = strategy;
        return this;
    }

    /**
     * Creates the scoped proxy object using the given provider.
     *
     * @param injector The injector.
     * @return The scoped proxy object.
     */
    @SuppressWarnings("unchecked")
    public T create(Injector injector) {
        Preconditions.checkNotNull(injector, "injector");
        Preconditions.checkState(this.dispatcher != null, "no provider set");

        this.enhancer.setCallbackType(this.dispatcher.getClass());
        final Class<T> enhancedClass = this.enhancer.createClass();
        final Errors errors = new Errors();
        final T proxyInstance = this.constructionStrategy
                .createInstance(enhancedClass, injector, errors);

        errors.throwProvisionExceptionIfErrorsExist();
        final Factory factory = (Factory) proxyInstance;
        factory.setCallback(CALLBACK_INDEX, this.dispatcher);
        return proxyInstance;
    }
}
