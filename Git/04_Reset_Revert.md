# Reset & Revert

## 1. Reset

> 지정 커밋 순간 이후의 모든 커밋과 로그를 삭제하면서 복귀

```shell
git log
commit 76cefd80ec4daeaa8b8c267920939159bbb06a22
Author: Ktae23 <pktpkt8917@gmail.com>
Date:   Sun Feb 7 19:38:35 2021 +0900

    Move README.md Location

commit 570766acc7cf1293a7f0a7e5c8c56215466f0ff8
Author: Ktae23 <pktpkt8917@gmail.com>
Date:   Sun Feb 7 19:31:44 2021 +0900

    Add README.md

commit d6b09b8fe1bb330d783c5c01d7330b00a9b140da
Author: Ktae23 <pktpkt8917@gmail.com>
Date:   Sun Feb 7 12:45:32 2021 +0900

    Modify README

commit 5caf8ab5f466b2e860e14d86183b03f6d085a5c6
Author: Ktae23 <pktpkt8917@gmail.com>
Date:   Sun Feb 7 01:36:39 2021 +0900

    Add If Statement For Null
```

> commit 번호의 앞 여섯 자리를 복사한 뒤 아래 명령어에 입력

```shell
git reset d6b09b --hard
```

> 특정 시점 이후를 파괴하기 때문에 다시 이후 시점으로 복귀 불가



## 2.Revert

> 복귀 시점이 아닌 취소할 시점을 찾아 commit 번호 앞 여섯 자리 복사한 뒤 아래 명령어에 입력

```shell
git revert d6b096b
```

> 그러면 새로운 commit message를 입력할지 묻는 창이 뜨는데 그대로 저장하길 원하면 `:wq` 입력.

>로그를 살펴보면 복귀 시점 이후의 커밋들이 살아 있다.
>
>지금까지의 변화의 정 반대의 실행을 하여 상쇄했기 때문. 
>
>Reset을 이용하여 다시 원래 시점으로 돌아갈 수 있음.