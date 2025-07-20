package com.human.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigLoader {

    private final String configPath;
    private final Path baseDir;
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final String DEFAULT_FILTERS_KEY = "default_filters";
    private final String BASE_PROBABILITY_FILTER_KEY = "base_probability_filter";
    public static final int DEFAULT_PROBABILITY = 66;
    private String dynamicFiltersPath;

    public ConfigLoader() {
        this.baseDir = null;
        this.configPath = "config.yaml";
    }

    public ConfigLoader(Path baseDir) {
        this.baseDir = baseDir;
        this.configPath = Paths.get(this.baseDir.toString(), "config.yaml").toString();
    }

    public ConfigLoader(String configPath) {
        this.baseDir = null;
        this.configPath = configPath;
    }

    public ConfigLoader(Path baseDir, String configPathWithoutBaseDir) {
        this.baseDir = baseDir;

        if (!Paths.get(configPathWithoutBaseDir).isAbsolute()) {
            this.configPath = Paths.get(this.baseDir.toString(), configPathWithoutBaseDir).toString();
        } else {
            this.configPath = configPathWithoutBaseDir;
        }
    }

    public ConfigLoader(String configWithAbsolutePath, Path baseDir) {
        this.baseDir = baseDir;
        this.configPath = configWithAbsolutePath;
    }

    public AppConfig load() throws IOException {
        Map<String, Object> config = loadConfigFile();

        String USERS_CONFIG_PATH_KEY = "users_config_path";
        String userConfigPath = (String) config.get(USERS_CONFIG_PATH_KEY);
        String DYNAMIC_QUERIES_PATH_KEY = "queries_path";
        String dynamicQueriesPath = (String) config.get(DYNAMIC_QUERIES_PATH_KEY);
        String DYNAMIC_FILTERS_PATH_KEY = "filters_path";
        String dynamicFiltersPath = (String) config.get(DYNAMIC_FILTERS_PATH_KEY);

        this.dynamicFiltersPath = dynamicFiltersPath;

        Integer baseProbabilityFilterObj = (Integer) config.get(BASE_PROBABILITY_FILTER_KEY);
        int baseProbabilityFilter = (baseProbabilityFilterObj != null) ? baseProbabilityFilterObj : DEFAULT_PROBABILITY;

        Map<String, RandomQueryConfig> dynamicQueryTemplates = loadDynamicQueries(dynamicQueriesPath);

        Map<String, BaseUserConfig> userConfigs = loadUserConfigs(userConfigPath, dynamicQueryTemplates, baseProbabilityFilter);

        Map<String, List<String>> allFiltersMap = loadDynamicFilters(dynamicFiltersPath);
        List<String> allFilters = allFiltersMap.getOrDefault(DEFAULT_FILTERS_KEY, Collections.emptyList());

        Map<String, List<String>> legacyQueryTemplates = convertRandomQueryConfigsToMap(dynamicQueryTemplates);

        return new AppConfig(
                userConfigs,
                dynamicQueryTemplates,
                allFilters,
                DEFAULT_FILTERS_KEY,
                baseProbabilityFilter
        );
    }

    private Map<String, Object> loadConfigFile() throws IOException {
        try (InputStream is = Files.newInputStream(resolvePath(configPath))) {
            return mapper.readValue(is, new TypeReference<Map<String, Object>>() {
            });
        }
    }

    private Map<String, BaseUserConfig> loadUserConfigs(String userConfigPath, Map<String, RandomQueryConfig> dynamicQueryTemplates, int defaultFilterProbability) throws IOException {
        Map<String, Object> config = loadConfigFile();
        Integer baseProbabilityFilterObj = (Integer) config.get(BASE_PROBABILITY_FILTER_KEY);
        int baseProbabilityFilter = (baseProbabilityFilterObj != null) ? baseProbabilityFilterObj : DEFAULT_PROBABILITY;

        try (InputStream is = Files.newInputStream(resolvePath(userConfigPath))) {
            List<BaseUserConfig> userList = mapper.readValue(is, new TypeReference<List<BaseUserConfig>>() {
            });

            List<BaseUserConfig> processedUsers = new ArrayList<>();

            for (BaseUserConfig user : userList) {
                BaseUserConfig processedUser = processUser(user, dynamicQueryTemplates, baseProbabilityFilter);
                processedUsers.add(processedUser);
            }

            return Collections.unmodifiableMap(processedUsers.stream()
                    .collect(Collectors.toMap(BaseUserConfig::getUserName, Function.identity())));
        }
    }

    private BaseUserConfig processUser(BaseUserConfig user, Map<String, RandomQueryConfig> dynamicQueryTemplates, int defaultFilterProbability) throws IOException {
        if (user instanceof StaticUserConfig) {
            return processStaticUser((StaticUserConfig) user, defaultFilterProbability);
        } else if (user instanceof RandomUserConfig) {
            return processRandomUser((RandomUserConfig) user, dynamicQueryTemplates, defaultFilterProbability);
        }
        return user;
    }

    private StaticUserConfig processStaticUser(StaticUserConfig user, int defaultFilterProbability) throws IOException {
        StaticUserConfig processedUser = user;

        if (user.hasQueriesFile()) {
            List<String> queriesFromFile = loadQueriesListFromFile(user.getQueriesFile());
            processedUser = user.withLoadedQueries(queriesFromFile);
        }

        processedUser = loadFiltersForUser(processedUser, defaultFilterProbability);

        return processedUser;
    }

    private RandomUserConfig processRandomUser(RandomUserConfig user, Map<String, RandomQueryConfig> dynamicQueryTemplates, int defaultFilterProbability) throws IOException {
        RandomUserConfig processedUser = user;

        if (user.hasQueriesFile()) {
            Map<String, RandomQueryConfig> queriesFromFile = loadQueriesAsRandomQueryConfigFromFile(user.getQueriesFile());
            processedUser = user.withLoadedQueries(queriesFromFile);
        } else if (user.usesDynamicTemplates()) {
            processedUser = user.withLoadedQueries(dynamicQueryTemplates);
        }

        processedUser = loadFiltersForUser(processedUser, defaultFilterProbability);

        return processedUser;
    }

    private Map<String, RandomQueryConfig> loadQueriesAsRandomQueryConfigFromFile(String queriesFile) throws IOException {
        Path path = resolvePath(queriesFile);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + queriesFile);
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonNode rootNode = mapper.readTree(is);

            if (!rootNode.isObject()) {
                throw new IOException("Expected object structure in " + queriesFile);
            }

            Map<String, RandomQueryConfig> result = new HashMap<>();

            rootNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                try {
                    RandomQueryConfig queryConfig = parseRandomQueryConfig(value);
                    result.put(key, queryConfig);
                } catch (IOException e) {
                    throw new RuntimeException("Error processing queries for key: " + key, e);
                }
            });

            return Collections.unmodifiableMap(result);
        }
    }

    private Map<String, List<String>> loadQueriesMapFromFile(String queriesFile) throws IOException {
        Path path = resolvePath(queriesFile);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + queriesFile);
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonNode rootNode = mapper.readTree(is);

            if (rootNode.isObject()) {
                Map<String, List<String>> result = new HashMap<>();

                rootNode.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode value = entry.getValue();
                    try {
                        List<String> queries = extractQueries(value);
                        result.put(key, queries);
                    } catch (IOException e) {
                        throw new RuntimeException("Error processing queries for key: " + key, e);
                    }
                });

                return Collections.unmodifiableMap(result);
            } else {
                throw new IOException("Expected object structure in file: " + queriesFile +
                        ", but got: " + rootNode.getNodeType());
            }
        }
    }

    private List<String> loadQueriesListFromFile(String queriesFile) throws IOException {
        Path path = resolvePath(queriesFile);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + queriesFile);
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonNode rootNode = mapper.readTree(is);

            if (rootNode.isArray()) {
                return mapper.convertValue(rootNode, new TypeReference<List<String>>() {
                });
            } else {
                throw new IOException("Expected array structure in file: " + queriesFile +
                        ", but got: " + rootNode.getNodeType());
            }
        }
    }

    private List<String> loadFiltersFromFile(String filtersFile) throws IOException {
        Path path = resolvePath(filtersFile);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filtersFile);
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonNode rootNode = mapper.readTree(is);

            if (rootNode.isArray()) {
                return mapper.convertValue(rootNode, new TypeReference<List<String>>() {
                });
            } else {
                throw new IOException("Expected array structure in filters file: " + filtersFile +
                        ", but got: " + rootNode.getNodeType());
            }
        }
    }

    private Map<String, RandomQueryConfig> loadDynamicQueries(String dynamicQueriesPath) throws IOException {
        Path path = resolvePath(dynamicQueriesPath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + dynamicQueriesPath);
        }

        try (InputStream is = Files.newInputStream(path)) {
            JsonNode rootNode = mapper.readTree(is);

            if (!rootNode.isObject()) {
                throw new IOException("Expected object structure in " + dynamicQueriesPath);
            }

            Map<String, RandomQueryConfig> result = new HashMap<>();

            rootNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                try {
                    RandomQueryConfig queryConfig = parseRandomQueryConfig(value);
                    result.put(key, queryConfig);
                } catch (IOException e) {
                    throw new RuntimeException("Error processing queries for key: " + key, e);
                }
            });

            return Collections.unmodifiableMap(result);
        }
    }

    private RandomQueryConfig parseRandomQueryConfig(JsonNode node) throws IOException {
        if (node.isArray()) {
            List<String> queries = mapper.convertValue(node, new TypeReference<List<String>>() {
            });
            return new RandomQueryConfig(queries, null, null, null, null, null);
        } else if (node.isObject()) {
            RandomQueryConfig config = mapper.convertValue(node, RandomQueryConfig.class);

            if (config.hasQueriesFile()) {
                try {
                    List<String> queriesFromFile = loadQueriesListFromFile(config.getQueriesFile());
                    return config.withLoadedQueries(queriesFromFile);
                } catch (IOException e) {
                    System.err.println("Warning: Could not load queries from file " + config.getQueriesFile() + ": " + e.getMessage());
                    return config;
                }
            }

            return config;
        } else {
            throw new IOException("Invalid node type for RandomQueryConfig: " + node.getNodeType());
        }
    }

    private List<String> extractQueries(JsonNode node) throws IOException {
        if (node.isArray()) {
            return mapper.convertValue(node, new TypeReference<List<String>>() {
            });
        } else if (node.isObject()) {
            if (node.has("queries_file")) {
                String queriesFile = node.get("queries_file").asText();
                return loadQueriesListFromFile(queriesFile);
            } else if (node.has("queries")) {
                JsonNode queriesNode = node.get("queries");
                return mapper.convertValue(queriesNode, new TypeReference<List<String>>() {
                });
            }
        }

        return mapper.convertValue(node, new TypeReference<List<String>>() {
        });
    }

    private Map<String, List<String>> loadDynamicFilters(String dynamicFiltersPath) throws IOException {
        try (InputStream is = Files.newInputStream(resolvePath(dynamicFiltersPath))) {
            JsonNode rootNode = mapper.readTree(is);
            Map<String, List<String>> result = new HashMap<>();

            if (rootNode.isArray()) {
                List<String> filters = mapper.convertValue(rootNode, new TypeReference<List<String>>() {
                });
                result.put(DEFAULT_FILTERS_KEY, filters);
            } else if (rootNode.isObject()) {
                rootNode.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode value = entry.getValue();

                    try {
                        List<String> filters = mapper.convertValue(value, new TypeReference<List<String>>() {
                        });
                        result.put(key, filters);
                    } catch (Exception e) {
                        throw new RuntimeException("Error processing filters for key: " + key, e);
                    }
                });
            }

            return Collections.unmodifiableMap(result);
        }
    }

    private Map<String, List<String>> convertRandomQueryConfigsToMap(Map<String, RandomQueryConfig> dynamicQueryTemplates) {
        Map<String, List<String>> result = new HashMap<>();

        for (Map.Entry<String, RandomQueryConfig> entry : dynamicQueryTemplates.entrySet()) {
            String key = entry.getKey();
            RandomQueryConfig config = entry.getValue();

            if (config.hasQueriesFile()) {
                try {
                    List<String> queriesFromFile = loadQueriesListFromFile(config.getQueriesFile());
                    result.put(key, queriesFromFile);
                } catch (IOException e) {
                    result.put(key, config.getQueries());
                }
            } else {
                result.put(key, config.getQueries());
            }
        }

        return result;
    }

    private <T extends BaseUserConfig> T loadFiltersForUser(T user, int defaultFilterProbability) throws IOException {
        if (user.hasInlineFilters()) {
            return user;
        }

        List<String> filtersToLoad = null;

        if (user.hasFiltersFile()) {
            filtersToLoad = loadFiltersFromFile(user.getFiltersFile());
        } else {
            filtersToLoad = loadDefaultFilters();
        }

        if (filtersToLoad != null && !filtersToLoad.isEmpty()) {
            return (T) user.withLoadedFilters(filtersToLoad, defaultFilterProbability);
        }

        return user;
    }

    private List<String> loadDefaultFilters() throws IOException {
        if (dynamicFiltersPath == null || dynamicFiltersPath.isEmpty()) {
            return Collections.emptyList();
        }

        return loadFiltersFromFile(dynamicFiltersPath);
    }

    /**
     * Resolves a file path by prepending baseDir if the path is relative and baseDir is not null/empty
     */
    private Path resolvePath(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        Path path = Paths.get(filePath);

        // If path is absolute or baseDir is null/empty, return path as is
        if (path.isAbsolute() || baseDir == null) {
            return path;
        }

        // Path is relative and baseDir is set, prepend baseDir
        return Paths.get(baseDir.toString(), filePath);
    }
}
