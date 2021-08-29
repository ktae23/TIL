## Authorization (권한)

- 기본적으로 권한은 인증이 모두 확인된 뒤에 체크할 수 있기 때문에 필터의 가장 마지막에 있는 FilterSecurityInterceptor 를 통해 체크한다.
- 하지만 모든 권한을 여기에서 체크할 수 있는 것은 아니다. Controller나 Service 안에서도 문맥에 따라 권한을 처리할 수 있고 AccessDeniedException을 발생시킬 수 있다.
- AccessDecisionManager 가 접근 가능 여부에 대한 Decesion(판결)을 내릴 수 있는 Voter 들을 모아서 판결을 내리는 구조이다.
