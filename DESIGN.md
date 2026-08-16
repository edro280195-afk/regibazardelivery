# Sistema visual Regi Bazar 2026

## Estado de migración

Esta guía es la fuente de verdad para el rediseño 2026. La migración se hará vista por vista para
evitar una mezcla accidental de componentes nuevos y anteriores. Las vistas migradas son
`LoginScreen`, `RoutesListScreen`, `AdminRouteScreen`, `RouteBuilderScreen`, `RouteScreen`,
`ChatScreen` y `RouteSummaryScreen`. Los tokens reutilizables están en
`ui/theme/RegiBazar2026.kt`.

## Escena de uso

Eduardo consulta y administra rutas desde el teléfono, en interiores y bajo luz exterior, con
atención dividida y una mano ocupada. Por eso la interfaz es clara primero, usa contraste alto,
objetivos táctiles grandes y deja la acción siguiente a la vista. El tema oscuro conserva la misma
jerarquía para trabajo nocturno, pero el tema claro es la referencia principal.

## Personalidad

Directa, cálida y operativa. El azul comunica acción y navegación; el naranja se reserva para
dinero; verde, ámbar y rojo comunican estado. La decoración nunca compite con la información de
entregas.

## Paleta

| Token | Claro | Uso |
|---|---:|---|
| `background` | `#F1F4F9` | Fondo general |
| `surface` | `#FCFDFF` | Tarjetas y controles elevados |
| `surfaceMuted` | `#E4E9F1` | Controles segmentados y skeletons |
| `textPrimary` | `#111827` | Títulos y datos principales |
| `textSecondary` | `#6B7280` | Metadatos y ayuda |
| `divider` | `#EAEEF4` | Separadores |
| `accent` | `#0B5FFF` | Acción primaria, selección y navegación |
| `accentPressed` | `#0A4FD6` | Estado presionado |
| `money` | `#FF7A00` | Cobros e importes |
| `success` | `#16A34A` | Entregas y rutas completadas |
| `warning` | `#B7791F` | Pendientes y modo offline |
| `error` | `#DC2626` | Fallas, cancelación y ubicación faltante |

Cada color semántico tiene una superficie suave. La variante oscura equivalente está definida en
el mismo archivo de tokens. No se usan negro o blanco puros.

## Tipografía

Se usa Poppins, ya empaquetada en la aplicación, para evitar descargas de fuentes en tiempo de
ejecución. Títulos: 19–26 sp, peso 700–800. Texto operativo: 12–14 sp. Datos monetarios: 21 sp,
peso 800. La jerarquía se consigue con tamaño y peso, no con efectos.

## Espaciado y forma

- Escala base: 4, 8, 10, 12, 14, 16, 20 y 24 dp.
- Margen horizontal de pantalla: 16 dp; encabezado: 20 dp.
- Objetivo táctil mínimo: 48 dp.
- Radio de tarjeta: 18 dp.
- Radio de control: 14 dp.
- Radio de etiqueta: 9 dp; estados cortos pueden usar forma de píldora.
- Elevación: 3 dp para controles y tarjetas; 1 dp al presionar.

## Componentes base

### Encabezado

Título corto a la izquierda. Acciones frecuentes a la derecha en superficies de 48 dp. Las
notificaciones muestran un punto rojo y concentran alertas C.A.M.I., conectividad e información
secundaria para no saturar la lista.

### Control segmentado

Dos opciones sobre superficie gris azulada, con indicador claro deslizante. Transición de 220 ms.
El texto seleccionado usa color primario de tinta, no solamente el color de acento.

### Banner operativo

Resume el volumen real de la pestaña actual. Azul para trabajo pendiente y verde para completado.
Debe mostrar datos reales, nunca mensajes genéricos desconectados del estado.

### Login

El login es una puerta de operación, no una landing de marca. Usa fondo claro con una acentuación
azul suave, marca centrada y tarjeta principal con selector Reparto/Admin, correo, contraseña,
error inline y acción primaria. La biometría aparece sólo cuando hay credenciales guardadas.

No usa movimiento decorativo, gradientes animados ni texto espaciado artificialmente. El cambio de
modo conserva los mismos campos porque ambos roles autentican con credenciales; la diferencia es de
contexto y destino operativo.

### Tarjeta de ruta

Orden fijo: número y estado, fecha y conteos, cobro esperado, etiquetas operativas y acceso al modo
reparto. Tocar la tarjeta abre administración. Las etiquetas disponibles son transferencias,
tandas y ubicación faltante. No se muestra el token técnico al usuario.

### Administración de ruta

Conserva el encabezado corto de la lista y entra desde la derecha en 240 ms para comunicar
continuidad espacial. El orden es: resumen de estado, acciones principales, acceso al modo reparto
y paradas. El resumen usa los mismos estados y color monetario que las tarjetas de la lista.

