package com.hanover.entp.dataext.config.async;

import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableAutoConfiguration
public class QuartzSchedulerConfig {

    /**
     * Creates a {@link JobFactory} that integrates Quartz with Spring's ApplicationContext,
     * allowing Quartz jobs to use Spring-managed beans.
     *
     * @param applicationContext the Spring application context
     * @return a configured {@link JobFactory}
     */
    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        SpringBeanJobFactory factory = new SpringBeanJobFactory();
        factory.setApplicationContext(applicationContext);
        return factory;
    }

    // =========================================================
    // EXTRACTION SCHEDULER
    // =========================================================

    /**
     * Configures the {@link SchedulerFactoryBean} for the Extraction scheduler
     * for non-test profiles (local, dev, int, uat, prod etc - anything that's not "test").
     *
     * @param quartzProps Quartz properties specific to the Extraction scheduler
     * @param jobFactory  the custom job factory for Spring integration
     * @param dataSource  the Quartz data source
     * @return a configured {@link SchedulerFactoryBean} for Extraction jobs
     */
    @Bean(name = "extractionSchedulerFactory")
    @Profile("!test")
    public SchedulerFactoryBean extractionSchedulerFactory(
            @Qualifier("extractionQuartzProperties") Properties quartzProps,
            @Qualifier("jobFactory") JobFactory jobFactory,
            DataSource dataSource) {

        SchedulerFactoryBean extractionSchedulerFactory = new SchedulerFactoryBean();
        extractionSchedulerFactory.setDataSource(dataSource);
        extractionSchedulerFactory.setQuartzProperties(quartzProps);
        extractionSchedulerFactory.setJobFactory(jobFactory);
        return extractionSchedulerFactory;
    }

    /**
     * Configures the {@link SchedulerFactoryBean} for the Extraction scheduler
     * for "test" profile.
     *
     * @param quartzProps Quartz properties specific to the Extraction scheduler
     * @param jobFactory  the custom job factory for Spring integration
     * @return a configured {@link SchedulerFactoryBean} for Extraction jobs
     */
    @Bean(name = "extractionSchedulerFactory")
    @Profile("test")
    public SchedulerFactoryBean testExtractionSchedulerFactory(
            @Qualifier("extractionQuartzProperties") Properties quartzProps,
            @Qualifier("jobFactory") JobFactory jobFactory) {

        SchedulerFactoryBean extractionSchedulerFactory = new SchedulerFactoryBean();
        extractionSchedulerFactory.setQuartzProperties(quartzProps);
        extractionSchedulerFactory.setJobFactory(jobFactory);
        return extractionSchedulerFactory;
    }

    /**
     * Creates a {@link Properties} bean populated with Quartz configuration settings
     * for the Extraction scheduler using Spring Boot's {@link ConfigurationProperties} mechanism.
     *
     * <p>This method binds all properties defined under the prefix
     * {@code spring.quartz.properties.extraction} in the application.properties
     * {@link Properties} object. These properties are then used by the Quartz
     * {@link org.springframework.scheduling.quartz.SchedulerFactoryBean} to configure
     * the Extraction scheduler instance.</p>
     *
     * <h2>Example Configuration:</h2>
     * <pre>
     * spring.quartz.properties.extraction.org.quartz.scheduler.instanceName=ExtractionScheduler
     * spring.quartz.properties.extraction.org.quartz.threadPool.threadCount=10
     * </pre>
     *
     * @return a {@link Properties} object containing all Quartz properties for the Extraction scheduler
     */
    @ConfigurationProperties(prefix = "spring.quartz.properties.extraction")
    @Bean
    public Properties extractionQuartzProperties() {
        return new Properties(); // Spring will populate this bean automatically
    }

    /**
     * Exposes the Extraction {@link Scheduler} bean for job scheduling.
     *
     * @param factory the Extraction scheduler factory
     * @return the {@link Scheduler} instance for Extraction jobs
     * @throws Exception if the scheduler cannot be retrieved
     */
    @Bean(name = "extractionScheduler")
    public Scheduler extractionScheduler(
            @Qualifier("extractionSchedulerFactory") SchedulerFactoryBean factory)
            throws Exception {
        return factory.getScheduler();
    }

    // =========================================================
    // DISPATCHER SCHEDULER
    // =========================================================

    /**
     * Configures the {@link SchedulerFactoryBean} for the Dispatcher scheduler
     * for non-test profiles (local, dev, int, uat, prod etc - anything that's not "test").
     *
     * @param quartzProps Quartz properties specific to the Dispatcher scheduler
     * @param jobFactory  the custom job factory for Spring integration
     * @param dataSource  the Quartz data source
     * @return a configured {@link SchedulerFactoryBean} for Dispatcher jobs
     */
    @Bean(name = "dispatcherSchedulerFactory")
    @Profile("!test")
    public SchedulerFactoryBean dispatcherSchedulerFactory(
            @Qualifier("dispatcherQuartzProperties") Properties quartzProps,
            @Qualifier("jobFactory") JobFactory jobFactory,
            DataSource dataSource) {

        SchedulerFactoryBean dispatcherSchedulerFactory = new SchedulerFactoryBean();
        dispatcherSchedulerFactory.setDataSource(dataSource);
        dispatcherSchedulerFactory.setQuartzProperties(quartzProps);
        dispatcherSchedulerFactory.setJobFactory(jobFactory);
        return dispatcherSchedulerFactory;
    }

    /**
     * Configures the {@link SchedulerFactoryBean} for the Dispatcher scheduler
     * for "test" profile.
     *
     * @param quartzProps Quartz properties specific to the Dispatcher scheduler
     * @param jobFactory  the custom job factory for Spring integration
     * @return a configured {@link SchedulerFactoryBean} for Dispatcher jobs
     */
    @Bean(name = "dispatcherSchedulerFactory")
    @Profile("test")
    public SchedulerFactoryBean testDispatcherSchedulerFactory(
            @Qualifier("dispatcherQuartzProperties") Properties quartzProps,
            @Qualifier("jobFactory") JobFactory jobFactory) {

        SchedulerFactoryBean dispatcherSchedulerFactory = new SchedulerFactoryBean();
        dispatcherSchedulerFactory.setQuartzProperties(quartzProps);
        dispatcherSchedulerFactory.setJobFactory(jobFactory);
        return dispatcherSchedulerFactory;
    }

    /**
     * Creates a {@link Properties} bean populated with Quartz configuration settings
     * for the Dispatcher scheduler using Spring Boot's {@link ConfigurationProperties} mechanism.
     *
     * <h2>Example Configuration:</h2>
     * <pre>
     * spring.quartz.properties.dispatcher.org.quartz.scheduler.instanceName=DispatcherScheduler
     * spring.quartz.properties.dispatcher.org.quartz.threadPool.threadCount=10
     * </pre>
     *
     * @return a {@link Properties} object containing all Quartz properties for the Dispatcher scheduler
     */
    @ConfigurationProperties(prefix = "spring.quartz.properties.dispatcher")
    @Bean
    public Properties dispatcherQuartzProperties() {
        return new Properties(); // Spring will populate this bean automatically
    }

    /**
     * Exposes the Dispatcher {@link Scheduler} bean for job scheduling.
     *
     * @param factory the Dispatcher scheduler factory
     * @return the {@link Scheduler} instance for Dispatcher jobs
     * @throws Exception if the scheduler cannot be retrieved
     */
    @Bean(name = "dispatcherScheduler")
    public Scheduler dispatcherScheduler(
            @Qualifier("dispatcherSchedulerFactory") SchedulerFactoryBean factory)
            throws Exception {
        return factory.getScheduler();
    }

    // =========================================================
    // INGESTION SCHEDULER
    // =========================================================

    /**
     * Configures the {@link SchedulerFactoryBean} for the Ingestion scheduler
     * for non-test profiles (local, dev, int, uat, prod etc - anything that's not "test").
     *
     * @param quartzProps Quartz properties specific to the Ingestion scheduler
     * @param jobFactory  the custom job factory for Spring integration
     * @param dataSource  the Quartz data source
     * @return a configured {@link SchedulerFactoryBean} for Ingestion jobs
     */
    @Bean(name = "ingestionSchedulerFactory")
    @Profile("!test")
    public SchedulerFactoryBean ingestionSchedulerFactory(
            @Qualifier("ingestionQuartzProperties") Properties quartzProps,
            @Qualifier("jobFactory") JobFactory jobFactory,
            DataSource dataSource) {

        SchedulerFactoryBean ingestionSchedulerFactory = new SchedulerFactoryBean();
        ingestionSchedulerFactory.setDataSource(dataSource);
        ingestionSchedulerFactory.setQuartzProperties(quartzProps);
        ingestionSchedulerFactory.setJobFactory(jobFactory);
        return ingestionSchedulerFactory;
    }

    /**
     * Configures the {@link SchedulerFactoryBean} for the Ingestion scheduler
     * for "test" profile.
     *
     * @param quartzProps Quartz properties specific to the Ingestion scheduler
     * @param jobFactory  the custom job factory for Spring integration
     * @return a configured {@link SchedulerFactoryBean} for Ingestion jobs
     */
    @Bean(name = "ingestionSchedulerFactory")
    @Profile("test")
    public SchedulerFactoryBean testIngestionSchedulerFactory(
            @Qualifier("ingestionQuartzProperties") Properties quartzProps,
            @Qualifier("jobFactory") JobFactory jobFactory) {

        SchedulerFactoryBean ingestionSchedulerFactory = new SchedulerFactoryBean();
        ingestionSchedulerFactory.setQuartzProperties(quartzProps);
        ingestionSchedulerFactory.setJobFactory(jobFactory);
        return ingestionSchedulerFactory;
    }

    /**
     * Creates a {@link Properties} bean populated with Quartz configuration settings
     * for the Ingestion scheduler using Spring Boot's {@link ConfigurationProperties} mechanism.
     *
     * <p>This method binds all properties defined under the prefix
     * {@code spring.quartz.properties.ingestion} in the application.properties
     * {@link Properties} object. These properties are then used by the Quartz
     * {@link org.springframework.scheduling.quartz.SchedulerFactoryBean} to configure
     * the Ingestion scheduler instance.</p>
     *
     * <h2>Example Configuration:</h2>
     * <pre>
     * spring.quartz.properties.ingestion.org.quartz.scheduler.instanceName=IngestionScheduler
     * spring.quartz.properties.ingestion.org.quartz.threadPool.threadCount=3
     * </pre>
     *
     * @return a {@link Properties} object containing all Quartz properties for the Ingestion scheduler
     */
    @ConfigurationProperties(prefix = "spring.quartz.properties.ingestion")
    @Bean
    public Properties ingestionQuartzProperties() {
        return new Properties(); // Spring will populate this bean automatically
    }

    /**
     * Exposes the Ingestion {@link Scheduler} bean for job scheduling.
     *
     * @param factory the Ingestion scheduler factory
     * @return the {@link Scheduler} instance for Ingestion jobs
     * @throws Exception if the scheduler cannot be retrieved
     */
    @Bean(name = "ingestionScheduler")
    public Scheduler ingestionScheduler(
            @Qualifier("ingestionSchedulerFactory") SchedulerFactoryBean factory)
            throws Exception {
        return factory.getScheduler();
    }
}




