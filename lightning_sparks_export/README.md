# Lightning Sparks Effect - Export for Infinity Stones Mod

This folder contains all the files needed to implement the lightning sparks visual effect from Palladium into your Infinity Stones mod.

## File Structure

### Core Implementation (`java/`)
- **renderlayer/LightningSparksRenderLayer.java** - Main lightning sparks renderer
- **renderlayer/PackRenderLayerManager.java** - Render layer management system
- **renderer/PalladiumRenderTypes.java** - Custom render types (LASER, LASER_NORMAL_TRANSPARENCY)

### Supporting Files (`supporting/`)
- **AbstractPackRenderLayer.java** - Base class for render layers
- **IPackRenderLayer.java** - Interface for render layers  
- **RenderLayerStates.java** - State management system
- **RenderUtil.java** - Utility methods for rendering

### Examples (`json/examples/`)
- **lightning_sparks_test.json** - Example render layer configuration
- **trail_test.json** - Example power using lightning sparks

## Usage for Infinity Stones

1. **Copy Java files** to your mod's source directory with appropriate package names
2. **Register the render layer** in your mod's client initialization:
   ```java
   PackRenderLayerManager.registerParser(new ResourceLocation(YourMod.MOD_ID, "lightning_sparks"), LightningSparksRenderLayer::parse);
   ```

3. **Create stone-specific configurations** in your assets folder:
   ```
   assets/your_mod/palladium/render_layers/
   ├── power_stone_sparks.json    (purple)
   ├── space_stone_sparks.json    (blue) 
   ├── reality_stone_sparks.json  (red)
   ├── soul_stone_sparks.json     (orange)
   ├── time_stone_sparks.json     (green)
   └── mind_stone_sparks.json     (yellow)
   ```

4. **Add to powers** using the render_layer ability type

## Color Suggestions for Infinity Stones
- **Power Stone**: `#9932CC` (purple core), `#DA70D6` (purple glow)
- **Space Stone**: `#0000FF` (blue core), `#87CEEB` (light blue glow)  
- **Reality Stone**: `#FF0000` (red core), `#FF6347` (red glow)
- **Soul Stone**: `#FF4500` (orange core), `#FFA500` (orange glow)
- **Time Stone**: `#00FF00` (green core), `#90EE90` (light green glow)
- **Mind Stone**: `#FFFF00` (yellow core), `#FFFFE0` (light yellow glow)