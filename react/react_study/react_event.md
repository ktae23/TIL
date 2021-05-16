### Event

현재 어떤 페이지인지 구분하기 위해 state에 mode라는 값을 줌

- 리액트에서는 Props나 State의 값이 변경 되면 해당 State를 가지고 있는 Component의 Render 함수가 다시 호출 됨
  - 이에 따라 Render 함수 하위의 Component의 Render 함수들도 모두 다시 호출하여 화면을 다시 그려 줌
- 이를 이용해 mode의 값에 따라 render를 다르게 호출 하도록 코드를 작성