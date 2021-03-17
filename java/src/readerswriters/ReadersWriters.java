//TODO:
// runtime measure (ok)
// scaling: how affects the runtime if we use more readers/writers (ok)
// waking up readers - signal vs. signalAll - what's the difference
package readerswriters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadersWriters {
    public static final int[] READERS_NUMBER = new int[]{5, 10, 50, 500, 10000, 1000000};
    public static final int[] WRITERS_NUMBER = new int[]{5, 10, 50, 500, 10000, 1000000};
    public static final int MAX_READERS_NUMBER = 2;

    /**
     * Starts the tasks (only runtime measure for now).
     * @param args CLI arguments. The first one is the path where the runtime results will be saved (mandatory).
     */
    public static void main(String[] args) {
        if(args.length == 0 || args[0].trim().isEmpty()){
            System.out.println("You have to provide a path (where the results will be saved) as a command line argument!");
        }
        else{
            double[][] runTimes = runScalingExamination(new Stopwatch());

            System.out.println("Saving runtime results...");
            System.out.println(Utils.saveResult(READERS_NUMBER, WRITERS_NUMBER, runTimes, args[0]));
        }
    }

    private static double[][] runScalingExamination(Stopwatch stopwatch){
        double[][] resultMatrix = new double[WRITERS_NUMBER.length][READERS_NUMBER.length];

        for(int wIndex = 0; wIndex < WRITERS_NUMBER.length; wIndex++){
            for(int rIndex = 0; rIndex < WRITERS_NUMBER.length; rIndex++){
                resultMatrix[wIndex][rIndex] =
                        (double)runReadersWritersWithParams(READERS_NUMBER[rIndex], WRITERS_NUMBER[wIndex], MAX_READERS_NUMBER, true, stopwatch) / 1000;
            }
        }
        return resultMatrix;
    }

    private static long runReadersWritersWithParams(int readersNumber, int writersNumber, int maxReaders, boolean wakeUpAllReaders, Stopwatch stopwatch){
        Book book = new Book(maxReaders, wakeUpAllReaders);
        Collection<Thread> readersAndWriters = new ArrayList<>();

        stopwatch.start();
        for(int i = 0;;i++){
            if(i < readersNumber){
                addThread(readersAndWriters, new Reader(book)).start();
            }
            if(i < writersNumber){
                addThread(readersAndWriters, new Writer(book)).start();
            }
            if(i >= readersNumber && i >= writersNumber){
                break;
            }
        }
        for (Thread t : readersAndWriters){
            try{
                t.join();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        return stopwatch.stop();
    }

    private static Thread addThread(Collection<Thread> threadCollection, Thread thread){
        threadCollection.add(thread);
        return thread;
    }
}

class Utils {
    public static String saveResult(int[] readersConfigs, int[] writersConfigs, double[][] runTimeTableRows, String targetPath){
        String errorMessage = null;
        File resultFile = new File(targetPath);
        try{
            resultFile.createNewFile();
            saveRunTimesAsCsv(readersConfigs, writersConfigs, runTimeTableRows, resultFile);
        }
        catch(FileNotFoundException fileNotFoundException){
            errorMessage = String.format("A problem occurred while saving results to %s", targetPath);
        }
        catch (IOException ioException){
            errorMessage = String.format("A problem occurred while creating the result file %s", targetPath);
        }
        return errorMessage == null ? String.format("Results successfully saved to %s", targetPath) : errorMessage;
    }


    public static void saveRunTimesAsCsv(int[] readersConfigs, int[] writersConfigs, double[][] runTimeTableRows, File targetFile) throws FileNotFoundException{
        Collection<String[]> csvLines = produceDataLines(readersConfigs, writersConfigs, runTimeTableRows);

        try (PrintWriter pw = new PrintWriter(targetFile)) {
            csvLines.stream()
                    .map((data) -> Stream.of(data).collect(Collectors.joining(",")))
                    .forEach(pw::println);
        }
    }

    private static Collection<String[]> produceDataLines(int[] readersConfigs, int[] writersConfigs, double[][] runTimeTableRows){
        ArrayList<String[]> lines = new ArrayList<>();

        String[] firstLine = new String[readersConfigs.length + 1];

        firstLine[0] = "writers | readers";
        for(int i = 0; i < readersConfigs.length; i++){
            firstLine[i + 1] = String.valueOf(readersConfigs[i]);
        }

        lines.add(firstLine);

        for(int i = 0; i < writersConfigs.length; i++){
            String[] line = new String[readersConfigs.length + 1];

            line[0] = String.valueOf(writersConfigs[i]);
            for(int j = 0; j < readersConfigs.length; j++){
                line[j + 1] = String.valueOf(runTimeTableRows[i][j]);
            }

            lines.add(line);
        }

        return lines;
    }
}