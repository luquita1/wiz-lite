# 💡 WiZ Lite

App Android **ligera y rápida** para controlar focos LED [WiZ](https://www.wizconnected.com/) por la red local (UDP), sin nube y sin cuentas. Incluye widget de escritorio y horarios programables.

> Proyecto personal. No está afiliado ni respaldado por WiZ ni Signify.

## ✨ Funciones

- **Encendido/apagado** instantáneo
- **Brillo** (10–100 %) y **temperatura de luz blanca** (2200–6500 K)
- **Rueda de color** con vista previa en vivo mientras arrastras
- **8 escenas**: Acogedor, Luz día, Nocturna, Enfoque, Relax, TV, Dormir y Fuego
- **Búsqueda automática** del foco en la red local
- **Widget** de escritorio con tres botones: 20 %, encender/apagar y 100 %
- **Horarios** por hora y día de la semana (encender con brillo o apagar), **editables**, que funcionan con la app cerrada y sobreviven al reinicio del móvil
- Tema **medio oscuro** con acento ámbar

## 🧰 Tecnologías

- Kotlin
- Jetpack Compose (Material 3)
- `AlarmManager` para los horarios y `AppWidgetProvider` para el widget
- Comunicación UDP nativa (`DatagramSocket`), sin librerías externas de red

## 📋 Requisitos

- Android 8.0 (API 26) o superior
- Un foco WiZ. (En mi caso usé un único "Philips Smart LED Connected by WiZ A60 E27 RGBW 8W")
- El móvil y el foco en la **misma red WiFi**

## 🚀 Cómo compilarla

1. Clona el repositorio:
   ```bash
   git clone https://github.com/luquita1/wiz-lite.git
   ```
2. Ábrelo en **Android Studio** (*Open*) y espera a que termine la sincronización de Gradle.
3. Si Gradle da errores con el JDK, ve a *Settings > Build Tools > Gradle* y elige el JDK 17 o 21 incluido en Android Studio (jbr).
4. Conecta tu móvil con la depuración USB activada y pulsa **Run ▶**, o genera un APK con *Build > Build APK(s)*.

## 📱 Uso

1. Abre la app y pulsa **Buscar** (o escribe la IP del foco a mano).
2. Controla la luz desde las tarjetas *Luz*, *Color* y *Escenas*.
3. Agrega el widget desde el escritorio: mantén pulsado > *Widgets* > *WiZ Lite*.
4. En *Horarios* toca **Añadir**, o toca uno existente para editarlo.

> Tip: reserva una IP fija para el foco en tu router, porque la app guarda la IP.

## 🔌 Cómo funciona

Los focos WiZ escuchan mensajes JSON por UDP en el puerto `38899`. Por ejemplo:

```json
{"method":"setPilot","params":{"state":true,"dimming":50}}
```

La app solo envía estos mensajes, por eso es tan ligera. Como UDP puede perder paquetes, los comandos se reintentan automáticamente.

## ⚠️ Limitaciones

- Solo funciona cuando el móvil está en la misma red local que el foco.
- Los horarios dependen del móvil: si el sistema restringe la app por ahorro de batería, ponla en *Sin restricciones*.
- Si fuerzas el cierre de la app desde los ajustes de Android, ábrela una vez para reactivar los horarios.
- Para horarios que funcionen sin el móvil, la app oficial de WiZ permite guardarlos en el propio foco.

## 🤝 Contribuir

Los *issues* y *pull requests* son bienvenidos.

## 📄 Licencia

[MIT](LICENSE)

---

*Desarrollado con Claude.*
