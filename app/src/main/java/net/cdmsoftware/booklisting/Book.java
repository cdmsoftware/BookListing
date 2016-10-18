package net.cdmsoftware.booklisting;

public class Book {
    private String mTitle;
    private String mAuthors;
    private String mPublisher;
    private String mCoverImage;

    public Book(String title, String authors, String publisher, String coverImage){
        this.mAuthors = authors;
        this.mPublisher = publisher;
        this.mTitle = title;
        this.mCoverImage = coverImage;
    }

    public String getAuthors(){
        return mAuthors;
    }

    public String getPublisher(){
        return mPublisher;
    }

    public String getTitle(){
        return mTitle;
    }

    public String getCoverImage(){
        return mCoverImage;
    }
}
