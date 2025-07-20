# QueryCraft

A Java library for generating queries for performance testing scenarios.
QueryCraft provides flexible configuration options to simulate different user behaviors and query patterns in load
testing environments.

## Features

- **Dynamic Query Generation**: Generate queries based on configurable templates and filters
- **Multiple User Types**: Support for static and random user configurations
- **Time Range Management**: Automatic time period calculations with configurable ranges
- **Filter Application**: Dynamic filter application based on user configurations
- **Performance Testing Integration**: Seamless integration with JMeter, LoadRunner, and Gatling
- **YAML Configuration**: Flexible YAML-based configuration system
- **Thread-Safe**: Designed for concurrent usage in multi-threaded environments

## Installation

### Maven Dependency

Add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>com.human</groupId>
    <artifactId>QueryCraft</artifactId>
    <version>version</version>
</dependency>
```

### Build from Source

```bash
git clone <repository-url>
cd QueryCraft
mvn clean package
```

This will create a fat JAR with all dependencies in the `target` directory.

## Quick Start

```java
// Load configuration
AppConfig config = ConfigLoader.load("my_config.yaml"); // or empty constructor for default config file (config.yaml)

// Initialize generator
QueryGenerator generator = new QueryGenerator(config);

// Generate queries for a user
QueryResult result = generator.generateQueries("userName");

// Access generated data
List<String> queries = result.getQueries();
String userType = result.getUserType();
long timeLeftBorder = result.getTimeLeftBorder();
long timeRightBorder = result.getTimeRightBorder();
```

## Configuration

### Basic Configuration Structure

Create a `config.yaml` file with the following structure:

```yaml
# Path to user configurations
users_config_path: "users.yaml"

# Path to query templates
queries_path: "queries.yaml"

# Path to filters
filters_path: "filters.yaml"

# Base probability for filter application (0-100)
base_probability_filter: 66
```

### User Configuration Files

#### User Configuration (`users.yaml`)

```yaml
- user_type: static
  user_name: Users
  queries:
    - "SELECT * FROM transaction WHERE (end >= ${time_left_border} AND end <= ${time_right_border})"
    - "SELECT COUNT(*), users FROM transaction WHERE (end >= ${time_left_border} AND end <= ${time_right_border}) GROUP BY users LIMIT 100"
  time_left_border: 28800
  time_right_border: 0

- user_type: static
  user_name: Users with filters
  queries:
    - "SELECT COUNT(*), users FROM transaction WHERE (end >= ${time_left_border} AND end <= ${time_right_border}) ${filter_and_block} GROUP BY users LIMIT 100"
  time_left_border: 1200
  time_right_border: 300

- user_type: static
  user_name: Some another user
  queries_file: static_user_queries.yaml
  using_filters: false
  times:
    - Last1h
    - Last8h
    - Custom
  max_time_left: 28800
  max_time_right: 0

- user_type: random
  user_name: Some random user
  filter_apply_probability: 80
  filters_file: "custom_filters.yaml"
  times:
    - Last1h
    - Last8h
    - Custom
  max_time_left: 28800
  max_time_right: 0

- user_type: random
  user_name: Some another random user
  filter_apply_probability: 60
  queries_file: "random_queries.yaml"
  time_left_border: 600
  time_right_border: 0
```

#### Dynamic Query Templates (`queries.yaml`)

```yaml
PG_Statements:
  - Select * from pg_stat_statements where "ts" >= ${time_left_border} AND "ts" <= ${time_right_border} ${filter_and_block}

PG_Activity:
  - Select * from pg_stat_activity where "query_start" >= ${time_left_border} AND "query_start" <= ${time_right_border} ${filter_and_block}

Vacuum:
  - Select * from pg_stat_statements where "ts" >= ${time_left_border} AND "ts" <= ${time_right_border} ${filter_and_block}

SomeAnotherQuery:
  queries_file: static_user_queries.yaml

SomeRandomQuery:
  is_all_select: false
  min_selected_queries: 2
  is_random_selection: false
  max_count_selected_in_percent: 50
  queries:
    - Select * from pg_stat_statements where "ts" >= ${time_left_border} AND "ts" <= ${time_right_border} ${filter_and_block}
    - Select * from pg_stat_activity where "query_start" >= ${time_left_border} AND "query_start" <= ${time_right_border} ${filter_and_block}
    - Select * from pg_stat_statements where "ts" >= ${time_left_border} AND "ts" <= ${time_right_border} ${filter_and_block}
    - Select * from pg_stat_activity where "query_start" >= ${time_left_border} AND "query_start" <= ${time_right_border} ${filter_and_block}
    - Select * from pg_stat_activity where "query_start" >= ${time_left_border} AND "query_start" <= ${time_right_border} ${filter_and_block}
    - Select * from pg_stat_activity where "query_start" >= ${time_left_border} AND "query_start" <= ${time_right_border} ${filter_and_block}
    - Select * from pg_stat_activity where "query_start" >= ${time_left_border} AND "query_start" <= ${time_right_border} ${filter_and_block}
