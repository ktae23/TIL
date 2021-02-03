# Git 협업

- 코드를 협업하는 방식
- 3가지 협업모델



## 협업의 전제

- 협업에서 가장 중요한 것 : 소통
- 협업은 **독재**
- 수직적 협업



## (1) Push & Pull

파일 생성 오가기, 끝말잇기 예제를 통해 연습

### 장점

- 단순함, 기본적인 git 활용만으로 협업 가능



### 단점

- 꼭 공유를 해야지만 협업이 가능
- synchronous한 협업 모델(한 사람의 업이 끝나야 다른 사람이 시작 가능)

- 2인 이상일 때 코드가 꼬일 가능성이 있음

```shell
mkdir practice
cd practice
git init
git remote add origin https://github.com/ktae23/practice
touch a.txt
git add a.txt
git commit -m "Add a.txt"
git push origin master
```

```shell
mkdir practice
cd practice
git init
git clone https://github.com/ktae23/practice //클론하면 .git 폴더 자동 생성
touch b.txt
git add b.txt
git commit -m "Add b.txt"
git push origin master
```

```shell
git pull https://github.com/ktae23/practice
touch c.txt
git add c.txt
git commit -m "Add c.txt"
git push origin master
```

두개의 깃 폴더를 이용해 z.txt까지 반복



## (2) Fork & PR

전공 입력하기 예제를 통해 연습

```shell
이름 : 
이름 : 
이름 : 
이름 : 
이름 : 
```

```shell
//해당 폴더 Fork 해오기
git clone https://github.com/ktae23/fork-pr
//내용 수정
git add README.md
git commit -m "Add my major"
git push origin master
//Pull requests 하기
```



- Asynchronous(비동기적) 협업이 가능함



## (3) Branch & PR
