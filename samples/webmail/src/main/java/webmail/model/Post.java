/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.model;

import org.jacuzzi.mapping.Id;
import org.jacuzzi.mapping.Transient;

import java.util.Date;

/** @author Mike Mirzayanov */
public class Post {
    @Id
    private long id;

    private long authorUserId;

    private long targetUserId;

    private String text;

    private Date creationTime;

    @Transient
    private String authorUserName;

    @Transient
    private String authorUserLogin;

    @Transient
    private String targetUserName;

    @Transient
    private String targetUserLogin;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(long authorUserId) {
        this.authorUserId = authorUserId;
    }

    public long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getAuthorUserName() {
        return authorUserName;
    }

    public void setAuthorUserName(String authorUserName) {
        this.authorUserName = authorUserName;
    }

    public String getAuthorUserLogin() {
        return authorUserLogin;
    }

    public void setAuthorUserLogin(String authorUserLogin) {
        this.authorUserLogin = authorUserLogin;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getTargetUserLogin() {
        return targetUserLogin;
    }

    public void setTargetUserLogin(String targetUserLogin) {
        this.targetUserLogin = targetUserLogin;
    }
}
