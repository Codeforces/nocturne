/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.module;

import com.google.inject.Binder;

/**
 * Interface to be implemented in modules to setup them.
 *
 * @author Mike Mirzayanov
 */
public interface Configuration {
    /**
     * This class should contain pages registration.
     * For example: {@code Links.add(UserPage.class);}
     */
    public abstract void addPages();

    /**
     * Should contain configuration of the module IoC.
     * Example: {@code binder.bind(BlogDao.class).to(BlogDaoImpl.class);}
     *
     * @param binder Guice binder.
     */
    public abstract void bind(Binder binder);
}
