## Docker build error solved

도커 빌드 중 node를 업데이트 하는 부분에서 인증 오류가 발생

<br/>

## Error

```shell
Err:2 https://deb.nodesource.com/node_14.x bionic Release Certificate verification failed: The certificate is NOT trusted. The certificate chain uses expired certificate. Could not handshake: Error in the certificate verification. [IP: 23.43.165.25 443]
```

<br />

검색해 보니 실시간으로 이슈가 논의 되고 있었다.

이 중 인증 절차를 해제하지 않으면서 작동하도록 도움을 받은 답변을 가져왔다.

<br/>

## [chris-altamimi 's Answer](https://github.com/nodesource/distributions/issues/1266#issuecomment-931807686)

```dockerfile
### TEMPORARY WORKAROUND FOR ISSUE https://github.com/nodesource/distributions/issues/1266

RUN apt-get -y update || echo "This is expected to fail."

RUN apt-get install -y ca-certificates && apt-get update

RUN rm -f /usr/local/share/ca-certificates/certificate.crt
# --fresh is needed to remove symlinks to no-longer-present certificates
RUN update-ca-certificates --fresh

# 위는 추가한 코드 (인증서를 강제로 업데이트 함)
# 아래는 기존 코드 (에러 발생 부분)
# install the newest node 
RUN curl -sL https://deb.nodesource.com/setup_14.x | sudo -E bash - 
RUN sudo apt-get install -y nodejs
RUN sudo apt-get install -y build-essential
RUN node -v

RUN npm install express --save
RUN npm install express-generator -g
```

