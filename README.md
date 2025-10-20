# LoginStreaks

A Bukkit plugin for Minecraft Beta 1.7.3 that tracks daily login streaks and rewards players with money for consistent logins.

## Features

- **Daily Login Tracking**: Automatically tracks when players log in each day
- **Streak System**: Maintains current and longest streak records for each player
- **Reward System**: Configurable monetary rewards that increase with each consecutive day
- **Leaderboard Caching**: Efficient top streaks leaderboard with periodic cache refresh
- **Timezone Support**: Configurable timezone for accurate day/midnight calculations
- **Essentials Integration**: Economy integration via Essentials plugin
- **MySQL Support**: Optional MySQL database storage for player data
- **Admin Commands**: Full admin control over player streaks
- **Customizable Messages**: All player messages are configurable with color codes

## Commands

### Player Commands
- `/loginstreak` or `/ls` - View your current streak status and next login deadline
- `/loginstreak <player>` - View another player's streak information
- `/loginstreak top` - View top 10 longest streaks leaderboard (cached)
- `/loginstreak help` - Show command help

### Admin Commands
Requires permission: `loginstreaks.admin` (default: OP)

- `/lsa set <player> <streak>` - Set a player's current streak to any number
- `/lsa reset <player>` - Reset a player's streak to 0
- `/lsa reload` - Reload the plugin configuration
- `/lsa refresh` - Force refresh the leaderboard cache immediately
- `/lsa help` - Show admin help

## How It Works

### Streak Logic
1. **Day 1**: Player logs in for the first time → Streak starts at day 1 (no reward)
2. **Day 2**: Player logs in after midnight → Streak increments to day 2 → First reward ($15 by default)
3. **Day 3+**: Continue logging in daily → Streak increases → Rewards increase by $15 each day
4. **Missed Login**: If player doesn't log in within 24 hours → Streak resets to day 1 (no reward on reset)
5. **Same Day**: Logging in multiple times on the same day doesn't increment the streak

### Reward Calculation
- **Day 1**: No reward
- **Day 2**: 1 × `reward.increase` = $15
- **Day 3**: 2 × `reward.increase` = $30
- **Day 4**: 3 × `reward.increase` = $45
- Continues until `reward.max` is reached (default: $500)

### Midnight Reset
The plugin uses your configured timezone to determine when a new day begins (midnight). You can log in after midnight to increment your streak.

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins/` folder
3. Ensure you have **Essentials** installed for economy support
4. (Optional) Set up MySQL database if you want database storage
5. Restart your server
6. Configure the plugin in `plugins/LoginStreaks/config.properties`

## Configuration

### Main Config (`config.properties`)

```properties
#LoginStreak configuration

# === TIMEZONE SETTINGS ===
# Use UTC offset numbers: 0=UTC, 1=GMT+1, -5=EST, 8=Asia, etc.
timezone=0

# === REWARD SETTINGS ===
# Base reward amount that increases each day
reward.increase=15.0
# Maximum reward cap (0 = no limit)
reward.max=500.0

# === CACHE SETTINGS ===
# How often to refresh the top streaks leaderboard cache (in minutes)
cache.refresh_minutes=5

# === PLAYER MESSAGES ===
message.reward=&a{player} reached &e{streak}d &astreak and earned &6${amount}&a!
message.continue=&e{player}&a's login streak: &e{streak}d&a.
message.reset=&cYour streak has reset.

# === DEBUG SETTINGS ===
debug=false
```

**Configuration Options:**
- `timezone` - UTC offset for your server's timezone (e.g., 1 for GMT+1, -5 for EST)
- `reward.increase` - Base reward amount that increases each day
- `reward.max` - Maximum reward cap (set to 0 for no limit)
- `cache.refresh_minutes` - How often to refresh the top streaks cache (default: 5 minutes)
- `message.*` - Customizable messages with color codes (&a, &e, &c, etc.)
- `debug` - Enable debug logging (true/false)

### Database Config (`database.properties`)

```properties
#LoginStreaks database configuration

# === DATABASE SETTINGS ===
# Enable MySQL database storage (uses file storage if disabled)
database.enabled=false
database.host=localhost
database.port=3306
database.name=minecraft
database.username=root
database.password=
database.useSSL=false
```

**Note:** Database support is optional. By default, player data is stored in flat files (`plugins/LoginStreaks/playerdata/`).

## Player Data Storage

Player data is stored in `plugins/LoginStreaks/playerdata/<playername>.yml`:

```properties
streak=5
lastLogin=1760058770310
longestStreak=10
```

- `streak` - Current consecutive login streak
- `lastLogin` - Timestamp of last login (milliseconds)
- `longestStreak` - Player's all-time longest streak

## Permissions

- `loginstreaks.admin` - Allows access to admin commands (default: OP)

## Dependencies

- **Essentials** - Required for economy/money rewards
- **MySQL Connector** - Optional, only if using database storage

## Compatibility

- **Minecraft Version**: Beta 1.7.3
- **Server Type**: Bukkit/CraftBukkit

## Examples

### Player Experience
```
[Player logs in for first time]
→ "ItsVollx's login streak: 1d."

[Player logs in next day]
→ "ItsVollx reached 2d streak and earned $15!"

[Player logs in day 3]
→ "ItsVollx reached 3d streak and earned $30!"

[Player checks their streak]
/ls
→ === Login Streak ===
→ Current streak: 3d
→ Longest streak: 3d
→ Last login: Oct 21, 2025 at 14:30
→ Next login available in: 9h 30m 15s
→ Streak expires in: 23h 45m 30s
→ Next reward (day 4): $45.0

[Player misses a day]
→ "Your streak has reset."
→ "ItsVollx's login streak: 1d."
```

### Admin Usage
```
# Set a player's streak to 10 days
/lsa set ItsVollx 10
→ "Set ItsVollx's streak to 10 days."

# Reset a player's streak
/lsa reset ItsVollx
→ "Reset ItsVollx's streak."

# Force refresh the leaderboard cache
/lsa refresh
→ "Leaderboard cache refreshed successfully."

# Reload configuration
/lsa reload
→ "LoginStreaks configuration reloaded successfully."
```

### Leaderboard
```
/ls top
→ === Top Login Streaks ===
→ 1. ItsVollx - 30d
→ 2. Player2 - 25d
→ 3. Player3 - 20d
→ Next update in: 4h 23m 15s
```

## Technical Details

### Cache System
The plugin uses an intelligent caching system for the leaderboard:
- Top streaks are cached in memory
- Cache automatically refreshes every X minutes (configurable)
- No file I/O when players run `/ls top` (instant response)
- Admins can force refresh with `/lsa refresh`

### Time Calculations
- **Streak Window**: 24 hours from last login
- **Day Change**: Midnight in your configured timezone
- **Next Login**: Calculated to next midnight (when streak can increment)
- **Expiration**: 24 hours from last login (when streak resets)

### Event System
The plugin uses a custom event listener for OSAS/Poseidon compatibility on Beta 1.7.3 servers.

## Support

For issues, suggestions, or contributions, please visit the GitHub repository.

## License

See LICENSE file for details.

## Author

ItsVollx

## Changelog

See CHANGELOG.md for version history and updates.
