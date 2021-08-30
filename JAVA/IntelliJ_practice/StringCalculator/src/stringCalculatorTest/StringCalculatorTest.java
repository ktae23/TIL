import org.junit.Before;
import org.junit.Test;
import stringCalculator.StringCalculator;

import static org.junit.Assert.*;

public class StringCalculatorTest {

    StringCalculator stringCalculator;

    @Before
    public void setUp() throws Exception {
        stringCalculator = new StringCalculator();
        System.out.println("before");

    }

    @Test
    public void calculator() {
        assertEquals(6, stringCalculator.calculator("1,2,3"));
        assertEquals(10, stringCalculator.calculator("//-\n1-2-3-4"));
        assertEquals(15, stringCalculator.calculator("1;2;3;4;5"));
        assertEquals(15, stringCalculator.calculator(";;;"));
        assertEquals(15, stringCalculator.calculator(""));
        assertEquals(15, stringCalculator.calculator(null));
    }


}