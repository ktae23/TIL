## USB/SD카드 포맷 오류 해결

- CMD - `diskpart` 입력 후 엔터를 누르면 Diskpart 프롬프트가 실행 된다.



```shell

Microsoft DiskPart 버전 10.0.19041.610

Copyright (C) Microsoft Corporation.
컴퓨터: [일렬-번호]

DISKPART> list disk

  디스크 ###  상태           크기     사용 가능     Dyn  Gpt
  ----------  -------------  -------  ------------  ---  ---
  디스크 0    온라인        465 GB           0 B
  디스크 1    온라인         14 GB           0 B

DISKPART> select disk 1

1 디스크가 선택한 디스크입니다.

DISKPART> list partition

  파티션 ###  종류              크기     오프셋
  ----------  ----------------  -------  -------
* 파티션 1    주                   14 GB      0 B

DISKPART> create partition primary

사용 가능한 범위가 없습니다. 사용할 수 있는 공간이 부족하여
지정한 크기와 오프셋으로 파티션을 만들 수 없습니다. 다른
크기와 오프셋 값을 지정하거나 아무 것도 지정하지 않고
최대 크기 파티션을 만드십시오. 디스크가 MBR 디스크 분할
포맷으로 분할되고, 디스크에 기본 파티션 4개(파티션을 더 이상
만들 수 없음) 또는 기본 파티션 3개와 확장 파티션 1개(논리
드라이브만 만들 수 있음)가 포함될 수 있습니다.
```

<br/>

- 파티션은 있으나 오프셋이 없기 때문에 파티션 생성은 안된다.
- clean 명령어로 파티션을 지워주자.

<br/>

```shell

DISKPART> clean

DiskPart에서 디스크를 정리했습니다.

DISKPART> create partition primary

DiskPart에서 지정한 파티션을 만들었습니다.

DISKPART> list partition

  파티션 ###  종류              크기     오프셋
  ----------  ----------------  -------  -------
* 파티션 1    주                   14 GB  1024 KB

```

<br/>

- 파티션이 정상적으로 생성 되었다.

```shell
DISKPART> select partition 1

1 파티션이 선택한 파티션입니다.

DISKPART> active

DiskPart에서 현재 파티션을 활성으로 표시했습니다.

DISKPART> format fs=fat32 quick

    0 퍼센트 완료

Diskpart에서 다음 오류가 발생했습니다. 매개 변수가 틀립니다.
자세한 내용은 시스템 이벤트 로그를 참조하십시오.

# (생략)

DISKPART> format fs=ntfs quick

    0 퍼센트 완료

Diskpart에서 다음 오류가 발생했습니다. 매개 변수가 틀립니다.
자세한 내용은 시스템 이벤트 로그를 참조하십시오.
```

- 위 뒤개의 포맷 방식으로도 모두 안된다.
- 그럼 남은 하나로 해보면 된다.

```shell

DISKPART> list disk

  디스크 ###  상태           크기     사용 가능     Dyn  Gpt
  ----------  -------------  -------  ------------  ---  ---
  디스크 0    온라인        465 GB           0 B
  디스크 1    온라인         14 GB           0 B

DISKPART> select disk 1

1 디스크가 선택한 디스크입니다.

DISKPART> list partition

  파티션 ###  종류              크기     오프셋
  ----------  ----------------  -------  -------
  파티션 1    주                   14 GB  1024 KB

DISKPART> select partition 1

1 파티션이 선택한 파티션입니다.

DISKPART> active

DiskPart에서 현재 파티션을 활성으로 표시했습니다.

DISKPART> format fs=exfat quick

  100 퍼센트 완료

DiskPart가 볼륨을 성공적으로 포맷했습니다.

DISKPART>
```

<br/>

됐다. 이클립스랑 인텔리제이에서 에러 잡는데 이골이 나서 이젠 뭐 안되면 그냥 다 구글링으로 검색해서 해결 한다.

이전의 나였다면 SD카드 안된다며 다른 SD카드를 찾았을 거다.

프로그래밍을 배운다는건 삶의 질이 정말 많이 올라가는 것 같다.

(반대급부로 떨어지는 파트도 있긴 하지만..ㅎ)

<br/>

이후 다시 포맷을 해주면 되는데 반드시 **빠른 포맷을 해제**해야 한다.

빠른 포맷 설정된채로 포맷 눌렀다가 위 작업을 한번 더 반복했다...ㅎ

<br/>

## 명령어 정리

#### 1. list disk

#### 2. select disk 번호

- 포맷하려는 디스크 번호

#### 3. clean

#### 4. create partition primary

#### 5. list partition

#### 6. select partition 번호

#### 7. active

#### 8. format fs=포맷형식 quick

- 포맷형식 종류 
  - fat32
  - exfat
  - ntfs

