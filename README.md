# Spittoon - A Bukkit Plugin for Minecraft

A playground for plugin experimentation using Clojure.

Spittoon:
> a receptacle for spit, usually in a public place

It also accurately describes the intention for this plugin, where my oddball
plugin ideas are the spit, and the plugin is my "bukkit".

## Setup

1. Download and install [SpigotMC](http://www.spigotmc.org) - follow their [BuildTools guide](https://www.spigotmc.org/wiki/buildtools/)
2. Install [Boot](http://boot-clj.com/) (Clojure build tooling)
3. Clone this repo locally (we'll call it ``$SPITTOON_DIR`)
4. From your ``$SPITTOON_DIR`, run `boot build`
5. Copy `$HOME/.m2/repository/org/kowboy/spittoon/0.1.0/spittoon-0.1.0.jar` into the `plugins` directory of your Spigot server

Optional - instead of copying the JAR file, for local development you can create a symbolic link to the maven artifact
repository location. Note, Spigot doesn't like it when you modify the plugin JAR file while the server is running, so
rebuilding the JAR will require a restart of the Spigot server. I don't think Spigot even attempts hot reloading of
plugins, so I have no idea why it behaves this way.

## REPL

The best way to hack on this is to use the REPL. If you are writing a bunch of code, recompiling, and restarting Spigot,
then you are doing this Clojure thing all wrong! Watch the server console when it starts up. The Spittoon plugin will
log what port the REPL is running on:

```
[18:37:14 INFO]: [Spittoon] REPL started on port 60981.
```

To add new commands or listeners, you will have to rebuild the JAR. Sad face.

## TODO

The REPL currently starts on a random port. It should use a default port to simplify connection. Optionally, it could
look in `plugins/Spittoon/config.yml` for a port number configuration setting.

## License

Copyright © 2017 Craig McDaniel

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
