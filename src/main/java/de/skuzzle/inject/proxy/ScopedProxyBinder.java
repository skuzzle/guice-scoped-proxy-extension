package de.skuzzle.inject.proxy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.internal.BindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.Toolable;

/**
 * Allows to bind classes and interfaces as scoped proxies. Use it in a
 * {@link Module} like this:
 *
 * <pre>
 * public class MyModule extends AbstractModule {
 *     &#64;Override
 *     public void configure() {
 *         ScopedProxyBinder.using(binder())
 *                 .bind(MyServiceImpl.class)
 *                 .in(RequestScope.class);
 *     }
 * }
 * </pre>
 *
 * You can then inject {@code MyServiceImpl} into broader scopes like singleton
 * as if you were injecting a {@code Provider<MyServiceImpl>}.
 *
 * @author Simon Taddiken
 */
public final class ScopedProxyBinder {

    private ScopedProxyBinder() {
        // hidden constructor;
    }

    /**
     * Starts building a scoped proxy binding using the given {@link Binder}.
     *
     * @param binder the binder to create the binding with.
     * @return Builder object for specifying the binding.
     */
    public static ScopedProxyBuilder using(Binder binder) {
        checkNotNull(binder, "binder");
        return new ScopedProxyBuilderImpl(binder);
    }

    public interface ScopedProxyBuilder {

        /**
         * Specifies the {@link ConstructionStrategy} that will be used to
         * instantiate proxy objects. the class {@link ConstructionStrategies}
         * holds some default implementations. In most cases modifying the
         * strategy is not necessary.
         *
         * @param strategy The construction strategy.
         * @return The builder object.
         */
        ScopedProxyBuilder andConstructionStrategy(ConstructionStrategy strategy);

        /**
         * Specifies the type to bind.
         *
         * @param <T> The type to bind.
         * @param cls The type to bind.
         * @return The builder object.
         */
        <T> LinkedBindingBuilder<T> bind(Class<T> cls);

        /**
         * Specifies the type to bind with an additional {@link BindingAnnotation}.
         *
         * @param <T> The type to bind.
         * @param cls The type to bind.
         * @param annotationClass The binding annotation type.
         * @return The builder object.
         */
        <T> LinkedBindingBuilder<T> bind(Class<T> cls,
                Class<? extends Annotation> annotationClass);

        /**
         * Specifies the type to bind with an additional {@link BindingAnnotation}.
         *
         * @param <T> The type to bind.
         * @param cls The type to bind.
         * @param annotation The binding annotation.
         * @return The builder object.
         */
        <T> LinkedBindingBuilder<T> bind(Class<T> cls, Annotation annotation);

        /**
         * Specifies the {@link Key} to bind.
         * @param <T> The type to bind.
         * @param key The key.
         * @return The builder object.
         */
        <T> LinkedBindingBuilder<T> bind(Key<T> key);
    }

    private static final class ScopedProxyBuilderImpl implements ScopedProxyBuilder {

        private final Binder binder;
        private ConstructionStrategy strategy = ConstructionStrategies.OBJENESIS;

        ScopedProxyBuilderImpl(Binder binder) {
            this.binder = binder;
        }

        @Override
        public ScopedProxyBuilder andConstructionStrategy(
                ConstructionStrategy strategy) {
            checkNotNull(strategy);
            this.strategy = strategy;
            return this;
        }

        @Override
        public <T> LinkedBindingBuilder<T> bind(Class<T> cls,
                Class<? extends Annotation> annotationClass) {
            return bind(Key.get(cls, annotationClass));
        }

        @Override
        public <T> LinkedBindingBuilder<T> bind(Class<T> cls, Annotation annotation) {
            return bind(Key.get(cls, annotation));
        }

        @Override
        public <T> LinkedBindingBuilder<T> bind(Class<T> cls) {
            return bind(Key.get(cls));
        }

        @Override
        public <T> LinkedBindingBuilder<T> bind(Key<T> sourceKey) {
            checkNotNull(sourceKey);
            return new FluentInterfaceImpl<T>(this.binder, sourceKey, this.strategy);
        }
    }

