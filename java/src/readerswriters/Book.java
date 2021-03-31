package readerswriters;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Book {

    private int maxReaders;
    private boolean wakeUpAllReaders;

    private int readersNumber;
    private boolean isWriting = false;

    private int readersInARowCounter;

    private ReentrantLock lock = new ReentrantLock();
    private Condition writing = lock.newCondition();
    private Condition reading = lock.newCondition();

    public Book(int maxReaders, boolean wakeUpAllReaders) {
        super();
        this.maxReaders = maxReaders;
        this.wakeUpAllReaders = wakeUpAllReaders;
        this.readersInARowCounter = 0;
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

    /**
     * Writer leaves its critical section.
     * @returns The number of readers entered in a row before a writer got the resource.
     */
    public int leaveWriting(){
        lock.lock();

        isWriting = false;
        int tmpCounter = readersInARowCounter;
        readersInARowCounter = 0;

        if (wakeUpAllReaders) {
            reading.signalAll();
        } else {
            reading.signal();
        }
        writing.signal();

        // System.out.println(String.format("Writer-exit: %s is about to leave...", Thread.currentThread().getName()));

        lock.unlock();
        return tmpCounter;
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
        readersInARowCounter++;
        // System.out.println(String.format("Reader-entry: %s is about to enter...", Thread.currentThread().getName()));

        lock.unlock();
    }

    public void leaveReading(){
        lock.lock();

        readersNumber--;
        if (wakeUpAllReaders) {
            reading.signalAll();
        } else {
            reading.signal();
        }
        writing.signal();

        // System.out.println(String.format("Reader-exit: %s is about to leave...", Thread.currentThread().getName()));

        lock.unlock();
    }

    /**
     * Writer's critical section.
     * @returns The number of readers entered in a row before a writer got the resource.
     */
    public int write() {
        enterWriting();
        return leaveWriting();
    }

    public void read() {
        enterReading();
        leaveReading();
    }
}
