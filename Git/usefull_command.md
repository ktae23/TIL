### 자주 사용하는 Git 명령어 요약



### 스테이지 관리

```shell
# 스테이징 상태 보기
git status

# 스테이지에 올리기
git add C:\Users\zz238\TIL\Git\git_command.md

# 스테이지에서 내리기
git reset HEAD C:\Users\zz238\TIL\Git\git_command.md

# 임시저장
git stash

# 임시 저장 확인
git stash list

# 임시 저장 파일 다시 반영
git stash apply -> 가장 최근거 1개 반영
or
git stash apply [stash 이름]

# 임시 저장 파일 스테이지에도 반영
git stash apply --index

# 임시 저장 파일 제거
git stash drop -> 가장 최근거 1개 반영
or
git stash drop [stash 이름]
```



### 커밋 관리

```shell
# 커밋하기
git commit

# 요약 커밋하기
git commit -m "커밋메시지"

# 커밋 이후에 스테이징 된 파일을 기존 커밋에 추가하기 
# (커밋 수정하기)
git commit --amend or git commit --amend -m "커밋메시지"

# 커밋 이력 전체 보기
git log

# 커밋 이력 2줄 보기
git log -2

# 커밋 이력간의 차이 보기
git log --patch or git log -p

# 커밋에서 어떤 파일이 얼마나 수정 또는 변경 되었는지 보여줌
git log --stat

# 커밋 한줄로 보기, 그래프로 보기, 포맷 정하기 등
git log --online
git log --graph
git log --oneline --graph
git log --pretty=format:"%h %s" --graph
```



### 원격 저장소 관리

```shell
# 원격 저장소 목록 보기
git remote

# 원격 저장소 추가
git remote add [원격저장소 이름] [원격저장소 주소]

# 원격 저장소 이름 변경
git remote rename [원래 이름] [새로운 이름]

# 원격 저장소 삭제
git remote rm [원격저장소 이름]

# 원격 저장소 브랜치 삭제
git push [원격저장소 이름] -d [브랜치 이름]

# 원격 저장소에서 변경사항 내려받기
git fetch [원격저장소 이름] [브랜치 이름]
(브랜치 이름 생략시 디폴트 브랜치)

# 원격 저장소에서 변경사항 내려받으면서 병합까지 하기
# git fetch + git merge
git pull [원격저장소 이름] [브랜치 이름]
(브랜치 이름 생략시 디폴트 브랜치)
```



### 브랜치 관리

```shell
# 브랜치 목록 보기
git branch

# 브랜치 생성
git branch [브랜치 이름]

# 브랜치 이름 변경
git branch -m [원래 이름] [새로운 이름]

# 브랜치 삭제
git branch rm [브랜치 이름]

# 브랜치 전환
git checkout [브랜치 이름]

# 브랜치 생성하면서 바로 전환
git checkout -b [브랜치 이름]

# 다른 브랜치 병합하기
git merge [브랜치 이름]
```

