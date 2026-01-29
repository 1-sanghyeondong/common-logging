#### Spring Boot 3.2+ 기반 공통 로깅 라이브러리 (common-logging)

##### [라이브러리 한 줄 요약]
- **Spring Boot 3.x 호환**: Jakarta EE 기반의 최신 Spring Boot 3.2.5 환경을 완벽하게 지원
- **표준화된 로깅**: HTTP 요청/응답 상태, 페이로드(Body), 클라우드 환경 정보(Pod, Node 등)를 자동으로 수집하여 표준화된 로그를 생성
- **안전한 아키텍처**: 최신 Spring Boot의 엄격한 빈 생성 규칙을 준수하여 순환 참조 문제를 해결하고 조건부 로딩을 통해 필요한 환경에서만 활성화

---

#### 🚀 퀵 스타트

##### 1. Gradle 의존성 추가
Nexus 에서 라이브러리를 가져오기 위해 의존성을 추가합니다.
```kotlin
dependencies {
    implementation("com.common:common-logging:0.0.1")
}
```

##### 2. application.yml 설정
라이브러리 동작에 필요한 필수 및 선택 옵션을 설정합니다.
```yaml
app:
  id: my-service-name  # [필수] 서비스 식별자

status-logger:
  response-logging:
    enabled: false     # [선택] 응답 바디(ResponseBody) 로깅 활성화 여부 (기본값: false)
  content-caching:
    enabled: true      # [선택] ContentCachingWrappingFilter 활성화 여부 (기본값: true)
    ignore-path-patterns: # [선택] 캐싱 및 로깅 제외 경로
      - "/actuator/**"
      - "/health"
```

##### 3. 라이브러리 활성화
메인 애플리케이션 클래스 또는 설정 클래스에 `@EnableLogging` 어노테이션을 추가하여 기능을 활성화합니다.
```kotlin
import com.common.logging.annotations.EnableLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableLogging // 로깅 기능 활성화
@SpringBootApplication
class MyServiceApplication

fun main(args: Array<String>) {
    runApplication<MyServiceApplication>(*args)
}
```

---

#### 🛠 상세 기능

##### 1. @EnableLogging
- **역할**: `common-logging` 라이브러리의 모든 빈(Bean)과 설정을 로드하는 진입점입니다.
- **적용 위치**: `SpringBootApplication`이 선언된 메인 클래스나 `@Configuration` 클래스에 선언합니다. 이 어노테이션이 없으면 로깅 기능이 동작하지 않습니다.

##### 2. 로그 메시지 빌더 (StatusLogMessageBuilder)
기본으로 제공되는 `CommonStatusLogMessageBuilder`는 시스템 환경 변수에서 클라우드 인프라 정보를 자동으로 추출하여 로그에 포함시킵니다.

**환경 변수 매핑 정보:**
| 필드명 | 환경 변수 (System Env) | 설명 | 기본값 |
|---|---|---|---|
| node | `NODE_NAME` | K8s Node 이름 | - |
| pod | `HOSTNAME` | K8s Pod 이름 | - |
| cluster | `CLUSTER` | 클러스터 정보 | - |
| version | `VERSION` | 애플리케이션 버전 | - |
| pinpointAgentId | `PINPOINT_ID` | Pinpoint Agent ID | - |
| type | `APP_TYPE` | 애플리케이션 타입 | - |

##### 3. HTTP 요청/응답 캐싱 및 로깅 (ContentCachingWrappingFilter)
- **역할**: `HttpServletRequest`와 `HttpServletResponse`를 래핑(Wrapping)하여, 요청 본문(RequestBody)과 응답 본문(ResponseBody)을 **여러 번 읽을 수 있도록 캐싱**합니다. 이를 통해 인터셉터나 필터 단계에서 바디 내용을 소모하지 않고 안전하게 로깅할 수 있습니다.
- **제외 설정**: `status-logger.content-caching.ignore-path-patterns` 설정을 통해 불필요한 경로(예: 헬스 체크, 정적 리소스 등)는 캐싱에서 제외하여 성능 저하를 방지할 수 있습니다.

---

#### 📊 로그 출력 가이드 (Log Format)
라이브러리 적용 시, 다음과 같이 ELK 스택에서 분석하기 최적화된 표준 JSON 로그가 생성됩니다.

##### 1. 출력 예시
```json
{
  "@timestamp": "2026-01-29T12:15:22.123+09:00",
  "service": "your-service",
  "phase": "production",
  "method": "GET",
  "path": "/api/v1/products/SKU-9921?locationId=JAKARTA-01",
  "statusCode": 200,
  "execTimemillis": 42,
  "message": "req: GET /api/v1/products/SKU-9921?locationId=JAKARTA-01\nres: 200 42ms\nfrom: 10.0.12.45",
  "responseBody": "{\"id\":\"SKU-9921\",\"name\":\"Indomie Mi Goreng\",\"stock\":150,\"price\":3500.00}",
  "node": "gke-cluster-node-01",
  "pod": "your-service-7d8f9b",
  "version": "1.2.0"
}
```

```json
{
  "@timestamp": "2026-01-29T12:20:05.881+09:00",
  "service": "your-service",
  "phase": "prod",
  "method": "POST",
  "path": "/api/v1/payments/execute",
  "statusCode": 201,
  "execTimemillis": 358,
  "requestBody": "{\"orderId\":\"ORD-2026-001\",\"amount\":50000,\"currency\":\"IDR\",\"paymentMethod\":\"QRIS\"}",
  "responseBody": "{\"transactionId\":\"TXN-998877\",\"status\":\"SUCCESS\",\"timestamp\":\"2026-01-29T12:20:06Z\"}",
  "message": "req: POST /api/v1/payments/execute\nres: 201 358ms\nfrom: 182.253.12.3",
  "node": "gke-cluster-node-02",
  "pod": "your-service-bc12",
  "version": "1.0.1-SNAPSHOT"
}
```

##### 2. 로그 필드 상세 설명
| 필드명 | 설명 | 비고 |
|---|---|---|
| `@timestamp` | 로그 발생 시간 | ISO 8601 형식 |
| `service` | 애플리케이션 식별자 | app.id 설정값 |
| `phase` | 실행 환경 | spring.profiles.active 값 |
| `method` | HTTP Method | GET, POST, PUT 등 |
| `path` | 요청 경로 | Query String 포함 전체 경로 |
| `statusCode` | HTTP 응답 코드 | 200, 404, 500 등 |
| `execTimemillis` | 총 실행 시간 | 밀리초(ms) 단위 |
| `message` | 요약 메시지 | 사람이 읽기 쉬운 텍스트 로그 |
| `responseBody` | 응답 본문 데이터 | 활성화 시에만 출력 |