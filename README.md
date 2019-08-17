# PPS-18-scalacraft
A Minecraft backend server written in Scala

The protocol version taken into account for this project is 1.13.2 (404)

All minecraft packet are supported, exception made for compression and cryptography which aren't handled.
The server legacy ping is supported to achieve server listing. The old listing process is not supported.

## The server core feature are:
- World navigation, with dynamic chunks, loaded while player moves
- Block breaking 
- Player Inventory
- Spawning and map navigation by Animals and mobs
- Crafting (shapeless and shaped)