    private static final class FluentInterfaceImpl<T> implements LinkedBindingBuilder<T>,
            ScopedBindingBuilder {

        private final Binder binder;
        private final ConstructionStrategy strategy;
        private final Key<T> source;
        private final Key<T> rewrittenKey;
        private BindingBuilder<T> targetBuilder;

        private FluentInterfaceImpl(Binder binder, Key<T> sourceKey,
                ConstructionStrategy strategy) {
            this.binder = binder;
            this.strategy = strategy;
            this.source = sourceKey;
            this.rewrittenKey = bindSource();
            bindRewritten();
        }

        private Key<T> bindSource() {
            // backup the original binding using an internal annotation to
            // create a unique hidden key.
            final UUID uuid = UUID.randomUUID();
            final Key<T> rewritten = Key.get(this.source.getTypeLiteral(),
                    Names.named(uuid.toString()));

            // bind the user specified source type to the provider which creates
            // the scoped proxy objects.
            this.binder.bind(this.source)
                    .toProvider(
                            new ScopedProxyProvider<>(this.source, rewritten, this.strategy))
                    .in(Singleton.class);
            return rewritten;
        }

        private BindingBuilder<T> bindRewritten() {
            if (this.targetBuilder == null) {
                this.targetBuilder = (BindingBuilder<T>) this.binder.bind(
                        this.rewrittenKey);
            }
            return this.targetBuilder;
        }

        @Override
        public ScopedBindingBuilder to(Key<? extends T> key) {
            checkNotNull(key);
            bindRewritten().to(key);
            return this;
        }

        @Override
        public ScopedBindingBuilder to(Class<? extends T> implementation) {
            return this.to(Key.get(implementation));
        }

        @Override
        public ScopedBindingBuilder to(TypeLiteral<? extends T> implementation) {
            return this.to(Key.get(implementation));
        }

        @Override
        public void toInstance(T instance) {
            this.bindRewritten().toInstance(instance);
        }

        @Override
        public ScopedBindingBuilder toProvider(Provider<? extends T> provider) {
            this.bindRewritten().toProvider(provider);
            return this;
        }

        @Override
        public ScopedBindingBuilder toProvider(
                javax.inject.Provider<? extends T> provider) {
            this.bindRewritten().toProvider(provider);
            return this;
        }

        @Override
        public ScopedBindingBuilder toProvider(
                Class<? extends javax.inject.Provider<? extends T>> providerType) {
            this.bindRewritten().toProvider(providerType);
            return this;
        }

        @Override
        public ScopedBindingBuilder toProvider(
                TypeLiteral<? extends javax.inject.Provider<? extends T>> providerType) {
            this.bindRewritten().toProvider(providerType);
            return this;
        }

        @Override
        public ScopedBindingBuilder toProvider(
                Key<? extends javax.inject.Provider<? extends T>> providerKey) {
            this.bindRewritten().toProvider(providerKey);
            return this;
        }

        @Override
        public <S extends T> ScopedBindingBuilder toConstructor(
                Constructor<S> constructor) {
            this.bindRewritten().toConstructor(constructor);
            return this;
        }

        @Override
        public <S extends T> ScopedBindingBuilder toConstructor(
                Constructor<S> constructor, TypeLiteral<? extends S> type) {
            this.bindRewritten().toConstructor(constructor, type);
            return this;
        }

        @Override
        public void in(Class<? extends Annotation> scopeAnnotation) {
            checkSingleton(scopeAnnotation);
            this.bindRewritten().in(scopeAnnotation);
        }

        @Override
        public void in(Scope scope) {
            this.bindRewritten().in(scope);
        }

        @Override
        public void asEagerSingleton() {
            checkSingleton(Singleton.class);
        }

        private static void checkSingleton(Class<? extends Annotation> scopeAnnotation) {
            if (Singleton.class.equals(scopeAnnotation) ||
                com.google.inject.Singleton.class.equals(scopeAnnotation)) {
                throw new UnsupportedOperationException("Scoped proxies can not be " +
                    "bound as singleton. Theres is no reason to do this");
            }

        }
    }

    private static class ScopedProxyProvider<T> implements Provider<T>, HasDependencies {

        final Key<T> rewritten;
        final ConstructionStrategy strategy;
        Set<Dependency<?>> dependencies;
        T ref;

        ScopedProxyProvider(Key<T> sourceKey, Key<T> rewrittenKey,
                ConstructionStrategy strategy) {
            this.rewritten = rewrittenKey;
            this.strategy = strategy;
            this.dependencies = Collections.singleton(
                    Dependency.get(Key.get(Injector.class)));
        }

        @Inject
        @Toolable
        @SuppressWarnings("unchecked")
        void initialize(Injector injector) {
            final Binding<T> realBinding = injector.getBinding(this.rewritten);
            final Provider<T> realProvider = injector.getProvider(realBinding.getKey());

            // The proxy will be a sub type of the source type of the binding
            final Class<T> proxyType = (Class<T>) realBinding.getKey()
                    .getTypeLiteral().getRawType();

            this.dependencies = Collections.singleton(
                    Dependency.get(this.rewritten));
            this.ref = InstanceBuilder.forType(proxyType)
                    .withConstructionStrategy(this.strategy)
                    .dispatchTo(realProvider)
                    .create(injector);
        }

        @Override
        public T get() {
            checkState(this.ref != null, "Scoped proxy provider not initialized");
            return this.ref;
        }

        @Override
        public Set<Dependency<?>> getDependencies() {
            return this.dependencies;
        }
    }
}
