import java.util.Random;

// Customer Thread
class Customer extends Thread {
    private final Buffet buffet;
    private final SimulationSpeed speedConfig;
    private final Random rand = new Random();

    public Customer(String name, Buffet buffet, SimulationSpeed speedConfig) {
        super(name);
        this.buffet = buffet;
        this.speedConfig = speedConfig;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                int action = rand.nextInt(7); 
                long actionTime = speedConfig.getRandomTime(); 

                switch (action) {
                    case 0: 
                        buffet.consume(getName(), 0, 0, 1, "a coffee", actionTime, "drinks"); 
                        Thread.sleep(actionTime); 
                        buffet.log(getName() + " finished drinking.");
                        break;
                    case 1: 
                        buffet.consume(getName(), 0, 1, 0, "a tea", actionTime, "drinks"); 
                        Thread.sleep(actionTime); 
                        buffet.log(getName() + " finished drinking.");
                        break;
                    case 2: 
                        buffet.consume(getName(), 1, 1, 0, "tea and cake", actionTime, "eats"); 
                        Thread.sleep(actionTime); 
                        buffet.log(getName() + " finished eating.");
                        break;
                    case 3: 
                        buffet.consume(getName(), 1, 0, 1, "coffee and cake", actionTime, "eats"); 
                        Thread.sleep(actionTime); 
                        buffet.log(getName() + " finished eating.");
                        break;
                    case 4: 
                        buffet.consume(getName(), 1, 0, 0, "a cake", actionTime, "eats"); 
                        Thread.sleep(actionTime); 
                        buffet.log(getName() + " finished eating.");
                        break;
                    case 5: 
                        buffet.listenToMusic(getName(), actionTime); 
                        break;
                    case 6: 
                        buffet.playPiano(getName(), actionTime); 
                        break;
                }
                Thread.sleep(speedConfig.getRandomTime() / 2); 
            }
        } catch (InterruptedException e) {
            interrupt();
        }
    }
}

// Staff Thread
class Staff extends Thread {
    private final Buffet buffet;
    private final SimulationSpeed speedConfig;
    private final String specialization; 
    private final Random rand = new Random();

    public Staff(String name, Buffet buffet, SimulationSpeed speedConfig, String specialization) {
        super(name);
        this.buffet = buffet;
        this.speedConfig = speedConfig;
        this.specialization = specialization;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                long prepTime = speedConfig.getRandomTime() * 2;
                Thread.sleep(prepTime); 

                int amount = rand.nextInt(3) + 1; 
                
                if (specialization.equals("Cake")) {
                    buffet.replenish(getName(), amount, 0, 0, "cakes");
                } else if (specialization.equals("Tea")) {
                    buffet.replenish(getName(), 0, amount, 0, "teas");
                } else if (specialization.equals("Coffee")) {
                    buffet.replenish(getName(), 0, 0, amount, "coffees");
                }
                
                buffet.log(getName() + " returns to kitchen.");
                Thread.sleep(speedConfig.getRandomTime() / 2); 
            }
        } catch (InterruptedException e) {
            interrupt();
        }
    }
}