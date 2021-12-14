리눅스 + 도커 기반 서버 관리 시 정말 자주 사용하는 명령어 모음



도커 명령어

```shell
** 컨테이너 / 이미지 목록 확인
docker ps	// 실행 중인 컨테이너 목록
docker ps -a	// 종료된 컨테이너 포함한 목록
docker images	// 이미지 목록

** 컨테이너 제거
docker rm [container id] | [container name]	// 컨테이너 제거
docker rm -f db_container // 실행 중일 경우 강제 종료 옵션
예시 : docker rm 2e1531kw2a
예시 : docker rm db_container

** 이미지 제거
docker rmi [image id] | [imgae name]	// 이미지 제거
docker rmi -f db_container // 실행 중일 경우 강제 종료 옵션
예시 : docker rmi 2e1531kw2a
예시 : docker rmi mysql

** 네트워크 생성 및 제거
docker network ls	// 네트워크 목록
docker network create [network name]	// 네트워크 생성
docker network rm [network name]	// 네트워크 제거

** 컨테이너 생성
docker build -t [container name] [dockerfile path]
예시 : docker build -t db_contianer .
-> 이미지를 빌드하는데 태그(컨테이너 이름)를 db_container로 해라
-> 도커 파일은 현재 디렉토리 경로에 있다.

** 컨테이너 실행 / 정지
docker run [container name]	// 컨테이너 생성 + 실행
docker start [container id] | [container name]	// 컨테이너 실행
docker stop [container id] | [container name]	// 컨테이너 정지

** 도커 허브에서 이미지 받기
docker pull [docker hub image name] // 태그 생략 시 latest
docker pull [docker hub image name]:[tag]

** 컨테이너 정보 보기
docker inspect [container id] | [container name]	// 컨테이너 정보 출력
docker logs [container id] | [container name]	// 컨테이너 로그 출력

** 실행중인 컨테이너 들어가기
dockar attach [container id] | [container name]	// 실행 중인 컨테이너에 붙기
-> 이 경우 실시간 확인이 가능하지만 실행 중인 컨테이너를 꺼버릴 위험이 많음
-> 대신 exec -it [container id] || [container name] bash 로 접속하는 걸 권장
예시 :  docker exec -it db_container bash
```



도커 옵션

```shell
-d	// 백그라운드 실행
-p	80:80	// 호스트 80포트를 컨테이너 80포트와 연결 (포워딩)
-v c:/Users/user:home/ubuntu	//호스트 디렉토리를 컨테이너 디렉토리와 연결 (마운트)
-e 컨테이너 내에서 사용하는 환경 변수 설정
--name db_container	// db_container라고 컨테이너 이름 설정
-i	// 상호 입출력 연결 설정
-t // tty 사용 설정 -> 터미널 사용
* 보통 -it 로 같이 사용 함
```





리눅스 명령어

```shell
sudo su	// 관리자 모드로 변경
exit	// 관리자 모드에서 일반 계정으로 나가기
ls -l	// 현재 디렉토리 내의 권한 + 목록
ls -al	// 현재 디렉토리 내의 숨김 파일을 포함한 권한 + 소유권 + 목록

** 소유권 설정
chown [소유권자]:[그룹식별자] [대상 폴더 | 파일] 소유권 변경
chown -R [소유권자]:[그룹식별자] [대상 폴더 | 파일]	// 하위 디렉토리까지
예시 : chown ubuntu:ubuntu test.conf
예시 : chown -R ubuntu:ubuntu test/

** 권한 설정
chmod [사용자][그룹][다른사용자] [대상 폴더 | 파일]
예시 : chmod 774 test.conf
0 : 권한 없음
1 : 실행 권한
2 : 쓰기 권한
3 : 실행 + 쓰기 (1 + 2)
4 : 읽기 권한 
5 : 실행 + 읽기 (1 + 4)
6 : 쓰기 + 읽기 (2 + 4)
7 : 전체 권한 (1 + 2 + 4)
```





