package gregtech.api.util;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;

public class RecipeLFUCache
{
    private final int capacity;
    private int cachedRecipeCount = 0;

    // Store actual key-value pairs
    private final Recipe[] recipeCaches;

    // Store frequency of each key
    private final Map<Integer, Integer> frequencyMap;

    // TreeMap for efficient frequency tracking
    private final TreeMap<Integer, LinkedHashSet<Integer>> frequencyCounter;

    private int cacheHit = 0;
    private int cacheMiss = 0;

    public RecipeLFUCache(int capacity) {
        this.capacity = capacity;
        this.recipeCaches = new Recipe[capacity];
        this.frequencyMap = new HashMap<>();
        this.frequencyCounter = new TreeMap<>();
    }

    public int getCachedRecipeCount() {
        return this.cachedRecipeCount;
    }

    public int getCacheHit() {
        return this.cacheHit;
    }

    public int getCacheMiss() {
        return this.cacheMiss;
    }

    public Recipe get(IItemHandlerModifiable inputItems, IMultipleTankHandler inputFluids) {
        NavigableMap<Integer, LinkedHashSet<Integer>> descendingFrequencyMap = frequencyCounter.descendingMap();
        Recipe foundRecipe = null;
        int recipeCacheIndex = 0;
        for(LinkedHashSet<Integer> cacheIndexes: descendingFrequencyMap.values()) {
            for (Integer cacheIndex: cacheIndexes) {
                Recipe recipeCache = recipeCaches[cacheIndex];
                if (recipeCache == null) {
                    continue;
                }
                boolean foundMatches = recipeCache.matches(false, inputItems, inputFluids);
                if (foundMatches) {
                    foundRecipe = recipeCache;
                    recipeCacheIndex = cacheIndex;
                    break;
                }
            }
        }

        if (foundRecipe == null) {
            this.cacheMiss++;
            return null;
        }

        // Update frequency
        int frequency = frequencyMap.get(recipeCacheIndex);
        frequencyMap.put(recipeCacheIndex, frequency + 1);

        // Update frequencyCounter
        frequencyCounter.get(frequency).remove(recipeCacheIndex);
        if (frequencyCounter.get(frequency).isEmpty()) {
            frequencyCounter.remove(frequency);
        }

        frequencyCounter.computeIfAbsent(frequency + 1, k -> new LinkedHashSet<>()).add(recipeCacheIndex);
        this.cacheHit++;
        return foundRecipe;
    }

    public void put(Recipe key) {
        if (capacity <= 0) {
            // Capacity is zero or negative, no caching
            return;
        }

        int replaceRecipeCacheIndex = cachedRecipeCount;
        if (cachedRecipeCount >= capacity) {
            // Least frequently used element
            int lowestFrequency = frequencyCounter.firstKey();
            replaceRecipeCacheIndex = frequencyCounter.get(lowestFrequency).iterator().next();

            // Remove from frequencyCounter
            frequencyCounter.get(lowestFrequency).remove(replaceRecipeCacheIndex);
            if (frequencyCounter.get(lowestFrequency).isEmpty()) {
                frequencyCounter.remove(lowestFrequency);
            }
        }
        else {
            cachedRecipeCount++;
        }
        GTLog.logger.debug("Writing cache at index {}", replaceRecipeCacheIndex);
        // Add the new key-value pair to cache
        recipeCaches[replaceRecipeCacheIndex] = key;

        // Update frequency maps
        frequencyMap.put(replaceRecipeCacheIndex, 1);

        // Update frequencyCounter
        frequencyCounter.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(replaceRecipeCacheIndex);
    }
}
