/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.module;

import com.google.inject.Binder;

/**
 * Interface to be implemented in modules to setup them.
 *
 * @author Mike Mirzayanov
 * @author Maxim Shipko (sladethe@gmail.com)
 */
public interface Configuration {
    /**
     * This class should contain pages registration.
     * For example: {@code Links.add(UserPage.class);}
     */
    void addPages();

    /**
     * Should contain configuration of the module IoC.
     * Example: {@code binder.bind(BlogDao.class).to(BlogDaoImpl.class);}
     *
     * @param binder Guice binder.
     */
    void bind(Binder binder);

    /**
     * Sends a shutdown signal to the module and exits. May perform synchronously some finalization routines,
     * but there is no guarantee that the module is completely stopped when {@code shutdown} method finishes.
     * <p>
     * The method should not fail in case of repeated/concurrent calls.
     */
    default void shutdown() {
    }
}
