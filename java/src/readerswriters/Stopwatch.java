package readerswriters;

public class Stopwatch{
    private boolean measuring;
    private long baseTime;

    public Stopwatch(){
        super();
        measuring = false;
        baseTime = 0;
    }

    public void start(){
        if(this.measuring){
            throw new UnsupportedOperationException("Method 'start' cannot be called while an other measure is in progress.");
        }
        baseTime = System.currentTimeMillis();
        measuring = true;
    }

    public long stop(){
        if(!this.measuring){
            throw new UnsupportedOperationException("Method 'stop' cannot be called if no measure is in progress.");
        }
        measuring = false;
        return System.currentTimeMillis() - baseTime;
    }
}
