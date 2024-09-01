package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;


@ExtendWith(MockitoExtension.class)
class DealerTest {

    Dealer dealer;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    
    private Player[] players;
    @Mock
    private Logger logger;

    @Mock
    private Env env;
    @Mock
    private Config config;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;


    @BeforeEach
    void setUp() {

        config=new Config(logger, (String) null);
         env = new Env(logger, config, ui, util);
        Player[] players=new Player[1];
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        table = new Table(env, slotToCard, cardToSlot);
        dealer = new Dealer(env, table, players);
        players[0] = new Player(env, dealer, table, 0, 0 < env.config.humanPlayers);
    }

    private void fillAllSlots() {
        for (int i = 0; i < slotToCard.length; ++i) {
            slotToCard[i] = i;
        }
    }

    @Test
    void terminate()
    {
        boolean expectedTerminate=!(dealer.getTerminate());
        dealer.terminate();
        assertEquals(expectedTerminate,dealer.getTerminate());
    }

    @Test
    void slotsToCards()
    {
        fillAllSlots();
        int[] numbers=new int [env.config.rows*env.config.columns];
        for(int i=0; i<numbers.length;i++)
        {
            numbers[i]=i;
        }
        assertArrayEquals(numbers,dealer.slotsToCards(numbers));
    }
}