=============================================

# ==================================================
# QUARTZ INGESTION SCHEDULER (Document ingestion, on-demand)
# ==================================================

# Unique scheduler name, SCHED_NAME column in QUARTZ tables. Serves as a mechanism for developers & support folks to distinguish schedulers
spring.quartz.properties.ingestion.org.quartz.scheduler.instanceName=IngestionScheduler

# Must be unique for all schedulers. Using 'AUTO' as the instanceId to get Quartz to auto generate the Id.
# INSTANCE_NAME column in the <tablePrefix>_SCHEDULER_STATE table and <tablePrefix>_FIRED_TRIGGERS table
spring.quartz.properties.ingestion.org.quartz.scheduler.instanceId=AUTO

# To instruct Quartz to wait for in progress jobs to complete before shutting down Quartz scheduler/application.
spring.quartz.properties.ingestion.org.quartz.scheduler.waitForJobsToComplete=true

# Thread pool size for ingestion jobs
spring.quartz.properties.ingestion.org.quartz.threadPool.threadCount=3

# JDBC JobStore for ingestion
spring.quartz.properties.ingestion.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.MSSQLDelegate

# Use different table prefix to avoid conflict with other scheduler
spring.quartz.properties.ingestion.org.quartz.jobStore.tablePrefix=QRTZ_

