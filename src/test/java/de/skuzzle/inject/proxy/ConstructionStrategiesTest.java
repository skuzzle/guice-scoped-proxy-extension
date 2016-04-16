package de.skuzzle.inject.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;
import com.google.inject.internal.Errors;

public class ConstructionStrategiesTest {

    public static class ClassWithNoArgConstructor {

    }

    public static class ClassWithConstructor {
        private final String attribute;
        public ClassWithConstructor(String parameter) {
            this.attribute = parameter;
        }
    }

    public static class ClassWithMultipleConstructors {
        public ClassWithMultipleConstructors() {}

        public ClassWithMultipleConstructors(String parameter) {}
    }

    public static class ClassWithPrivateConstructor {
        private ClassWithPrivateConstructor() {}
    }


    @Before
    public void setUp() throws Exception {}

    @Test
    public void testObjenesis() throws Exception {
        final Errors errors = new Errors();
        final ClassWithConstructor inst = ConstructionStrategies.OBJENESIS
                .createInstance(ClassWithConstructor.class, mock(Injector.class),
                        errors);

        assertNotNull(inst);
        assertEquals(0, errors.size());
    }

    @Test
    public void testFailOnConstructorWithConstructor() throws Exception {
        final Errors errors = new Errors();
        final ClassWithConstructor inst = ConstructionStrategies.FAIL_ON_CONSTRUCTOR.createInstance(
                ClassWithConstructor.class, mock(Injector.class), errors);
        assertNull(inst);
        assertEquals(1, errors.size());
    }

    @Test
    public void testFailOnConstructor() throws Exception {
        final Errors errors = new Errors();
        final ClassWithNoArgConstructor inst = ConstructionStrategies.FAIL_ON_CONSTRUCTOR.createInstance(
                ClassWithNoArgConstructor.class, mock(Injector.class), errors);
        assertNotNull(inst);
        assertEquals(0, errors.size());
    }

    @Test
    public void testNullValues() throws Exception {
        final Errors errors = new Errors();
        final ClassWithConstructor inst = ConstructionStrategies.NULL_VALUES.createInstance(
                ClassWithConstructor.class, mock(Injector.class), errors);
        assertNotNull(inst);
        assertNull(inst.attribute);
        assertEquals(0, errors.size());
    }

    @Test
    public void testNullValuesMultipleConstructors() throws Exception {
        final Errors errors = new Errors();
        final ClassWithMultipleConstructors inst = ConstructionStrategies.NULL_VALUES.createInstance(
                ClassWithMultipleConstructors.class, mock(Injector.class), errors);
        assertNull(inst);
        assertEquals(1, errors.size());
    }

    @Test
    public void testNullValuesPrivateConstructors() throws Exception {
        final Errors errors = new Errors();
        final ClassWithPrivateConstructor inst = ConstructionStrategies.NULL_VALUES.createInstance(
                ClassWithPrivateConstructor.class, mock(Injector.class), errors);
        assertNull(inst);
        assertEquals(1, errors.size());
    }
}
