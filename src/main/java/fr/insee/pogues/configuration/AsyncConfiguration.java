package fr.insee.pogues.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean("releaseProcessExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(10);
        delegate.setMaxPoolSize(100);
        delegate.setQueueCapacity(1000);
        delegate.setThreadNamePrefix("async-task-");
        delegate.initialize();

        // Allow authentication to be enabled in async context
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }
}