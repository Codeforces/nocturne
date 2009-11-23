/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.dao;

import webmail.model.Post;
import webmail.model.User;

import java.util.List;

/** @author Mike Mirzayanov */
public interface PostDao {
    List<Post> findTargetedFor(User user);
    List<Post> findAuthoredBy(User user);
    List<Post> findRalatedTo(User user);
    Post addPost(User from, User to, String text);
}
