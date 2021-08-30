package stringCalculator;

public class StringCalculator {

    public static void main(String[] args) {
        StringCalculator calculator = new StringCalculator();
        calculator.calculator("1,2,3");
        calculator.calculator("//-\n1-2-3-4");
        calculator.calculator("1;2;3;4;5");
        calculator.calculator(";;;");
        calculator.calculator("");
        calculator.calculator(null);

    }

    public int calculator(String input){
        int output = 0;
        String[] tmp;
        if(input != null && !input.isEmpty()){
            System.out.println(input.substring(1,2));
            if(input.startsWith("//")){
                input = input.replaceAll("//","").replaceAll("\n","");
                String separator = input.substring(0,1);
                tmp = input.split(separator);

                for(int i=1; i<tmp.length; i++){
                    output += Integer.parseInt(tmp[i]);
                }
            } else if( ";".equals(input.substring(1,2))){
                tmp = input.split(";");
                for(int i=0; i<tmp.length; i++){
                    output += Integer.parseInt(tmp[i]);
                }

            }
            else if(",".equals(input.substring(1,2))){
                tmp = input.split(",");
                for(int i=0; i<tmp.length; i++){
                    output += Integer.parseInt(tmp[i]);
                }

            }
        }
        if(input == null || input.isEmpty()){
            System.out.println("결과 : null");
            return output;
        }
        System.out.println("결과 : " + output);
        return output;
    }
}
