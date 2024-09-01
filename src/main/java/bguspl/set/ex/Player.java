package bguspl.set.ex;

import java.util.logging.Level;

import bguspl.set.Env;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    
    /**
     * Incoming Actions (key presses) to player thread 
     */
    private ArrayBlockingQueue<Integer> keyPresses;
    
    /**
     * Dealer obj.
     */
    private Dealer dealer;

    private boolean penaltyTime=false;
    private boolean pointTime=false;
    
    
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer=dealer;
        keyPresses=new ArrayBlockingQueue<Integer>(env.config.featureSize, true); //we need to change this if we want to support magic numbers!!!!***
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            //wait on this until keyPressed
            int slotChoice = -1;
            try {
                   slotChoice=keyPresses.take();
                }
            catch(InterruptedException ignored) {};

            boolean flag=false;
            synchronized(table.slotsWithTokens[id])
            {     
                //check if slot already in array of token
                for(int i=0;i<table.slotsWithTokens[id].length;i++)
                {
                    if(table.slotsWithTokens[id][i]==slotChoice)
                    {
                        table.removeTokenByPlayer(id, slotChoice); //remove the token from table 
                        flag=true;
                            break;
                    }
                }
            }
            if((!flag)&&(table.findFreeCellInMatrix(id)!=-1))
                {
                    if(!dealer.doNothing)
                        table.placeToken(id, slotChoice);
                    if(table.thirdChoiceWasMade(id))
                    {
                        try {
                            dealer.declareSet(id);
                            synchronized(this)
                            {
                                this.wait();
                            }
                        }
                            catch(InterruptedException ignored) {
                            };
                        sleepAfterSet();
                    }
                }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                int random_key_press=new Random().nextInt(12); //generte random integer between 0-11
                env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "press " +random_key_press);
                try{
                    if(keyPresses.remainingCapacity()>0)
                    {
                        keyPresses.put(random_key_press);
                    }
                }
                catch(InterruptedException ignored){}
                
                    //keyPressed(); //call keyPressed with random slot
                // try {
                //     synchronized (this) { wait(500); }
                // } catch (InterruptedException ignored) {}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate=true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        //notifyAll on this  if array isnt full
        if(keyPresses.remainingCapacity()>0)
        {
            try {
                keyPresses.put(slot);
            }
            catch(InterruptedException ignored){}
        }
        

    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() 
    {
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        synchronized(this) {
            this.notifyAll();
        }
        env.logger.log(Level.INFO, "Point was given via method point of class player.");
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() 
    {
        env.logger.warning("Thread " + Thread.currentThread().getName() + " penalty ");

        synchronized(this) {notifyAll();}
        env.logger.log(Level.INFO, "Player " + id + " penalty. My slotsWithTokens: " + table.slotsWithTokens[id]);
    }

    public int score() {
        return score;
    }

    public void createPlayerThreadAndStart() {
        String threadName = "player" + Integer.toString(id);
        this.playerThread = new Thread(this, threadName);
        this.playerThread.start();
    }

    public void setPointTime(boolean bool)
    {
        pointTime=bool;
    }

    public void setPenaltyTime(boolean bool)
    {
        penaltyTime=bool;
    }

    public void sleepAfterSet()
    {
        if(pointTime){
            long timePoint=env.config.pointFreezeMillis;
            timePoint+=System.currentTimeMillis();
            while(timePoint>System.currentTimeMillis())
            {
                env.ui.setFreeze(id, timePoint-System.currentTimeMillis());
                try{
                    Thread.sleep(950);
                }
                catch(InterruptedException ignore) {}
            }
            env.ui.setFreeze(id, 0);
            pointTime=false;
        }
        if(penaltyTime)
        {
            long timePenalty=env.config.penaltyFreezeMillis;
            timePenalty+=System.currentTimeMillis();
            while(timePenalty>System.currentTimeMillis())
            {
                env.ui.setFreeze(id, timePenalty-System.currentTimeMillis());
                try{
                    Thread.sleep(950); 
                }
                catch(InterruptedException e){}
            }
            env.ui.setFreeze(id, 0);
            penaltyTime=false;
        }
    }
    public boolean getTerminate()
    {
        return terminate;
    }

    public boolean getPenaltyTime()
    {
        return penaltyTime;
    }
    public boolean getPointTime()
    {
        return pointTime;
    }

    public void wakeMeUp()
    {
        synchronized(this)
        {
            this.notifyAll();
        }
    }
}
