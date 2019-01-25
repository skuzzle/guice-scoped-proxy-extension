/**
 * @author Simon Taddiken
 */
module de.skuzzle.inject.proxy {
    exports de.skuzzle.inject.proxy;

    requires cglib;
    requires com.google.guice;
    requires transitive com.google.common;
    requires javax.inject;
    requires org.objenesis;
}