# Quartz's clustering features bring both high availability and scalability to the scheduler via fail-over and load balancing functionality
spring.quartz.properties.ingestion.org.quartz.jobStore.isClustered=true

# By default, Quartz uses StdRowLockSemaphore, which issues SQL like: SELECT * FROM QRTZ_LOCKS WHERE LOCK_NAME = ? FOR UPDATE
# SQL Server does not support FOR UPDATE outside cursors as the app is throwing errors like: FOR UPDATE clause allowed only for DECLARE CURSOR
# So, using UpdateLockRowSemaphore to change the locking strategy to use an UPDATE statement instead: UPDATE QRTZ_LOCKS SET LOCK_NAME = LOCK_NAME WHERE LOCK_NAME = ?
spring.quartz.properties.ingestion.org.quartz.jobStore.lockHandler.class=org.quartz.impl.jdbcjobstore.UpdateLockRowSemaphore

# ==================================================
# Properties used by our code to configure Ingestion job & triggers the way we want.
# ==================================================

# Job name prefix for document ingestion jobs. The logic appends a unique id to this prefix to uniquely identify each job
app.quartz.ingestion.jobNamePrefix=doc.ingestion
# Indicates whether to replace Ingestion job or not if one already exists in the job store with the same name
app.quartz.ingestion.replaceJob=true
# Indicates whether to recover Ingestion job or not if it's interrupted due to application/scheduler shutdown
app.quartz.ingestion.requestRecovery=true
# Max Retry Count for retrying quartz jobs in case of transient exceptions.
app.quartz.ingestion.maxRetryCount=3


