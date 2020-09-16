package org.nocturne.reset;

/**
 * @author Mike Mirzayanov
 */
public class ObjectFieldsResetter extends FieldsResetter {
    private final Class<?> resetStopClass;

    public ObjectFieldsResetter(Object object, Class<?> resetStopClass) {
        super(object);
        this.resetStopClass = resetStopClass;
    }

    @Override
    boolean isResetStopClass(Class<?> clazz) {
        return clazz == resetStopClass;
    }
}
