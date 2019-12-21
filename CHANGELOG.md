# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of 
[keepachangelog.com](http://keepachangelog.com/).

## [0.1.0] - 2017-10-23
### Added
- Initial release simply starts a Clojure REPL on port 7788.

## [1.0-SNAPSHOT unreleased]
### Removed
- All clojure project files.
- No more REPL, as this will be deployed to a server in the public cloud.
- Many of the "toy" plugins have been removed.

### Changed
- Converted project to pure Java targeting Java 11.
- Changed from using [Spigot](https://spigotmc.org) API to [Paper](https://papermc.io) API.

### Added
- Implemented light-level and find commands (and their sub-commands).

