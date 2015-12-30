package de.skuzzle.inject.proxy;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

final class ObjenesisHolder {

    // with caching
    private static final Objenesis INSTANCE = new ObjenesisStd(true);

    static Objenesis getInstance() {
        return INSTANCE;
    }

    private ObjenesisHolder() {
        // hidden
    }
}
