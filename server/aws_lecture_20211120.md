###  코드 인프라를 위한 AWS 아키텍쳐 설계 및 운영 과정 5회차 (2021 - 11 - 20)

##### 클라우드 서비스 유형

- Packaged Software : 전체 소프트웨어 제작

- Infrastructure as a Service :  S3 / 이미지 호스팅

- Platform as a Service : EC2 / 코드 분석 및 배포 자동화, 코드만 올려서 사용하는 서비스

- Sorftware as a Service : 아이디만 받아서 바로 사용하는 구독 서비스



#!/bin/bash

yum install -y httpd

systemctl start httpd

systemctl enable httpd

echo "WEB01" > /var/www/html/index.html





### 코드 인프라를 위한 AWS 아키텍쳐 설계 및 운영 과정 5회차 (2021 - 11 - 27)

가비아 도메인 구매

AWS - Route53 - 호스팅 영역 - [호스팅 영역 생성] - 퍼블릭 라우팅 생성

ns-521.awsdns-01.net

ns-1455.awsdns-53.org

ns-406.awsdns-50.com

ns-1829.awsdns-36.co.uk



=> 가비아 - 도메인 - 네임 서버 설정 입력

12시간 내에 삭제하지 않으면 한달치 과금 0.5달러



제어판 - 네트워크 및 인터넷 - 네트워크 및 공유센터 - 어댑터 설정 변경 - 속성 - 인터넷 프로토콜 버전 4(TCP/IPv4) 속성 - 속성 -  다음 DNS 서버 주소 사용

8.8.8.8 - 구글 / 168.126.63.1 KTDS



EC2 - VPC - 2a - 활성화

보안그룹 - 

SSH 22 위치 무관 0.0.0.0/::/0, 

HTTP 80 위치 무관  0.0.0.0/::/0, 

모든 ICMP-ipv4 80 위치 무관 0.0.0.0/::/0



Route53 - 레코드 생성 - 퍼블릭ip (3.34.200.212) 입력



lsblk - 블록 스토리지 리스트 명령어

볼륨 생성 - 볼륨 연결 - 마운트



df -h 디스크 사용량

mkfs -t ext4 /dev/xvdf 파일시스템을 ext4 포맷으로 생성

mkdir /data

mnt /dev/xvdf /data

 sudo cp /etc/fstab /etc/fstab.orig 백업

sudo blkid 블록 id

 sudo vim /etc/fstab

UUID=131a92e1-307c-4b9b-af37-053719d9b69b     /           xfs    defaults,noatime  1   1
UUID=74f9aff3-0262-4558-9a73-1dcb2075d694       /data   ext4    defaults,nofail 0 2

리부팅 되더라도 마운트 유지 되도록 설정

