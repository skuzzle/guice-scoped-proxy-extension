/**
 * @author Simon Taddiken
 */
module de.skuzzle.inject.proxy {
    exports de.skuzzle.inject.proxy;

    requires org.objenesis;
    requires com.google.guice;
    requires com.google.common;

    requires cglib;
    requires javax.inject;
}