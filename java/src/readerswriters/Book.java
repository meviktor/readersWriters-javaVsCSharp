package readerswriters;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Book {

    private int maxReaders;
    private boolean wakeUpAllReaders;

    private int readersNumber;
    private boolean isWriting = false;

    private ReentrantLock lock = new ReentrantLock();
    private Condition writing = lock.newCondition();
    private Condition reading = lock.newCondition();

    public Book(int maxReaders, boolean wakeUpAllReaders) {
        super();
        this.maxReaders = maxReaders;
        this.wakeUpAllReaders = wakeUpAllReaders;
    }

    public void enterWriting(){
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

        // System.out.println(String.format("Writer-entry: %s is about to enter...", Thread.currentThread().getName()));

        lock.unlock();
    }

    public void leaveWriting(){
        lock.lock();

        isWriting = false;
        if (this.wakeUpAllReaders) {
            reading.signalAll();
        } else {
            reading.signal();
        }
        writing.signal();

        // System.out.println(String.format("Writer-exit: %s is about to leave...", Thread.currentThread().getName()));

        lock.unlock();
    }

    public void enterReading(){
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

        // System.out.println(String.format("Reader-entry: %s is about to enter...", Thread.currentThread().getName()));

        lock.unlock();
    }

    public void leaveReading(){
        lock.lock();

        readersNumber--;
        if (this.wakeUpAllReaders) {
            reading.signalAll();
        } else {
            reading.signal();
        }
        writing.signal();

        // System.out.println(String.format("Reader-exit: %s is about to leave...", Thread.currentThread().getName()));

        lock.unlock();
    }

    public void write() {
        enterWriting();
        leaveWriting();
    }

    public void read() {
        enterReading();
        leaveReading();
    }
}