```

#### Dynamic Filters (`filters.yaml`)

```yaml
- some_filter == "value"
- some_filter != "value"
- some_filter > "value"
- some_filter < "value"
- some_filter >= "value"
- some_filter <= "value"
- some_filter in ["value1", "value2", "value3"]
- some_filter not in ["value1", "value2", "value3"]
- some_filter contains "value"
```

## Performance Testing Integration

### JMeter Integration

#### Step 1: Setup Thread Group Configuration

Create a **setUp Thread Group** to initialize the QueryCraft generator:

```groovy
// setUp Thread Group - JSR223 PreProcessor (Groovy)

import com.human.config.ConfigLoader
import com.human.QueryGenerator

try {
    // Load configuration
    def baseDir = Paths.get(FileServer.getFileServer().getBaseDir())
    def generator = new QueryGenerator(new ConfigLoader(baseDir, "my_config.yaml").load())
    // or only baseDir in constructor if config file name is "config.yaml"

    // Store in JMeter properties for thread sharing
    props.put("queryGenerator", generator)

    log.info("QueryCraft generator initialized successfully")
} catch (Exception e) {
    log.error("Failed to initialize QueryCraft: " + e.getMessage())
    throw e
}
```

#### Step 2: Use in Regular Thread Groups

In your main **Thread Group**, use a **JSR223 PreProcessor (Groovy)**:

```groovy
// Thread Group - JSR223 PreProcessor (Groovy)
import com.human.QueryGenerator
import com.human.service.QueryResult

try {
    // Get generator from properties
    def generator = props.get("queryGenerator")

    // Generate queries for current user
    def userName = "Random" // or get from CSV/variables
    QueryResult result = generator.generateQueries(userName)

    // Store results in JMeter variables
    vars.putObject("queries", result.getQueries()) // Store as object array, not string
    vars.put("userType", result.getUserType())
    vars.put("userName", result.getUserName())
    vars.put("timePeriod", result.getTimePeriodName())
    vars.put("appliedFilter", result.getAppliedFilter())
    vars.put("timeLeftBorder", String.valueOf(result.getTimeLeftBorder()))
    vars.put("timeRightBorder", String.valueOf(result.getTimeRightBorder()))

    // Use individual queries
    def queries = result.getQueries()
    for (int i = 0; i < queries.size(); i++) {
        vars.put("query_" + i, queries.get(i))
    }

    log.info("Generated " + queries.size() + " queries for user: " + userName)
} catch (Exception e) {
    log.error("Failed to generate queries: " + e.getMessage())
    throw e
}
```

#### Step 3: JMeter Test Plan Structure

```
Test Plan
├── setUp Thread Group (1 thread, 1 iteration)
│   └── JSR223 PreProcessor (Initialize QueryCraft)
├── Thread Group (N threads, M iterations)
│   ├── JSR223 PreProcessor (Generate queries)
│   ├── HTTP Request (Use ${query_0}, ${query_1}, etc.)
│   └── Response Assertions
└── tearDown Thread Group (cleanup if needed)
```

#### Example HTTP Request Configuration

```
HTTP Request:
- Server: your-bloomberg-server.com
- Path: /api/query
- Method: POST
- Body: ${query_0}
- Headers: 
  - Content-Type: application/json
  - User-Type: ${userType}
  - Time-Range: ${timeLeftBorder}-${timeRightBorder}
```

### LoadRunner Integration

#### Step 1: Initialize in vuser_init()

```java
// vuser_init.java (Java Vuser)

import com.human.QueryGenerator;
import com.human.config.ConfigLoader;
import com.human.config.AppConfig;

public class Actions {

    private static QueryGenerator generator;

    public int init() throws Throwable {
        try {
            // Load configuration
            ConfigLoader configLoader = new ConfigLoader("my_config.yaml"); // or empty constructor for default config file (config.yaml)
            AppConfig config = configLoader.load();

            // Initialize generator
            generator = new QueryGenerator(config);

            lr.output_message("QueryCraft generator initialized successfully");
            return 0;
        } catch (Exception e) {
            lr.error_message("Failed to initialize QueryCraft: " + e.getMessage());
            return -1;
        }
    }
}
```

#### Step 2: Generate Queries in Action

```java
// Action.java

import com.human.service.QueryResult;

import java.util.List;

