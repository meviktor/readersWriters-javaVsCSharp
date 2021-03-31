package readerswriters;

import java.util.Collection;

public class Writer extends Thread {

    private Book book;
    private Collection<Integer> readersInARowSynchronizedCollection;

    public Writer(Book book, Collection<Integer> readersInARowSynchronizedCollection) {
        super();
        this.book = book;
        this.readersInARowSynchronizedCollection = readersInARowSynchronizedCollection;
    }

    @Override
    public void run() {
        int readersInARowBeforeMe = book.write();
        readersInARowSynchronizedCollection.add(readersInARowBeforeMe);
    }

}
