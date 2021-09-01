package stringCalculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator3 {

    public static void main(String[] args) {
        StringCalculator3 stringCalculator = new StringCalculator3();

        stringCalculator.calculator("1,   2,3");
        stringCalculator.calculator("//-\n1-2-3-4");
        stringCalculator.calculator("//;\n1;2 ;3; 4");
        stringCalculator.calculator("1:2:3:4:5:6:7");
        stringCalculator.calculator("1:2,3:  4,5  :8");
        stringCalculator.calculator(":,:,:");
        stringCalculator.calculator("  ");
        stringCalculator.calculator(null);
        stringCalculator.calculator("2:-1:3");
    }

    public int calculator(String input) {


        

        if(input != null && !input.isEmpty()){
            input = input.replaceAll(" ", "");
        }

        if (input == null || "".equals(input)) {
            System.out.println("결과 : 입력 없음");
            return 0;
        }
        return defaultCalculator(input);
    }

    private int defaultCalculator(String input) {

        if (input.length() == 1) {
            return isPositive(input);
        }
        if(input.contains(":") || input.contains(",")) {
            System.out.println("구분자 : , or :");
            return sum(input.split(":|,"));
        }
        return customCalculator(input);
    }

    private int customCalculator (String input) {

        int output = 0;
        Matcher match = Pattern.compile("//(.)\n(.*)").matcher(input);
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
        System.out.println("결과 : " + output);
        return output;
    }

    private int isPositive(String input){

        int number = Integer.parseInt(input);
        if(number < 0){
            System.out.println("결과 : 음수 입력 됨");
            throw new RuntimeException();
        }
        return number;
    }
}
