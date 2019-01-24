/**
 *
 */
/**
 * @author Simon Taddiken
 */
module de.skuzzle.inject.proxy {
    exports de.skuzzle.inject.proxy;

    requires cglib;
    requires guava;
    requires transitive guice;
    requires javax.inject;
    requires org.objenesis;
}