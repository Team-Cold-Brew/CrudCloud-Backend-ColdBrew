# Módulo MercadoPago - Checkout Pro

Este módulo implementa la integración con MercadoPago utilizando Checkout Pro para procesar pagos y gestionar suscripciones en CrudCloud.

## 📋 Características

- **Checkout Pro**: Creación de preferencias de pago con la interfaz de MercadoPago
- **Gestión de Pagos**: Procesamiento y seguimiento de pagos
- **Suscripciones**: Gestión automática de suscripciones basadas en pagos aprobados
- **Webhooks**: Recepción y procesamiento de notificaciones de MercadoPago
- **Integración con Planes**: Vinculación con los planes de usuario del sistema

## 🏗️ Arquitectura

El módulo sigue la arquitectura del monolito modular de CrudCloud:

```
mercadoPago/
├── config/               # Configuración del SDK de MercadoPago
├── controller/           # Endpoints REST
├── dto/                  # DTOs de Request/Response
├── model/               # Entidades específicas del módulo
├── repository/          # Repositorios JPA
└── service/             # Lógica de negocio
```

### Modelos Compartidos (common/models)

- `Payment` - Registro de pagos realizados
- `PaymentStatus` - Estados de un pago
- `Subscription` - Suscripciones de usuarios
- `SubscriptionStatus` - Estados de suscripción

### Modelos del Módulo

- `PaymentPreference` - Preferencias de Checkout Pro creadas

## 🔧 Configuración

### application.properties

```properties
# Access Token de MercadoPago
mercadopago.access.token=TEST-xxxxx (para pruebas) o APP_USR-xxxxx (producción)

# Public Key (para frontend)
mercadopago.public.key=TEST-xxxxx o APP_USR-xxxxx

# URLs de redirección
mercadopago.notification.url=https://tu-dominio.com/api/webhooks/mercadopago
mercadopago.success.url=http://localhost:3000/payment/success
mercadopago.failure.url=http://localhost:3000/payment/failure
mercadopago.pending.url=http://localhost:3000/payment/pending
```

### Obtener Credenciales

1. Ingresa a [https://www.mercadopago.com.ar/developers](https://www.mercadopago.com.ar/developers)
2. Ve a "Tus integraciones" → "Credenciales"
3. Copia el **Access Token** y **Public Key**
4. Para pruebas, usa las credenciales de "Modo Sandbox"

## 🚀 Endpoints

### Pagos

#### Crear Checkout (Crear preferencia de pago)
```http
POST /api/payments/checkout
Content-Type: application/json

{
  "userId": 1,
  "planId": 2,
  "quantity": 1,
  "externalReference": "ORDER-12345"
}
```

**Response:**
```json
{
  "preferenceId": "123456789-abc123",
  "initPoint": "https://www.mercadopago.com/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com/checkout/v1/redirect?pref_id=...",
  "externalReference": "PLAN-2-USER-1-abc12345",
  "message": "Checkout preference created successfully"
}
```

#### Obtener Pago por ID
```http
GET /api/payments/{paymentId}
```

#### Obtener Pagos de Usuario
```http
GET /api/payments/user/{userId}
```

#### Obtener Pagos Aprobados de Usuario
```http
GET /api/payments/user/{userId}/approved
```

### Suscripciones

#### Obtener Suscripción por ID
```http
GET /api/subscriptions/{subscriptionId}
```

#### Obtener Suscripciones de Usuario
```http
GET /api/subscriptions/user/{userId}
```

#### Obtener Suscripción Activa de Usuario
```http
GET /api/subscriptions/user/{userId}/active
```

#### Cancelar Suscripción
```http
DELETE /api/subscriptions/{subscriptionId}
```

#### Verificar si Usuario tiene Suscripción Activa
```http
GET /api/subscriptions/user/{userId}/has-active
```

### Webhooks

#### Webhook de MercadoPago
```http
POST /api/webhooks/mercadopago
Content-Type: application/json

{
  "action": "payment.updated",
  "type": "payment",
  "data": {
    "id": "123456789"
  }
}
```

## 🔄 Flujo de Pago

1. **Cliente solicita checkout**
   - Frontend hace POST a `/api/payments/checkout`
   - Backend crea preferencia en MercadoPago
   - Devuelve `initPoint` para redireccionar al usuario

2. **Usuario paga en MercadoPago**
   - Usuario completa el pago en la interfaz de MercadoPago
   - MercadoPago procesa el pago

3. **Notificación vía Webhook**
   - MercadoPago envía notificación a `/api/webhooks/mercadopago`
   - Backend procesa el pago y actualiza estado
   - Si pago aprobado, crea/actualiza suscripción

4. **Redirección**
   - Usuario es redirigido a success/failure/pending URL

## 📊 Estados de Pago

- `PENDING` - Pago pendiente
- `APPROVED` - Pago aprobado
- `REJECTED` - Pago rechazado
- `CANCELLED` - Pago cancelado
- `REFUNDED` - Pago reembolsado
- `IN_PROCESS` - Pago en proceso
- `IN_MEDIATION` - Pago en mediación
- `CHARGED_BACK` - Contracargo

## 📊 Estados de Suscripción

- `PENDING` - Suscripción pendiente de activación
- `ACTIVE` - Suscripción activa
- `CANCELLED` - Suscripción cancelada
- `EXPIRED` - Suscripción expirada
- `SUSPENDED` - Suscripción suspendida

## 🧪 Testing

### Usando Postman

1. Importa la colección desde `docs/CrudCloud-MercadoPago.postman_collection.json`
2. Configura las variables de entorno
3. Ejecuta los requests

### Tarjetas de Prueba (Sandbox)

**Tarjeta Aprobada:**
- Número: `5031 7557 3453 0604`
- Código de seguridad: 123
- Fecha de vencimiento: cualquier fecha futura

**Tarjeta Rechazada:**
- Número: `5031 4332 1540 6351`

Ver más tarjetas de prueba en: [MercadoPago Test Cards](https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/additional-content/test-cards)

## 🔐 Seguridad

- Las credenciales de MercadoPago deben mantenerse en variables de entorno
- El webhook debe validar que las notificaciones provengan de MercadoPago
- No exponer el Access Token en el frontend (solo Public Key)

## 📝 Notas Importantes

1. **External Reference**: Se genera automáticamente con formato `PLAN-{planId}-USER-{userId}-{uuid}` para identificar pagos
2. **Webhooks**: Deben estar accesibles públicamente (usar ngrok para desarrollo local)
3. **Suscripciones**: Se crean/actualizan automáticamente cuando un pago es aprobado
4. **Billing Cycle**: Basado en el campo `billingCycle` del Plan (monthly/yearly)

## 🔗 Referencias

- [MercadoPago Checkout Pro Docs](https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/landing)
- [MercadoPago SDK Java](https://github.com/mercadopago/sdk-java)
- [Webhooks MercadoPago](https://www.mercadopago.com.ar/developers/es/docs/checkout-pro/additional-content/your-integrations/notifications/webhooks)

## 👥 Contribuidores

- Team Cold Brew
