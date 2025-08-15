package net.threetag.palladium.client.renderer.renderlayer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.Palladium;
import net.threetag.palladium.addonpack.log.AddonPackLog;
import net.threetag.palladium.addonpack.parser.AddonParser;
import net.threetag.palladium.client.renderer.PalladiumRenderTypes;
import net.threetag.palladium.client.renderer.item.armor.ArmorRendererData;
import net.threetag.palladium.entity.BodyPart;
import net.threetag.palladium.item.ArmorWithRenderer;
import net.threetag.palladium.item.IAddonItem;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;
import net.threetag.palladium.power.ability.RenderLayerProviderAbility;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.json.GsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PackRenderLayerManager extends SimpleJsonResourceReloadListener {

    private static PackRenderLayerManager INSTANCE;
    private static final List<Provider> RENDER_LAYERS_PROVIDERS = new ArrayList<>();
    private static final Map<ResourceLocation, Function<JsonObject, IPackRenderLayer>> RENDER_LAYERS_PARSERS = new HashMap<>();
    private static final Map<ResourceLocation, RenderTypeFunction> RENDER_TYPES = new HashMap<>();
    private Map<ResourceLocation, IPackRenderLayer> renderLayers = new HashMap<>();

    static {
        // Abilities
        registerProvider((entity, layers) -> {
            if (entity instanceof LivingEntity livingEntity) {
                var manager = PackRenderLayerManager.getInstance();
                for (AbilityInstance entry : AbilityUtil.getEnabledRenderLayerInstances(livingEntity)) {
                    IPackRenderLayer layer = ((RenderLayerProviderAbility) entry.getConfiguration().getAbility()).getRenderLayer(entry, livingEntity, manager);
                    if (layer != null) {
                        layers.accept(DataContext.forAbility(livingEntity, entry), layer);
                    }
                }
            }
        });

        // Armor
        registerProvider((entity, layers) -> {
            if (entity instanceof LivingEntity livingEntity) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    var stack = livingEntity.getItemBySlot(slot);

                    if (!stack.isEmpty()) {
                        var context = DataContext.forArmorInSlot(livingEntity, slot);

                        if (stack.getItem() instanceof IAddonItem addonItem && addonItem.getRenderLayerContainer() != null) {
                            var container = addonItem.getRenderLayerContainer();

                            for (ResourceLocation id : container.get(slot.getName())) {
                                IPackRenderLayer layer = PackRenderLayerManager.getInstance().getLayer(id);

                                if (layer != null) {
                                    layers.accept(context, layer);
                                }
                            }
                        }

                        if (slot.isArmor() && stack.getItem() instanceof ArmorWithRenderer armorWithRenderer && armorWithRenderer.getCachedArmorRenderer() instanceof ArmorRendererData renderer) {
                            for (IPackRenderLayer layer : renderer.getRenderLayers()) {
                                layers.accept(context, layer);
                            }
                        }
                    }
                }
            }
        });

        registerParser(new ResourceLocation(Palladium.MOD_ID, "default"), PackRenderLayer::parse);
        registerParser(new ResourceLocation(Palladium.MOD_ID, "compound"), CompoundPackRenderLayer::parse);
        registerParser(new ResourceLocation(Palladium.MOD_ID, "skin_overlay"), SkinOverlayPackRenderLayer::parse);
        registerParser(new ResourceLocation(Palladium.MOD_ID, "lightning_sparks"), LightningSparksRenderLayer::parse);
        registerParser(new ResourceLocation(Palladium.MOD_ID, "thrusters"), ThrusterPackRenderLayer::parse);

        registerRenderType(new ResourceLocation("minecraft", "solid"), (source, texture, glint) -> ItemRenderer.getArmorFoilBuffer(source, RenderType.entityTranslucent(texture), false, glint));
        registerRenderType(new ResourceLocation("minecraft", "cutout"), (source, texture, glint) -> ItemRenderer.getArmorFoilBuffer(source, RenderType.entityCutout(texture), false, glint));
        registerRenderType(new ResourceLocation("minecraft", "glow"), new RenderTypeFunction() {
            @Override
            public VertexConsumer createVertexConsumer(MultiBufferSource buffer, ResourceLocation texture, boolean withGlint) {
                return ItemRenderer.getArmorFoilBuffer(buffer, PalladiumRenderTypes.getGlowing(texture), false, withGlint);
            }

            @Override
            public int getPackedLight(int packedLight) {
                return 15728640;
            }
        });
    }

    public PackRenderLayerManager() {
        super(AddonParser.GSON, "palladium/render_layers");
        INSTANCE = this;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.renderLayers.values().forEach(IPackRenderLayer::onUnload);

        ImmutableMap.Builder<ResourceLocation, IPackRenderLayer> builder = ImmutableMap.builder();

        object.forEach((resourceLocation, jsonElement) -> {
            try {
                IPackRenderLayer layer = parseLayer(GsonHelper.convertToJsonObject(jsonElement, "$"));
                builder.put(resourceLocation, layer);
            } catch (Exception e) {
                AddonPackLog.error("Parsing error loading render layer {}", resourceLocation, e);
            }
        });

        this.renderLayers = builder.build();
        this.renderLayers.values().forEach(IPackRenderLayer::onLoad);
    }

    public IPackRenderLayer getLayer(ResourceLocation id) {
        return this.renderLayers.get(id);
    }

    public static IPackRenderLayer parseLayer(JsonObject json) {
        ResourceLocation parserId = GsonUtil.getAsResourceLocation(json, "type", new ResourceLocation(Palladium.MOD_ID, "default"));

        if (!RENDER_LAYERS_PARSERS.containsKey(parserId)) {
            throw new JsonParseException("Unknown render layer type '" + parserId + "'");
        }

        var layer = IPackRenderLayer.parseConditions(RENDER_LAYERS_PARSERS.get(parserId).apply(json), json);

        if (layer instanceof AbstractPackRenderLayer abstractPackRenderLayer) {
            GsonUtil.ifHasKey(json, "hidden_body_parts", el -> {
                if (el.isJsonPrimitive()) {
                    var string = el.getAsString();
                    if (string.equalsIgnoreCase("all")) {
                        for (BodyPart bodyPart : BodyPart.values()) {
                            abstractPackRenderLayer.addHiddenBodyPart(bodyPart);
                        }
                    } else {
                        abstractPackRenderLayer.addHiddenBodyPart(BodyPart.fromJson(string));
                    }
                } else if (el.isJsonArray()) {
                    JsonArray jsonArray = el.getAsJsonArray();
                    for (JsonElement jsonElement : jsonArray) {
                        abstractPackRenderLayer.addHiddenBodyPart(BodyPart.fromJson(jsonElement.getAsString()));
                    }
                } else {
                    throw new JsonParseException("hidden_body_parts setting must either be a string or an array");
                }
            });
        }

        return layer;
    }

    public static PackRenderLayerManager getInstance() {
        return INSTANCE;
    }

    public static void registerProvider(Provider provider) {
        RENDER_LAYERS_PROVIDERS.add(provider);
    }

    public static void registerParser(ResourceLocation id, Function<JsonObject, IPackRenderLayer> function) {
        RENDER_LAYERS_PARSERS.put(id, function);
    }

    public static void registerRenderType(ResourceLocation id, RenderTypeFunction function) {
        RENDER_TYPES.put(id, function);
    }

    public static RenderTypeFunction getRenderType(ResourceLocation id) {
        return RENDER_TYPES.get(id);
    }

    public static void forEachLayer(Entity entity, BiConsumer<DataContext, IPackRenderLayer> consumer) {
        for (Provider provider : RENDER_LAYERS_PROVIDERS) {
            provider.addRenderLayers(entity, consumer);
        }
    }

    public interface Provider {

        void addRenderLayers(Entity entity, BiConsumer<DataContext, IPackRenderLayer> layers);

    }

}
