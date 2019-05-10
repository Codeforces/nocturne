package bloggy.dao.impl;

import com.google.inject.Singleton;
import bloggy.dao.PostDao;
import bloggy.model.Post;
import bloggy.model.User;

import java.util.List;

@Singleton
public class PostDaoImpl extends ApplicationDaoImpl<Post> implements PostDao {
    @Override
    public Post find(long id) {
        return super.find(id);
    }

    @Override
    public List<Post> findAll() {
        return findBy("NOT deleted ORDER BY updateTime DESC");
    }

    @Override
    public List<Post> findByUser(User user) {
        return findBy("userId=? AND NOT deleted ORDER BY updateTime DESC", user.getId());
    }
}
