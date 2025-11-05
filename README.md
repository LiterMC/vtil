
# Vtil


[![Modrinth Downloads](https://img.shields.io/modrinth/dt/vtil?color=4&label=Downloads&logo=modrinth)](https://modrinth.com/mod/vtil/versions)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/vtil.svg)](https://www.curseforge.com/minecraft/mc-mods/vtil/files/all)

Vtil is a library mod that provides multiple useful APIs related to VS.

Current API includes:

- Assemble API
   - Assemble ships with extra mod compatibility (e.g. assemble Create contraption)
   - `/vtil assemble` command
- Connectivity API
   - Provides extra information about block connectivity on ships, which can then be used in Assembly and/or ship split.
   - By default wraps vanilla `canSurvive`, mod makers can implement `IBlockAnchor` and define their own block connection.
- Teleport API
   - Teleport valkyrien ships to another dimension with conserved velocity and omega.
   - Teleport an entity and its passengers all together to another dimension, with extra mod compatibility.