==============================  QuartzJobSchedulerService

    package com.hanover.entp.dataext.service.async;

import com.hanover.entp.dataext.job.DataExtractionJob;
import com.hanover.entp.dataext.job.DocumentIngestionJob;
import com.hanover.entp.dataext.job.ResponseOrchestrationJob;
import com.hanover.entp.dataext.model.job.ExtractionJobMap;
import com.hanover.entp.dataext.model.job.RequestScopedJobParams;
import com.hanover.entp.dataext.util.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Uses two schedulers - one for scheduling on demand Data extraction jobs and the other for
 * scheduling recurring Response Aggregation & Dispatch jobs.
 * Uses quartz configuration properties from application.properties.
 */
@Service
@Slf4j
public class QuartzJobSchedulerService implements JobSchedulerService {

    // =========================================================
    // Dispatcher config
    // =========================================================

    /**
     * Indicates whether to replace Dispatcher job or not if one already exists in the job store
     */
    @Value("${app.quartz.dispatcher.replaceJob}")
    private boolean replaceDispatcherJob;

    /**
     * Indicates whether to store Dispatcher job or not in the job store when there are no active triggers.
     */
    @Value("${app.quartz.dispatcher.storeDurably}")
    private boolean storeDispatcherJobDurably;

    // =========================================================
    // Extraction config
    // =========================================================

    /**
     * Indicates whether to replace Extractor job or not if one already exists in the job store
     */
    @Value("${app.quartz.extraction.replaceJob}")
    private boolean replaceExtractionJob;

    /**
     * Indicates whether to recover Extractor job or not if it's interrupted due to application/scheduler shutdown
     */
    @Value("${app.quartz.extraction.requestRecovery}")
    private boolean requestExtractionJobRecovery;

    // =========================================================
    // Ingestion config
    // =========================================================

