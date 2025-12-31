# Sistema de Assinaturas - Microserviços

Sistema de gestão de assinaturas desenvolvido em arquitetura de microserviços.

## Arquitetura

O sistema é composto por 5 microserviços:

- **user-service** (8081): Gestão de usuários - PostgreSQL
- **subscription-service** (8082): Gestão de assinaturas - MongoDB + Redis + Quartz + Kafka
- **payment-service** (8083): Processamento assíncrono de pagamentos - Kafka
- **api-gateway** (8080): Spring Cloud Gateway - Roteamento e load balancing
- **eureka-server** (8761): Service Discovery

## Como Executar

### Pré-requisitos

- Docker e Docker Compose
- Java 21
- Gradle (wrapper incluído)

### Docker Compose

```bash
docker-compose up -d
```

Aguarde alguns segundos e verifique os serviços:

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080/actuator/health
- **Swagger UI**:
  - User Service: http://localhost:8081/swagger-ui/index.html
  - Subscription Service: http://localhost:8082/swagger-ui/index.html
  - Payment Service: http://localhost:8083/swagger-ui/index.html
- **Grafana**: http://localhost:3000 (admin/admin)

## Tecnologias

- **Java 21** + **Spring Boot 3.5.x** + **Spring Cloud 2025.0.x**
- **PostgreSQL 16** - Banco relacional (user-service)
- **MongoDB 7** - Banco não-relacional (subscription-service)
- **Redis 7** - Cache e distributed locking
- **Apache Kafka** - Mensageria assíncrona
- **Quartz Scheduler** - Agendamento de tarefas
- **Eureka Server** - Service Discovery
- **Spring Cloud Gateway** - API Gateway
- **Prometheus** + **Grafana** - Métricas
- **Loki** + **Promtail** - Logs

## Comunicação entre Microserviços

### Síncrona (HTTP/REST)

**Spring Cloud OpenFeign** para comunicação síncrona:
- Subscription Service → User Service: validação de existência de usuário

**API Gateway** como ponto de entrada único:
- Todas as requisições passam pelo Gateway (porta 8080)
- Roteamento via Eureka Service Discovery
- Load balancing automático

### Assíncrona (Kafka)

**Tópicos:**
- `subscription-events`: Eventos de assinaturas
- `payment-events`: Eventos de pagamentos

**Fluxo de Eventos:**

1. **Subscription Service → Payment Service**:
   - `SUBSCRIPTION_CREATED`: Nova assinatura criada
   - `SUBSCRIPTION_RENEWAL_REQUESTED`: Renovação solicitada

2. **Payment Service → Subscription Service**:
   - `PAYMENT_PROCESSED_SUCCESS`: Pagamento aprovado
   - `PAYMENT_PROCESSED_FAILED`: Pagamento recusado

3. **Notificações**:
   - `SUBSCRIPTION_RENEWED`: Renovada com sucesso
   - `SUBSCRIPTION_CANCELLED`: Cancelada
   - `SUBSCRIPTION_SUSPENDED`: Suspensa após 3 falhas

### Service Discovery

Todos os microserviços se registram no **Eureka Server** para descoberta automática.

### Cache e Distributed Locking

**Redis** utilizado para:
- Cache de assinaturas ativas (TTL: 5 minutos)
- Distributed locking nos jobs do Quartz (Redisson)

## Regras de Negócio

### Criação de Assinatura

1. Valida se o usuário existe no `user-service`
2. Valida que o usuário não possui assinatura ativa ou em processamento
3. Cria assinatura com status `PROCESSING`
4. Publica evento `SUBSCRIPTION_CREATED` no Kafka
5. Payment Service processa o pagamento
6. Atualiza status: `ACTIVE` (sucesso) ou `FAILED` (falha)

### Planos

- **BASIC**: R$ 19,90/mês
- **PREMIUM**: R$ 39,90/mês
- **FAMILY**: R$ 59,90/mês

### Status da Assinatura

- **PROCESSING**: Aguardando confirmação de pagamento
- **ACTIVE**: Ativa e válida
- **CANCELLED**: Cancelada (acesso até `expirationDate`)
- **SUSPENDED**: Suspensa após 3 tentativas de renovação falhadas
- **FAILED**: Pagamento inicial falhou

### Renovação Automática

- **Agendamento**: Diariamente às 00:00
- **Processo**:
  1. Busca assinaturas ativas expiradas ou expirando hoje
  2. Adquire distributed lock (Redis)
  3. Publica evento `SUBSCRIPTION_RENEWAL_REQUESTED`
  4. Payment Service processa pagamento
  5. Se sucesso: atualiza datas e zera tentativas
  6. Se falha: incrementa tentativas (após 3 falhas, suspende)

### Limpeza de Assinaturas Pendentes

- **Agendamento**: A cada 8 horas
- Cancela assinaturas em `PROCESSING` há mais de 24 horas

### Processamento de Pagamentos

- Simulação com 80% de taxa de sucesso
- Processamento assíncrono via Kafka
- Retry com exponential backoff

## Endpoints da API

### User Service

Base URL: `http://localhost:8080/api/users`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/users` | Criar usuário |
| GET | `/api/users/{id}` | Buscar usuário por ID |
| GET | `/api/users` | Listar todos os usuários |
| PUT | `/api/users/{id}` | Atualizar usuário |
| DELETE | `/api/users/{id}` | Deletar usuário |

### Subscription Service

Base URL: `http://localhost:8080/api/subscriptions`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/subscriptions` | Criar assinatura |
| GET | `/api/subscriptions/{id}` | Buscar assinatura por ID |
| GET | `/api/subscriptions/user/{userId}` | Buscar assinatura ativa do usuário |
| GET | `/api/subscriptions` | Listar todas as assinaturas |
| POST | `/api/subscriptions/{id}/cancel` | Cancelar assinatura |

## Padrões e Boas Práticas

- **SOLID Principles**
- **DDD/Hexagonal Architecture**
- **Design Patterns**: Repository, Service Layer, Strategy
- **Event-Driven Architecture** (Kafka)
- **Cache-Aside Pattern** (Redis)
- **Distributed Locking** (Redisson)
- **Optimistic Locking** (MongoDB)
- **DTOs como Records** (Java Records)
- **Global Exception Handler**
- **MapStruct** para mapeamento

## Testes

```bash
./gradlew test
```
