# LoginStreaks

A Bukkit plugin for Minecraft Beta 1.7.3 that tracks daily login streaks and rewards players for consistent logins.

## Features

- **Daily Login Tracking**: Automatically tracks when players log in each day
- **Streak Management**: Maintains current and longest streak records for each player
- **Reward System**: Configurable monetary rewards that increase with streak length
- **Player Commands**: 
  - `/loginstreak` - View your own streak information
  - `/loginstreak <player>` - View another player's streak
  - `/loginstreak top` - View top 10 longest streaks leaderboard
  - `/loginstreak help` - Show command help
- **Essentials Integration**: Optional economy integration with Essentials plugin
- **Database Support**: Optional MySQL database storage (file-based storage by default)
- **Timezone Support**: Configurable timezone for streak resets
- **Customizable Messages**: All player messages are configurable

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Configure the plugin in `plugins/LoginStreaks/config.properties`

## Configuration

### Main Config (`config.properties`)
```properties
# Timezone (UTC offset: 0=UTC, 1=GMT+1, -5=EST, etc.)
timezone=0

# Reward settings
reward.increase=15.0
reward.max=500.0

# Player messages
message.reward=&a{player} reached &e{streak}d &astreak and earned &6${amount}&a!
message.continue=&e{player}&a's login streak: &e{streak}d&a.
message.reset=&cYour streak has reset.

# Debug mode
debug=false
```

### Database Config (`database.yml`)
```yaml
database:
  enabled: false
  host: localhost
  port: 3306
  name: loginstreaks
  username: root
  password: 
  useSSL: false
```

## Requirements

- Minecraft Beta 1.7.3 server
- Bukkit API compatible with Beta 1.7.3
- Java 6 or higher
- (Optional) Essentials plugin for economy features

## Commands

| Command | Description |
|---------|-------------|
| `/loginstreak` | Show your current streak status |
| `/loginstreak <player>` | View another player's streak information |
| `/loginstreak top` | Display top 10 longest streaks leaderboard |
| `/loginstreak help` | Show command help |

## Permissions

No permissions required - all players can use the basic commands.

## How It Works

1. **Daily Reset**: Streaks reset at midnight in the configured timezone
2. **Grace Period**: Players have 24 hours from their last login to maintain their streak
3. **Rewards**: Calculated as `streak_day * reward.increase` (capped by `reward.max`)
4. **Data Storage**: Player data stored in `plugins/LoginStreaks/playerdata/` as individual files

## Building from Source

1. Clone this repository
2. Ensure you have the required dependencies:
   - Bukkit API for Beta 1.7.3
   - Essentials plugin JAR (optional)
3. Compile using your preferred Java IDE or build tool
4. The compiled JAR will be ready for server deployment

## Contributing

Feel free to submit issues, fork the repository, and create pull requests for improvements.

## License

This project is open source. Feel free to use and modify as needed.

## Support

For support or questions, please create an issue on GitHub.
