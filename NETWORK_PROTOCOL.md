# SXBans Network Protocol (Redis)

This document describes the protocol SXBans uses to coordinate punishments across
multiple servers (a network) via Redis pub/sub. This protocol is completely
**proxy-agnostic** — it doesn't matter whether you're behind BungeeCord/Waterfall
or Velocity, because everything travels over Redis rather than a BungeeCord-specific
plugin-messaging channel. That means writing a companion plugin for Velocity is
exactly as easy as writing one for BungeeCord.

## Prerequisites
- `redis.enabled: true` in `config.yml` on every server you want to be part of the network.
- `network.server-name` must be **unique** per server (e.g. `survival`, `skyblock`, `lobby-1`).
- `network.sync-punishments: true` (enabled by default).

## Channels

### `sxbans:punishments`
Whenever a punishment is applied or removed, this JSON message is published on this channel:

```json
{
  "action": "apply",           // or "remove"
  "punishment": { ... }         // the full Punishment object, serialized with Gson
}
```

Key fields inside `punishment`:
- `playerUUID`, `playerName`
- `type`: one of `BAN, TEMP_BAN, IP_BAN, MUTE, TEMP_MUTE, IP_MUTE, KICK, WARN`
- `reason`, `duration` (milliseconds, `-1` means permanent)
- `ipAddress` (for IP_BAN/IP_MUTE)
- `executorUUID`, `executorName`
- `serverName`: the name of the server that issued this punishment (from `network.server-name`)
- `startTime`, `endTime`, `status`

Every server on the network subscribes to this channel. If the message's `serverName`
matches its own `network.server-name`, it ignores the message (since it issued it itself).

### `sxbans:banwave`
```json
{ "waveId": "...", "player": "...", "executor": "...", "timestamp": 0 }
```

### `sxbans:sync` / `sxbans:sync:<serverName>`
Used for a full sync of the punishment list when a server starts up. It sends a
`type: request` message on `sxbans:sync`, and the other servers reply with
`type: response` and the full punishment list on `sxbans:sync:<requesting-serverName>`.

## Writing a companion plugin for Velocity

Since Velocity connects to the same Redis instance too (rather than talking to the
Spigot plugin directly), all you need to do is:

1. Connect to the same Redis instance using Jedis/Lettuce (same host/port/password
   as this plugin's config).
2. Subscribe to `sxbans:punishments`.
3. When an `action: apply` message with `type: BAN` or `IP_BAN` arrives, kick/disconnect
   the player from Velocity with `player.disconnect(...)` (or, for IP_BAN, block the
   connection before it's even established with `event.setResult(ComponentResult.denied(...))`
   on `PreLoginEvent`).
4. For MUTE/IP_MUTE, keep an in-memory list of muted IPs/UUIDs and block matching
   messages on `PlayerChatEvent` (if you're handling chat at the proxy level).
5. To publish your own actions (e.g. a network-wide ban command issued from the
   proxy), send exactly the same JSON structure shown above with `action: apply`
   and your own `serverName` (e.g. `"proxy"`).

The exact same structure works for BungeeCord/Waterfall too — only the plugin API
differs; the protocol and message format are identical.

## A note on the older plugin-messaging channel (BungeeCord)

In addition to Redis, this plugin also has an optional plugin-messaging channel
(`network.bungee-messaging.enabled: true`) that only works with BungeeCord/Waterfall
(Velocity doesn't support this older mechanism). It's kept purely for backward
compatibility — **using Redis instead is strongly recommended**, since it works with
both Velocity and BungeeCord, and it doesn't require at least one player to be online
to relay a message (an inherent limitation of Bukkit's plugin-messaging channels).
