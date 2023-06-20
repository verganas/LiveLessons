package utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * This class emulates a "compare and swap"-style spin-lock with
 * non-recursive semantics using the Java {@link VarHandle} class.
 */
public class NonReentrantSpinLock
       implements Lock {
    /**
     * The {@link VarHandle} used to access the 'value' field below.
     */
    private static final VarHandle VALUE;

    /*
     * This block of code is run prior to any instance of the {@link
     * NonReentrantSpinLock} being created.
     */
    static {
        try {
            // Get a {@link Lookup} object that can be used to reflect
            // on NonReentrantSpinLock objects.
            MethodHandles.Lookup l = MethodHandles.lookup();

            // Initialize the VarHandle via Java reflection.
            VALUE = l.findVarHandle(NonReentrantSpinLock.class,
                                    "value",
                                    int.class);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    /**
     * The value used to implement the spin-lock.
     */
    private volatile int value;

    /**
     * Acquire the lock only if it is free at the time of invocation.

     * @return True if the lock is immediately available and false
     * otherwise
     */
    @Override
    public boolean tryLock() {
        // Try to set owner's value to true, which succeeds iff its
        // current value is false.
        return VALUE.compareAndSet(this, 0, 1);
    }

    /**
     * Acquire the lock non-interruptibly.  The current thread spins
     * until the lock has been acquired.
     */
    @Override
    public void lock() {
        // Loop trying to set the owner's value to true, which
        // succeeds iff its current value is false.  Each iteration
        // should also check if a shutdown has been requested and if
        // so throw a cancellation exception.

        // Only try to get the lock if its 0, which improves
        // cache performance.  See the stackoverflow article at
        // http://15418.courses.cs.cmu.edu/spring2013/article/31
        // and stackoverflow.com/a/4939383/13411862 for details.
        while (!(value == 0 && tryLock()))
            continue;
    }

    /**
     * Acquire the lock interruptibly.  If the lock is not available
     * then the current thread spins until the lock has been acquired
     * or an interrupt occurs.
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        for (;;) {
            // Only try to get the lock if its null, which improves
            // cache performance.
            if (value == 0 && tryLock())
                // Break out of the loop if we got the lock.
                break;
            else if (Thread.interrupted())
                // Break out of the loop if we were interrupted.
                throw new InterruptedException();
        }
    }

    /**
     * Release the lock.  Throws {@link IllegalMonitorStateException}
     * if the calling thread doesn't own the lock.
     */
    @Override
    public void unlock() {
        // Atomically release the lock that's currently held by the
        // owner.  If the lock is not held by the owner, then throw an
        // IllegalMonitorStateException.
        if ((int) VALUE.getAndSet(this, 0) != 1)
            throw new IllegalMonitorStateException
                ("Unlock called when not locked");
    }

    /**
     * @throw {@link UnsupportedOperationException}
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        throw new UnsupportedOperationException
            ("Timed tryLock() method not implemented");
    }

    /**
     * @throw {@link UnsupportedOperationException}
     */    
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException
            ("newCondition() method not implemented");
    }
}
