/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.nocturne.module.Configuration;

import java.util.List;

/**
 * Wraps IoC module specified by nocturne.guice-module-class-name and invokes
 * IoC modules for all registered application modules.
 * 
 * @author Mike Mirzayanov
 */
class GenericIocModule implements Module {
    private Module module;

    public void setModule(Module module) {
        this.module = module;
    }

    @Override
    public void configure(Binder binder) {
        if (module != null) {
            module.configure(binder);
        }

        List<org.nocturne.module.Module> modules =
                ApplicationContext.getInstance().getModules();

        for (org.nocturne.module.Module i : modules) {
            Configuration configuration = i.getConfiguration();
            configuration.bind(binder);
        }
    }
}
