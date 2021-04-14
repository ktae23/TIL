ssh 원격 연결 도구 사용 필요 - putty

이를 container 단의로 가상화 하여 조금 더 간편하게 하는 것이 docker hub

AWS - IaaS

Docker - PaaS

Git - SaaS



docker hub 직접 접속

docker 설치

<br/>

#### 서버 시간대 변경

```shell
1)	date

2)	sudo rm /etc/localtime

3)	sudo ln -s -f   /usr/share/zoneinfo/Asia/Seoul /etc/localtime
```

<br/>

#### 호스트 이름 변경

```shell
1)	hostname

2)	sudo vi /etc/cloud/cloud.cfg에서 preserve_hostname: true로 변경

3)	sudo vi /etc/hosts에서 127.0.0.1 [호스트 이름]으로 변경

4)	sudo vi /etc/hostname에서 ip-로 시작하는 이름을 [호스트 이름]으로 바꾼 뒤

5)	sudo reboot

6)	hostname 해보면 [호스트 이름] 나옴
```

<br/>

#### 우분투에 도커 설치

```shell
1)  sudo apt update
 
2)  sudo apt install apt-transport-https ca-certificates curl software-properties-common
 
3)  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
 
4)  sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu bionic stable"
 
5)  sudo apt update
 
6)  apt-cache policy docker-ce
 
7)  sudo apt install docker-ce
 
8)  docker -v

```

<br/>

#### 우분투에 Mysql 5.37 설치

```shell
# 도커에 mysql 5.7 설치, 가동
1)	[호스트 이름]> sudo docker run -d -p 3306:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=true --name mysql mysql:5.7
  
# mysql이 돌고 있는 컨테이너(ex Debian9)에 들어가서 bash를 실행하기
2)	[호스트 이름]> sudo docker exec -it mysql /bin/bash

# mysql 클라이언트를 실행하기
3)	[호스트 이름]> mysql -uroot

# mysql 명령해보기
4)	[호스트 이름]> show databases;

# mysql client 빠져 나오기
5)	[호스트 이름]> quit;

# 컨테이너 나오기
6)	[호스트 이름]> exit

# 현재 머신 확인
7)	[호스트 이름]> cat /etc/issue

# 도커에서 작동중인 컨테이너 확인
8)	[호스트 이름]> sudo docker ps

# 도커 mysql client로 바로 들어가기
9)	[호스트 이름]> sudo docker exec –it mysql mysql -uroot

# DB와 테이블 생성 테스트
10)	[호스트 이름]> create database test;
	[호스트 이름]> use test;
	[호스트 이름]> create table member( no int primary key  auto_increment, name varchar(30), id varchar(10), pw varchar(10));
	[호스트 이름]> insert into member(name,id,pw) values('pkt','secure','block');
	[호스트 이름]> select * from member;
	[호스트 이름]> quit;

# 컨테이너 종료 했다가 다시 작동하기
11)	[호스트 이름]> sudo docker ps
	[호스트 이름]> sudo docker stop mysql
	[호스트 이름]> sudo docker ps
	[호스트 이름]> sudo docker start mysql
	[호스트 이름]> sudo docker ps
	[호스트 이름]> sudo docker exec –it mysql mysql –uroot
	[호스트 이름]> show databases;
(이전에 했던 작업이 저장되어 있는 것을 확인한다)
	[호스트 이름]> quit;
```

