
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
   - By default only check if two blocks are direct neighbour, mod makers can implement `IBlockAnchor` on their blocks to define their own block connections.
- Teleport API
   - Teleport valkyrien ships to another dimension with conserved velocity and omega.
   - Teleport an entity and its passengers all together to another dimension, with extra mod compatibility.

## How to Use

Add the maven repo:
```gradle
maven {
	name = "LiterMC maven"
	url = "https://litermc.github.io/maven/"
	content {
		includeGroupAndSubgroups("com.github.litermc")
	}
}
```

**Fabric**:
```gradle
modImplementation "com.github.litermc.vtil:vtil-fabric-${minecraft_version}:${vtil_version}"
```

**Forge**:
```gradle
implementation fg.deobf("com.github.litermc.vtil:vtil-forge-${minecraft_version}:${vtil_version}")
```
a