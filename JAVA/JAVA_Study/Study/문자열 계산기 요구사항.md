

## 문자열 계산기 요구사항

### 전달하는 문자를 구분자로 분리한 후 각 숫자의 합을 구하여 반환

- 쉼표(,)  또는 콜론(;)을 구분자로 가지는 문자열을 전달하는 경우 구분자를 기준으로 분리한 각 숫자의 합을 반환한다.
- 커스텀 구분자를 지정 할 수 있다. 커스텀 구분자는 문자열 앞 부분의 "//"와 "\\n" 사이에 위치하는 문자를 커스텀 구분자로 사용한다. 예를 들어 "//;\n1;2;3"과 같이 값을 입력할 경우 커스텀 구분자는 세미콜론(;)이며, 결과 값은 6이 반환되어야 한다.
- 문자열 계산기에 음수를 전달하는 경우 RuntimeException으로 예외 처리 해야 한다.

<br/>

#### 문자열 계산기 클래스

```java
package stringCalculator;

public class StringCalculator {

    public int calculator(String input){
        int output = 0;
        String[] tmp;
        if(input == null || input.isEmpty()){
            System.out.println("결과 : " + output);
            return output;
        }
        if(input != null && !input.isEmpty()){
            if(input.startsWith("//")){
                input = input.replaceAll("//","").replaceAll("\n","");
                String separator = input.substring(0,1);
                System.out.println("구분자 : " + separator);
                tmp = input.split(separator);

                for(int i=1; i<tmp.length; i++){
                    output += Integer.parseInt(tmp[i]);
                }
            } else if( ":".equals(input.substring(1,2))){
                System.out.println("구분자 : :");
                tmp = input.split(":");
                for(int i=0; i<tmp.length; i++){
                    output += Integer.parseInt(tmp[i]);
                }

            }
            else if(",".equals(input.substring(1,2))){
                System.out.println("구분자 : ,");
                tmp = input.split(",");
                for(int i=0; i<tmp.length; i++){
                    output += Integer.parseInt(tmp[i]);
                }

            }
        }
        System.out.println("결과 : " + output);
        return output;
    }
}

```

<br/>

#### 문자열 계산기 테스트

```java
import org.junit.Before;
import org.junit.Test;
import stringCalculator.StringCalculator;

import static org.junit.Assert.assertEquals;

public class StringCalculatorTest {

    StringCalculator stringCalculator;

    @Before
    public void setUp() throws Exception {
        stringCalculator = new StringCalculator();
    }

    @Test
    public void calculator() {
        assertEquals(6, stringCalculator.calculator("1,2,3"));
        assertEquals(10, stringCalculator.calculator("//-\n1-2-3-4"));
        assertEquals(15, stringCalculator.calculator("1:2:3:4:5"));
        assertEquals(0, stringCalculator.calculator(":::"));
        assertEquals(0, stringCalculator.calculator(""));
        assertEquals(0, stringCalculator.calculator(null));
    }


}
```





#### 결과

![image-20210830202517544](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.08.30/image-20210830202517544.png)



일차적으로 코드를 작성하고 나니 굉장히 하드코딩 되었다는것을 느낄 수 있다.

입력 값이 조금만 변경되거나 요구 사항이 변경 될 경우 유연한 대처가 어렵다.



추가 요구사항과 힌트를 살펴 보자.



- 빈 문자열 또는 null 값을 입력할 경우 0을 반환해야 한다.

```java
if (input == null){}
if (input.isEmpty()){}	
```

- 숫자 하나를 문자열로 입력 할 경우 해당 숫자를 반환

```java
int number = INteger.parseInt(input);
```

- 숫자 두개를 쉼표(,) 구분자로 입력 할 경우 두 숫자의 합을 반환한다.

```java
String[] numbers = input.split(",");
```

- 쉼표 외에 콜론을 동시 사용 가능

```java
String[] tokens = input.split(",|:");
```

- "//"와 "\\n" 사이에 커스텀 구분자 지정 가능

```java
Matcher m = pattern.compile("//(.)\n(.*)").matcher(input);
if(m.find()){
    String customDelimeter = m.group(1);
    String[] tokens = m.group(2).split(customDelimeter);
}
```

- 음수를 전달 할 경우 RuntimeException 발생

```java
@Test(expected = RuntimeException.class)
public int stringCalculator(){
    ....
}
```





## 힌트를 이용하여 다시 작성한 코드

#### 문자열 계산기 클래스

```java
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

```

<br/>

#### 문자열 계산기 테스트

```java
import org.junit.Before;
import org.junit.Test;
import stringCalculator.StringCalculator;

import static org.junit.Assert.assertEquals;

public class StringCalculatorTest {

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
```



#### 결과

![image-20210830213020678](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.08.30/image-20210830213020678.png)



##### 추가 요구 사항(리팩토링)

- 메소드가 한 가지 책임만 가지도록 구현
- 인덴트 깊이를 1단계로 유지
- else 사용 금지



<다음에 이어서>
