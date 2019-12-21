# Spittoon - A Bukkit Plugin for Minecraft
_Spittoon_
> a receptacle for spit, usually in a public place

A veritable spit bucket of ideas and utilities for [Bukkit](https://bukkit.org) servers (should also work on derivatives 
like [Spigot MC](https://spigotmc.org) and [Paper MC](https://papermc.io)).

___Requires Java 11.___

## Features

#### Light Level
Ever have the need to light up caves and other areas in order to prevent mobs from spawning? This feature allows you 
to see which blocks are not "lit up enough" to prevent mob spawning. This helps tremendously with efficient placement of
light sources.

__To activate the visual, the player needs to be sneaking and holding a torch in either hand.__

[ADD SCREENSHOT]

#### Find Things
Ever been really frustrated by bad luck mining for diamonds? While exploration is a key part of the game, no one wants 
to waste days looking for something specific. If you want to find something fast, this is the feature for you!

The current list of things you can find with this feature includes:
* __Biomes__ - The search range is limited because this can be very expensive for rare biomes. Be kind to your server.
* __Entities__ - Specifically living entities (excluding players).
* __Blocks__ - Find the nearest blocks of a certain type, or all mineral veins nearby.
* __Slime Chunks__ - Find slime chunks near the player.

## Commands
* __/spit light-level on__ - Turns on the light-level feature.
* __/spit light-level off__ - Turns the light-level feature off.
* __/spit light-level apothem &lt;int>__ - Sets the apothem<sup>[1](#apothem)</sup> used by the light level effect.
* __/spit find biome &lt;biome-type>__ - Searches nearby chunks for the given biome.
* __/spit find block &lt;block-type> [apothem]__<sup>[1](#apothem)</sup> - Searches for nearby veins of the given 
    block type.
* __/spit find blocks [apothem]__<sup>[1](#apothem)</sup> - Searches for nearby veins of a configured set of block 
    types.
* __/spit find entities__ - Finds the nearest entity of each type for all living entities currently spawned.
* __/spit find entity &lt;entity-type> [apothem]__<sup>[1](#apothem)</sup> - Searches for nearby entities of the given
    type.
* __/spit find slime [apothem]__<sup>[1](#apothem)</sup> - Finds nearby slime chunks.

<a name="apothem"><sup>1</sup></a>: ___Apothem___ - The perpendicular line or distance from the center of a regular
polygon (square) to any of its sides. You might think of this as the ___radius___, but technically that is the 
distance from the center to any of the corners.

## Setup

1. Install a Bukkit server. I recommend [Paper MC](https://papermc.io).
2. Grab the plugin JAR from [insert URL here later].
3. Place the JAR file in the _plugins_ directory of your server.

## License

Copyright © 2017 Craig McDaniel

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
