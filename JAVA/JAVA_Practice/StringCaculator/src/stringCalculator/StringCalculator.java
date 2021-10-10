package stringCalculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator {

    public static void main(String[] args) {
        StringCalculator stringCalculator = new StringCalculator();

        stringCalculator.calculator("1,2,3");
        stringCalculator.calculator("//-\n1-2-3-4");
        stringCalculator.calculator("//:\n1:2:3:4");
        stringCalculator.calculator("1:2:3:4:5");
        stringCalculator.calculator("1:2,3:  4,5  ");
        stringCalculator.calculator(":::");
        stringCalculator.calculator("  ");
        stringCalculator.calculator(null);
        stringCalculator.calculator("-1");
    }

    public int calculator(String input) {
        int output = 0;
        String[] tmp;

        if(input == null){
            System.out.println("결과 : " + output);
            return output;
        }
        input = input.replaceAll(" ","");
        if ("".equals(input) || input.length() < 1 || input.isEmpty()) {
            System.out.println("결과 : " + output);
            return output;
        }


        if (input.length() == 1) {
            output = isPositive(input);
            System.out.println("결과 : " + output);
            return output;
        }

        Matcher match = Pattern.compile("//(.)\n(.*)").matcher(input);
        if (match.find()) {
            String delimeter = match.group(1);
            System.out.println("구분자 : " + delimeter);
            tmp = match.group(2).split(delimeter);

            for (int i = 0; i < tmp.length; i++) {
                output += isPositive(tmp[i]);
            }
            System.out.println("결과 : " + output);
            return output;
        }

        System.out.println("구분자 : : | ,");
        tmp = input.split(":|,");
        for (int i = 0; i < tmp.length; i++) {
            output += isPositive(tmp[i]);
        }

        System.out.println("결과 : " + output);
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
