import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Interface for logging to the GUI
interface SimulationLogger {
    void log(String message);
}

// Helper class for dynamic speed
class SimulationSpeed {
    private int baseMultiplier = 1000; // Default normal speed
    
    public void setFast() { baseMultiplier = 200; }
    public void setSlow() { baseMultiplier = 2000; }
    
    public long getRandomTime() {
        return (long) (Math.random() * baseMultiplier) + 500;
    }
}

public class CafeGUI extends JFrame implements SimulationLogger {
    private JTextArea logArea;
    private Buffet buffet;
    private SimulationSpeed speedConfig = new SimulationSpeed();
    private List<Thread> activeThreads = new ArrayList<>();
    private int customerCount = 0;
    
    // UI Input Fields
    private JTextField txtCustomers;
    private JTextField txtStaff;
    private JTextField txtCakes;
    private JTextField txtTeas;
    private JTextField txtCoffees;
    
    // Buttons
    private JButton btnStart;
    private JButton btnAddCustomer;

    public CafeGUI() {
        setTitle("Betty's Cafe Simulation");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Central Log Area ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        // --- Top Panel for Configuration Inputs ---
        JPanel inputPanel = new JPanel(new FlowLayout());
        
        inputPanel.add(new JLabel("Customers:"));
        txtCustomers = new JTextField("6", 3); // Default value 6, width 3
        inputPanel.add(txtCustomers);
        
        inputPanel.add(new JLabel("Staff:"));
        txtStaff = new JTextField("3", 3);
        inputPanel.add(txtStaff);
        
        inputPanel.add(new JLabel("Cakes:"));
        txtCakes = new JTextField("5", 3);
        inputPanel.add(txtCakes);
        
        inputPanel.add(new JLabel("Teas:"));
        txtTeas = new JTextField("5", 3);
        inputPanel.add(txtTeas);
        
        inputPanel.add(new JLabel("Coffees:"));
        txtCoffees = new JTextField("5", 3);
        inputPanel.add(txtCoffees);
        
        add(inputPanel, BorderLayout.NORTH);

        // --- Bottom Panel for Simulation Controls ---
        JPanel controlPanel = new JPanel();
        
        btnStart = new JButton("Start Simulation");
        btnAddCustomer = new JButton("Add Customer dynamically");
        btnAddCustomer.setEnabled(false); // Disabled until simulation actually starts
        
        JButton btnSpeedFast = new JButton("Speed: Fast");
        JButton btnSpeedSlow = new JButton("Speed: Slow");

        controlPanel.add(btnStart);
        controlPanel.add(btnAddCustomer);
        controlPanel.add(btnSpeedFast);
        controlPanel.add(btnSpeedSlow);
        add(controlPanel, BorderLayout.SOUTH);

        // --- Button Actions ---
        
        btnStart.addActionListener(e -> {
            try {
                int customers = Integer.parseInt(txtCustomers.getText());
                int staff = Integer.parseInt(txtStaff.getText());
                int cakes = Integer.parseInt(txtCakes.getText());
                int teas = Integer.parseInt(txtTeas.getText());
                int coffees = Integer.parseInt(txtCoffees.getText());
                
                startSimulation(customers, staff, cakes, teas, coffees);
                
                btnStart.setEnabled(false);
                txtCustomers.setEnabled(false);
                txtStaff.setEnabled(false);
                txtCakes.setEnabled(false);
                txtTeas.setEnabled(false);
                txtCoffees.setEnabled(false);
                
                btnAddCustomer.setEnabled(true);
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter valid whole numbers for all fields.", 
                    "Input Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        btnAddCustomer.addActionListener(e -> {
            if (buffet != null) {
                customerCount++;
                Customer c = new Customer("Client-" + customerCount, buffet, speedConfig);
                c.start();
                activeThreads.add(c);
                log(">>> Dynamically added Client-" + customerCount + " <<<");
            }
        });

        btnSpeedFast.addActionListener(e -> {
            speedConfig.setFast();
            log(">>> Simulation speed set to FAST <<<");
        });
        
        btnSpeedSlow.addActionListener(e -> {
            speedConfig.setSlow();
            log(">>> Simulation speed set to SLOW <<<");
        });
    }

    private void startSimulation(int initialCustomers, int numStaff, int cakes, int teas, int coffees) {
        log("Starting program with " + initialCustomers + " clients and " + numStaff + " staff.");
        log("Buffet (" + cakes + " cakes, " + teas + " teas, " + coffees + " coffees)");
        
        buffet = new Buffet(cakes, teas, coffees, this);
        customerCount = initialCustomers;

        // Create and start Staff threads
        String[] specializations = {"Cake", "Tea", "Coffee"};
        for (int i = 0; i < numStaff; i++) {
            Staff s = new Staff("Staff-" + (i + 1), buffet, speedConfig, specializations[i % 3]);
            s.start();
            activeThreads.add(s);
        }

        // Create and start Customer threads
        for (int i = 0; i < initialCustomers; i++) {
            Customer c = new Customer("Client-" + (i + 1), buffet, speedConfig);
            c.start();
            activeThreads.add(c);
        }
    }

    @Override
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto-scroll to bottom
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CafeGUI().setVisible(true));
    }
}