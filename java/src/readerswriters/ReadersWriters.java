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
    public static final int[] READERS_NUMBER = new int[]{5, 10, 50, 500, 10000, 100000};
    public static final int[] WRITERS_NUMBER = new int[]{5, 10, 50, 500, 10000, 100000};
    public static final double[] PARALLEL_READERS_RATE = new double[]{0.3, 0.6, 0.9};

    /**
     * Starts the tasks (only runtime measure for now).
     * @param args CLI arguments. The first one is the path where the runtime results will be saved (mandatory).
     */
    public static void main(String[] args) {
        if(args.length == 0 || args[0].trim().isEmpty()){
            System.out.println("You have to provide a path (where the results will be saved) as a command line argument!");
        }
        else{
            Collection<RuntimeResultDto> runtimeResults = new ArrayList<>();

            for(double parallelReadersRate : PARALLEL_READERS_RATE){
                double[][] runTimesTable = runScalingExamination(new Stopwatch(), parallelReadersRate);
                runtimeResults.add(new RuntimeResultDto(parallelReadersRate, runTimesTable));
                System.out.println("Runtime measure is ready forgit stT: rate " + parallelReadersRate * 100 + "%");
            }

            System.out.println("Saving runtime results...");
            System.out.println(Utils.saveResult(READERS_NUMBER, WRITERS_NUMBER, runtimeResults, args[0]));
        }
    }

    private static double[][] runScalingExamination(Stopwatch stopwatch, double parallelReadersRate){
        double[][] resultMatrix = new double[WRITERS_NUMBER.length][READERS_NUMBER.length];

        for(int wIndex = 0; wIndex < WRITERS_NUMBER.length; wIndex++){
            for(int rIndex = 0; rIndex < WRITERS_NUMBER.length; rIndex++){
                resultMatrix[wIndex][rIndex] =
                        (double)runReadersWritersWithParams(READERS_NUMBER[rIndex], WRITERS_NUMBER[wIndex], parallelReadersRate, true, stopwatch) / 1000;
            }
        }
        return resultMatrix;
    }

    private static long runReadersWritersWithParams(int readersNumber, int writersNumber, double parallelReadersRate, boolean wakeUpAllReaders, Stopwatch stopwatch){
        Book book = new Book(calculateMaxReaders(readersNumber, parallelReadersRate), wakeUpAllReaders);
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

    /**
     * Calculates the
     * @param readersNumber Total number of readers.
     * @param readersRate Rate of readers can be work parallel. Must be a number in range ]0;1].
     * @return How many readers can work in parallel. If the result is not an integer it will be rounded up to the next integer value.
     */
    private static int calculateMaxReaders(int readersNumber, double readersRate){
        return (int) Math.ceil(readersNumber * readersRate);
    }
}

class RuntimeResultDto {
    private double[][] runtimeTable;
    private double parallelReadersRate;

    public RuntimeResultDto(double parallelReadersRate, double[][] runtimeTable){
        this.parallelReadersRate = parallelReadersRate;
        this.runtimeTable = runtimeTable;
    }

    public double[][] getRuntimeTable() {
        return runtimeTable;
    }

    public double getParallelReadersRate() {
        return parallelReadersRate;
    }
}

class Utils {
    public static String saveResult(int[] readersConfigs, int[] writersConfigs, Collection<RuntimeResultDto> runtimeResults, String targetPath){
        String errorMessage = null;
        File resultFile = new File(targetPath);
        try{
            resultFile.createNewFile();
            saveRunTimesAsCsv(readersConfigs, writersConfigs, runtimeResults, resultFile);
        }
        catch(FileNotFoundException fileNotFoundException){
            errorMessage = String.format("A problem occurred while saving results to %s", targetPath);
        }
        catch (IOException ioException){
            errorMessage = String.format("A problem occurred while creating the result file %s", targetPath);
        }
        return errorMessage == null ? String.format("Results successfully saved to %s", targetPath) : errorMessage;
    }


    public static void saveRunTimesAsCsv(int[] readersConfigs, int[] writersConfigs, Collection<RuntimeResultDto> runtimeResults, File targetFile) throws FileNotFoundException{
        Collection<String[]> csvLines = produceDataLines(readersConfigs, writersConfigs, runtimeResults);

        try (PrintWriter pw = new PrintWriter(targetFile)) {
            csvLines.stream()
                    .map((data) -> Stream.of(data).collect(Collectors.joining(",")))
                    .forEach(pw::println);
        }
    }

    private static Collection<String[]> produceDataLines(int[] readersConfigs, int[] writersConfigs, Collection<RuntimeResultDto> runtimeResults){
        ArrayList<String[]> lines = new ArrayList<>();

        for(RuntimeResultDto resultsForOneRate : runtimeResults){
            lines.addAll(produceDataLinesForOneParallelReadersRate(readersConfigs, writersConfigs, resultsForOneRate));
        }

        return lines;
    }

    private static Collection<String[]> produceDataLinesForOneParallelReadersRate(int[] readersConfigs, int[] writersConfigs, RuntimeResultDto resultsForOneRate){
        ArrayList<String[]> lines = new ArrayList<>();

        lines.add(new String[]{"Parallel readers rate:", resultsForOneRate.getParallelReadersRate() * 100 + "%"});

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
                line[j + 1] = String.valueOf(resultsForOneRate.getRuntimeTable()[i][j]);
            }

            lines.add(line);
        }

        return lines;
    }
}
