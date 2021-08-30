Show subcategory list as per category select in dropdown select box use Thymeleaf, JQuery (or Javascript)



상위 카테고리에 따른 하위 카테고리 생성의 방법은 여러가지이다.



내 상황을 말하자면, 컨트롤러에서 넘겨주는 값에 상위, 하위 카테고리의 값들이 모두 하나의 카테고리 객체에 담겨 리스트로 넘겨지고 있었다.



```html
    <select  id="mainCategory" name="mainCategory">
        <option selected disabled="disabled">대분류 *</option>
    </select>
    <select id="subCategory" name="subCategory">
        <option selected disabled="disabled">소분류 *</option>
    </select>
```



```javascript
$(document).ready(function () {
    // 자바스크립트에서 타임리프 변수를 사용하기 위해 CData 태그를 주석처리한채로 사용한다.
        /*<![CDATA[*/
        let categoryTypeArray = new Array();
        let categoryTypeObject;
        let categoryNameArray = new Array();
        let categoryNameObject;
    
    // model에서 넘겨 받은 리스트를 하나씩 꺼내 View에서만 사용 할 객체를 만들어 리스트에 담아 준다.
    // 대분류 카테고리에는 대분류만 넣어 준다.
        /*[# th:each="ct : ${categoryList}"]*/
        mainCategoryObject = new Object();
            mainCategoryObject.mainCategory = /*[[${ct.mainCategory}]]*/
                    mainCategoryArray.push(mainCategoryObject);
        /*[/]*/

    // 소분류 카테고리에는 비교용 대분류와 소분류 값을 넣어 준다.
        /*[# th:each="ct : ${categoryList}"]*/
        subCategoryObject = new Object();
        subCategoryObject.subCategory = /*[[${ct.subCategory}]]*/
            subCategoryObject.mainCategory = /*[[${ct.mainCategory}]]*/
                    subCategoryArray.push(subCategoryObject);
        /*[/]*/

        let mainCategorySelectBox = $("select[name='mainCategory']");

        for (let i = 0; i < categoryTypeArray.length; i++) {
            categoryTypeSelectBox.append("<option value='" + mainCategoryArray[i].mainCategory
                                         + "'>" + mainCategoryArray[i].mainCategory + "</option>");
        }

        $(document).on("change", "select[name='mainCategory']", function () {

            let subCategorySelectBox = $("select[name='subCategory']");
            //기존 리스트 삭제
            subCategorySelectBox.children().remove(); 

            $("option:selected", this).each(function () {
                let selectValue = $(this).val(); //
                for (let i = 0; i < subCategoryArray.length; i++) {
                    // 대분류가 같은 소분류만 카테고리에 넣어 준다.           
                    if (selectValue == subCategoryArray[i].mainCategory) {

                        subCategorySelectBox.append("<option value='" + subCategoryArray[i].subCategory
                                                    + "'>" + subCategoryArray[i].subCategory + "</option>");

                    }
                }
            });

        });
    });


    /*]]>*/
```

경우에 따라 object에 id나 다른 값들을 넣어 더 풍부하게 만들 수 있다.

[참고 블로그](https://huskdoll.tistory.com/497)

[자바스크립트로 구현하기](https://www.studentstutorial.com/javascript/subcategory-list-show)

