using ReadersWritersDotnet.Condition;

namespace ReadersWritersDotnet
{
    public class BookWithConditionVariable
    {
        private readonly int _maxReaders;
        private readonly bool _wakeUpAllReaders;

        private int _readersNumber;
        private bool _isWriting;
        
        private int _readersInARowCounter;

        private readonly object _lock;
        private readonly ConditionVariable _writing;
        private readonly ConditionVariable _reading;

        public BookWithConditionVariable(int maxReaders, bool wakeUpAllReaders) {
            _maxReaders = maxReaders;
            _wakeUpAllReaders = wakeUpAllReaders;

            _lock = new object();
            _reading = new ConditionVariable();
            _writing = new ConditionVariable();

            _readersNumber = 0;
            _isWriting = false;
            
            _readersInARowCounter = 0;
        }

        private void EnterWriting()
        {
            lock (_lock)
            {
                while (_isWriting || _readersNumber > 0)
                {
                    _writing.Wait(_lock);
                }

                _isWriting = true;
            }
        }
        
        private int LeaveWriting(){
            lock (_lock)
            {
                _isWriting = false;

                var tmpCounter = _readersInARowCounter;
                if (_wakeUpAllReaders)
                {
                    _reading.PulseAll();
                }
                else
                {
                    _reading.Pulse();
                }
                _writing.Pulse();
                
                return tmpCounter;
            }
        }

        private void EnterReading(){
            lock (_lock)
            {
                while(_isWriting || _readersNumber >= _maxReaders){
                    _reading.Wait(_lock);
                }
                _readersNumber++;
                _readersInARowCounter++;
            }
        }

        private void LeaveReading(){
            lock (_lock)
            {
                _readersNumber--;
                if (_wakeUpAllReaders)
                {
                    _reading.PulseAll();
                } 
                else 
                {
                    _reading.Pulse();
                }
                _writing.Pulse();
            }
        }

        public void Read() {
            EnterReading();
            LeaveReading();
        }

        /// <summary>Writer's critical section.</summary>
        /// <returns>The number of readers entered in a row before a writer got the resource.</returns>
        public int Write()
        {
            EnterWriting();
            return LeaveWriting();
        }
    }
}