import java.util.HashMap;
import java.util.Map;

public class CacheManager {
    private static CacheManager instance;
    private Map<String, double[]> dataCache;

    private CacheManager() {
        dataCache = new HashMap<>();
    }

    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public void addToCache(String key, double[] values) {
        dataCache.put(key, values);
    }

    public double[] getFromCache(String key) {
        return dataCache.get(key);
    }
}