Cada parada muestra posición, clienta, dirección y condición de cobro. Navegar es la acción
principal; llamada, reordenamiento y retiro permanecen disponibles como acciones compactas. Los
controles destructivos siempre solicitan confirmación. Cuando falla la red, se muestra la última
versión guardada desde la lista y se identifica claramente como información offline.

### Armar, editar y previsualizar ruta

`RouteBuilderScreen` cubre tres estados del mismo flujo: nueva ruta, edición de ruta existente y
preview. Debe sentirse como una mesa de preparación: primero selección rápida, después revisión del
orden y al final guardado. No se separa en pantallas distintas porque perdería continuidad y
obligaría a repetir contexto.

La pantalla usa el mismo encabezado corto de administración y una barra inferior persistente cuando
hay paradas seleccionadas. La acción primaria es crear o guardar; preview es secundario pero visible
para validar mapa, distancia, tiempo y cobro antes de confirmar. C.A.M.I. vive como acción compacta
en el resumen superior para ayudar sin desplazar la selección manual.

Cada candidato muestra selección, clienta, dirección, condición de cobro, producto y estado de
ubicación. Pedidos y tandas comparten el mismo vocabulario visual, pero las tandas no suman cobro
esperado. El naranja se usa sólo en importes por cobrar; azul para selección, transferencias y
tandas; verde para ubicaciones listas o pagos cubiertos; ámbar para ubicaciones faltantes.

El preview adopta composición mapa + lista de orden propuesto. Si faltan coordenadas, el aviso
explica que esas paradas quedan al final. Las paradas omitidas se muestran como alerta compacta,
sin bloquear guardar cuando el backend ya respondió con una ruta válida.

### Modo reparto

Es la vista operativa principal y adopta una estructura mapa-first. El mapa ocupa el fondo y sólo
funciona como contexto; la parada activa vive en una hoja inferior clara. La cápsula superior
muestra paradas procesadas, cobro acumulado y progreso real sin convertirse en un tablero de
métricas.

La jerarquía de la parada es fija: posición, clienta, dirección, condición de cobro, contexto,
acciones de llegada, pago y confirmación. El naranja se reserva para dinero por cobrar; azul para
navegación y transferencia; verde para pagos cubiertos y confirmaciones.

La entrega requiere deslizar al menos 82% del control. El gesto evita activaciones accidentales,
usa transición de 180–220 ms sin rebote y muestra estado ocupado mientras se envía o encola. Al
confirmar aparece un mensaje sobrio y se avanza automáticamente a la siguiente parada.

Foto, firma, gasto y falla son acciones secundarias de igual tamaño. QR, productos, notas y pagos
divididos permanecen disponibles sin competir con la acción principal. Offline, cambios de pedido,
proximidad y mensajes de C.A.M.I. se muestran en una sola zona contextual para evitar overlays
simultáneos.

Las referencias de Dribbble se usan sólo como investigación de patrones: mapa con tarea activa,
ETA visible, acciones de llamada/mensaje y confirmación de entrega. La composición, paleta y
comportamiento final siguen el sistema propio de Regi Bazar.

### Chat

El chat usa el mismo encabezado corto del flujo de reparto: regreso, avatar funcional, nombre y
estado de conexión. El campo de mensaje vive en una barra inferior fija y respeta teclado y navegación
del sistema. Las burbujas diferencian conductor y contraparte por alineación, superficie y color, no
por emojis ni efectos.

El empty state explica para qué sirve el canal. El aviso superior es compacto y operativo: soporte de
ruta para admin o conversación ligada a la parada para clienta.

### Resumen de ruta

El resumen final cierra la jornada con resultado, cobro y rendimiento. Evita estética de dashboard
genérico: no hay glassmorphism, animaciones largas ni gráficas decorativas. La información se ordena
por decisión: efectividad, cobro por método, métricas útiles y cierre de paradas.

Finalizar jornada es la acción primaria en barra inferior; compartir resumen es secundaria pero
persistente. El texto compartido usa español directo y montos formateados para México.

### Estados de pantalla

- Carga: skeletons con la geometría del contenido.
- Vacío global: explica qué hacer y ofrece crear ruta.
- Sin resultados: explica cómo cambiar búsqueda o pestaña.
- Error: mensaje claro y acción de reintento.
- Offline: aviso ámbar sin bloquear la información guardada.

## Movimiento y accesibilidad

Las transiciones de estado duran entre 150 y 250 ms y no animan propiedades de layout de forma
continua. No hay entradas escalonadas ni movimiento decorativo. Todos los iconos interactivos
tienen descripción, el estado no depende sólo del color y los objetivos táctiles miden al menos
48 dp.

## Navegación inferior

El prototipo contempla Rutas, Mapa, Gastos, Historial y Perfil. No debe incorporarse hasta que
existan destinos reales para las cinco opciones; una navegación con botones sin función rompe la
confianza del usuario. Cuando se implementen esos módulos se usará fondo de superficie, indicador
superior azul de 3 dp e icono con etiqueta.