    /**
     * Indicates whether to replace Ingestion job or not if one already exists in the job store
     */
    @Value("${app.quartz.ingestion.replaceJob}")
    private boolean replaceIngestionJob;

    /**
     * Indicates whether to recover Ingestion job or not if it's interrupted due to application/scheduler shutdown
     */
    @Value("${app.quartz.ingestion.requestRecovery}")
    private boolean requestIngestionJobRecovery;

    // =========================================================
    // Scheduler beans
    // =========================================================

    /**
     * Used to schedule data extraction jobs
     */
    private final Scheduler extractionScheduler;

    /**
     * Used to schedule response aggregation & dispatch recurring job
     */
    private final Scheduler dispatcherScheduler;

    /**
     * Used to ingest document from incoming request sources.
     */
    private final Scheduler ingestionScheduler;

    // =========================================================
    // Constructor
    // =========================================================

    /**
     * Constructs QuartzJobSchedulerService with required dependencies.
     *
     * @param extractionScheduler  to schedule data extraction jobs
     * @param dispatcherScheduler  to schedule recurring response aggregation & dispatch job
     * @param ingestionScheduler   to schedule document ingestion jobs
     */
    public QuartzJobSchedulerService(
            final @Qualifier("extractionScheduler") Scheduler extractionScheduler,
            final @Qualifier("dispatcherScheduler") Scheduler dispatcherScheduler,
            final @Qualifier("ingestionScheduler")  Scheduler ingestionScheduler) {
        this.extractionScheduler = extractionScheduler;
        this.dispatcherScheduler = dispatcherScheduler;
        this.ingestionScheduler  = ingestionScheduler;
    }

    // =========================================================
    // scheduleDataExtractionJobsAsync
    // =========================================================

    /**
     * Schedules jobs in batch to avoid multiple trips by Quartz to the database.
     * Iterates through the list of jobs map to build Job Details & Trigger for each job.
     * Appends documentID passed in the job map for each item to create a unique job & trigger names.
     *
     * @param jobNamePrefix Prefix to the job names. Appended with the documentId to uniquely identify each job.
     *                      Goes in JOB_NAME column of the quartz tables. Also used as prefix for
     *                      JOB_GROUP, TRIGGER_NAME & TRIGGER_GROUP tables
     * @param jobs          List of documentId & job parameters objects to be used to create jobs in batch
     * @param delay         Duration after which the job needs to be scheduled, to facilitate scheduling
     *                      retries with backoff. The start time for the trigger.
     */
    @Override
    public void scheduleDataExtractionJobsAsync(
            String jobNamePrefix,
            List<ExtractionJobMap> jobs,
            Duration delay) throws SchedulerException {

        try {
            String jobGroup     = jobNamePrefix + ".group";
            String triggerGroup = jobNamePrefix + ".trigger.group";
            Map<JobDetail, Set<? extends Trigger>> jobsAndTriggers = new HashMap<>();

            for (ExtractionJobMap jobMap : jobs) {

                String triggerName      = jobNamePrefix + ".trigger." + jobMap.getDocumentID();
                String updatedJobName   = jobNamePrefix + ".job."     + jobMap.getDocumentID();
                JobKey    jobKey        = JobKey.jobKey(updatedJobName, jobGroup);
                TriggerKey triggerKey   = TriggerKey.triggerKey(triggerName, triggerGroup);

                JobDetail jobDetail = JobBuilder.newJob(DataExtractionJob.class)
                        .withIdentity(jobKey)
                        .setJobData(new JobDataMap(jobMap.getJobMap()))
                        .requestRecovery(requestExtractionJobRecovery)
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .forJob(jobDetail)
                        .startAt(Date.from(Instant.now().plus(delay)))
                        .build();

                jobsAndTriggers.put(jobDetail, Set.of(trigger));
            }

            extractionScheduler.scheduleJobs(jobsAndTriggers, replaceExtractionJob);

        } catch (SchedulerException exception) {
            throw new SchedulerException(
                "Failed to schedule Quartz job: " + jobNamePrefix, exception);
        }
    }

    // =========================================================
    // scheduleRecurringDispatcherJobWithCron
    // =========================================================

