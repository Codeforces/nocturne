package webmail;

import com.google.inject.Binder;
import com.google.inject.Module;
import webmail.dao.PostDao;
import webmail.dao.UserDao;
import webmail.dao.impl.PostDaoImpl;
import webmail.dao.impl.UserDaoImpl;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(UserDao.class).to(UserDaoImpl.class);
        binder.bind(PostDao.class).to(PostDaoImpl.class);
    }
}