public int action() throws Throwable {
    try {
        String userName = "Random"; // or get from parameter

        // Generate queries
        QueryResult result = generator.generateQueries(userName);

        // Extract result data
        List<String> queries = result.getQueries();
        String userType = result.getUserType();
        String timePeriod = result.getTimePeriodName();
        String appliedFilter = result.getAppliedFilter();
        long timeLeftBorder = result.getTimeLeftBorder();
        long timeRightBorder = result.getTimeRightBorder();

        // Use first query in HTTP request
        String query = queries.get(0);

        lr.save_string(query, "query");
        lr.save_string(userType, "userType");
        lr.save_string(String.valueOf(timeLeftBorder), "timeLeftBorder");
        lr.save_string(String.valueOf(timeRightBorder), "timeRightBorder");

        // Execute HTTP request
        web.custom_request("Query",
                "URL=http://your-server.com/api/query",
                "Method=POST",
                "Body={query}",
                "Headers=Content-Type: application/json\n" +
                        "User-Type: {userType}\n" +
                        "Time-Range: {timeLeftBorder}-{timeRightBorder}",
                LAST);

        lr.output_message("Executed query for user: " + userName +
                ", generated " + queries.size() + " queries");

        return 0;
    } catch (Exception e) {
        lr.error_message("Failed to generate/execute queries: " + e.getMessage());
        return -1;
    }
}
```

### Gatling Integration

#### Step 1: Java Vuser Setup

Add QueryCraft JAR to your Gatling classpath and create a Java Vuser simulation:

```java
// QueryCraftSimulation.java
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import com.human.QueryGenerator;
import com.human.config.ConfigLoader;
import com.human.config.AppConfig;
import com.human.service.QueryResult;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class QueryCraftSimulation extends Simulation {

    // Initialize QueryCraft generator (shared across all virtual users)
    private static final QueryGenerator generator;
    
    static {
        try {
            ConfigLoader configLoader = new ConfigLoader("my_config.yaml"); // or empty constructor for default config file (config.yaml)
            AppConfig config = configLoader.load();
            generator = new QueryGenerator(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize QueryCraft generator", e);
        }
    }

    // HTTP protocol configuration
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://your-server.com")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json");

    // User feeder for different user types
    Iterator<Map<String, Object>> userFeeder = listFeeder(
        Arrays.asList(
            Map.of("userName", "Users"),
            Map.of("userName", "Users with filters"), 
            Map.of("userName", "Some another user"),
            Map.of("userName", "Some random user"),
            Map.of("userName", "Some another random user")
        )
    ).random();

    // Custom function to generate queries
    ChainBuilder generateQueries = exec(session -> {
        try {
            String userName = session.getString("userName");
            QueryResult result = generator.generateQueries(userName);
            
            return session
                .set("queries", result.getQueries())
                .set("userType", result.getUserType())
                .set("userName", result.getUserName())
                .set("timePeriod", result.getTimePeriodName())
                .set("appliedFilter", result.getAppliedFilter())
                .set("timeLeftBorder", result.getTimeLeftBorder())
                .set("timeRightBorder", result.getTimeRightBorder());
        } catch (Exception e) {
            System.err.println("Failed to generate queries: " + e.getMessage());
            return session.markAsFailed();
        }
    });

    // Execute queries scenario
    ScenarioBuilder queryScenario = scenario("QueryCraft Load Test")
        .feed(userFeeder)
        .exec(generateQueries)
        .foreach("#{queries}", "query").on(
            exec(http("Execute Query")
                .post("/api/query")
                .body(StringBody("#{query}"))
                .header("User-Type", "#{userType}")
                .header("Time-Range", "#{timeLeftBorder}-#{timeRightBorder}")
                .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1))
        );

    // Load simulation setup
    {
        setUp(
            queryScenario.injectOpen(
                rampUsers(50).during(Duration.ofSeconds(30)),
                constantUsersPerSec(10).during(Duration.ofMinutes(2))
            )
        ).protocols(httpProtocol)
         .maxDuration(Duration.ofMinutes(5))
         .assertions(
             global().responseTime().max().lt(5000),
             global().successfulRequests().percent().gt(95.0)
         );
    }
}
```

#### Step 2: Advanced Java Vuser Features

```java
// Custom query selection strategy
ChainBuilder selectRandomQuery = exec(session -> {
    @SuppressWarnings("unchecked")
    List<String> queries = (List<String>) session.get("queries");
    if (queries != null && !queries.isEmpty()) {
        Random random = new Random();
        String selectedQuery = queries.get(random.nextInt(queries.size()));
        return session.set("selectedQuery", selectedQuery);
    }
    return session.markAsFailed();
});

// Conditional execution based on user type
ChainBuilder conditionalExecution = 
    doIf(session -> "static".equals(session.getString("userType"))).then(
        exec(http("Static User Query")
            .post("/api/query/static")
            .body(StringBody("#{selectedQuery}"))
        )
    )
    .doIf(session -> "random".equals(session.getString("userType"))).then(
        exec(http("Random User Query")
            .post("/api/query/dynamic") 
            .body(StringBody("#{selectedQuery}"))
        )
    );

