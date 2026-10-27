# POS Inventario

Proyecto Maven con Java 21 y Spring Boot 3.5.16. Incluye Web, Data JPA,
Security, Thymeleaf, MySQL Driver y Validation. Incluye autenticación por email y una vista de login.

## Base de datos

Se utiliza el esquema MySQL existente `pos_inventario_db` en localhost:3306,
usuario `root` y contraseña vacía. La configuración está en
`src/main/resources/application.properties`.
`spring.jpa.hibernate.ddl-auto=validate` comprueba el esquema al arrancar:
no crea la base de datos ni cambia las tablas. Ejecuta previamente tu SQL si aún no existen.

## Entidades y relaciones

Todas las claves primarias son `Long` con generación IDENTITY (AUTO_INCREMENT).
Los importes son `BigDecimal` con precisión 10 y escala 2.
`Venta.fecha` usa `LocalDateTime` y corresponde a DATETIME, sin zona horaria.

| Relación | Cardinalidad | Clave foránea | Borrado en MySQL |
| --- | --- | --- | --- |
| Categoria → Producto | Una categoría, muchos productos | productos.categoria_id, nullable | SET NULL |
| Usuario → Venta | Un usuario, muchas ventas | ventas.usuario_id, nullable | SET NULL |
| Venta → DetalleVenta | Una venta, muchos detalles | detalle_ventas.venta_id, obligatoria | CASCADE |
| Producto → DetalleVenta | Un producto, muchos detalles | detalle_ventas.producto_id, obligatoria | CASCADE |

Las asociaciones Java son unidireccionales: Producto tiene categoria,
Venta tiene usuario y DetalleVenta tiene venta y producto.
Todas usan `@ManyToOne(fetch = LAZY)`. Las referencias opcionales pueden ser null.
No se añaden colecciones inversas ni columnas ajenas al SQL.

Proveedor es independiente: tu esquema no contiene una clave foránea entre
proveedores y productos.

Los ON DELETE los ejecutan las claves foráneas de MySQL ya creadas.
No se usa CascadeType.REMOVE en las asociaciones ManyToOne: eliminar un detalle
no debe eliminar su venta ni su producto.
Si borras una categoría o usuario, MySQL conserva los registros dependientes y
anula su referencia. Si borras una venta o producto, MySQL elimina sus detalles.
Después de un borrado efectuado en la base, recarga cualquier entidad que ya
estuviera cargada en el contexto de persistencia antes de consultar su estado.

Cada entidad tiene un repositorio `JpaRepository<Entidad, Long>` con operaciones
CRUD, paginación y ordenación. Los campos de texto respetan las longitudes del
SQL; rol y metodoPago son String porque las tablas no definen ENUM.

## Compilar y ejecutar

Requisitos: JDK 21, Maven 3.6.3 o superior y MySQL local con las tablas creadas.

```powershell
mvn clean package
mvn spring-boot:run
```

SecurityConfig permite el acceso público a `/login` y exige autenticación para
las demás rutas. LoginController devuelve la vista `login` para GET `/login`.
La plantilla `src/main/resources/templates/login.html` usa Bootstrap 5.3.0 desde CDN,
con el CSS en el head y el bundle JavaScript al final del body.
Su formulario envía POST a `/login` con `username` (email), `password` y el token CSRF.
Spring Security procesa ese POST; no necesita un método POST en el controlador.

Se declara un bean PasswordEncoder con BCryptPasswordEncoder. No hace falta otra
dependencia: spring-boot-starter-security incluye spring-security-crypto.
BCrypt genera un hash; usa `encode` para guardar y `matches` para verificar.
UsuarioDetailsService busca el email en usuarios y devuelve el hash y el rol.
Los roles en la base se guardan como ADMIN o CAJERO; Spring Security los convierte
en las autoridades ROLE_ADMIN y ROLE_CAJERO. Ambos roles pueden acceder a las
rutas autenticadas actuales; todavía no hay restricciones por rol.

DataInitializer crea estos usuarios al arrancar, solo si su email no existe:

| Email | Contraseña inicial | Rol |
| --- | --- | --- |
| admin@pos.com | admin123 | ADMIN |
| cajero@pos.com | cajero123 | CAJERO |

Las contraseñas se guardan como hashes BCrypt usando el PasswordEncoder configurado.
Los usuarios existentes conservan sus datos y contraseñas al reiniciar.
Después del login se vuelve a la ruta solicitada o a `/`. HomeController devuelve
la vista index para `/`: un panel de bienvenida con la Navbar Sistema POS y un
botón hacia `/productos`. ProductoController ofrece GET `/productos` (modelo
`productos`, vista `productos/lista`), GET `/productos/nuevo` (modelos `producto`
y `categorias`, vista `productos/formulario`), POST `/productos/guardar` y
GET `/productos/eliminar/{id}`. Guardar y eliminar redirigen a `/productos`.
Las plantillas `templates/productos/lista.html` y
`templates/productos/formulario.html` incluyen tabla adaptable, estado vacío y
formulario con nombre, código de barras, categoría opcional, precio y stock.
El CSS institucional compartido está en `static/css/styles.css`. Inicio, login
y productos lo cargan después de Bootstrap 5.3.0. La navegación compartida está
en `templates/fragments/navbar.html` e incluye cierre de sesión por POST con CSRF.
Thymeleaf agrega automáticamente el token CSRF al formulario de producto por su
atributo `th:action`. Las categorías se cargan junto con el listado mediante
EntityGraph. Los códigos de barras vacíos se guardan como NULL.
No se han creado reglas de negocio de ventas o stock.
