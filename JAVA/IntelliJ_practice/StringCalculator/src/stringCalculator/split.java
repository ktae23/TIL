package stringCalculator;

public class split {


    public static void main(String[] args) {



        private String[] splitStr(String str) {
            String[] strArray = new String[str.length()];

            for (int i = 0; i < str.length(); i++) {
                if (i == result.length() - 1) {
                    strArray[i] = str.substring(i);
                    break;
                }
                strArray[i] = str.substring(i, i + 1);
            }
            return strArray;
        }


        System.out.println(splitStr("OXOXOXOXOX"));

    }


}