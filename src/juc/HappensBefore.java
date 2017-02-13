package juc;

/**
 * 
 * @author shichaopeng
 *
 *         当多个线程访问一个类时，如果不用考虑这些线程在运行时环境下的调度和交替运行，并且不需要额外的同步及在调用方代码不必做其他的协调，这个类的行为仍然是正确的，那么这个类就是线程安全的。
 * 
 *         显然只有资源竞争时才会导致线程不安全，因此无状态对象永远是线程安全的。
 * 
 *         原子操作的描述是： 多个线程执行一个操作时，其中任何一个线程要么完全执行完此操作，要么没有执行此操作的任何步骤，那么这个操作就是原子的。
 */
public class HappensBefore {
	public static void main(String[] args) throws InterruptedException {
		ReorderingDemo reorderingDemo = new ReorderingDemo();
		reorderingDemo.test();
	}
}

/**
 * 
 * @author shichaopeng 在这个例子中one/two两个线程修改区x,y,a,b四个变量， 在执行100次的情况下，可能得到(0
 *         1)或者（1 0）或者（1 1）。事实上按照JVM的规范以及CPU的特性有很可能得到（0 0）。当然上面的代码大家不一定能得到（0 0），
 *         因为run()里面的操作过于简单，可能比启动一个线程花费的时间还少，因此上面的例子难以出现（0,0）。但是在现代CPU和JVM上确实是存在的。由于run()里面的动作对于结果是无关的，因此里面的指令可能发生指令重排序，即使是按照程序的顺序执行，数据变化刷新到主存也是需要时间的。假定是按照a=1;x=b;b=1;y=a;执行的，x=0是比较正常的，虽然a=1在y=a之前执行的，但是由于线程one执行a=1完成后还没有来得及将数据1写回主存（这时候数据是在线程one的堆栈里面的），线程two从主存中拿到的数据a可能仍然是0（显然是一个过期数据，但是是有可能的），这样就发生了数据错误。
 */
class ReorderingDemo {
	static int x = 0, y = 0, a = 0, b = 0;

	public void test() throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			x = y = a = b = 0;
			Thread one = new Thread() {
				public void run() {
					a = 1;
					x = b;
				}
			};
			Thread two = new Thread() {
				public void run() {
					b = 1;
					y = a;
				}
			};
			one.start();
			two.start();
			one.join();
			two.join();
			System.out.println(x + " " + y);
		}
	}

}

/**
 * 
 * Java语言规范规定了JVM线程内部维持顺序化语义，也就是说只要程序的最终结果等同于它在严格的顺序化环境下的结果，那么指令的执行顺序就可能与代码的顺序不一致。这个过程通过叫做指令的重排序。指令重排序存在的意义在于：JVM能够根据处理器的特性（CPU的多级缓存系统、多核处理器等）适当的重新排序机器指令，使机器指令更符合CPU的执行特点，最大限度的发挥机器的性能。
 * 
 * 程序执行最简单的模型是按照指令出现的顺序执行，这样就与执行指令的CPU无关，最大限度的保证了指令的可移植性。这个模型的专业术语叫做顺序化一致性模型。但是现代计算机体系和处理器架构都不保证这一点（因为人为的指定并不能总是保证符合CPU处理的特性）。
 */

/**
 * 应用volatile变量的三个原则：
 * 
 * （1）写入变量不依赖此变量的值，或者只有一个线程修改此变量
 * 
 * （2）变量的状态不需要与其它变量共同参与不变约束
 * 
 * （3）访问变量不需要加锁
 */

/**
 * volatile语义
 * 
 * 到目前为止，我们多次提到volatile，但是却仍然没有理解volatile的语义。
 * 
 * volatile相当于synchronized的弱实现，也就是说volatile实现了类似synchronized的语义，却又没有锁机制。它确保对volatile字段的更新以可预见的方式告知其他的线程。
 * 
 * volatile包含以下语义：
 * 
 * （1）Java 存储模型不会对valatile指令的操作进行重排序：这个保证对volatile变量的操作时按照指令的出现顺序执行的。
 * 
 * （2）volatile变量不会被缓存在寄存器中（只有拥有线程可见）或者其他对CPU不可见的地方，每次总是从主存中读取volatile变量的结果。也就是说对于volatile变量的修改，其它线程总是可见的，并且不是使用自己线程栈内部的变量。也就是在happens-before法则中，对一个valatile变量的写操作后，其后的任何读操作理解可见此写操作的结果。
 * 
 * 尽管volatile变量的特性不错，但是volatile并不能保证线程安全的，也就是说volatile字段的操作不是原子性的，volatile变量只能保证可见性（一个线程修改后其它线程能够理解看到此变化后的结果），要想保证原子性，目前为止只能加锁！
 */

/**
 * Happens-before法则
 * 
 * Java存储模型有一个happens-before原则，就是如果动作B要看到动作A的执行结果（无论A/B是否在同一个线程里面执行），那么A/B就需要满足happens-before关系。
 * 
 * 在介绍happens-before法则之前介绍一个概念：JMM动作（Java Memeory Model
 * Action），Java存储模型动作。一个动作（Action）包括：变量的读写、监视器加锁和释放锁、线程的start()和join()。后面还会提到锁的的。
 * 
 * happens-before完整规则：
 * 
 * （1）同一个线程中的每个Action都happens-before于出现在其后的任何一个Action。
 * 
 * （2）对一个监视器的解锁happens-before于每一个后续对同一个监视器的加锁。
 * 
 * （3）对volatile字段的写入操作happens-before于每一个后续的同一个字段的读操作。
 * 
 * （4）Thread.start()的调用会happens-before于启动线程里面的动作。
 * 
 * （5）Thread中的所有动作都happens-before于其他线程检查到此线程结束或者Thread.join（）中返回或者Thread.isAlive()==false。
 * 
 * （6）一个线程A调用另一个另一个线程B的interrupt（）都happens-before于线程A发现B被A中断（B抛出异常或者A检测到B的isInterrupted（）或者interrupted()）。
 * 
 * （7）一个对象构造函数的结束happens-before与该对象的finalizer的开始
 * 
 * （8）如果A动作happens-before于B动作，而B动作happens-before与C动作，那么A动作happens-before于C动作。
 */