package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.UserInterface;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * Matrix represent the player's tokens.
     */
    int [][] slotsWithTokens; // each row represent specific player tokens


    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        slotsWithTokens=new int [env.config.players] [env.config.featureSize];
        for (int[] subarr : slotsWithTokens) {
            Arrays.fill(subarr, -1);
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        cardToSlot[slotToCard[slot]] = -1;
        slotToCard[slot] = -1;
        env.ui.removeCard(slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        synchronized(slotsWithTokens)
        {
            env.ui.placeToken(player, slot); //userIterface update
            int cell=findFreeCellInMatrix(player); //find a free cell to insret the token
            slotsWithTokens[player][cell]=slot; //add the slot to the slotWithTokens matrix
        }

        env.logger.warning("Thread " + Thread.currentThread().getName() + " TABLE after placeToken: "+ slotsWithTokens[player][0]+", "+slotsWithTokens[player][1]+","+slotsWithTokens[player][2]);

    }

    /**
     * Removes a token of a player from a grid slot.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int slot) {
        env.ui.removeTokens(slot);
        // empty relevant cell in slotsWithTokens (delete non-existing tokens)
        boolean removed = false;
        synchronized(slotsWithTokens) {
            for (int pId = 0; pId < slotsWithTokens.length; pId++) {
                for (int s = 0; s < slotsWithTokens[pId].length; s++) {
                    if(slotsWithTokens[pId][s] == slot) {
                        slotsWithTokens[pId][s] = -1;
                        removed = true;
                    }
                }
            }
        }
        return removed;
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeTokenByPlayer(int player, int slot) {
        env.ui.removeToken(player, slot);
        int slotIdx;
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " trying to obatin access to 1-d array.");
        synchronized(slotsWithTokens[player]) {
            slotIdx = indexOf(slotsWithTokens[player], slot);
            if(slotIdx < 0) {
                return false;
            }
            slotsWithTokens[player][slotIdx] = -1;
            return true;
        }
    }

    /**
     * Finds the index of the given value in the array. This method returns (-1) for not found.
     */
    private int indexOf(int[] arr, int value)
    {
        int index = -1;
        for (int i = 0; i < arr.length; i++)
        {
            if(arr[i] == value) {
                index = i;
                return index;
            }
        }
        return index;
    }

    public boolean thirdChoiceWasMade (int playerId)
    {
        synchronized(slotsWithTokens[playerId])
        {
            for(int i=0;i<slotsWithTokens[playerId].length;i++)
            {
                if(slotsWithTokens[playerId][i]==-1) //not a token
                    return false;
            }
        }
        return true;
    }

    public List<Integer> findPlayerWithToken(int slot)
    {
        List<Integer> playersWithToken = new ArrayList<Integer>();
        for(int i=0;i<slotsWithTokens.length;i++)
        {
            for(int  j=0;j<slotsWithTokens[i].length;j++)
            {
                if(slotsWithTokens[i][j]==slot)
                    playersWithToken.add(i);
            }
        }
        return playersWithToken;
    }

    public int findFreeCellInMatrix(int player)
    {
        synchronized(slotsWithTokens[player])
        {
            for(int i=0; i<slotsWithTokens[player].length;i++)
            {
                if(slotsWithTokens[player][i]==-1)
                    return i;
            }
        }
        return -1; // Shouldn't get here
    }

    public int [][] getSlotsWithToken()
    {
        return slotsWithTokens;
    }
}
