//TODO:
// runtime measure
// scaling: how affects the runtime if we use more readers/writers
// waking up readers - signal vs. signalAll - what's the difference
package readerswriters;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReadersWriters {
    public static final int READERS_NUMBER = 10;
    public static final int WRITERS_NUMBER = 20;
    public static final int MAX_READERS_NUMBER = 2;

    public static void main(String[] args) {
        Book book = new Book(MAX_READERS_NUMBER);
        for (int i = 0; i < WRITERS_NUMBER; i++) {
            Thread thread = new Thread(new Writer(book));
            thread.start();
        }
        for (int i = 0; i < READERS_NUMBER; i++) {
            Thread thread = new Thread(new Reader(book));
            thread.start();
        }
    }
}

class Book {

    private int maxReaders;
    private int readersNumber;

    private boolean isWriting = false;

    private ReentrantLock lock = new ReentrantLock();
    private Condition writing = lock.newCondition();
    private Condition reading = lock.newCondition();

    public Book(int maxReaders) {
        super();
        this.maxReaders = maxReaders;
    }

    public void write() {
        lock.lock();
        while(isWriting == true || readersNumber > 0){
            try{
                writing.await();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        isWriting = true;
        System.out.println(Thread.currentThread().getName() + "started writing.");
        lock.unlock();

        lock.lock();
        isWriting = false;
        reading.signalAll();
        writing.signal();
        System.out.println(Thread.currentThread().getName() + "finished writing.");
        lock.unlock();
    }

    public void read() {
        lock.lock();
        while(isWriting == true || readersNumber >= maxReaders){
            try{
                reading.await();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        readersNumber++;
        System.out.println(Thread.currentThread().getName() + "started reading.");
        lock.unlock();

        lock.lock();
        readersNumber--;
        reading.signalAll();
        writing.signal();
        System.out.println(Thread.currentThread().getName() + "finished reading.");
        lock.unlock();
    }
}

class Writer implements Runnable {

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

class Reader implements Runnable {

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