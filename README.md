# FriendsEconomy (Paper 1.21.11)

Plugin de economia para servidores Paper con:
- moneda interna (SQLite)
- venta rapida con GUI (`/sell`)
- marketplace entre jugadores (`/shop`)
- historial, ranking, loteria, jobs y comandos admin

## 1) Requisitos

- Java 21
- Servidor Paper 1.21.x (recomendado 1.21.11)
- Archivo del plugin: `friendseconomy-1.0.0-SNAPSHOT.jar`

## 2) Instalacion

1. Copia el jar a la carpeta `plugins/` de tu servidor.
2. Inicia el servidor una vez.
3. Se creara la carpeta del plugin con:
   - `plugins/FriendsEconomy/config.yml`
   - `plugins/FriendsEconomy/economy.db`
   - `plugins/FriendsEconomy/audit.log` (cuando uses comandos admin)
4. Apaga, ajusta `config.yml` y vuelve a iniciar.

## 3) Comandos de jugador

- `/balance`  
  Muestra tu balance actual.

- `/pay <jugador> <monto>`  
  Prepara un pago.

- `/pay confirm`  
  Confirma el ultimo pago pendiente.

- `/sell`  
  Abre GUI de venta rapida segun `sell-prices` en config.

- `/shop`  
  Abre marketplace (pagina 1).

- `/shop <pagina>`  
  Abre una pagina especifica.

- `/shop search <texto>`  
  Busca listings por nombre.

- `/shop list <precio>`  
  Lista el item en tu mano principal.

- `/shop cancel <id>`  
  Cancela tu listing y devuelve item al inventario.

- `/history [N]`  
  Muestra ultimas transacciones (limitado por config).

- `/baltop [pagina]`  
  Ranking de balances.

- `/lottery buy <cantidad>`  
  Compra tickets de loteria.

- `/jobs`  
  Muestra jobs disponibles.

- `/jobs select <job>`  
  Selecciona job.

## 4) Comandos admin (`friendseconomy.admin`)

- `/eco give <jugador> <monto>`
- `/eco take <jugador> <monto>`
- `/eco set <jugador> <monto>`
- `/eco reset <jugador>`
- `/eco resetall` + `/eco confirm` (jugador)
- `/eco resetall confirm` (consola)
- `/eco reload`

Todo uso sensible de admin queda en `audit.log`.

## 5) Configuracion (`config.yml`)

### Moneda

```yml
currency:
  symbol: "$"
  starting-balance: 0.0
  max-balance: 1000000000.0
  history-default-limit: 10
  history-max-limit: 50
```

### Venta rapida

```yml
sell-prices:
  DIAMOND: 100.0
  IRON_INGOT: 15.0
```

### Marketplace

```yml
shop:
  gui-title: "Player Marketplace"
  page-size: 45
  tax-percent: 5.0
  listing-expiry-days: 7
```

### Baltop

```yml
baltop:
  page-size: 10
```

### Loteria

```yml
lottery:
  enabled: true
  draw-interval-minutes: 60
  ticket-price: 100.0
```

### Jobs

```yml
jobs:
  Miner:
    materials:
      DIAMOND_ORE: 20.0
```

Puedes definir pagos por:
- `materials` (bloques/items)
- `mobs` (entidades)
- `fish-catch` (pesca)

## 6) Como usarlo en tu servidor (flujo recomendado)

1. Define precios en `sell-prices`.
2. Ajusta `tax-percent`, `listing-expiry-days` y limites de moneda.
3. Prueba con 2 jugadores:
   - Jugador A lista item: `/shop list 150`
   - Jugador B compra desde `/shop`
   - Verifica saldo de ambos con `/balance`
4. Ejecuta `/history` y `/baltop`.
5. Prueba `/lottery buy 3`.
6. Prueba jobs:
   - `/jobs select Miner`
   - rompe bloques configurados y revisa balance.

## 7) Notas importantes

- Si un listing expira y el jugador esta offline o sin espacio, el item se devuelve automaticamente al entrar cuando haya espacio.
- Si cambias config en caliente, usa `/eco reload`.
- Si algo falla, revisa:
  - consola del servidor
  - `plugins/FriendsEconomy/audit.log`

## 8) Solucion de problemas

- **No carga el plugin**: verifica Java 21 y que usas Paper compatible.
- **No aparecen comandos**: revisa errores al iniciar en consola.
- **No guarda datos**: revisa permisos de escritura en carpeta `plugins/FriendsEconomy`.
- **Errores SQL**: apaga servidor antes de manipular `economy.db`.

