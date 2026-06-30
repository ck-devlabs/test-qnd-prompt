
@Override
public void scheduleDocumentIngestionJobAsync(
        String jobNamePrefix,
        RequestScopedJobParams jobParams,
        PromptModel promptModel) throws SchedulerException {

    try {
        String requestId    = jobParams.getClientRequestId().toString();

        String jobGroup     = jobNamePrefix + ".group";
        String triggerGroup = jobNamePrefix + ".trigger.group";
        String jobName      = jobNamePrefix + requestId;
        String triggerName  = jobNamePrefix + ".trigger." + requestId;

        JobKey    jobKey    = JobKey.jobKey(jobName, jobGroup);
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

        // Flatten jobParams into primitive map entries — JDBC JobStore requires
        // everything in JobDataMap to be Serializable, and RequestScopedJobParams
        // (and PromptModel) are not. Use the same AppConstants.QUARTZ_* keys that
        // RequestScopedJobParams(JobDataMap) already knows how to reconstruct from.
        JobDataMap dataMap = jobParams.getJobDataMap();
        dataMap.put(AppConstants.QUARTZ_PROMPT_ID, jobParams.getPromptId()); // promptID already flattened; promptModel itself is NOT put in the map

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

        JobDetail existingJob = ingestionScheduler.getJobDetail(jobKey);

        if (existingJob == null) {
            ingestionScheduler.scheduleJob(jobDetail, trigger);
            return;
        }

        ingestionScheduler.addJob(jobDetail, replaceIngestionJob);

        Trigger existingTrigger = ingestionScheduler.getTrigger(triggerKey);
        if (existingTrigger == null) {
            ingestionScheduler.scheduleJob(trigger);
        } else {
            ingestionScheduler.rescheduleJob(triggerKey, trigger);
        }

    } catch (SchedulerException exception) {
        throw new SchedulerException(
            "Failed to schedule document ingestion Quartz job: " + jobNamePrefix, exception);
    }
}
