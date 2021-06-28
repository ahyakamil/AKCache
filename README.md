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
3. ttl: 1 hour

#### TLDR;
- UpdateType SMART will run method again and update its values and ttl, if cache accessed more than 75% of ttl


### To using cache, follow the given example case:
#### Case 1
We want to cache with ttl 1 hour

    @AKCache
    public String example() {
        return "What a beautiful day!"
    }