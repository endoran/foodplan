package com.endoran.foodplan.service;

import com.endoran.foodplan.config.SharedMongoConfig.SharedMongoHolder;
import com.endoran.foodplan.dto.GlobalRecipeBookStatus;
import com.endoran.foodplan.dto.PinnedRecipeResponse;
import com.endoran.foodplan.dto.SharedRecipeIngredientResponse;
import com.endoran.foodplan.dto.SharedRecipeResponse;
import com.endoran.foodplan.model.PinnedRecipe;
import com.endoran.foodplan.model.Recipe;
import com.endoran.foodplan.model.SharedRecipe;
import com.endoran.foodplan.model.SharedRecipeIngredient;
import com.endoran.foodplan.repository.PinnedRecipeRepository;
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
import java.util.stream.Collectors;

@Service
public class GlobalRecipeService {

    private static final Logger log = LoggerFactory.getLogger(GlobalRecipeService.class);

    private final MongoTemplate sharedMongo;
    private final PinnedRecipeRepository pinnedRecipeRepository;
    private final RecipeRepository recipeRepository;
    private final boolean enabled;
    private final String instanceId;
    private final String instanceName;

    public GlobalRecipeService(
            SharedMongoHolder sharedMongoHolder,
            PinnedRecipeRepository pinnedRecipeRepository,
            RecipeRepository recipeRepository,
            @Qualifier("globalRecipeBookEnabled") boolean enabled,
            @Value("${foodplan.instance.id:}") String instanceId,
            @Value("${foodplan.instance.name:}") String instanceName) {
        this.sharedMongo = sharedMongoHolder.getTemplate();
        this.pinnedRecipeRepository = pinnedRecipeRepository;
        this.recipeRepository = recipeRepository;
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
        try {
            sharedMongo.getDb().runCommand(new org.bson.Document("ping", 1));
            return new GlobalRecipeBookStatus(true, true);
        } catch (Exception e) {
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

        pin = pinnedRecipeRepository.save(pin);
        log.info("Pinned shared recipe '{}' (version {})", pin.getName(), pin.getPinnedVersion());
        return toPinnedResponse(pin, shared.getVersion(), false);
    }

    public void unpin(String orgId, String pinnedId) {
        PinnedRecipe pin = pinnedRecipeRepository.findByIdAndOrgId(pinnedId, orgId)
                .orElseThrow(() -> new RecipeNotFoundException("Pinned recipe " + pinnedId));
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

    public boolean isRecipeShared(String orgId, String recipeId) {
        if (!isEnabled()) return false;
        try {
            Query query = Query.query(Criteria.where("sourceInstanceId").is(instanceId)
                    .and("sourceRecipeId").is(recipeId)
                    .and("sourceOrgId").is(orgId));
            return sharedMongo.exists(query, SharedRecipe.class);
        } catch (Exception e) {
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
        return local.getIngredients().stream()
                .map(ri -> new SharedRecipeIngredient(
                        ri.getIngredientName(),
                        ri.getMeasurement().getQuantity().doubleValue(),
                        ri.getMeasurement().getUnit().name(),
                        ri.getSection()))
                .toList();
    }

    private SharedRecipeResponse toSharedResponse(SharedRecipe shared) {
        List<SharedRecipeIngredientResponse> ingredients = shared.getIngredients().stream()
                .map(i -> new SharedRecipeIngredientResponse(
                        i.getIngredientName(), i.getQuantity(), i.getUnit(), i.getSection()))
                .toList();
        return new SharedRecipeResponse(
                shared.getId(), shared.getName(), shared.getInstructions(),
                shared.getBaseServings(), ingredients, shared.getAttribution(),
                shared.getSourceInstanceName(), shared.getVersion(),
                shared.getSharedAt(), shared.getUpdatedAt(),
                instanceId.equals(shared.getSourceInstanceId()));
    }

    private PinnedRecipeResponse toPinnedResponse(PinnedRecipe pin, Integer latestVersion, boolean sourceRemoved) {
        boolean hasUpdate = !sourceRemoved && latestVersion != null && latestVersion > pin.getPinnedVersion();
        List<SharedRecipeIngredientResponse> ingredients = pin.getIngredients().stream()
                .map(i -> new SharedRecipeIngredientResponse(
                        i.getIngredientName(), i.getQuantity(), i.getUnit(), i.getSection()))
                .toList();
        return new PinnedRecipeResponse(
                pin.getId(), pin.getSharedRecipeId(), pin.getName(),
                pin.getInstructions(), pin.getBaseServings(), ingredients,
                pin.getAttribution(), pin.getSourceInstanceName(),
                pin.getPinnedVersion(), hasUpdate, latestVersion,
                sourceRemoved, pin.getPinnedAt());
    }
}
