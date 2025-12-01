package bloggy.model;

import com.codeforces.commons.time.TimeUtil;
import org.jacuzzi.mapping.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Entity implements Serializable {
    @Id
    private long id;
    private Date updateTime;
    private Date creationTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = TimeUtil.toDate(updateTime);
    }

    @SuppressWarnings("unused")
    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = TimeUtil.toDate(creationTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                '}';
    }
}
