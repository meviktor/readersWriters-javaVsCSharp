//using System;
using System.Threading;

namespace ReadersWritersDotnet
{
    public class Book : IBook
    {
        private readonly int _maxReaders;
        private readonly bool _wakeUpAllReaders;
        private readonly object _reading;
        private readonly object _writing;

        private int _readersNumber;
        private int _readersInARowCounter;

        public Book(int maxReaders, bool wakeUpAllReaders) {
            _maxReaders = maxReaders;
            _wakeUpAllReaders = wakeUpAllReaders;
            _reading = new object();
            _writing = new object();

            _readersNumber = 0;
            _readersInARowCounter = 0;
        }

        /// <summary>Writer's critical section.</summary>
        /// <returns>The number of readers entered in a row before a writer got the resource.</returns>
        public int Write(){
            //Console.WriteLine($"{Thread.CurrentThread.Name} (writer) - step in, before lock...");
            int tmpCounter;
            lock (_writing)
            {
                lock (_reading)
                {
                    while(_readersNumber > 0)
                    {
                        Monitor.Wait(_reading);
                    }
                    
                    tmpCounter = _readersInARowCounter;
                    _readersInARowCounter = 0;
                    
                    if (_wakeUpAllReaders)
                    {
                        Monitor.PulseAll(_reading);
                    }
                    else Monitor.Pulse(_reading);
                }
            }
            //Console.WriteLine($"{Thread.CurrentThread.Name} (writer) - finished.");
            return tmpCounter;
        }

        private void EnterReading(){
            //Console.WriteLine($"{Thread.CurrentThread.Name} (reader) - step in, before lock...");
            lock (_reading)
            {
                while(_readersNumber >= _maxReaders)
                {
                    Monitor.Wait(_reading);
                }
                _readersNumber++;
                _readersInARowCounter++;
            }
            //Console.WriteLine($"{Thread.CurrentThread.Name} (reader) - is in...");
        }

        private void LeaveReading(){
            //Console.WriteLine($"{Thread.CurrentThread.Name} (reader) - leaving, before lock...");
            lock (_reading)
            {
                _readersNumber--;
                if (_wakeUpAllReaders)
                {
                    Monitor.PulseAll(_reading);
                }
                else Monitor.Pulse(_reading);
            }
            //Console.WriteLine($"{Thread.CurrentThread.Name} (reader) - finished.");
        }

        public void Read() {
            EnterReading();
            LeaveReading();
        }
    }
}