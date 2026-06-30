
@Override
public void scheduleDocumentIngestionJobAsync(
        String jobNamePrefix,
        RequestScopedJobParams jobParams) throws SchedulerException {

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
        // is not. writeTo()/toMap() already produce only Serializable String entries.
        JobDataMap dataMap = new JobDataMap(jobParams.toMap());

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



private List<ExtractionJobMap> buildExtractionJobMaps(
        RequestScopedJobParams jobParams,
        List<StorageService.StoredFileInfo> supportedDocumentsToExtract) {

    return supportedDocumentsToExtract.stream()
            .map(storedFileInfo -> {
                UUID documentID = UUID.randomUUID();

                Map<String, Object> jobMap = jobParams.toMap();
                jobMap.put("storedFileInfo", storedFileInfo);

                return new ExtractionJobMap(documentID, jobMap);
            })
            .toList();
}
