import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;

public class Buffet {
    private int cakes;
    private int teas;
    private int coffees;
    
    private final SimulationLogger logger;
    private final ReentrantLock buffetLock = new ReentrantLock(true); 
    private final Condition itemAvailable = buffetLock.newCondition();
    private final Semaphore piano = new Semaphore(2, true); 
    
    // List to track customers waiting for items
    private final List<String> waitingQueue = new ArrayList<>();

    public Buffet(int cakes, int teas, int coffees, SimulationLogger logger) {
        this.cakes = cakes;
        this.teas = teas;
        this.coffees = coffees;
        this.logger = logger;
    }

    // Proxy method so threads can print generic messages
    public void log(String message) {
        logger.log(message);
    }

    public void consume(String customerName, int reqCakes, int reqTeas, int reqCoffees, String itemDesc, long time, String action) throws InterruptedException {
        buffetLock.lock();
        try {
            logger.log(customerName + " wants " + itemDesc + ".");
            
            // If items aren't available, join the waiting queue
            if (cakes < reqCakes || teas < reqTeas || coffees < reqCoffees) {
                waitingQueue.add(customerName);
                logger.log(customerName + " waits. [Queue: " + waitingQueue + "]");
                
                while (cakes < reqCakes || teas < reqTeas || coffees < reqCoffees) {
                    itemAvailable.await(); 
                }
                
                // Items are now available, leave the queue
                waitingQueue.remove(customerName);
            }
            
            cakes -= reqCakes;
            teas -= reqTeas;
            coffees -= reqCoffees;
            
            logger.log(customerName + " takes " + itemDesc + " and " + action + " for " + time + "ms.");
            logger.log("Buffet (" + cakes + " cakes, " + teas + " teas, " + coffees + " coffees).");
        } finally {
            buffetLock.unlock();
        }
    }

    public void replenish(String staffName, int addCakes, int addTeas, int addCoffees, String itemDesc) {
        buffetLock.lock();
        try {
            cakes += addCakes;
            teas += addTeas;
            coffees += addCoffees;
            
            logger.log(staffName + " brings " + (addCakes + addTeas + addCoffees) + " " + itemDesc + ".");
            logger.log("Buffet (" + cakes + " cakes, " + teas + " teas, " + coffees + " coffees).");
            
            itemAvailable.signalAll();
        } finally {
            buffetLock.unlock();
        }
    }

    public void playPiano(String customerName, long duration) throws InterruptedException {
        logger.log(customerName + " wants to play the piano.");
        piano.acquire();
        try {
            logger.log(customerName + " plays the piano for " + duration + "ms.");
            Thread.sleep(duration);
        } finally {
            logger.log(customerName + " finished playing the piano.");
            piano.release();
        }
    }
    
    public void listenToMusic(String customerName, long duration) throws InterruptedException {
        logger.log(customerName + " listens to music for " + duration + "ms.");
        Thread.sleep(duration);
        logger.log(customerName + " finished listening to music.");
    }
}