# CrudActivity – CrudCloud

### Proyecto grupal de despliegue y gestión de servicios en la nube

---

## Contexto

Tanto personas naturales como empresas necesitan bases de datos disponibles en la nube para ejecutar aplicaciones, hacer pruebas o desarrollar soluciones. Sin embargo, crear y administrar estas bases de datos manualmente suele requerir tiempo, conocimientos avanzados y configuraciones complejas.

**Crudzaso** busca lanzar **CrudCloud**, una plataforma que permita a cualquier usuario —individual o corporativo— **crear y gestionar instancias reales de bases de datos** en la nube, ejecutadas dentro de contenedores Docker en una **VPS**.

El sistema debe ofrecer un flujo completo de creación, acceso seguro y pago por suscripción, garantizando control, trazabilidad y seguridad.

---

## Problema

Actualmente, los usuarios que necesitan una base de datos para sus proyectos deben instalar y configurar manualmente cada motor. Esto genera demoras, errores frecuentes y dificultad para escalar.
Crudzaso necesita una solución que **automatice la provisión de bases de datos**, controle el acceso mediante planes de servicio, y permita ampliar capacidades mediante **suscripciones pagas con Mercado Pago**.

El equipo de desarrollo será responsable de **construir y desplegar** la plataforma completa, asegurando que funcione de manera real en la VPS asignada, que los límites por plan se apliquen correctamente y que los contenedores de bases de datos puedan iniciarse, detenerse y eliminarse desde la aplicación.

---

## Alcance funcional

1. **Usuarios y acceso**

   * Registro e inicio de sesión.
   * El sistema debe admitir tanto **usuarios individuales** como **organizaciones**.

2. **Catálogo de motores disponibles**

   * MySQL
   * SQL Server
   * PostgreSQL
   * Redis
   * Cassandra
   * MongoDB

3. **Instancias de base de datos**

   * Cada instancia se ejecuta como **contenedor Docker real** dentro de la VPS.
   * Cada instancia corresponde a **una única base de datos**.
   * En el **plan Free**, el nombre de la base de datos se genera **automáticamente** (hash o cadena aleatoria).
   * En planes pagos, el usuario puede definir el nombre.
   * Estados esperados: **CREATING**, **RUNNING**, **SUSPENDED**, **DELETED**.
   * Debe permitir crear, listar, suspender, reanudar, rotar contraseña y eliminar instancias.

4. **Credenciales y seguridad**

   * Al crear una instancia, el sistema genera credenciales (host, puerto, nombre, usuario, contraseña).
   * La **contraseña solo se muestra una vez** tras la creación.
   * Se **envía un correo** con la información esencial (sin contraseña en texto plano).
   * El usuario puede **descargar un PDF** con los datos, donde la contraseña aparece por única vez.
   * El sistema permite **rotar o restaurar** la contraseña, generando una nueva, invalidando la anterior y enviando un nuevo correo.

5. **Planes y límites**

   * **Free:** hasta 2 instancias.
   * **Standard:** hasta 5 instancias.
   * **Premium:** hasta 10 instancias.
   * La plataforma debe aplicar los límites según el plan vigente y bloquear la creación al alcanzarlos.

6. **Pagos y suscripciones**

   * Los usuarios pueden cambiar de plan mediante **Mercado Pago**.
   * El flujo debe implementarse primero con **Sandbox** y luego con **Producción**.
   * Al aprobarse el pago, el plan y los límites se actualizan.
   * Si el pago se pausa o falla, la plataforma restringe nuevas creaciones.
   * Debe existir trazabilidad de transacciones y del estado del plan.

7. **Notificaciones**

   * Correos automáticos para creación de instancia, rotación de contraseña y cambios de plan.

---

## Requerimientos técnicos

### Backend

* **Spring Boot** con arquitectura por capas: controller → service → repository → model (+ dto/config).
* **Spring JPA + Hibernate** con carga **LAZY** por defecto.
* **Validación** de entradas con **Spring Validator (Bean Validation)**.
* **Manejo de errores global** con `@ControllerAdvice` y `@ExceptionHandler`.
* Uso de **ResponseEntity** en las respuestas.
* **Orquestación de contenedores Docker** desde el backend para crear, iniciar, detener y eliminar instancias en la VPS.
* **Gestión segura de credenciales**: almacenamiento cifrado o con hash; no persistir contraseñas en texto plano.

### Frontend

* Aplicación en **React**.
* Vistas para:

  * Dashboard de usuario.
  * Catálogo de motores.
  * Listado y detalle de instancias.
  * Acciones de suspensión, reanudación, eliminación y rotación.
  * Visualización del plan y sus límites.
* Manejo de estados, validaciones de formulario y errores del backend.

### Despliegue y dominios

* **Backend** y **Frontend** desplegados dentro de la VPS provista.
* Cada servicio con su **Dockerfile**.
* Los contenedores de bases de datos deben correr realmente en la VPS.
* Subdominios por equipo:

  * **Frontend:** `name-team.crudzaso.com`
  * **Backend:** `api.name-team.crudzaso.com`
  * **Documentación:** `docs.name-team.crudzaso.com`
* Repositorios con nombres coherentes bajo la organización:
  `crudcloud-frontend-name-team`,
  `crudcloud-backend-name-team`,
  `crudcloud-docs-name-team`.

### Documentación y control de proceso

* **Docusaurus** publicado en `docs.name-team.crudzaso.com`.
* **Azure Boards** para trazabilidad (historias, tareas, bugs, vínculos con commits y PR).
* **Git Flow** para ramas y flujo de desarrollo.
* **Conventional Commits** en todos los commits.

---

## Criterios de verificación

1. Plataforma desplegada y accesible desde los subdominios asignados.
2. Creación de instancias que levante contenedores Docker reales y entregue credenciales.
3. Contraseña visible una sola vez, correo y PDF generados correctamente.
4. Funcionalidad de rotación de contraseña operativa.
5. Límites de plan aplicados (2/5/10) y bloqueo al alcanzarlos.
6. Cambio de plan funcional tras pago en Mercado Pago.
7. Acciones de suspensión, reanudación y eliminación actualizan estado del contenedor.
8. Trazabilidad visible en Azure Boards y documentación publicada en Docusaurus.

---

## Entregables

* Plataforma completa (frontend y backend) en ejecución en la VPS.
* Dockerfiles para los servicios y configuración del entorno.
* Integración con Mercado Pago operativa (Sandbox validado, Producción configurada).
* Documentación técnica publicada.
* Trazabilidad registrada en Azure Boards.
* Repositorios correctamente nombrados y estructurados.

---