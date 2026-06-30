
@Override
public void scheduleDocumentIngestionJobAsync(
        String jobNamePrefix,
        RequestScopedJobParams jobParams,
        UUID uniqueID,
        PromptModel promptModel) throws SchedulerException {

    try {
        String requestId    = jobParams.getRequest().getReqHeader().getRequestID();

        String jobGroup     = jobNamePrefix + ".group";
        String triggerGroup = jobNamePrefix + ".trigger.group";
        String jobName      = jobNamePrefix + requestId;
        String triggerName  = jobNamePrefix + ".trigger." + requestId;

        JobKey    jobKey    = JobKey.jobKey(jobName, jobGroup);
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroup);

        // Build JobDataMap carrying jobParams, uniqueID, and promptModel
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobParams", jobParams);
        dataMap.put("dataExtractionUniqueID", uniqueID);
        dataMap.put("promptModel", promptModel);

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



======= Wrapper -- AIExtraction

  /**
 * Schedules a DocumentIngestionJob asynchronously via Quartz, passing along the request,
 * generated UUID, and resolved PromptModel so the job has everything it needs to perform
 * ingestion and subsequently trigger extraction scheduling.
 *
 * @param jobParameters    RequestScopedJobParams containing the request and serverRequestUID
 * @param serverRequestUID Unique ID for this data extraction run
 * @param promptModel      The resolved PromptModel for this request
 * @throws JobSchedulerException if the job cannot be scheduled
 */
private void scheduleDocumentIngestionJobAsync(
        RequestScopedJobParams jobParameters,
        UUID serverRequestUID,
        PromptModel promptModel) throws JobSchedulerException {

    try {
        jobSchedulerService.scheduleDocumentIngestionJobAsync(
                ingestionJobNamePrefix,
                jobParameters,
                serverRequestUID,
                promptModel
        );
    } catch (SchedulerException ex) {
        throw new JobSchedulerException(
            "Failed to schedule document ingestion job for requestID: " + serverRequestUID, ex);
    }
}




public AIExtractionResponse processRequest(AIExtractionRequest request) throws ValidationException, PromptException,
        StorageAccessException, EmailValidationException, LLMException, PremiumExtractionException, JobSchedulerException {

    /*
     * Generate a unique identifier to identify this request everywhere in the application.
     */
    UUID serverRequestUID = UUID.randomUUID();
    int httStatusInACK = HttpStatus.OK.value(); // Default to 200 OK, can be updated in case of exceptions during processing to reflect the appropriate error status code in the
    String errorCdInACK = null; // Default to null, can be updated in case of exceptions during processing to reflect the appropriate error code in the ACK response
    String errorMsgInACK = null; // Default to null, can be updated in case of exceptions during processing to reflect the appropriate error message in the ACK response
    String updatedStatus = AppConstants.EXTRACTION_PENDING; // Default to EXTRACTION_SCHEDULED, can be updated in case of exceptions during processing to reflect t

    try {

        log.info("Generated UUID {} for request : {}", serverRequestUID, request.getReqHeader().requestID);

        AIExtractionResponse response = AIExtractionResponse.initResponse(serverRequestUID);

        // Insert entry into request_state table
        transactionStMgmtService.insertRequestState(request, serverRequestUID);

        PromptModel promptModel = promptService.getPromptByIdChecked(request.getData().getPromptID());

        // Build job params up front so ingestion job has everything it needs
        RequestScopedJobParams jobParameters = new RequestScopedJobParams(request, serverRequestUID);

        // Schedule document ingestion job asynchronously via Quartz (replaces direct ingestionStrategy.ingest() call)
        scheduleDocumentIngestionJobAsync(jobParameters, serverRequestUID, promptModel);

        return response;

    } catch(DataAccessException ex) {
        if(ErrorUtils.isRetryable(ex)) {
            errorMsgInACK = String.format("Transient database error while updating request_state for requestID: %s. This is expected to be retried. ", serverRequestUID);
            log.error(errorMsgInACK, ex);
            // ... existing error handling continues
        }
    }
    // ... rest of catch blocks unchanged
}

  







