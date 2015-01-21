package org.jboss.pull.shared.connectors.bugzilla;

import java.util.Date;
import java.util.Map;

public class Comment implements Comparable<Comment> {

    public static final int ID = 0;
    public static final int AUTHOR = 1;
    public static final int TEXT = 2;
    public static final int TIME = 3;
    public static final int COUNT = 4;
    public static final int CREATION_TIME = 5;
    public static final int IS_PRIVATE = 6;
    public static final int BUG_ID = 7;
    public static final int CREATOR_ID = 8;
    public static final int CREATOR = 9;

    public static final Object[] include_fields = { "id", "author", "text", "time", "count", "creation_time", "is_private",
            "bug_id", "creator_id", "creator" };

    private int id;
    private String author;
    private String text;
    private Date time;
    private int count;
    private Date creationTime;
    private boolean visibility; // can name the variable 'public' or private !
    private int bugId;
    private int creatorId;
    private String creator;

    public Comment(Map<String, Object> commentMap) {
        id = (Integer) commentMap.get(include_fields[ID]);
        author = (String) commentMap.get(include_fields[AUTHOR]);
        text = (String) commentMap.get(include_fields[TEXT]);
        time = (Date) commentMap.get(include_fields[TIME]);
        count = (Integer) commentMap.get(include_fields[COUNT]);
        creationTime = (Date) commentMap.get(include_fields[CREATION_TIME]);
        visibility = (Boolean) commentMap.get(include_fields[IS_PRIVATE]);
        bugId = (Integer) commentMap.get(include_fields[BUG_ID]);
        creatorId = (Integer) commentMap.get(include_fields[CREATOR_ID]);
        creator = (String) commentMap.get(include_fields[CREATOR]);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public boolean isPrivate() {
        return visibility;
    }

    public void setPrivate(boolean visibility) {
        this.visibility = visibility;
    }

    public int getBugId() {
        return bugId;
    }

    public void setBugId(int bugId) {
        this.bugId = bugId;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public String toString() {
        return "Comment [id=" + id + ", author=" + author + ", text=" + text + ", time=" + time + ", count=" + count
                + ", creationTime=" + creationTime + ", visibility=" + visibility + ", bugId=" + bugId + ", creatorId="
                + creatorId + ", creator=" + creator + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((author == null) ? 0 : author.hashCode());
        result = prime * result + bugId;
        result = prime * result + count;
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((creator == null) ? 0 : creator.hashCode());
        result = prime * result + creatorId;
        result = prime * result + id;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + (visibility ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof Comment ) {
            return ( this.getId() == ((Comment)obj).getId() ? true : false);
        }
        return false;
    }

    @Override
    public int compareTo(Comment o) {
        if ( o == null )
            throw new IllegalArgumentException("Can't compare instance of comment [ID:" + o.getId() + "] with a 'null' instance of Comment !");
        if (this.getBugId() != o.getBugId() )
            throw new IllegalArgumentException("Can't compare comment [" + "BugId:" + o.getBugId() + " that does not belong to the same issue [BugId:" + this.getBugId());
        if ( this.getCount() == o.getCount())
            return 0;
        return this.getCount() > o.getCount() ? 1 : -1;
    }

}
