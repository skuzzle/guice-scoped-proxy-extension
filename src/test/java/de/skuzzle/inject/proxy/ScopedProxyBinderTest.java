package de.skuzzle.inject.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class ScopedProxyBinderTest {

    public static interface SampleClass {

    }

    public static class ConcreteSampleClassWithCtor {
        private final SampleClass s;

        @Inject
        public ConcreteSampleClassWithCtor(SampleClass s) {
            this.s = s;
        }
    }

    public static class ConcreteSampleClassWithMultipleCtors {
        private final SampleClass s;

        public ConcreteSampleClassWithMultipleCtors() {
            this.s = null;
        }

        public ConcreteSampleClassWithMultipleCtors(SampleClass s) {
            this.s = s;
        }
    }

    public static class ConcreateSampleClassWithPrivateCtor {
        private ConcreateSampleClassWithPrivateCtor() {}
    }

    public static class SampleClassImpl implements SampleClass {

    }

    @Test
    public void testSamehashCodeBecauseTargetIsSingleton() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                bind(SampleClassImpl.class).asEagerSingleton();
                ScopedProxyBinder.using(binder())
                        .bind(SampleClass.class)
                        .to(SampleClassImpl.class);
            }
        });

        final SampleClass sampleClass = injector.getInstance(SampleClassImpl.class);
        final int hash1 = sampleClass.hashCode();
        final int hash2 = sampleClass.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test(expected = CreationException.class)
    public void testFailOnConstructor() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .andConstructionStrategy(
                                ConstructionStrategies.FAIL_ON_CONSTRUCTOR)
                        .bind(ConcreteSampleClassWithCtor.class)
                        .to(ConcreteSampleClassWithCtor.class);
            }
        });

        injector.getInstance(ConcreteSampleClassWithCtor.class);
    }

    @Test
    public void testUseObjenesis() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .andConstructionStrategy(ConstructionStrategies.OBJENESIS)
                        .bind(ConcreteSampleClassWithCtor.class)
                        .to(ConcreteSampleClassWithCtor.class);
            }
        });

        injector.getInstance(ConcreteSampleClassWithCtor.class);
    }

    @Test
    @Ignore
    public void testBindNoTarget() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .bind(SampleClassImpl.class);
            }
        });

        final SampleClass sampleClass = injector.getInstance(SampleClassImpl.class);
        final int hash1 = sampleClass.hashCode();
        final int hash2 = sampleClass.hashCode();

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testDifferentHashCodes() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .bind(SampleClass.class)
                        .to(SampleClassImpl.class);
            }
        });

        final SampleClass sampleClass = injector.getInstance(SampleClass.class);
        final int hash1 = sampleClass.hashCode();
        final int hash2 = sampleClass.hashCode();

        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testDifferentHashCodesWithAnnotation() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .bind(SampleClass.class, Names.named("test"))
                        .to(SampleClassImpl.class);
            }
        });

        final SampleClass sampleClass = injector.getInstance(
                Key.get(SampleClass.class, Names.named("test")));
        final int hash1 = sampleClass.hashCode();
        final int hash2 = sampleClass.hashCode();

        assertNotEquals(hash1, hash2);
    }

    @Test(expected = RuntimeException.class)
    public void testForbidGuiceSingleton() throws Exception {
        Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .bind(SampleClass.class, Named.class)
                        .to(SampleClassImpl.class)
                        .in(Singleton.class);
            }
        });
    }

    @Test(expected = RuntimeException.class)
    public void testForbidJavaxSingleton() throws Exception {
        Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .bind(SampleClass.class, Named.class)
                        .to(SampleClassImpl.class)
                        .in(javax.inject.Singleton.class);
            }
        });
    }

    @Test(expected = RuntimeException.class)
    public void testForbidEagerSingleton() throws Exception {
        Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .bind(SampleClass.class, Named.class)
                        .to(SampleClassImpl.class)
                        .asEagerSingleton();
            }
        });
    }

    @Test
    public void testNullConstructionStrategy() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .andConstructionStrategy(ConstructionStrategies.NULL_VALUES)
                        .bind(ConcreteSampleClassWithCtor.class)
                        .to(ConcreteSampleClassWithCtor.class);
            }
        });

        final ConcreteSampleClassWithCtor instance = injector.getInstance(
                ConcreteSampleClassWithCtor.class);
        assertNull(instance.s);
    }

    @Test(expected = CreationException.class)
    public void testNullConstructionStrategyMultipleCtors() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .andConstructionStrategy(ConstructionStrategies.NULL_VALUES)
                        .bind(ConcreteSampleClassWithMultipleCtors.class)
                        .to(ConcreteSampleClassWithMultipleCtors.class);
            }
        });
        injector.getInstance(ConcreteSampleClassWithMultipleCtors.class);
    }

    @Test(expected = CreationException.class)
    public void testNullConstructionStrategyNoCtor() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                ScopedProxyBinder.using(binder())
                        .andConstructionStrategy(ConstructionStrategies.NULL_VALUES)
                        .bind(ConcreateSampleClassWithPrivateCtor.class)
                        .to(ConcreateSampleClassWithPrivateCtor.class);
            }
        });
        injector.getInstance(ConcreteSampleClassWithMultipleCtors.class);
    }
}
