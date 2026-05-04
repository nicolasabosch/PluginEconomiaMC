# Quick Start (para amigos)

Guia rapida para levantar FriendsEconomy y probarlo en 10 minutos.

## 1) Instalar en 2 minutos

1. Copia `friendseconomy-1.0.0-SNAPSHOT.jar` a `plugins/` de tu servidor Paper 1.21.x.
2. Inicia el servidor una vez para que se creen:
   - `plugins/FriendsEconomy/config.yml`
   - `plugins/FriendsEconomy/economy.db`
3. Apaga el servidor para ajustar la config minima.

## 2) Config minima (solo lo esencial)

Edita `plugins/FriendsEconomy/config.yml`:

```yml
currency:
  starting-balance: 200.0

sell-prices:
  DIAMOND: 100.0
  IRON_INGOT: 15.0

shop:
  tax-percent: 5.0
```

Con eso ya puedes probar pagos, venta rapida y marketplace.

## 3) 7 comandos esenciales

- `/balance` -> ver saldo.
- `/pay <jugador> <monto>` -> preparar pago.
- `/pay confirm` -> confirmar pago.
- `/sell` -> abrir venta rapida (usa `sell-prices`).
- `/shop` -> abrir marketplace.
- `/shop list <precio>` -> publicar item de la mano principal.
- `/history` -> ver ultimas transacciones.

## 4) Prueba de 10 minutos (2 jugadores)

### Preparacion

1. Entra con Jugador A y Jugador B.
2. (Opcional, admin) da saldo inicial si hace falta:
   - `/eco give <jugador> 500`

### Flujo recomendado

1. **Transferencia**  
   A ejecuta `/pay B 50` y luego `/pay confirm`.  
   Ambos revisan con `/balance`.
2. **Venta rapida**  
   A pone diamante/hierro en inventario, ejecuta `/sell`, vende y revisa `/balance`.
3. **Marketplace**  
   A sostiene un item y usa `/shop list 150`.  
   B abre `/shop` y compra el item.  
   Ambos revisan `/balance`.
4. **Verificacion final**  
   A y B ejecutan `/history` para confirmar los movimientos.

Si esto funciona, el plugin ya esta listo para uso basico en tu server.
