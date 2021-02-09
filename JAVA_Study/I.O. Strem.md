## I.O. Strem

### 외부 자원 사용 방법

1. 객체화 (Has a)
2. 상속 (Is a)
3. 외부 파일 인풋 받기

```java
public class FileReadTest {

	public static void main(String[] args) {
		try {
			FileInputStream finput = new FileInputStream("C:\\Users\\zz238\\Desktop\\교육\\멀티캠퍼스\\_temp_a.txt");
			int i = finput.read();
			System.out.println(i);
		
		
		} catch (FileNotFoundException e) {
			e.printStackTrace(); 
		} catch (IOException e) {
			e.printStackTrace(); 
		}
	}
}
```

>  `e.printStackTrace();` : 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용

> ```java
> catch (FileNotFoundException | IOException e) {
> 			e.printStackTrace();
> ```
>
> 위 처럼 멀티 캐치하는 방식은 좋지 않음, 나눠서 catch 권장