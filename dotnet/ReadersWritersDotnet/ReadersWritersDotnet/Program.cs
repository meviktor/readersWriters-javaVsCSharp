using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace ReadersWritersDotnet
{
    public static class Program
    {
        private static readonly int[] READERS_NUMBER = new int[] { 5, 10, 50, 1000, 10000, 100000 };
        private static readonly int[] WRITERS_NUMBER = new int[] { 5, 10, 50, 1000, 10000, 100000 };
        private static readonly double[] PARALLEL_READERS_RATE = new double[] { 0.9, 0.6, 0.3};
        private static readonly BlockingCollection<int> READERS_IN_A_ROW = new BlockingCollection<int>();
        private static bool WAKE_UP_ALL_READERS;

        private static readonly ParameterizedThreadStart _reader = (book) =>
        {
            ((BookWithConditionVariable)book)!.Read();
        };

        private static readonly ParameterizedThreadStart _writer = (book) =>
        {
            var readersInARowBeforeMe = ((BookWithConditionVariable)book)!.Write();
            READERS_IN_A_ROW.Add(readersInARowBeforeMe);
        };
        
        public static async Task Main(string[] args)
        {
            if (!Validate(args)) return;
            
            WAKE_UP_ALL_READERS = bool.Parse(args[1]);

            Console.WriteLine($"Output path: {args[0]}");
            Console.WriteLine($"Waking up all readers: {WAKE_UP_ALL_READERS}");

            IList<RuntimeResultDto> runtimeResults = new List<RuntimeResultDto>();

            foreach(var parallelReadersRate in PARALLEL_READERS_RATE){
                var runTimesTable = RunScalingExamination(new Stopwatch(), parallelReadersRate);
                runtimeResults.Add(new RuntimeResultDto(parallelReadersRate, runTimesTable));
                    
                Console.WriteLine($"Runtime measure is ready for: rate {parallelReadersRate * 100}%");
            }

            Console.WriteLine("Saving runtime results...");
            Console.WriteLine(await Utils.SaveResult(READERS_NUMBER, WRITERS_NUMBER, runtimeResults, READERS_IN_A_ROW, WAKE_UP_ALL_READERS, args[0]));
        }
        
        private static double[,] RunScalingExamination(Stopwatch stopwatch, double parallelReadersRate){
            var resultMatrix = new double[WRITERS_NUMBER.Length, READERS_NUMBER.Length];

            for(var wIndex = 0; wIndex < WRITERS_NUMBER.Length; wIndex++){
                for(var rIndex = 0; rIndex < WRITERS_NUMBER.Length; rIndex++){
                    resultMatrix[wIndex, rIndex] =
                            (double)RunReadersWritersWithParams(READERS_NUMBER[rIndex], WRITERS_NUMBER[wIndex], parallelReadersRate, WAKE_UP_ALL_READERS, stopwatch) / 1000;
                    Console.WriteLine($"Parallel readers: {parallelReadersRate * 100}%, readers: {READERS_NUMBER[rIndex]}, writers: {WRITERS_NUMBER[wIndex]}, time: {resultMatrix[wIndex, rIndex]} sec");
                }
            }
            return resultMatrix;
        }

        private static long RunReadersWritersWithParams(int readersNumber, int writersNumber, double parallelReadersRate, bool wakeUpAllReaders, Stopwatch stopwatch){
            var book = new BookWithConditionVariable(CalculateMaxReaders(readersNumber, parallelReadersRate), wakeUpAllReaders);
            IList<Thread> readersAndWriters = new List<Thread>();

            stopwatch.Restart();
            
            for(var i = 0;;i++){
                if(i < readersNumber){
                    readersAndWriters.AddThread(new Thread(_reader)).WithName($"R-{i}").Start(book);
                }
                if(i < writersNumber){
                    readersAndWriters.AddThread(new Thread(_writer)).WithName($"W-{i}").Start(book);
                }
                if(i >= readersNumber && i >= writersNumber){
                    break;
                }
            }

            foreach (var thread in readersAndWriters){
                thread.Join();
            }
            
            stopwatch.Stop();
            return stopwatch.ElapsedMilliseconds;
        }

        private static Thread AddThread(this IList<Thread> threadCollection, Thread thread){
            threadCollection.Add(thread);
            return thread;
        }
        
        private static Thread WithName(this Thread thread, string name)
        {
            thread.Name = name;
            return thread;
        }
        
        private static int CalculateMaxReaders(int readersNumber, double readersRate){
            return (int) Math.Ceiling(readersNumber * readersRate);
        }

        private static bool Validate(string[] args)
        {
            if(args.Length == 0 || string.IsNullOrWhiteSpace(args[0].Trim())){
                Console.WriteLine("You have to provide a path (where the results will be saved) as a command line argument!");
                return false;
            }
            
            if(args.Length == 1 || (args[1].ToLower() != "true" && args[1].ToLower() != "false" )){
                Console.WriteLine("You have specify if all readers has to be woken up (true/false) after the output file path as a command line argument!");
                return false;
            }

            var dir = Path.GetDirectoryName(args[0]);
            if (!Directory.Exists(dir))
            {
                Console.WriteLine($"The following path does not exist: '{dir}'");
            }
            
            if (!File.Exists(args[0])) return true;
            
            string ch;
            do
            {
                Console.Write($"The file '{args[0]}' already exists. Do you want to overwrite it? (y/n) ");
                ch = Console.ReadLine();
            } while (ch != null && (!ch.Equals("y") && !ch.Equals("n")));

            return ch?.Equals("y") ?? false;
        }
    }
    
    public class RuntimeResultDto {
        public double[,] RunTimeTable { get; private set; }
        public double ParallelReadersRate { get; private set; }

        public RuntimeResultDto(double parallelReadersRate, double[,] runtimeTable){
            ParallelReadersRate = parallelReadersRate;
            RunTimeTable = runtimeTable;
        }
    }
    
    public static class Utils {
        public static async Task<string> SaveResult(int[] readersConfigs, int[] writersConfigs, IEnumerable<RuntimeResultDto> runtimeResults, IEnumerable<int> readersInARowResults, bool wakingUpAllReaders, string targetPath){
            string errorMessage = null;
            await using (var writer = File.CreateText(targetPath))
            {
                try
                {
                    await SaveResultsAsCsv(readersConfigs, writersConfigs, runtimeResults, readersInARowResults, wakingUpAllReaders, writer); 
                }                                                                                                                           
                catch (IOException)
                {                                                                                            
                    errorMessage = $"An I/O problem occurred while creating the result file {targetPath}";               
                }                                                                                                                           
                catch(Exception){                                                                                                         
                    errorMessage = $"A problem occurred while saving results to {targetPath}";                            
                }                                                                                                                           
                return errorMessage ?? $"Results successfully saved to {targetPath}";
            }
        }

        private static async Task SaveResultsAsCsv(int[] readersConfigs, int[] writersConfigs, IEnumerable<RuntimeResultDto> runtimeResults, IEnumerable<int> readersInARowResults, bool wakingUpAllReaders, TextWriter csvWriter)
        {
            foreach (var runTimeResultDto in runtimeResults)
            {
                foreach (var line in ProduceDataLinesForOneParallelReadersRate(readersConfigs, writersConfigs, runTimeResultDto))
                {
                    await csvWriter.WriteAsync($"{string.Join(',', line)}\r\n");
                }
            }
            foreach (var line in AppendReadersInARowResults(readersInARowResults, wakingUpAllReaders))
            {
                await csvWriter.WriteAsync($"{string.Join(',', line)}\r\n");
            }
        }
        
        private static IEnumerable<string[]> ProduceDataLinesForOneParallelReadersRate(int[] readersConfigs, int[] writersConfigs, RuntimeResultDto resultsForOneRate){
            IList<string[]> lines = new List<string[]>();

            lines.Add(new string[]{"Parallel readers rate:", $"{resultsForOneRate.ParallelReadersRate * 100} %"});

            var firstLine = new string[readersConfigs.Length + 1];

            firstLine[0] = "writers | readers";
            for(var i = 0; i < readersConfigs.Length; i++){
                firstLine[i + 1] = readersConfigs[i].ToString();
            }
            lines.Add(firstLine);

            for(var i = 0; i < writersConfigs.Length; i++){
                var line = new string[readersConfigs.Length + 1];

                line[0] = writersConfigs[i].ToString();
                for(var j = 0; j < readersConfigs.Length; j++){
                    line[j + 1] = $"{resultsForOneRate.RunTimeTable[i,j]}";
                }

                lines.Add(line);
            }

            return lines;
        }

        private static IEnumerable<string[]> AppendReadersInARowResults(IEnumerable<int> readersInARowResults, bool wakingUpAllReaders)
        {
            var lines = new List<string[]>();
            var numOfItems = readersInARowResults.Count();

            lines.Add(new string[]{"Waking up all readers:", $"{wakingUpAllReaders}"});
            lines.Add(new string[]{$"{(numOfItems <= 100 ? numOfItems : 100)} highest value"});
            
            var largestValues = readersInARowResults.OrderByDescending(x => x).Take(100);
            lines.AddRange(largestValues.Select(x => new string[]{$"{x}"}));

            return lines;
        }
    }
}