// Enhanced scenario with custom logic
ScenarioBuilder enhancedScenario = scenario("Enhanced QueryCraft Test")
    .feed(userFeeder)
    .exec(generateQueries)
    .exec(selectRandomQuery)
    .exec(conditionalExecution)
    .pause(Duration.ofSeconds(2));
```

#### Step 3: Maven Configuration

Add to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.human</groupId>
        <artifactId>QueryCraft</artifactId>
        <version>version</version>
    </dependency>
    <dependency>
        <groupId>io.gatling.highcharts</groupId>
        <artifactId>gatling-charts-highcharts</artifactId>
        <version>3.9.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>io.gatling</groupId>
            <artifactId>gatling-maven-plugin</artifactId>
            <version>4.3.7</version>
            <configuration>
                <simulationClass>QueryCraftSimulation</simulationClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### Step 4: Running the Test

```bash
# Run with Maven
mvn gatling:test

# Or run specific simulation
mvn gatling:test -Dgatling.simulationClass=QueryCraftSimulation
```

## API Reference

### QueryGenerator

#### Constructor

```java
public QueryGenerator(AppConfig appConfig)
```

#### Methods

```java
public QueryResult generateQueries(String userName)
```

Generates queries for the specified user based on their configuration.

**Parameters:**

- `userName` - The name of the user configuration to use

**Returns:**

- `QueryResult` - Object containing generated queries and metadata

**Throws:**

- `IllegalArgumentException` - If userName is null/empty or user config not found
- `RuntimeException` - If query generation fails

### QueryResult

#### Fields Access Methods

```java
public List<String> getQueries()           // Generated queries

public String getUserType()                // Type of user (static/random)

public String getUserName()                // User name used for generation

public String getTimePeriodName()          // Applied time period

public String getAppliedFilter()           // Applied filter name

public long getTimeLeftBorder()            // Start time (Unix timestamp)

public long getTimeRightBorder()           // End time (Unix timestamp)

public String getFormattedTimeRange()      // Formatted time range string
```

### ConfigLoader

#### Constructors

```java
public ConfigLoader()
```
Creates a ConfigLoader with default configuration file path (`config.yaml`) and no base directory.

```java
public ConfigLoader(Path baseDir)
```
Creates a ConfigLoader with the specified base directory. The configuration file will be resolved as `config.yaml` within the base directory.

**Parameters:**
- `baseDir` - The base directory path where the config file is located

```java
public ConfigLoader(String configPath)
```
Creates a ConfigLoader with the specified configuration file path and no base directory.

**Parameters:**
- `configPath` - The path to the configuration file (can be relative or absolute)

```java
public ConfigLoader(Path baseDir, String configPathWithoutBaseDir)
```
Creates a ConfigLoader with a base directory and a configuration file path. If the config path is relative, it will be resolved relative to the base directory. If absolute, the base directory is ignored for the config path but still used for other file paths.

**Parameters:**
- `baseDir` - The base directory path
- `configPathWithoutBaseDir` - The configuration file path (relative to baseDir if not absolute)

```java
public ConfigLoader(String configWithAbsolutePath, Path baseDir)
```
Creates a ConfigLoader with an absolute configuration file path and a base directory. The base directory is used for resolving other relative file paths referenced in the configuration.

**Parameters:**
- `configWithAbsolutePath` - The absolute path to the configuration file
- `baseDir` - The base directory path for resolving other relative file paths

#### Static Methods

```java
public static AppConfig load() // Loads config file
```

Loads application configuration from YAML file.

## Best Practices

### Performance Testing

1. **Initialize Once**: Initialize the `QueryGenerator` once per test execution (setUp/init phase)
2. **Thread Safety**: The generator is thread-safe and can be shared across virtual users
3. **Configuration Caching**: Configuration is loaded once and cached for performance
4. **Memory Management**: `QueryResult` objects are lightweight and can be created frequently

### Configuration Management

1. **Environment-Specific Configs**: Use different config files for different environments
2. **User Variety**: Configure multiple user types to simulate realistic load patterns
3. **Time Period Variation**: Use various time periods to test different query complexities
4. **Filter Combinations**: Test different filter combinations for comprehensive coverage

## Troubleshooting

### Common Issues

1. **Configuration File Not Found**
    - Ensure config files are in the classpath or provide absolute paths
    - Check file permissions and accessibility

2. **User Configuration Missing**
    - Verify user names match exactly with configuration
    - Check YAML syntax and structure

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions and support:

- Create an issue in the repository
- Check the documentation and examples
- Review the troubleshooting section

---

**Java Compatibility**: Java 8+  
**Dependencies**: SnakeYAML 2.2, Apache Commons Text 1.10.0, Jackson 2.15.2
