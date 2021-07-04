## git push/pull takes forever (무한 로딩) 해결

git을 새로 설치한 뒤 pull 또는 push 등 명령어가 먹히지 않고 시간만 축내는 경우가 발생한다.

이 경우 `git push forever` 라는 검색어로 구글링을 하여 아래 스택오버 플로우에 나오는 해결 방법 중 마지막 방법으로 해결했다.

<br />

[스택오버플로우 깃 무한 로딩](https://stackoverflow.com/questions/15175715/git-push-takes-forever/67979404#67979404?newreg=721956b7f16c4a0997ad5d1d64342b71)

<br />

이는 2단계 인증 방법을 선택하지 않아서 진행되지 않았던 문제였고, git bash나 saurce tree에서는 나오지 않고 git cmd에서만 나오는 이상한..;; 상황이었다.

<br />

git push를 했을 때 git cmd에서만 아래와 같은 2단계 인증 방법 선택 문구가 나왔고, 원하는 옵션의 숫자를 넣고 엔터를 누르면 인증이 진행 된 후 정상적으로 git 명령어를 이용 할 수 있다.

<br />

![img](https://github.com/ktae23/TIL/blob/master/Problem_Solve/imgs/20210704194712.png)

