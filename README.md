# AKCache
Simple and powerful caching

### How to use?
In this example, we using spring boot

Add pom

    <dependency>
      <groupId>com.ahyakamil.AKCache</groupId>
      <artifactId>AKCache</artifactId>
    </dependency>


Create configuration class, ex: AKCacheConfig.java

    @Aspect
    @Configuration
    public class AKCacheConfig {
        @Autowired
        AKService akService;
    
        @Bean
        public void akCacheSetup() {
            AKCacheSetup.setupConnection("localhost", 6379, "", "");
        }
    
        @Around("execution(* *(..)) && @annotation(com.ahyakamil.AKCache.annotation.AKCache) || @annotation(com.ahyakamil.AKCache.annotation.AKCacheUpdate)")
        public Object setListener(ProceedingJoinPoint pjp) throws Throwable {
            Object result = AKCacheSetup.setListener(pjp);
            akService.renewCache();
            return result;
        }
    }
    
    @Service
    class AKService {
        @Async
        @Transactional
        public void renewCache() throws Throwable {
            AKCacheSetup.renewCache();
        }
    }
    
**IMPORTANT**

- Do renewCache() asynchronously for better experience
- Transactional used for avoid lazy session exception hibernate

### Default value
@AKCache
1. updateType: SMART
2. serializeClass: Object.class
3. ttl: 3 hours

### NOTES
- UpdateType SMART : will return cache and if ttl is more than 75%, it will execute real method to update cache
- UpdateType FETCH : will return cache and execute real method immediately to update cache

### Example

	@AKCache(serializeClass = HttpEntity.class)
	@GetMapping("/hello")
	public Object newEmployee() {
		return ResponseEntity.ok("hello world");
	}

Since spring boot ResponseEntity doesn't have "no args constructor" which is used for serialization,
we can use HttpEntity for serializing.


**TODO**

Will update for more feature, stay tuned..


## License
Licensed under [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html)

## Buy Me A Chocolate
[Buy Me A Chocolate](https://www.paypal.com/paypalme/ahyaalkamil1)

