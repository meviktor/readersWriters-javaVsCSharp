package readerswriters;

public class Writer extends Thread {

    private Book book;

    public Writer(Book book) {
        super();
        this.book = book;
    }

    @Override
    public void run() {
        book.write();
    }

}