    /**
     * Schedules job that runs on schedule based on the cron expression provided.
     * Checks if a job already exists with the same jobName & group before creating a new one.
     * If one exists, updates the existing job by replacing the job.
     * Similarly, checks if a trigger already exists with the same trigger name & group before creating a new one.
     * If one exists, reschedules the job using the new trigger.
     *
     * @param jobName        Name of the job. Goes in JOB_NAME column of the quartz tables.
     *                       Also used as prefix for JOB_GROUP, TRIGGER_NAME & TRIGGER_GROUP tables
     * @param jobParameters  Data Map to be passed to the job
     * @param cronExpression Cron expression indicating the schedule
     */
    @Override
    public void scheduleRecurringDispatcherJobWithCron(
            String jobName,
            Map<String, Object> jobParameters,
            String cronExpression) {

        try {
            String triggerName  = jobName + ".trigger";
            String jobGroup     = jobName + ".group";
            String triggerGroup = jobName + ".trigger.group";

            JobKey    jobKey    = JobKey.jobKey(jobName, jobGroup);
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

            JobDetail jobDetail = JobBuilder.newJob(ResponseOrchestrationJob.class)
                    .withIdentity(jobKey)
                    .setJobData(new JobDataMap(jobParameters))
                    .storeDurably(storeDispatcherJobDurably) // So we can update the job on restart
                    .build();

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                    .cronSchedule(cronExpression)
                    .withMisfireHandlingInstructionDoNothing();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .withSchedule(scheduleBuilder)
                    .build();

            /*
             * Check if job exists
             */
            JobDetail existingJob = dispatcherScheduler.getJobDetail(jobKey);

            if (existingJob == null) {
                /*
                 * No job in DB. So, create new job + trigger
                 */
                dispatcherScheduler.scheduleJob(jobDetail, trigger);
                return;
            }

            /*
             * Job exists. So, update job definition (if changed).
             */
            dispatcherScheduler.addJob(jobDetail, replaceDispatcherJob); // "true" means replace existing

            /*
             * Update or create the trigger
             */
            Trigger existingTrigger = dispatcherScheduler.getTrigger(triggerKey);

            if (existingTrigger == null) {
                /*
                 * Create trigger if it doesn't exist
                 */
                dispatcherScheduler.scheduleJob(trigger);
            } else {
                /*
                 * Replace trigger on every restart to pick up code changes.
                 * Useful when the cron schedule was updated in prod.
                 */
                dispatcherScheduler.rescheduleJob(triggerKey, trigger);
            }

        } catch (SchedulerException exception) {
            throw new RuntimeException(
                "Failed to schedule recurring Quartz job: " + jobName, exception);
        }
    }

    // =========================================================
    // scheduleResponseAggregationJobAsync
    // =========================================================

    @Override
    public void scheduleResponseAggregationJobAsync(
            String jobNamePrefix,
            Map<String, Object> jobParameters) {

        try {
            String requestId    = String.valueOf(jobParameters.get(AppConstants.QUARTZ_REQUEST_ID));

            String jobGroup     = jobNamePrefix + ".group";
            String triggerGroup = jobNamePrefix + ".trigger.group";
            String triggerName  = jobNamePrefix + ".trigger" + requestId;
            String jobName      = jobNamePrefix + requestId;

            JobKey    jobKey    = JobKey.jobKey(jobName, jobGroup);
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

            JobDetail jobDetail = JobBuilder.newJob(ResponseOrchestrationJob.class)
                    .withIdentity(jobKey)
                    .setJobData(new JobDataMap(jobParameters))
                    .requestRecovery(true)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .startNow()
                    .build();

            dispatcherScheduler.scheduleJob(jobDetail, trigger);

        } catch (SchedulerException exception) {
            throw new RuntimeException(
                "Failed to schedule Quartz job: " + jobNamePrefix, exception);
        }
    }

    // =========================================================
    // scheduleDocumentIngestionJobAsync
    // =========================================================

