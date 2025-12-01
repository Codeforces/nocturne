package bloggy.dao.impl;

import bloggy.database.ApplicationDataSource;
import bloggy.model.Entity;
import org.jacuzzi.core.GenericDaoImpl;

import java.util.Date;
import java.util.List;

public class ApplicationDaoImpl<T extends Entity> extends GenericDaoImpl<T, Long> {
    public ApplicationDaoImpl() {
        super(ApplicationDataSource.getInstance());
    }

    @Override
    public void insert(T object) {
        if (object == null) {
            return;
        }

        Date now = findNow();
        object.setCreationTime(now);
        object.setUpdateTime(now);

        super.insert(object);
    }

    @Override
    public void insert(List<T> objects) {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        Date now = findNow();
        for (T object : objects) {
            object.setCreationTime(now);
            object.setUpdateTime(now);
        }

        super.insert(objects);
    }

    @Override
    public void update(T object) {
        if (object == null) {
            return;
        }

        Date now = findNow();
        object.setUpdateTime(now);

        super.update(object);
    }
}
