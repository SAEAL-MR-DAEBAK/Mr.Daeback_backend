# Mr.Daeback_backend

## 🛠 기술 스택
- Language: Java 17
- Framework: Spring Boot
- Database: PostgreSQL
- Infra: AWS EC2, RDS
- Auth: JWT


## 📁 프로젝트 구조


```
📂 main
┣ 📂 java
┃ ┗ 📂 com.saeal.MrDaebackService
┃ ┣ 📂 auth — 로그인, 인증, JWT 토큰 처리 등
┃ ┣ 📂 cart — 결제 이전 장바구니 기능
┃ ┣ 📂 config — SecurityFilterChain·PasswordEncoder 빈, Swagger(OpenAPI) 설정
┃ ┣ 📂 dinner — 디너 관련 도메인
┃ ┣ 📂 jwt — JWT 토큰 발급·검증 및 Authentication 변환
┃ ┣ 📂 menuItems — 단품 메뉴 재고 관리
┃ ┣ 📂 order — 결제 이후 주문 정보 관리
┃ ┣ 📂 product — 상품 생성 및 상품별 기본 메뉴 세팅
┃ ┣ 📂 security — JWT 필터, UserDetails, 인증/인가 예외 핸들러
┃ ┣ 📂 servingStyle — 서빙스타일 관련 도메인
┃ ┣ 📂 user — 사용자 도메인 (유저 정보, 통계, 프로필 등)
┗ 📂 resources — 설정 파일 (e.g. application.yml) 및 정적 자원