    /**
     * Schedules a {@link DocumentIngestionJob} for immediate, one-shot execution.
     * Builds a JobDetail and a fire-once Trigger, then submits to the ingestionScheduler.
     * Checks if a job/trigger already exists and replaces/reschedules as needed,
     * consistent with the extraction and dispatcher scheduler patterns.
     *
     * @param jobNamePrefix Prefix for job/trigger/group names. Appended with requestId
     *                      to uniquely identify each job.
     * @param jobParams     {@link RequestScopedJobParams} containing source type, source location,
     *                      request context etc.
     * @param uniqueID      Unique ID for this data extraction run
     * @throws SchedulerException if the job cannot be scheduled
     */
    @Override
    public void scheduleDocumentIngestionJobAsync(
            String jobNamePrefix,
            RequestScopedJobParams jobParams,
            UUID uniqueID) throws SchedulerException {

        try {
            String requestId    = jobParams.getRequest().getReqHeader().getRequestID();

            String jobGroup     = jobNamePrefix + ".group";
            String triggerGroup = jobNamePrefix + ".trigger.group";
            String jobName      = jobNamePrefix + requestId;
            String triggerName  = jobNamePrefix + ".trigger." + requestId;

            JobKey    jobKey    = JobKey.jobKey(jobName, jobGroup);
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

            // Build JobDataMap carrying both jobParams and uniqueID
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("jobParams", jobParams);
            dataMap.put("dataExtractionUniqueID", uniqueID);

            JobDetail jobDetail = JobBuilder.newJob(DocumentIngestionJob.class)
                    .withIdentity(jobKey)
                    .setJobData(dataMap)
                    .requestRecovery(requestIngestionJobRecovery)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .startNow()
                    .build();

            /*
             * Check if job exists
             */
            JobDetail existingJob = ingestionScheduler.getJobDetail(jobKey);

            if (existingJob == null) {
                /*
                 * No job in DB. So, create new job + trigger
                 */
                ingestionScheduler.scheduleJob(jobDetail, trigger);
                return;
            }

            /*
             * Job exists. So, update job definition (if changed).
             */
            ingestionScheduler.addJob(jobDetail, replaceIngestionJob);

            /*
             * Update or create the trigger
             */
            Trigger existingTrigger = ingestionScheduler.getTrigger(triggerKey);

            if (existingTrigger == null) {
                /*
                 * Create trigger if it doesn't exist
                 */
                ingestionScheduler.scheduleJob(trigger);
            } else {
                /*
                 * Replace trigger on every restart to pick up any changes.
                 */
                ingestionScheduler.rescheduleJob(triggerKey, trigger);
            }

        } catch (SchedulerException exception) {
            throw new SchedulerException(
                "Failed to schedule document ingestion Quartz job: " + jobNamePrefix, exception);
        }
    }
}


============================JobSchedulerService

package com.hanover.entp.dataext.service.async;

import com.hanover.entp.dataext.model.job.ExtractionJobMap;
import com.hanover.entp.dataext.model.job.RequestScopedJobParams;
import org.quartz.SchedulerException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface JobSchedulerService {

    /**
     * Schedules data extraction jobs in batch.
     *
     * @param jobNamePrefix Prefix to the job names
     * @param jobs          List of documentId & job parameters objects
     * @param delay         Duration after which the job needs to be scheduled
     */
    void scheduleDataExtractionJobsAsync(
            String jobNamePrefix,
            List<ExtractionJobMap> jobs,
            Duration delay) throws SchedulerException;

    /**
     * Schedules a recurring dispatcher job using a cron expression.
     *
     * @param jobName        Name of the job
     * @param jobParameters  Data Map to be passed to the job
     * @param cronExpression Cron expression indicating the schedule
     */
    void scheduleRecurringDispatcherJobWithCron(
            String jobName,
            Map<String, Object> jobParameters,
            String cronExpression);

    /**
     * Schedules a one-shot response aggregation job for immediate execution.
     *
     * @param jobNamePrefix  Prefix for job/trigger/group names
     * @param jobParameters  Data Map to be passed to the job
     */
    void scheduleResponseAggregationJobAsync(
            String jobNamePrefix,
            Map<String, Object> jobParameters);

    /**
     * Schedules a {@link com.hanover.entp.dataext.job.DocumentIngestionJob}
     * for immediate, one-shot execution via the ingestion scheduler.
     *
     * @param jobNamePrefix Prefix for job/trigger/group names. Appended with requestId
     *                      to uniquely identify each job.
     * @param jobParams     {@link RequestScopedJobParams} containing source type,
     *                      source location, request context etc.
     * @param uniqueID      Unique ID for this data extraction run
     * @throws SchedulerException if the job cannot be scheduled
     */
    void scheduleDocumentIngestionJobAsync(
            String jobNamePrefix,
            RequestScopedJobParams jobParams,
            UUID uniqueID) throws SchedulerException;
}

