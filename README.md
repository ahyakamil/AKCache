# AKCache
Simple and powerful caching

### How to use?
In this example, we using spring boot

Add pom

    <dependency>
      <groupId>com.ahyakamil.AKCache</groupId>
      <artifactId>AKCache</artifactId>
      <version>1.0.0</version>
    </dependency>


Create configuration class

    @Aspect
    @Configuration
    public class AKCacheConfig {
        @Bean
        public void akCacheSetup() {
            AKCacheSetup.setupConnection("localhost", 6379, "", "");
        }

        @Around("execution(* *(..)) && @annotation(com.ahyakamil.AKCache.annotation.AKCache)")
        public Object setListener(ProceedingJoinPoint pjp) throws Throwable {
            return AKCacheSetup.setListener(pjp);
        }
    }

### Default value
@AKCache
1. updateType: SMART
2. serializeClass: Object.class
3. ttl: 3 hours

#### TLDR;
- UpdateType SMART : if cache accessed more than 75% of its ttl, it will execute real method then update its values and ttl
- UpdateType FETCH : will return an existing cache to user then execute real method to update its value and ttl

### To using cache, follow the given example case:
#### Case 1
We want to cache with ttl 3 hours

    @AKCache
    public String example() {
        return "What a beautiful day!"
    }