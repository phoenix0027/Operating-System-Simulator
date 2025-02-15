import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.Queue;
import java.util.concurrent.*;

interface SchedulingAlgorithm {
    void addProcess(Process p, BestFitMemoryAllocation memoryManager);
    void executeProcesses();
}

abstract class Process implements Runnable {
    private static final Random random = new Random();
    private static final Set<Integer> usedIds = new HashSet<>();
    private final int processId;
    private final int priority;
    private final int arrivalTime;
    private final int memory;
    private String state;
    private int remainingTime;
    private JLabel label;
    private JProgressBar progressBar;
    private JLabel stateLabel;

    Process(int priority, int arrivalTime, int burstTime, int memory, JLabel label, JProgressBar progressBar, JLabel stateLabel) {
        this.priority = priority;
        this.processId = generateId();
        this.arrivalTime = arrivalTime;
        this.remainingTime = burstTime;
        this.memory = memory;
        this.state = "NEW";
        this.label = label;
        this.progressBar = progressBar;
        this.stateLabel = stateLabel;
        progressBar.setMaximum(burstTime);
    }

    private int generateId() {
        int id;
        do {
            id = 10000 + random.nextInt(90000);
        } while (usedIds.contains(id));
        usedIds.add(id);
        return id;
    }

    public abstract void start();

    @Override
    public void run() {
        start();
        while (remainingTime > 0) {
            executeTimeSlice();
        }
        terminate();
    }

    private void executeTimeSlice() {
        setState("RUNNING");
        int timeSlice = Math.min(remainingTime, 1);
        remainingTime -= timeSlice;
        updateLabel();
        updateProgressBar();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    protected void updateLabel() {
        SwingUtilities.invokeLater(() -> {
            label.setText("Process ID: " + processId + " | Remaining Time: " + remainingTime);
            stateLabel.setText("State: " + state);
        });
    }

    private void updateProgressBar() {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getMaximum() - remainingTime));
    }

    public abstract void terminate();

    public int getArrivalTime() {
        return arrivalTime;
    }

    public String getState() {
        return this.state;
    }

    public int getMemory() {
        return this.memory;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getProcessId() {
        return processId;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public int getPriority() {
        return priority;
    }
}

class MyProcess extends Process {
    MyProcess(int priority, int arrivalTime, int burstTime, int memory, JLabel label, JProgressBar progressBar, JLabel stateLabel) {
        super(priority, arrivalTime, burstTime, memory, label, progressBar, stateLabel);
    }

    @Override
    public void start() {
        setState("RUNNING");
        System.out.println("Process ID: " + getProcessId() + " is starting.");
    }

    @Override
    public void terminate() {
        setState("TERMINATED");
        updateLabel();
        System.out.println("Process ID: " + getProcessId() + " has terminated.");
    }
}

abstract class ProcessScheduler implements SchedulingAlgorithm {
    protected Queue<Process> readyQueue;

    ProcessScheduler() {
        this.readyQueue = new LinkedList<>();
    }

    public boolean isReadyQueueEmpty() {
        return readyQueue.isEmpty();
    }
}

class RoundRobinScheduler extends ProcessScheduler {
    private ScheduledExecutorService executorService;

    RoundRobinScheduler() {
        super();
        executorService = Executors.newScheduledThreadPool(5);
    }

    @Override
    public void addProcess(Process p, BestFitMemoryAllocation memoryManager) {
        if (memoryManager.allocateMemory(p.getMemory())) {
            readyQueue.add(p);
            executorService.submit(p);
            System.out.println("Process ID: " + p.getProcessId() + " added to the queue.");
        } else {
            System.out.println("Process ID: " + p.getProcessId() + " not added due to insufficient memory.");
        }
    }

    @Override
    public void executeProcesses() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}

class BestFitMemoryAllocation {
    private int[] memoryBlocks;
    private TEMP gui;

    BestFitMemoryAllocation(TEMP gui) {
        this.gui = gui;
        memoryBlocks = new int[]{100, 400, 200, 500, 250, 450, 150, 1000, 150, 550};
    }

    public boolean allocateMemory(int memorySize) {
        Integer bestBlock = null;
        int bestIndex = -1;

        for (int i = 0; i < memoryBlocks.length; i++) {
            int blockSize = memoryBlocks[i];
            if (blockSize >= memorySize) {
                if (bestBlock == null || blockSize < bestBlock) {
                    bestBlock = blockSize;
                    bestIndex = i;
                }
            }
        }

        if (bestBlock != null) {
            memoryBlocks[bestIndex] -= memorySize;
            gui.updateMemoryPanel();
            return true;
        } else {
            return false;
        }
    }

    public int[] getMemoryBlocks() {
        return memoryBlocks;
    }
}

public class TEMP extends JFrame {
    private JPanel processPanel;
    private JPanel memoryPanel;
    private BestFitMemoryAllocation memoryManager;
    private RoundRobinScheduler scheduler;

    public TEMP() {
        setTitle("CPU Scheduling Visualization");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        processPanel = new JPanel();
        processPanel.setLayout(new BoxLayout(processPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(processPanel), BorderLayout.CENTER);

        memoryPanel = new JPanel();
        memoryPanel.setLayout(new GridLayout(2, 5, 5, 5));
        add(memoryPanel, BorderLayout.SOUTH);

        memoryManager = new BestFitMemoryAllocation(this); 
        scheduler = new RoundRobinScheduler();

        updateMemoryPanel();
        setVisible(true);
    }

    public void addProcessGUI(int priority, int arrivalTime, int burstTime, int memory) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel processLabel = new JLabel("Process ID: ");
        JProgressBar progressBar = new JProgressBar(0, burstTime);
        
        progressBar.setStringPainted(true);
        
        JLabel stateLabel = new JLabel("State: NEW");

        panel.add(processLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(stateLabel, BorderLayout.SOUTH);

        processPanel.add(panel);
        revalidate();
        repaint();

        MyProcess process = new MyProcess(priority, arrivalTime, burstTime, memory, processLabel, progressBar, stateLabel);
        scheduler.addProcess(process, memoryManager);
    }

    public void updateMemoryPanel() {
        memoryPanel.removeAll();
        for (int block : memoryManager.getMemoryBlocks()) {
            JLabel blockLabel = new JLabel("Block: " + block);
            blockLabel.setHorizontalAlignment(JLabel.CENTER);
            blockLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            memoryPanel.add(blockLabel);
        }
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        TEMP gui = new TEMP();
        gui.addProcessGUI(2, 0, 5, 300);
        gui.addProcessGUI(1, 2, 3, 200);
        gui.addProcessGUI(6, 1, 4, 150);
        gui.addProcessGUI(3, 5, 7, 440);
        gui.addProcessGUI(7, 10, 20, 1000);

        gui.scheduler.executeProcesses();
    }
}