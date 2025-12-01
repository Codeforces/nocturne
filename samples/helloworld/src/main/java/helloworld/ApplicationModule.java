/*
 * Copyright 2009 Mike Mirzayanov
 */

package helloworld;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationModule implements Module {
    @Override
    public void configure(Binder binder) {
        // No IoC binding.
    }
}
