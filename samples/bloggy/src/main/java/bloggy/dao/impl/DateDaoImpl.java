package bloggy.dao.impl;

import bloggy.dao.DateDao;
import bloggy.model.Entity;

import java.util.Date;

public class DateDaoImpl extends ApplicationDaoImpl<Entity> implements DateDao {
    @Override
    public Date findNow() {
        return super.findNow();
    }
}
