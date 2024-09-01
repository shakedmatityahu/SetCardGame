package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * A fair queue contains all player ids that claim to have a set
     */
    private ArrayBlockingQueue<Integer> playersToCheck;

    private int playerToReward;
    private boolean removedAllCards;
    private long nextSecond = Long.MAX_VALUE;
    private long nextMilli = Long.MAX_VALUE;
    private boolean warningSeconds=false; 
    private int[] setSlots;
    public boolean doNothing=true;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.playersToCheck = new ArrayBlockingQueue<Integer>(players.length, true);
        this.playerToReward = -1;
        this.removedAllCards = true;
        setSlots = new int[env.config.featureSize];
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        // Creates and starts the player threads
        for(Player p : players) {
            p.createPlayerThreadAndStart();
        }
        while (!shouldFinish()) {
            warningSeconds = (reshuffleTime - System.currentTimeMillis() < env.config.turnTimeoutWarningMillis) ? true : false; 
            placeCardsOnTable();
            doNothing=false;
            timerLoop();
            wakeAllPlayers();
            updateTimerDisplay(true);
            doNothing=true;
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        updateTimerDisplay(false);
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            nextSecond = System.currentTimeMillis() + 1000;
            nextMilli = System.currentTimeMillis() + 1;
            warningSeconds = (reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis) ? true : false; 
            playerToReward = -1;
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            if(deck.size() >= env.config.featureSize)
                placeCardsOnTable();
            else
                terminate();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        for(Player p : players) {
            p.terminate();

        } 
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them. Only for threesomes
     */
    private void removeCardsFromTable() {
        if(playerToReward != -1)
        {
            synchronized(table.slotsWithTokens)
            {
                for(int s : setSlots) {
                    if(table.slotToCard[s]==-1)
                        System.out.println("Player id: "+playerToReward+ " "+ setSlots[0]+setSlots[1]+setSlots[2]);
                    table.removeToken(s);
                    table.removeCard(s);
                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        int slotToFill;
        synchronized (table.slotsWithTokens) {
            if (playerToReward != -1) {
                // a set was removed from the table, need to draw new cards, take first in deck and place
                for (int i = 0; i < env.config.featureSize; i++) {
                    slotToFill = setSlots[i];
                    table.placeCard(getRandomCardFromDeck(), slotToFill);
                }
            }
            else if (removedAllCards) {
                placeCardsOnEntireTable();
                removedAllCards = false;
            }
        }
    }

    /**
     * All slots are empty, shuffle the deck and place cards.
     */
    private void placeCardsOnEntireTable() {
        env.logger.log(Level.INFO, "Placing cards on ENTIRE table");
        int card, slot;

        for (int r = 0; r < env.config.rows; r++) {
            for (int c = 0; c < env.config.columns; c++) {
                card = getRandomCardFromDeck();
                slot = r * env.config.columns + c;
                table.placeCard(card, slot);
            }
        }
    }

    private int getRandomCardFromDeck() {
        int random_idx = new Random().nextInt(deck.size());
        return deck.remove(random_idx);
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // try to take out the first element from playersToCheck (=if empty, wait until timeout)
        warningSeconds = (reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis) ? true : false;
        Integer pToCheck;
        try {
            env.logger.warning("Thread " + Thread.currentThread().getName() + " sleepUntilWokenOrTimeout"); 
            env.logger.warning("Thread " + Thread.currentThread().getName() + " sleepUntilWokenOrTimeout");
            if(warningSeconds)
                //pToCheck=playersToCheck.take();
                pToCheck = playersToCheck.poll(nextMilli-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            else
                pToCheck = playersToCheck.poll(nextSecond - System.currentTimeMillis(), TimeUnit.MILLISECONDS); 
            env.logger.warning("Thread " + Thread.currentThread().getName() + " pToCheck "+ pToCheck); 
            if (pToCheck != null) {
                if(table.thirdChoiceWasMade(pToCheck)) {
                    env.logger.warning("Thread " + Thread.currentThread().getName() + " thirdChoiceWasMade "+ pToCheck);
                    handlePlayerToCheck(pToCheck);
                }
                else
                    players[pToCheck].wakeMeUp();
        }
        }
        catch(NullPointerException ignored) {
            env.logger.warning("Thread " + Thread.currentThread().getName() + " NullPointerException ");

        }
        catch(InterruptedException ignored){}
    }

    /**
     * Rewards or penalizes the player.
     */
    public void handlePlayerToCheck(int pId) {
        setSlots = Arrays.copyOf(table.slotsWithTokens[pId], table.slotsWithTokens[pId].length);
        int[] setCards = slotsToCards(setSlots);

        if(env.util.testSet(setCards)) {
            playerToReward = pId;
            updateTimerDisplay(true);
            players[pId].setPointTime(true);
            players[pId].point();
        }
        else {
            players[pId].setPenaltyTime(true);
            players[pId].penalty();
        }
    }

    /**
     * Gets an array of slots; Returns an array of cards.
     */
    public int[] slotsToCards(int[] slotsArr) {
        int[] cardsArr = new int[slotsArr.length];
        for (int i = 0; i < slotsArr.length; i++) {
            cardsArr[i] = table.slotToCard[slotsArr[i]];
        }
        return cardsArr;
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if (reset) {
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        }
        else {
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), warningSeconds);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        int slot;
        int card;
    
        synchronized(table.slotsWithTokens) {
            for (int r = 0; r < env.config.rows; r++) {
                for (int c = 0; c < env.config.columns; c++) {
                    slot = r * env.config.columns + c;
                    card = table.slotToCard[slot];
                    table.removeToken(slot);
                    table.removeCard(slot);
                    backToDeck(card);
                }
            }
            this.removedAllCards = true;
            playersToCheck.clear();
            wakeAllPlayers();
        }
    }

    private void wakeAllPlayers()
    {
        for (Player p : players)
        {
            p.wakeMeUp();
        }
    }

    private void backToDeck(int card) {
        deck.add(card);
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        List<Integer> winnersLst = new ArrayList<Integer>();
        int maxScore = 0;

        for (Player p : players) {
            if (p.score() >= maxScore) {
                maxScore = p.score();
            }
        }
        for (Player p : players) {
            if (p.score() == maxScore) {
                winnersLst.add(p.id);
            }
        }

        int[] winnersArr = winnersLst.stream().mapToInt(Integer::intValue).toArray();
        env.ui.announceWinner(winnersArr);
    }

    /**
     * Gets player that placed 3 tokens and inserts to playersToCheck
     */
    public void declareSet(int playerId) {
        try {
            this.playersToCheck.put(playerId);
            }
        catch(InterruptedException ignored){};
    }

    public ArrayBlockingQueue <Integer> getPlayersToCheck()
    {
        return playersToCheck;
    }
    
    public boolean getTerminate()
    {
        return terminate;
    }
    
}
