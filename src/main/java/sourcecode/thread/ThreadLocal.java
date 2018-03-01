/**
 * 拷贝java.lang.ThreadLocal类源码，以方便学习记录
 */

package sourcecode.thread;
import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * ThreadLocal类提供线程局部变量。 这些变量与它们的正常副本不同，因为每个线程访问变量
 * （通过它的get或set方法）都有自己的，独立初始化的变量副本。 ThreadLocal实例通常被
 * 声明为private static，以此与线程的状态相关联（例如，用户ID或事务ID）。
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 *
 * 例如，下面的类为每个线程生成本地唯一的标识符。
 *
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 *
 * 线程的ID在第一次调用ThreadId.get时被分配，并在随后的调用中保持不变。
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * 只要线程处于活动状态且可以访问ThreadLocal实例，每个线程就会保留对其线程局部变量副本
 * 的隐式引用; 在线程消亡后，线程本地实例的所有副本都将进行垃圾回收（除非存在对这些副本的其他引用）。
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     *
     *
     * ThreadLocals（Thread.threadLocals和inheritableThreadLocals）依赖于附加到每个线程
     * 的线性探测哈希映射。 ThreadLocal对象充当键，通过threadLocalHashCode进行搜索。 这是一
     * 个自定义哈希代码（仅在ThreadLocalMaps中有用），它可以消除通常情况下同一个线程连续构造多个
     * ThreadLocals的冲突，同时在不常见的情况下保持良好行为。
     *
     * 对于每一个ThreadLocal对象，都有一个final修饰的int型的threadLocalHashCode不可变属性，
     * 对于基本数据类型，可以认为它在初始化后就不可以进行修改，所以可以唯一确定一个ThreadLocal对象。
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     *
     * 下一个需要发放的哈希码。 原子更新。 从零开始。
     */
    private static AtomicInteger nextHashCode =
            new AtomicInteger();

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     *
     * 连续生成的哈希码之间的差值。可以让生成出来的值或者说ThreadLocal的ID较为均匀地分布在2的幂次方大小的数组中
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code.
     *
     * 返回下一个哈希码
     *
     * 它是在上一个被构造出的hashcode/ThreadLocal的ID/threadLocalHashCode的基础上加上一个魔数0x61c88647。
     * 这个魔数的选取与斐波那契散列有关，0x61c88647对应的十进制为1640531527。斐波那契散列的乘数可以用
     * (long) ((1L << 31) * (Math.sqrt(5) - 1))可以得到2654435769，如果把这个值给转为带符号的int，
     * 则会得到-1640531527。换句话说 (1L << 32) - (long) ((1L << 31) * (Math.sqrt(5) - 1))得到的结果
     * 就是1640531527也就是0x61c88647。通过理论与实践，当我们用0x61c88647作为魔数累加为每个ThreadLocal分配
     * 各自的ID也就是threadLocalHashCode再与2的幂取模，得到的结果分布很均匀。
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *
     * 返回此线程局部变量的当前线程的“初始值”。 在线程首次访问get方法时，将调用此方法，除非线程先前调用了set方法，
     * 在这种情况下，initialValue方法不会被该线程调用。 通常，每个线程最多调用一次此方法，但在后续调用remove后再调用
     * get时可能会再次调用此方法。
     *
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * 这个实现只是返回null; 如果程序员希望线程局部变量的初始值不是null，则必须实现ThreadLocal的子类，并重写此方法。
     * 通常，将使用匿名内部类。
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * 创建一个线程局部变量。 变量的初始值是通过调用Supplier上的get方法来确定的。
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     *
     * 无参构造函数
     *
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * 返回此线程局部变量的当前线程副本中的值。 如果变量在当前线程没有值，则返回调用initialValue方法返回的值。
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        //获取当前线程
        Thread t = Thread.currentThread();
        //获取线程t中ThreadLocal.ThreadLocalMap属性threadLocals，ThreadLocal的get方法主要就是通过ThreadLocalMap操作的
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            //map.getEntry(this)得到ThreadLocalMap.Entry，是因为ThreadLocalMap是以ThreadLocal作为key存储的
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }

    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * 变种的set方法，创建initialValue。
     * @return the initial value
     */
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            //创建与ThreadLocal关联的ThreadLocalMap
            createMap(t, value);
        return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * 将此线程局部变量的当前线程副本设置为指定值。 大多数子类都不需要重写此方法，仅依靠initialValue方法设置线程本地值初始值。
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        //获取当前线程
        Thread t = Thread.currentThread();
        //获取线程t中ThreadLocal.ThreadLocalMap属性threadLocals，ThreadLocal的set方法主要就是通过ThreadLocalMap操作的
        ThreadLocalMap map = getMap(t);
        if (map != null)
            //map.set(this, value)是因为ThreadLocalMap是将this, value作为一个Entry进行存储
            map.set(this, value);
        else
            //创建与ThreadLocal关联的ThreadLocalMap
            createMap(t, value);
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * 删除当前线程的线程局部变量值。 如果此线程随后调用get读取threadlocal中的值，则通过调用
     * 其initialValue方法重新初始化其值，除非其值为在过渡期间调用set方法设置了。
     * 这可能会导致在当前线程中多次调用initialValue方法。
     *
     * @since 1.5
     */
    public void remove() {
        //获取当前线程相关联的ThreadLocalMap
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            //删除以ThreadLocal作为key的entry对象
            m.remove(this);
    }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * 获取与ThreadLocal关联的ThreadLocalMap。 在InheritableThreadLocal中重写。
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * 创建与ThreadLocal关联的ThreadLocalMap。 在InheritableThreadLocal中重写。
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * 工厂方法创建ThreadLocalMap。 设计为仅从Thread构造函数中调用。
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     *
     * 方法childValue在子类InheritableThreadLocal中可见地定义，但是在此为了提供
     * createInheritedMap工厂方法而在内部定义，而不需要继承InheritableThreadLocal中的地图类。
     * 此技术优于在方法中嵌入instanceof测试的替代方案。
     *
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     *
     * ThreadLocal的扩展，从指定的Supplier获得其初始值。
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     *
     * ThreadLocalMap是一个自定义哈希映射，仅适用于维护线程本地值。 没有任何操作被导出到ThreadLocal类之外。
     * 该类是包私有的，允许在Thread类中声明字段。 为了处理大量的和长期使用的用法，哈希表条目使用WeakReferences作为键。
     * 但是，由于没有使用引用队列，因此只有当表开始用尽空间时，才会删除陈旧的条目。
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         *
         * 这个hash表的entry继承了WeakReference，使用它的主引用字段作为键（它总是一个ThreadLocal对象）。
         * 请注意，null键（即entry.get（）== null）表示该键不再被引用，因此可以从表中删除条目。
         * 这些条目在下面的代码中被称为"陈旧条目"
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         *
         * hash表初始容量——必须为2的幂。
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         *
         * 数组，必要时调整大小。数组长度必须为2的幂
         */
        private Entry[] table;

        /**
         * The number of entries in the table.
         *
         * hash表中entry的数量
         */
        private int size = 0;

        /**
         * The next size value at which to resize.
         *
         * 下一次调整大小数组大小的临界值
         */
        private int threshold;

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         *
         * 设置调整大小阈值以保持最坏的2/3负载系数。
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         *
         * 构建一个最初包含（firstKey，firstValue）的ThreadLocalMap。 ThreadLocalMaps是惰性构造的，
         * 所以我们只至少要存入一个条目时才实例化ThreadLocalMap。
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            //Entry在table数组中存放的位置
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * 获取与key相关联的条目。这种方法本身只处理快速路径：直接击中现有key。
         * 否则，继续执行getEntryAfterMiss方法。这是为了最大限度地提高直接命中的性能而设计的，
         * 同事是为了使此方法易于理解。
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntry(ThreadLocal<?> key) {
            //Entry在table数组中存放的位置
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                //没找到key对应的entry
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * 未在其直接散列槽中找到key时使用getEntryAfterMiss方法。
         * 当getEntry方法在hash槽中没找到对应的key时，调用getEntryAfterMiss方法
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            //基于线性探测法不断向后探测直到遇到空entry
            while (e != null) {
                ThreadLocal<?> k = e.get();
                // 找到目标
                if (k == key)
                    return e;
                // 该entry对应的ThreadLocal已经被回收，调用expungeStaleEntry来清理无效的entry
                if (k == null)
                    //删除旧的entry
                    expungeStaleEntry(i);
                else
                    // 环形意义下往后面走
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            // 线性探测法解决hash冲突的情况
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();
                // 找到对应的entry,覆盖entry的value
                if (k == key) {
                    e.value = value;
                    return;
                }
                // key失效，替换失效的entry
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            //找到Entry数组空位，插入新的Entry
            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         *
         * 删除以ThreadLocal作为key的entry
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * 这个函数是ThreadLocal中核心清理函数，它做的事情：
         * 就是从staleSlot开始遍历，将无效（弱引用指向对象被回收）清理，即对应entry中的value置为null，
         * 将指向这个entry的table[i]置为null，直到扫到空entry。
         * 另外，在过程中还会对非空的entry作rehash。
         * 可以说这个函数的作用就是从staleSlot开始清理连续段中的slot（断开强引用，rehash slot等）
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // 因为entry对应的ThreadLocal已经被回收，将该entry对应的value设为null，显式断开强引用
            tab[staleSlot].value = null;
            // 显式将执行该entry的table[i]为null，以便垃圾回收
            tab[staleSlot] = null;
            size--;

            // Rehash until we encounter null
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                // 清理对应ThreadLocal已经被回收的entry
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    /*
                     * 对于还没有被回收的情况，需要做一次rehash。
                     * 如果对应的ThreadLocal的ID对len取模出来的索引h不为当前位置i，
                     * 则从h向后线性探测到第一个空的slot，把当前的entry给挪过去。
                     */
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        /*
                         * 在原代码的这里有句注释值得一提，原注释如下：
                         *
                         * Unlike Knuth 6.4 Algorithm R, we must scan until
                         * null because multiple entries could have been stale.
                         *
                         * 这段话提及了Knuth高德纳的著作TAOCP（《计算机程序设计艺术》）的6.4章节（散列）
                         * 中的R算法。R算法描述了如何从使用线性探测的散列表中删除一个元素。
                         * R算法维护了一个上次删除元素的index，当在非空连续段中扫到某个entry的哈希值取模后的索引
                         * 还没有遍历到时，会将该entry挪到index那个位置，并更新当前位置为新的index，
                         * 继续向后扫描直到遇到空的entry。
                         *
                         * ThreadLocalMap因为使用了弱引用，所以其实每个slot的状态有三种也即
                         * 有效（value未回收），无效（value已回收），空（entry==null）。
                         * 正是因为ThreadLocalMap的entry有三种状态，所以不能完全套高德纳原书的R算法。
                         *
                         * 因为expungeStaleEntry函数在扫描过程中还会对无效slot清理将之转为空slot，
                         * 如果直接套用R算法，可能会出现具有相同哈希值的entry之间断开（中间有空entry）。
                         */
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * 启发式地清理slot,
         * i对应entry是非无效（指向的ThreadLocal没被回收，或者entry本身为空）
         * n是用于控制控制扫描次数的
         * 正常情况下如果log n次扫描没有发现无效slot，函数就结束了
         * 但是如果发现了无效的slot，将n置为table的长度len，做一次连续段的清理
         * 再从下一个空的slot开始继续扫描
         *
         * 这个函数有两处地方会被调用，一处是插入的时候可能会被调用，另外个是在替换无效slot的时候可能会被调用，
         * 区别是前者传入的n为元素个数，后者为table的容量
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                // i在任何情况下自己都不会是一个无效slot，所以从下一个开始判断
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    // 扩大扫描控制因子
                    n = len;
                    removed = true;
                    // 清理一个连续段
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         *
         * 重新包装/重新设置table数组的大小。 首先扫描整个表格，删除陈旧的条目。
         * 如果这不能充分缩小表格的大小，则将表格大小加倍。
         */
        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         *
         * 数组容量翻倍
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        // 线性探测来存放Entry
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         *
         * 做一次全量清理
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}
