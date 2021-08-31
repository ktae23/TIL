package stringCalculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator2 {

    public static void main(String[] args) {
        StringCalculator2 stringCalculator = new StringCalculator2();

        stringCalculator.calculator("1,2,3");
        stringCalculator.calculator("//-\n1-2-3-4");
        stringCalculator.calculator("//;\n1;2;3;4");
        stringCalculator.calculator("1:2:3:4:5");
        stringCalculator.calculator("1:2,3:  4,5  ");
        stringCalculator.calculator(":::");
        stringCalculator.calculator("  ");
        stringCalculator.calculator(null);
        stringCalculator.calculator("-1");
    }

    public int calculator(String input) {
        int output = 0;
        String[] tmp = null;

        if (input == null || "".equals(input) || input.length() < 1 || input.isEmpty()) {
            return 0;
        }
        input = input.replaceAll(" ", "");

        if (input.length() == 1) {
            return isPositive(input);
        }

        output = defaultCalculator(input);

        if(output == 0){
            output = customCalculator(input);
        }

        System.out.println("결과 : " + output);
        return output;
    }

    private int defaultCalculator(String input) {
        int output = 0;
        System.out.println("구분자 : ': | ,'");
        output = sum(input.split(":|,"));
        return output;
    }

    private int customCalculator (String input) {
        int output = 0;
        Matcher match = Pattern.compile("//(.)\n(.*)").matcher(input);
        System.out.println(match.toString());
        if (match.find()) {
            String delimeter = match.group(1);
            System.out.println("구분자 : " + delimeter);

            output = sum(match.group(2).split(delimeter));
            return output;
        }
        return 0;
    }

    private int sum(String[] tmp){
        int output = 0;
        for (int i = 0; i < tmp.length; i++) {
            output += isPositive(tmp[i]);
        }
        return output;
    }

    private int isPositive(String input){

        int number = Integer.parseInt(input);
        if(number < 0){
            throw new RuntimeException();
        }
        return number;
    }
}
