package de.skuzzle.inject.proxy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.internal.BindingBuilder;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.Toolable;

/**
 * Allows to bind classes and interfaces as scoped proxies.
 * @author Simon Taddiken
 */
public final class ScopedProxyBinder {

    private ScopedProxyBinder() {
        // hidden constructor;
    }

    public static ScopedProxyBuilder using(Binder binder) {
        checkNotNull(binder, "binder");
        return new ScopedProxyBuilderImpl(binder);
    }

    public interface ScopedProxyBuilder {

        ScopedProxyBuilder andConstructionStrategy(ConstructionStrategy strategy);

        <T> LinkedBindingBuilder<T> bind(Class<T> cls);

        <T> LinkedBindingBuilder<T> bind(Class<T> cls,
                Class<? extends Annotation> annotationClass);

        <T> LinkedBindingBuilder<T> bind(Class<T> cls, Annotation annotation);

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
            return new FluentInterfaceImpl<T>(this.binder, sourceKey,
                    this.strategy);
        }
    }

    private static final class FluentInterfaceImpl<T> implements LinkedBindingBuilder<T>,
            ScopedBindingBuilder {

        private final Binder binder;
        private final ConstructionStrategy strategy;
        private final Key<T> source;
        private final Key<T> rewritten;
        private BindingBuilder<T> targetBuilder;

        private FluentInterfaceImpl(Binder binder, Key<T> sourceKey,
                ConstructionStrategy strategy) {
            this.binder = binder;
            this.strategy = strategy;
            this.source = sourceKey;
            this.rewritten = bindSource();
            bindRewritten();
        }

        private Key<T> bindSource() {
            final String name = this.source.getTypeLiteral().getRawType().getName();
            // backup the original binding using an internal annotation to
            // create a unique hidden key.
            final Key<T> rewritten = Key.get(this.source.getTypeLiteral(),
                    new OriginalImpl(name));

            // bind the user specified source type to the provider which creates
            // the scoped proxy objects.
            this.binder.bind(this.source)
                    .toProvider(new ScopedProxyProvider<>(
                            this.source, rewritten, this.strategy))
                    .in(Singleton.class);
            return rewritten;
        }

        private BindingBuilder<T> bindRewritten() {
            if (this.targetBuilder == null) {
                this.targetBuilder = (BindingBuilder<T>) this.binder.bind(this.rewritten);
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

        private void checkSingleton(Class<? extends Annotation> scopeAnnotation) {
            if (Singleton.class.equals(scopeAnnotation)
                    || com.google.inject.Singleton.class.equals(scopeAnnotation)) {
                    throw new UnsupportedOperationException("Scoped proxies can not be " +
                            "bound as singleton. Theres is no reason to do this");
            }

        }
    }

    private static class ScopedProxyProvider<T> implements Provider<T> {

        final Key<T> sourceKey;
        final Key<T> rewritten;
        final ConstructionStrategy strategy;
        Set<Dependency<?>> dependencies;
        T ref;

        ScopedProxyProvider(Key<T> sourceKey, Key<T> rewrittenKey,
                ConstructionStrategy strategy) {
            this.sourceKey = sourceKey;
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

        //@Override
        public Set<Dependency<?>> getDependencies() {
            return this.dependencies;
        }
    }

    @Retention(RUNTIME)
    @Qualifier
    private @interface Original {
        String value();
    }

    private static final class OriginalImpl implements Annotation, Serializable, Original {
        private static final long serialVersionUID = 0;
        private final String value;

        OriginalImpl(String value) {
            this.value = checkNotNull(value, "value");
        }

        @Override
        public String value() {
            return this.value;
        }

        @Override
        public int hashCode() {
            // This is specified in java.lang.Annotation.
            return (127 * "value".hashCode()) ^ this.value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Original) {
                final Original other = (Original) o;
                return this.value.equals(other.value());
            }
            return false;
        }

        @Override
        public String toString() {
            return "@Original" + (this.value.isEmpty()
                    ? ""
                    : "(value=" + this.value + ")");
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Original.class;
        }
    }
}