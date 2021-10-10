



## 문자열 계산기 요구사항

### 전달하는 문자를 구분자로 분리한 후 각 숫자의 합을 구하여 반환

- 쉼표(,)  또는 콜론(;)을 구분자로 가지는 문자열을 전달하는 경우 구분자를 기준으로 분리한 각 숫자의 합을 반환한다.
- 커스텀 구분자를 지정 할 수 있다. 커스텀 구분자는 문자열 앞 부분의 "//"와 "\\n" 사이에 위치하는 문자를 커스텀 구분자로 사용한다. 예를 들어 "//;\n1;2;3"과 같이 값을 입력할 경우 커스텀 구분자는 세미콜론(;)이며, 결과 값은 6이 반환되어야 한다.
- 문자열 계산기에 음수를 전달하는 경우 RuntimeException으로 예외 처리 해야 한다.

<br/>



##### 추가 요구 사항(리팩토링)

- 메소드가 한 가지 책임만 가지도록 구현
- 인덴트 깊이를 1단계로 유지
- else 사용 금지



#### 문자열 계산기 클래스

```
package stringCalculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCalculator2 {

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
        if(input.contains(":") || input.contains(",")) {
            System.out.println("구분자 : , or :");
            output = sum(input.split(":|,"));
        }
        return output;
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
```



#### 문자열 계산기 테스트

```
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
        assertEquals(0, stringCalculator.calculator("\"2:-1:3\""));
    }
}
```



##### 결과

![20210901075722](C:\Users\zz238\til\JAVA\JAVA_Study\Study\imgs\2021.08.30\20210901075722.png)





### 책에 나온 예제를 참고하여 리팩토링을 한번 더 해보자



#### 문자열 계산기 클래스

```
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

```



#### 문자열 계산기 테스트

```
import org.junit.Before;
import org.junit.Test;
import stringCalculator.StringCalculator;

import static org.junit.Assert.assertEquals;

public class StringCalculatorTest3 {

    StringCalculator stringCalculator;

    @Before
    public void setUp() throws Exception {
        stringCalculator = new StringCalculator();
    }

    @Test(expected = RuntimeException.class)
    public void calculator() {
        assertEquals(6, stringCalculator.calculator("1,   2,3"));
        assertEquals(10, stringCalculator.calculator("//-\n1-2-3-4"));
        assertEquals(10, stringCalculator.calculator("//;\n1;2 ;3; 4"));
        assertEquals(15, stringCalculator.calculator("1:2:3:4:5:6:7"));
        assertEquals(15, stringCalculator.calculator("1:2,3:  4,5  :8"));
        assertEquals(0, stringCalculator.calculator(":,:,:"));
        assertEquals(0, stringCalculator.calculator("  "));
        assertEquals(0, stringCalculator.calculator(null));
        assertEquals(0, stringCalculator.calculator("2:-1:3"));
    }
}
```



##### 결과

![20210901081510](C:\Users\zz238\til\JAVA\JAVA_Study\Study\imgs\2021.08.30\20210901081510.png)