====================== DocumentIngestionJob

package com.hanover.entp.dataext.job;

import com.hanover.entp.dataext.model.api.aiextraction.AIExtractionRequest;
import com.hanover.entp.dataext.model.data.prompt.PromptModel;
import com.hanover.entp.dataext.model.job.RequestScopedJobParams;
import com.hanover.entp.dataext.service.PromptService;
import com.hanover.entp.dataext.service.dataextraction.IngestionStrategy;
import com.hanover.entp.dataext.service.dataextraction.IngestionStrategyResolver;
import com.hanover.entp.dataext.service.dataextraction.SourceType;
import com.hanover.entp.dataext.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIngestionJob implements Job {

    private final IngestionStrategyResolver ingestionStrategyResolver;
    private final PromptService promptService;

    /**
     * Executes the document ingestion job.
     * <p>
     * Retrieves {@link RequestScopedJobParams} and the data extraction unique ID
     * from the {@link JobDataMap}, resolves the appropriate {@link IngestionStrategy}
     * based on the source type, and calls {@link IngestionStrategy#ingest} to produce
     * a list of {@link StorageService.StoredFileInfo} objects.
     *
     * @param context the Quartz job execution context
     * @throws JobExecutionException if the job fails; non-retryable for unsupported
     *                               source types, retryable for all other exceptions
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap dataMap = context.getMergedJobDataMap();
        RequestScopedJobParams jobParams =
                (RequestScopedJobParams) dataMap.get("jobParams");
        UUID dataExtractionUniqueID =
                (UUID) dataMap.get("dataExtractionUniqueID");

        AIExtractionRequest request = jobParams.getRequest();

        log.info("DocumentIngestionJob started for requestID: {}, uniqueID: {}",
                request.getReqHeader().getRequestID(),
                dataExtractionUniqueID);

        try {
            // Resolve ingestion strategy based on source type
            IngestionStrategy ingestionStrategy = ingestionStrategyResolver.resolve(
                    SourceType.fromValue(
                            request.getData().getDocumentSource().getStorageType()
                    )
            );

            // Load the prompt model
            PromptModel promptModel = promptService.getPromptByIdChecked(
                    request.getData().getPromptID()
            );

            // Ingest documents — returns list of StoredFileInfo objects
            List<StorageService.StoredFileInfo> storedFiles = ingestionStrategy.ingest(
                    request,
                    dataExtractionUniqueID,
                    promptModel
            );

            log.info("DocumentIngestionJob completed for requestID: {}, uniqueID: {}. " +
                            "Files ingested: {}",
                    request.getReqHeader().getRequestID(),
                    dataExtractionUniqueID,
                    storedFiles.size());

        } catch (UnsupportedOperationException e) {
            log.error("No ingestion strategy found for sourceType. requestID: {}, uniqueID: {}",
                    request.getReqHeader().getRequestID(), dataExtractionUniqueID, e);
            // false = do NOT re-fire; unsupported source type won't fix itself on retry
            throw new JobExecutionException(e, false);
        } catch (Exception e) {
            log.error("DocumentIngestionJob failed for requestID: {}, uniqueID: {}",
                    request.getReqHeader().getRequestID(), dataExtractionUniqueID, e);
            // true = re-fire; transient failures (network, DB) may succeed on retry
            throw new JobExecutionException(e, true);
        }
    }
}    


    



    
