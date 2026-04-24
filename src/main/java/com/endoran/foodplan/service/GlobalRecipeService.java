package com.endoran.foodplan.service;

import com.endoran.foodplan.config.SharedMongoConfig.SharedMongoHolder;
import com.endoran.foodplan.dto.GlobalRecipeBookStatus;
import com.endoran.foodplan.dto.PinnedRecipeResponse;
import com.endoran.foodplan.dto.SharedRecipeIngredientResponse;
import com.endoran.foodplan.dto.RecipeIngredientRequest;
import com.endoran.foodplan.dto.RecipeIngredientResponse;
import com.endoran.foodplan.dto.RecipeResponse;
import com.endoran.foodplan.dto.SharedRecipeResponse;
import com.endoran.foodplan.model.PinnedRecipe;
import com.endoran.foodplan.model.Recipe;
import com.endoran.foodplan.model.SharedRecipe;
import com.endoran.foodplan.model.Measurement;
import com.endoran.foodplan.model.MeasurementUnit;
import com.endoran.foodplan.model.Recipe;
import com.endoran.foodplan.model.SharedRecipeIngredient;
import com.endoran.foodplan.repository.MealPlanEntryRepository;
import com.endoran.foodplan.repository.PinnedRecipeRepository;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class GlobalRecipeService {

    private static final Logger log = LoggerFactory.getLogger(GlobalRecipeService.class);

    private final MongoTemplate sharedMongo;
    private final PinnedRecipeRepository pinnedRecipeRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final MealPlanEntryRepository mealPlanEntryRepository;
    private final IngredientLinkerService ingredientLinkerService;
    private final boolean enabled;
    private final String instanceId;
    private final String instanceName;

    // Circuit breaker: skip shared-mongo calls for 60s after a failure
    private final AtomicLong sharedMongoDownUntil = new AtomicLong(0);
    private static final long CIRCUIT_BREAKER_MS = 60_000;

    public GlobalRecipeService(
            SharedMongoHolder sharedMongoHolder,
            PinnedRecipeRepository pinnedRecipeRepository,
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            MealPlanEntryRepository mealPlanEntryRepository,
            IngredientLinkerService ingredientLinkerService,
            @Qualifier("globalRecipeBookEnabled") boolean enabled,
            @Value("${foodplan.instance.id:}") String instanceId,
            @Value("${foodplan.instance.name:}") String instanceName) {
        this.sharedMongo = sharedMongoHolder.getTemplate();
        this.pinnedRecipeRepository = pinnedRecipeRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.mealPlanEntryRepository = mealPlanEntryRepository;
        this.ingredientLinkerService = ingredientLinkerService;
        this.enabled = enabled;
        this.instanceId = instanceId;
        this.instanceName = instanceName;
    }

    public boolean isEnabled() {
        return enabled && sharedMongo != null;
    }

    public GlobalRecipeBookStatus status() {
        if (!isEnabled()) {
            return new GlobalRecipeBookStatus(false, false);
        }
        if (!isSharedMongoReachable()) {
            return new GlobalRecipeBookStatus(true, false);
        }
        try {
            sharedMongo.getDb().runCommand(new org.bson.Document("ping", 1));
            return new GlobalRecipeBookStatus(true, true);
        } catch (Exception e) {
            markSharedMongoDown();
            log.warn("Shared MongoDB unreachable: {}", e.getMessage());
            return new GlobalRecipeBookStatus(true, false);
        }
    }

    public SharedRecipeResponse share(String orgId, String recipeId) {
        Recipe local = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException(recipeId));
        if (!orgId.equals(local.getOrgId())) {
            throw new RecipeNotFoundException(recipeId);
        }

        Query query = Query.query(Criteria.where("sourceInstanceId").is(instanceId)
                .and("sourceRecipeId").is(recipeId));
        SharedRecipe existing = sharedMongo.findOne(query, SharedRecipe.class);

        SharedRecipe shared;
        if (existing != null) {
            shared = existing;
            shared.setVersion(shared.getVersion() + 1);
            shared.setUpdatedAt(Instant.now());
        } else {
            shared = new SharedRecipe();
            shared.setSourceInstanceId(instanceId);
            shared.setSourceInstanceName(instanceName);
            shared.setSourceOrgId(orgId);
            shared.setSourceRecipeId(recipeId);
            shared.setVersion(1);
            shared.setSharedAt(Instant.now());
            shared.setUpdatedAt(Instant.now());
        }

        shared.setName(local.getName());
        shared.setInstructions(local.getInstructions());
        shared.setBaseServings(local.getBaseServings());
        shared.setIngredients(toSharedIngredients(local));
        shared.setAttribution(instanceName);

        shared = sharedMongo.save(shared);
        log.info("Shared recipe '{}' to global book (version {})", shared.getName(), shared.getVersion());
        return toSharedResponse(shared);
    }

    public void unshare(String orgId, String recipeId) {
        Query query = Query.query(Criteria.where("sourceInstanceId").is(instanceId)
                .and("sourceRecipeId").is(recipeId)
                .and("sourceOrgId").is(orgId));
        SharedRecipe existing = sharedMongo.findOne(query, SharedRecipe.class);
        if (existing == null) {
            throw new RecipeNotFoundException("Shared recipe not found for local recipe " + recipeId);
        }
        sharedMongo.remove(existing);
        log.info("Unshared recipe '{}' from global book", existing.getName());
    }

    public List<SharedRecipeResponse> myShares(String orgId) {
        Query query = Query.query(Criteria.where("sourceInstanceId").is(instanceId)
                .and("sourceOrgId").is(orgId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        return sharedMongo.find(query, SharedRecipe.class).stream()
                .map(this::toSharedResponse)
                .toList();
    }

    public List<SharedRecipeResponse> browse(String search, int page, int size) {
        Query query = new Query();
        if (search != null && !search.isBlank()) {
            query.addCriteria(Criteria.where("name").regex(search, "i"));
        }
        query.with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        query.with(PageRequest.of(page, size));
        return sharedMongo.find(query, SharedRecipe.class).stream()
                .map(this::toSharedResponse)
                .toList();
    }

    public SharedRecipeResponse getShared(String sharedId) {
        SharedRecipe shared = sharedMongo.findById(sharedId, SharedRecipe.class);
        if (shared == null) {
            throw new RecipeNotFoundException("Shared recipe " + sharedId);
        }
        return toSharedResponse(shared);
    }

    public PinnedRecipeResponse pin(String orgId, String sharedId) {
        pinnedRecipeRepository.findByOrgIdAndSharedRecipeId(orgId, sharedId)
                .ifPresent(p -> {
                    throw new IllegalStateException("Recipe already pinned");
                });

        SharedRecipe shared = sharedMongo.findById(sharedId, SharedRecipe.class);
        if (shared == null) {
            throw new RecipeNotFoundException("Shared recipe " + sharedId);
        }

        PinnedRecipe pin = new PinnedRecipe();
        pin.setOrgId(orgId);
        pin.setSharedRecipeId(sharedId);
        pin.setPinnedVersion(shared.getVersion());
        pin.setName(shared.getName());
        pin.setInstructions(shared.getInstructions());
        pin.setBaseServings(shared.getBaseServings());
        pin.setIngredients(new ArrayList<>(shared.getIngredients()));
        pin.setSourceInstanceName(shared.getSourceInstanceName());
        pin.setAttribution(shared.getAttribution());
        pin.setPinnedAt(Instant.now());

        ingredientLinkerService.linkSharedIngredients(orgId, pin.getIngredients());

        pin = pinnedRecipeRepository.save(pin);
        log.info("Pinned shared recipe '{}' (version {})", pin.getName(), pin.getPinnedVersion());
        return toPinnedResponse(pin, shared.getVersion(), false);
    }

    public int unpinCalendarCount(String orgId, String pinnedId) {
        pinnedRecipeRepository.findByIdAndOrgId(pinnedId, orgId)
                .orElseThrow(() -> new RecipeNotFoundException("Pinned recipe " + pinnedId));
        return mealPlanEntryRepository.findByOrgIdAndPinnedId(orgId, pinnedId).size();
    }

    public void unpin(String orgId, String pinnedId, boolean cascade) {
        PinnedRecipe pin = pinnedRecipeRepository.findByIdAndOrgId(pinnedId, orgId)
                .orElseThrow(() -> new RecipeNotFoundException("Pinned recipe " + pinnedId));
        if (cascade) {
            mealPlanEntryRepository.deleteByOrgIdAndPinnedId(orgId, pinnedId);
            log.info("Cascade-deleted meal plan entries for pinned recipe '{}'", pin.getName());
        }
        pinnedRecipeRepository.delete(pin);
        log.info("Unpinned recipe '{}'", pin.getName());
    }

    public PinnedRecipeResponse acceptUpdate(String orgId, String pinnedId) {
        PinnedRecipe pin = pinnedRecipeRepository.findByIdAndOrgId(pinnedId, orgId)
                .orElseThrow(() -> new RecipeNotFoundException("Pinned recipe " + pinnedId));

        SharedRecipe shared = sharedMongo.findById(pin.getSharedRecipeId(), SharedRecipe.class);
        if (shared == null) {
            throw new RecipeNotFoundException("Source recipe no longer available");
        }

        pin.setPinnedVersion(shared.getVersion());
        pin.setName(shared.getName());
        pin.setInstructions(shared.getInstructions());
        pin.setBaseServings(shared.getBaseServings());
        pin.setIngredients(new ArrayList<>(shared.getIngredients()));

        ingredientLinkerService.linkSharedIngredients(orgId, pin.getIngredients());

        pin = pinnedRecipeRepository.save(pin);
        log.info("Accepted update for pinned recipe '{}' (now version {})", pin.getName(), pin.getPinnedVersion());
        return toPinnedResponse(pin, shared.getVersion(), false);
    }

    public List<PinnedRecipeResponse> listPins(String orgId) {
        List<PinnedRecipe> pins = pinnedRecipeRepository.findByOrgId(orgId);
        if (pins.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, SharedRecipe> sharedVersions = fetchSharedVersions(pins);

        return pins.stream()
                .map(pin -> {
                    SharedRecipe shared = sharedVersions.get(pin.getSharedRecipeId());
                    boolean sourceRemoved = shared == null;
                    Integer latestVersion = shared != null ? shared.getVersion() : null;
                    return toPinnedResponse(pin, latestVersion, sourceRemoved);
                })
                .toList();
    }

    public RecipeResponse copyAsOwn(String orgId, String pinnedId) {
        PinnedRecipe pin = pinnedRecipeRepository.findByIdAndOrgId(pinnedId, orgId)
                .orElseThrow(() -> new RecipeNotFoundException("Pinned recipe " + pinnedId));

        Recipe recipe = new Recipe();
        recipe.setOrgId(orgId);
        recipe.setName(pin.getName());
        recipe.setInstructions(pin.getInstructions());
        recipe.setBaseServings(pin.getBaseServings());
        recipe.setIngredients(pin.getIngredients().stream()
                .map(si -> {
                    var ri = new com.endoran.foodplan.model.RecipeIngredient(
                            si.getSection(),
                            null,
                            si.getIngredientName(),
                            new Measurement(
                                    java.math.BigDecimal.valueOf(si.getQuantity()),
                                    MeasurementUnit.valueOf(si.getUnit())));
                    return ri;
                })
                .toList());

        // Build source metadata map from pinned ingredients (keyed by lowercase name)
        Map<String, SharedRecipeIngredient> sourceMetadata = pin.getIngredients().stream()
                .collect(Collectors.toMap(
                        si -> si.getIngredientName().toLowerCase(),
                        si -> si,
                        (a, b) -> a));
        autoCreateIngredients(orgId, recipe.getIngredients(), sourceMetadata);
        recipe = recipeRepository.save(recipe);
        pinnedRecipeRepository.delete(pin);
        log.info("Copied pinned recipe '{}' as local recipe '{}'", pin.getName(), recipe.getId());

        List<RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(ri -> new RecipeIngredientResponse(
                        ri.getSection(), ri.getIngredientId(), ri.getIngredientName(),
                        ri.getMeasurement().getQuantity(), ri.getMeasurement().getUnit()))
                .toList();

        return new RecipeResponse(
                recipe.getId(), recipe.getName(), recipe.getInstructions(),
                recipe.getBaseServings(), recipe.getBaseServings(),
                ingredients, false,
                RecipeDietaryLabels.compute(recipe.getIngredients().stream()
                        .map(ri -> ri.getIngredientName()).toList()));
    }

    private void autoCreateIngredients(String orgId, List<com.endoran.foodplan.model.RecipeIngredient> ingredients,
                                       Map<String, SharedRecipeIngredient> sourceMetadata) {
        for (var ri : ingredients) {
            if (ri.getIngredientId() != null && !ri.getIngredientId().isBlank()) continue;
            ri.setIngredientName(IngredientAliasDictionary.resolveAndNormalize(ri.getIngredientName()));
            var existing = ingredientRepository.findByOrgIdAndNameIgnoreCase(orgId, ri.getIngredientName());
            if (existing.isPresent()) {
                ri.setIngredientId(existing.get().getId());
            } else {
                Ingredient newIng = new Ingredient();
                newIng.setOrgId(orgId);
                newIng.setName(ri.getIngredientName());

                SharedRecipeIngredient src = sourceMetadata != null
                        ? sourceMetadata.get(ri.getIngredientName().toLowerCase())
                        : null;

                if (src != null && src.getGroceryCategory() != null) {
                    try {
                        newIng.setGroceryCategory(com.endoran.foodplan.model.GroceryCategory.valueOf(src.getGroceryCategory()));
                    } catch (IllegalArgumentException e) {
                        newIng.setGroceryCategory(IngredientCategoryInference.infer(ri.getIngredientName()).grocery());
                    }
                } else {
                    newIng.setGroceryCategory(IngredientCategoryInference.infer(ri.getIngredientName()).grocery());
                }

                if (src != null && src.getStorageCategory() != null) {
                    try {
                        newIng.setStorageCategory(com.endoran.foodplan.model.StorageCategory.valueOf(src.getStorageCategory()));
                    } catch (IllegalArgumentException e) {
                        newIng.setStorageCategory(IngredientCategoryInference.infer(ri.getIngredientName()).storage());
                    }
                } else {
                    newIng.setStorageCategory(IngredientCategoryInference.infer(ri.getIngredientName()).storage());
                }

                if (src != null && src.getDietaryTags() != null && !src.getDietaryTags().isEmpty()) {
                    java.util.Set<com.endoran.foodplan.model.DietaryTag> tags = new java.util.HashSet<>();
                    for (String tagName : src.getDietaryTags()) {
                        try {
                            tags.add(com.endoran.foodplan.model.DietaryTag.valueOf(tagName));
                        } catch (IllegalArgumentException e) {
                            // skip unknown tags
                        }
                    }
                    newIng.setDietaryTags(tags);
                }

                newIng.setNeedsReview(false);
                newIng = ingredientRepository.save(newIng);
                ri.setIngredientId(newIng.getId());
            }
        }
    }

    private boolean isSharedMongoReachable() {
        return System.currentTimeMillis() >= sharedMongoDownUntil.get();
    }

    private void markSharedMongoDown() {
        sharedMongoDownUntil.set(System.currentTimeMillis() + CIRCUIT_BREAKER_MS);
        log.warn("Shared MongoDB circuit breaker open for {}s", CIRCUIT_BREAKER_MS / 1000);
    }

    public boolean isRecipeShared(String orgId, String recipeId) {
        if (!isEnabled() || !isSharedMongoReachable()) return false;
        try {
            Query query = Query.query(Criteria.where("sourceInstanceId").is(instanceId)
                    .and("sourceRecipeId").is(recipeId)
                    .and("sourceOrgId").is(orgId));
            return sharedMongo.exists(query, SharedRecipe.class);
        } catch (Exception e) {
            markSharedMongoDown();
            return false;
        }
    }

    private Map<String, SharedRecipe> fetchSharedVersions(List<PinnedRecipe> pins) {
        try {
            Set<String> sharedIds = pins.stream()
                    .map(PinnedRecipe::getSharedRecipeId)
                    .collect(Collectors.toSet());
            Query query = Query.query(Criteria.where("_id").in(sharedIds));
            query.fields().include("_id").include("version");
            return sharedMongo.find(query, SharedRecipe.class).stream()
                    .collect(Collectors.toMap(SharedRecipe::getId, Function.identity()));
        } catch (Exception e) {
            log.warn("Failed to fetch shared recipe versions: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<SharedRecipeIngredient> toSharedIngredients(Recipe local) {
        // Build a map of ingredientId -> Ingredient for all ingredients in this recipe
        List<String> ingredientIds = local.getIngredients().stream()
                .map(ri -> ri.getIngredientId())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        Map<String, Ingredient> ingredientMap = new java.util.HashMap<>();
        if (!ingredientIds.isEmpty()) {
            ingredientRepository.findAllById(ingredientIds).forEach(ing ->
                    ingredientMap.put(ing.getId(), ing));
        }

        return local.getIngredients().stream()
                .map(ri -> {
                    Ingredient ing = ingredientMap.get(ri.getIngredientId());
                    Set<String> tags = Collections.emptySet();
                    String grocery = null;
                    String storage = null;
                    if (ing != null) {
                        if (ing.getDietaryTags() != null && !ing.getDietaryTags().isEmpty()) {
                            tags = ing.getDietaryTags().stream()
                                    .map(Enum::name)
                                    .collect(Collectors.toSet());
                        }
                        if (ing.getGroceryCategory() != null) {
                            grocery = ing.getGroceryCategory().name();
                        }
                        if (ing.getStorageCategory() != null) {
                            storage = ing.getStorageCategory().name();
                        }
                    }
                    return new SharedRecipeIngredient(
                            ri.getIngredientName(),
                            ri.getMeasurement().getQuantity().doubleValue(),
                            ri.getMeasurement().getUnit().name(),
                            ri.getSection(),
                            tags, grocery, storage);
                })
                .toList();
    }

    private SharedRecipeResponse toSharedResponse(SharedRecipe shared) {
        List<SharedRecipeIngredientResponse> ingredients = shared.getIngredients().stream()
                .map(i -> new SharedRecipeIngredientResponse(
                        i.getIngredientName(), i.getQuantity(), i.getUnit(), i.getSection(),
                        i.getDietaryTags(), i.getIngredientId(),
                        i.getGroceryCategory(), i.getStorageCategory()))
                .toList();
        return new SharedRecipeResponse(
                shared.getId(), shared.getName(), shared.getInstructions(),
                shared.getBaseServings(), ingredients, shared.getAttribution(),
                shared.getSourceInstanceName(), shared.getVersion(),
                shared.getSharedAt(), shared.getUpdatedAt(),
                instanceId.equals(shared.getSourceInstanceId()),
                RecipeDietaryLabels.compute(shared.getIngredients().stream()
                        .map(i -> i.getIngredientName()).toList()));
    }

    private PinnedRecipeResponse toPinnedResponse(PinnedRecipe pin, Integer latestVersion, boolean sourceRemoved) {
        boolean hasUpdate = !sourceRemoved && latestVersion != null && latestVersion > pin.getPinnedVersion();
        List<SharedRecipeIngredientResponse> ingredients = pin.getIngredients().stream()
                .map(i -> new SharedRecipeIngredientResponse(
                        i.getIngredientName(), i.getQuantity(), i.getUnit(), i.getSection(),
                        i.getDietaryTags(), i.getIngredientId(),
                        i.getGroceryCategory(), i.getStorageCategory()))
                .toList();
        return new PinnedRecipeResponse(
                pin.getId(), pin.getSharedRecipeId(), pin.getName(),
                pin.getInstructions(), pin.getBaseServings(), ingredients,
                pin.getAttribution(), pin.getSourceInstanceName(),
                pin.getPinnedVersion(), hasUpdate, latestVersion,
                sourceRemoved, pin.getPinnedAt(),
                RecipeDietaryLabels.compute(pin.getIngredients().stream()
                        .map(i -> i.getIngredientName()).toList()));
    }
}
