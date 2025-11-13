# 🚀 Guía de Pruebas con Postman - Módulo MercadoPago

## 📋 Configuración Inicial

### 1. **Servidor Local**
```
Base URL: http://localhost:8080
```

### 2. **Headers Comunes**
```
Content-Type: application/json
Accept: application/json
```

---

## 🔍 **1. Health Check - Verificar que el módulo funciona**

### **GET** `/api/payments/health`
```
URL: http://localhost:8080/api/payments/health
Method: GET
Headers: Accept: application/json
```

**Respuesta esperada:**
```json
{
    "status": "UP",
    "module": "mercadoPago",
    "message": "MercadoPago integration is running",
    "timestamp": "2024-11-12T21:53:00.123456"
}
```

---

## ⚙️ **2. Test de Configuración**

### **GET** `/api/payments/config-test`
```
URL: http://localhost:8080/api/payments/config-test
Method: GET
Headers: Accept: application/json
```

**Respuesta esperada:**
```json
{
    "status": "OK",
    "mercadoPagoConfigured": true,
    "endpoints": [
        "POST /api/payments/create-preference",
        "POST /api/payments/webhook",
        "GET /api/payments/history?userId={id}",
        "GET /api/payments/transaction/{id}",
        "GET /api/payments/status/{id}"
    ],
    "timestamp": "2024-11-12T21:53:00.123456"
}
```

---

## 💳 **3. Crear Preferencia de Pago (Checkout Pro)**

### **POST** `/api/payments/create-preference`
```
URL: http://localhost:8080/api/payments/create-preference
Method: POST
Headers: 
  Content-Type: application/json
  Accept: application/json
```

**Body (JSON):**
```json
{
    "planId": 2,
    "userId": 1,
    "organizationId": null,
    "amount": 29.99,
    "currency": "USD",
    "description": "Upgrade to STANDARD plan"
}
```

**Respuesta esperada (Checkout Pro):**
```json
{
    "preferenceId": "123456789-abcd-1234-5678-123456789abc",
    "publicKey": "APP_USR-12345678-abcd-1234-5678-123456789abc",
    "transactionId": 1,
    "status": "created",
    "message": "Preference created successfully for Checkout Pro"
}
```

> **📝 Nota:** Con Checkout Pro, usas el `preferenceId` y `publicKey` en el frontend con MercadoPago.js para mostrar el modal de pago integrado.

---

## 📊 **4. Obtener Historial de Transacciones**

### **GET** `/api/payments/history?userId={userId}`
```
URL: http://localhost:8080/api/payments/history?userId=1
Method: GET
Headers: Accept: application/json
```

**Respuesta esperada:**
```json
[
    {
        "transactionId": 1,
        "providerTransactionId": "pref_123456789",
        "amount": 29.99,
        "currency": "USD",
        "status": "PENDING",
        "paymentMethod": null,
        "createdAt": "2024-11-12T21:53:00.123456",
        "approvalDate": null,
        "userId": 1,
        "username": "testuser",
        "organizationId": null
    }
]
```

---

## 🔍 **5. Obtener Detalles de Transacción**

### **GET** `/api/payments/transaction/{transactionId}`
```
URL: http://localhost:8080/api/payments/transaction/1
Method: GET
Headers: Accept: application/json
```

**Respuesta esperada:**
```json
{
    "transactionId": 1,
    "providerTransactionId": "pref_123456789",
    "amount": 29.99,
    "currency": "USD",
    "status": "PENDING",
    "paymentMethod": null,
    "createdAt": "2024-11-12T21:53:00.123456",
    "approvalDate": null,
    "userId": 1,
    "username": "testuser",
    "organizationId": null
}
```

---

## 📈 **6. Verificar Estado de Pago**

### **GET** `/api/payments/status/{transactionId}`
```
URL: http://localhost:8080/api/payments/status/1
Method: GET
Headers: Accept: application/json
```

**Respuesta esperada:**
```json
{
    "transactionId": 1,
    "status": "PENDING",
    "amount": 29.99,
    "currency": "USD",
    "createdAt": "2024-11-12T21:53:00.123456",
    "approvalDate": null
}
```

---

## 🔗 **7. Webhook de MercadoPago (Simulación)**

### **POST** `/api/payments/webhook`
```
URL: http://localhost:8080/api/payments/webhook
Method: POST
Headers: 
  Content-Type: application/json
  Accept: application/json
```

**Body (JSON) - Simulando webhook de pago aprobado:**
```json
{
    "type": "payment",
    "data": {
        "id": "123456789"
    }
}
```

**Respuesta esperada:**
```json
{
    "success": true,
    "transactionId": "1",
    "paymentStatus": "approved",
    "message": "Webhook processed successfully"
}
```

---

## 🚨 **Casos de Error Comunes**

### **Error 404 - Usuario no encontrado**
```json
{
    "code": "PAYMENT_NOT_FOUND",
    "message": "User not found with ID: 999",
    "details": {
        "transactionId": "999",
        "paymentId": "UNKNOWN"
    },
    "timestamp": "2024-11-12T21:53:00.123456",
    "path": "uri=/api/payments/create-preference"
}
```

### **Error 400 - Datos inválidos**
```json
{
    "code": "INVALID_PAYMENT_DATA",
    "message": "Invalid amount: must be greater than 0",
    "details": {
        "fieldName": "amount",
        "fieldValue": "-10.0"
    },
    "timestamp": "2024-11-12T21:53:00.123456",
    "path": "uri=/api/payments/create-preference"
}
```

### **Error 502 - Error de MercadoPago**
```json
{
    "code": "MERCADO_PAGO_ERROR",
    "message": "Error creating MercadoPago preference: Invalid access token",
    "details": {
        "mercadoPagoErrorCode": "401",
        "mercadoPagoErrorMessage": "Invalid credentials"
    },
    "timestamp": "2024-11-12T21:53:00.123456",
    "path": "uri=/api/payments/create-preference"
}
```

---

## 📝 **Orden de Pruebas Recomendado**

1. **Health Check** → Verificar que el servidor está funcionando
2. **Config Test** → Verificar que MercadoPago está configurado
3. **Create Preference** → Crear una preferencia de pago
4. **Transaction History** → Ver las transacciones creadas
5. **Transaction Details** → Ver detalles específicos
6. **Payment Status** → Verificar estado actual

---

## 🔧 **Datos de Prueba Necesarios**

Antes de probar, asegúrate de tener:

1. **Usuario existente** en la base de datos (userId: 1)
2. **Plan existente** en la base de datos (planId: 2)
3. **Credenciales de MercadoPago** configuradas en `application.properties`
4. **Base de datos** ejecutándose con el esquema correcto

---

## 🎯 **Próximos Pasos**

1. Ejecutar el servidor: `mvn spring-boot:run`
2. Importar esta colección en Postman
3. Probar endpoints en orden
4. Verificar logs del servidor para debugging
5. Revisar base de datos para confirmar datos

¿Necesitas ayuda configurando algún endpoint específico?
