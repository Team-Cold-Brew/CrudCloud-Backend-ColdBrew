# 💳 Módulo MercadoPago - CrudCloud

## 📋 Descripción

Este módulo implementa la pasarela de pagos integrada con MercadoPago para el sistema CrudCloud. Permite a los usuarios actualizar sus planes de suscripción mediante pagos seguros.

## 🏗️ Arquitectura del Módulo

Siguiendo el patrón de **monolito modular** del proyecto:

```
mercadoPago/
├── controller/
│   └── PaymentController.java          # Endpoints REST para pagos
├── service/
│   └── PaymentService.java             # Lógica de negocio de pagos
├── repository/
│   ├── TransactionRepository.java      # Repositorio de transacciones
│   ├── PaymentProviderRepository.java  # Repositorio de proveedores
│   └── CurrencyRepository.java         # Repositorio de monedas
├── model/
│   ├── Transaction.java                # Entidad de transacciones
│   ├── TransactionStatus.java          # Estados de transacciones
│   ├── PaymentProvider.java            # Entidad de proveedores
│   └── Currency.java                   # Entidad de monedas
├── dto/
│   ├── request/
│   │   └── CreatePreferenceRequest.java
│   └── response/
│       ├── PreferenceResponse.java
│       ├── TransactionResponse.java
│       └── WebhookResponse.java
└── config/
    └── MercadoPagoConfig.java          # Configuración del SDK
```

## 🚀 Funcionalidades Implementadas

### ✅ **Crear Preferencias de Pago**
- Endpoint: `POST /api/payments/create-preference`
- Crea preferencias de pago para upgrades de plan
- Integración completa con MercadoPago SDK

### ✅ **Procesar Webhooks**
- Endpoint: `POST /api/payments/webhook`
- Maneja notificaciones automáticas de MercadoPago
- Actualiza estados de transacciones automáticamente
- Upgrade automático de planes al aprobar pagos

### ✅ **Historial de Transacciones**
- Endpoint: `GET /api/payments/history?userId={id}`
- Consulta historial completo de pagos por usuario

### ✅ **Consulta de Transacciones**
- Endpoint: `GET /api/payments/transaction/{id}`
- Detalles específicos de una transacción

### ✅ **Estado de Pagos**
- Endpoint: `GET /api/payments/status/{id}`
- Consulta rápida del estado de un pago

## ⚙️ Configuración

### Variables de Entorno Requeridas

```properties
# MercadoPago Credentials
MERCADOPAGO_ACCESS_TOKEN=your_access_token_here
MERCADOPAGO_PUBLIC_KEY=your_public_key_here
MERCADOPAGO_WEBHOOK_SECRET=your_webhook_secret (opcional)
MERCADOPAGO_SANDBOX=true  # false para producción
```

### Configuración en application.properties

```properties
# MercadoPago Configuration
mercadopago.access-token=${MERCADOPAGO_ACCESS_TOKEN:YOUR_ACCESS_TOKEN_HERE}
mercadopago.public-key=${MERCADOPAGO_PUBLIC_KEY:YOUR_PUBLIC_KEY_HERE}
mercadopago.webhook-secret=${MERCADOPAGO_WEBHOOK_SECRET:}
mercadopago.sandbox=${MERCADOPAGO_SANDBOX:true}
```

## 🔄 Flujo de Pago

1. **Usuario solicita upgrade de plan**
   ```http
   POST /api/payments/create-preference
   {
     "planId": 2,
     "userId": 1,
     "amount": 19.99,
     "currency": "ARS",
     "description": "Upgrade to Standard Plan"
   }
   ```

2. **Sistema crea preferencia en MercadoPago**
   - Genera preferencia con MercadoPago SDK
   - Crea registro de transacción en BD
   - Retorna URLs de pago

3. **Usuario completa pago en MercadoPago**
   - Redirección a checkout de MercadoPago
   - Usuario completa el pago

4. **MercadoPago envía webhook**
   ```http
   POST /api/payments/webhook
   {
     "type": "payment",
     "data": { "id": "payment_id" }
   }
   ```

5. **Sistema procesa webhook**
   - Consulta estado del pago en MercadoPago
   - Actualiza transacción en BD
   - Upgrade automático del plan del usuario

## 🗄️ Modelo de Datos

### Transaction
- `transaction_id` (PK)
- `user_id` (FK → users)
- `provider_id` (FK → payment_providers)
- `currency_id` (FK → currency)
- `provider_transaction_id` (MercadoPago ID)
- `amount`, `status`, `payment_method`
- `created_at`, `updated_at`, `approval_date`

### TransactionStatus (Enum)
- `PENDING` - Pago iniciado
- `APPROVED` - Pago aprobado
- `REJECTED` - Pago rechazado
- `REFUNDED` - Pago reembolsado

## 🔧 Uso del Módulo

### Crear una Preferencia de Pago

```java
@Autowired
private PaymentService paymentService;

CreatePreferenceRequest request = new CreatePreferenceRequest();
request.setPlanId(2);
request.setUserId(1);
request.setAmount(new BigDecimal("19.99"));
request.setCurrency("ARS");

PreferenceResponse response = paymentService.createPreference(request);
// Redirigir usuario a response.getInitPoint()
```

### Consultar Historial

```java
List<TransactionResponse> history = paymentService.getTransactionHistory(userId);
```

## 🛡️ Manejo de Excepciones

Reutiliza las excepciones del módulo `auth`:
- `ResourceNotFoundException` - Usuario/Plan no encontrado
- `BadRequestException` - Error en MercadoPago o datos inválidos
- `ConflictException` - Conflictos de estado

## 🧪 Testing

### Datos de Prueba MercadoPago (Sandbox)

**Tarjetas de Prueba:**
- **Visa aprobada:** 4509 9535 6623 3704
- **Mastercard rechazada:** 5031 7557 3453 0604
- **Amex pendiente:** 3711 803032 57522

**Usuarios de Prueba:**
- Email: test_user_123@testuser.com
- Password: qatest123

## 📚 Dependencias

- **MercadoPago SDK:** `com.mercadopago:sdk-java:2.1.7`
- **Spring Boot:** Para inyección de dependencias
- **JPA/Hibernate:** Para persistencia
- **Lombok:** Para reducir boilerplate

## 🔗 Enlaces Útiles

- [Documentación MercadoPago](https://www.mercadopago.com.ar/developers)
- [SDK Java MercadoPago](https://github.com/mercadopago/sdk-java)
- [Testing en Sandbox](https://www.mercadopago.com.ar/developers/en/guides/testing)

---

**Implementado siguiendo el patrón arquitectural del proyecto CrudCloud** 🚀
