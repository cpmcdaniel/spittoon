# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of 
[keepachangelog.com](http://keepachangelog.com/).

## [0.1.0] - 2017-10-23
### Added
- Initial release simply starts a Clojure REPL on port 7788.

## [1.0] - 2019-12-22
### Removed
- All clojure project files.
- No more REPL, as this will be deployed to a server in the public cloud.
- Many of the "toy" plugins have been removed.

### Changed
- Converted project to pure Java targeting Java 11.
- Changed from using [Spigot](https://spigotmc.org) API to [Paper](https://papermc.io) API.

### Added
- Implemented light-level and find commands (and their sub-commands).
- Implemented permission filtering.
- Better colorized output for improved readability.

## [1.1] - 2020-01-31
### Changed
- Converted from Maven build to Gradle.
- Migrating from Java to Kotlin

## [1.2] - 2020-02-06
### Added
- Glowing effect has been added to entities found via `find entity`.

## [1.3] - unreleased
### Added
- Glowing effect for `find entities` command.
- New command to exclude entities from `find entities`.
- Light-level now works when _any_ light-source item is held.
### Changed
- Renamed `block-filter` config setting to `block-whitelist`