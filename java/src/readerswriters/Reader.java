package readerswriters;

public class Reader extends Thread {

    private Book book;

    public Reader(Book book) {
        super();
        this.book = book;
    }

    @Override
    public void run() {
        book.read();
    }
}
