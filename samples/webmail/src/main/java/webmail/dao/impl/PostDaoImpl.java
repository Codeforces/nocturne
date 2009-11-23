/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.dao.impl;

import org.jacuzzi.core.GenericDaoImpl;
import org.jacuzzi.core.Row;
import org.jacuzzi.core.TypeOracle;
import webmail.dao.PostDao;
import webmail.database.ApplicationDataSourceFactory;
import webmail.model.Post;
import webmail.model.User;

import java.util.List;
import java.util.ArrayList;

/** @author Mike Mirzayanov */
public class PostDaoImpl extends GenericDaoImpl<Post, Long> implements PostDao {
    private TypeOracle<Post> postTypeOracle = TypeOracle.getTypeOracle(Post.class);

    protected PostDaoImpl() {
        super(ApplicationDataSourceFactory.getInstance());
    }

    private List<Post> findTogetherWithUsers(String query, Object... params) {
        List<Row> rows = getJacuzzi().findRows(query, params);
        List<Post> posts = new ArrayList<Post>();
        for (Row row : rows) {
            Post post = postTypeOracle.convertFromRow(row);
            post.setAuthorUserLogin(row.get("authorUserLogin").toString());
            post.setAuthorUserName(row.get("authorUserName").toString());
            post.setTargetUserLogin(row.get("targetUserLogin").toString());
            post.setTargetUserName(row.get("targetUserName").toString());
            posts.add(post);
        }
        return posts;
    }

    @Override
    public List<Post> findTargetedFor(User user) {
        return findTogetherWithUsers("SELECT Post.*, " +
                "A.name as authorUserName, A.login as authorUserLogin, " +
                "T.name as targetUserName, T.login as targetUserLogin " +
                "FROM Post, User A, User T " +
                "WHERE Post.targetUserId=? AND A.id=Post.authorUserId AND T.id=Post.targetUserId " +
                "ORDER BY Post.creationTime DESC", user.getId());
    }

    @Override
    public List<Post> findAuthoredBy(User user) {
        return findTogetherWithUsers("SELECT Post.*, " +
                "A.name as authorUserName, A.login as authorUserLogin, " +
                "T.name as targetUserName, T.login as targetUserLogin " +
                "FROM Post, User A, User T " +
                "WHERE Post.authorUserId=? AND A.id=Post.authorUserId AND T.id=Post.targetUserId " +
                "ORDER BY Post.creationTime DESC", user.getId());
    }

    @Override
    public List<Post> findRalatedTo(User user) {
        return findTogetherWithUsers("SELECT Post.*, " +
                "A.name as authorUserName, A.login as authorUserLogin, " +
                "T.name as targetUserName, T.login as targetUserLogin " +
                "FROM Post, User A, User T " +
                "WHERE (Post.targetUserId=? OR Post.authorUserId=?) AND A.id=Post.authorUserId AND T.id=Post.targetUserId " +
                "ORDER BY Post.creationTime DESC", user.getId(), user.getId());
    }

    @Override
    public Post addPost(User from, User to, String text) {
        Post post = new Post();
        post.setAuthorUserId(from.getId());
        post.setTargetUserId(to.getId());
        post.setText(text);
        post.setCreationTime(getJacuzzi().findDate("SELECT NOW()"));
        insert(post);
        return post;
    }
}
