import org.junit.Before;
import org.junit.Test;
import stringCalculator.StringCalculator;

import static org.junit.Assert.assertEquals;

public class StringCalculatorTest2 {

    StringCalculator stringCalculator;

    @Before
    public void setUp() throws Exception {
        stringCalculator = new StringCalculator();
    }

    @Test(expected = RuntimeException.class)
    public void calculator() {
        assertEquals(6, stringCalculator.calculator("1,2,3"));
        assertEquals(10, stringCalculator.calculator("//-\n1-2-3-4"));
        assertEquals(10, stringCalculator.calculator("//;\n1;2;3;4"));
        assertEquals(15, stringCalculator.calculator("1:2:3:4;5"));
        assertEquals(15, stringCalculator.calculator("1:2:3:  4,5  "));
        assertEquals(0, stringCalculator.calculator(":::"));
        assertEquals(0, stringCalculator.calculator("  "));
        assertEquals(0, stringCalculator.calculator(null));
        assertEquals(0, stringCalculator.calculator("-1"));
    }
}