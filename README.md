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
    
