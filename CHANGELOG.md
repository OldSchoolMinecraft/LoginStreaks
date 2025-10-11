# Changelog

All notable changes to the LoginStreaks project will be documented in this file.

## [1.0.0] - 2025-10-12

### Added
- Initial release of LoginStreaks plugin for Minecraft Beta 1.7.3
- Daily login streak tracking system
- Player data storage with current and longest streak records
- Configurable reward system with increasing payouts
- Essential commands:
  - `/loginstreak` - View personal streak information
  - `/loginstreak <player>` - View other players' streaks
  - `/loginstreak top` - Top 10 longest streaks leaderboard
  - `/loginstreak help` - Command help system
- Essentials plugin integration for economy rewards
- Timezone configuration support
- MySQL database support (optional)
- File-based player data storage (default)
- Customizable player messages and rewards
- Automatic streak expiration after 24 hours
- Longest streak tracking and display

### Features
- **Streak Management**: Tracks both current active streaks and personal best longest streaks
- **Reward Calculation**: Configurable base reward that increases with streak length
- **Time Zone Support**: Proper handling of different server time zones for daily resets
- **Data Persistence**: Multiple storage options (files or MySQL database)
- **Player Commands**: Comprehensive command system for viewing streak information
- **Leaderboard**: Top streaks ranking system with colored display
- **Economy Integration**: Optional Essentials plugin support for monetary rewards

### Technical Details
- Compatible with Bukkit Beta 1.7.3
- Java 6+ support
- Modular design with separate configuration classes
- Efficient caching system for player data
- Proper error handling and logging
