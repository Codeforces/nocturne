/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
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
        binder.bindListener(new ClassToTypeLiteralMatcherAdapter(Matchers.subclassesOf(Component.class)), new TypeListener() {
            @Override
            public <I> void hear(final TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
                typeEncounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(Object i) {
                        Component component = (Component) i;
                        component.resetFields();
                    }
                });
            }
        });

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

    private static final class ClassToTypeLiteralMatcherAdapter extends
            AbstractMatcher<TypeLiteral> {
        private final Matcher<Class> classMatcher;

        public ClassToTypeLiteralMatcherAdapter(Matcher<Class>
                                                        classMatcher) {
            this.classMatcher = classMatcher;
        }

        public boolean matches(TypeLiteral typeLiteral) {
            return classMatcher.matches(typeLiteral.getRawType());
        }
    }
}
