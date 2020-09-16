package org.nocturne.reset;

import org.nocturne.main.Component;
import org.nocturne.main.Frame;
import org.nocturne.main.Page;

/**
 * @author Mike Mirzayanov
 */
public class ComponentFieldsResetter extends FieldsResetter {
    public ComponentFieldsResetter(Component component) {
        super(component);
    }

    @Override
    boolean isResetStopClass(Class<?> clazz) {
        return clazz.getCanonicalName().equals(Component.class.getCanonicalName())
                || clazz.getCanonicalName().equals(Page.class.getCanonicalName())
                || clazz.getCanonicalName().equals(Frame.class.getCanonicalName());
    }
}
