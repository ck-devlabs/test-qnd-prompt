
@Override
public void execute(JobExecutionContext context) throws JobExecutionException {

    JobDataMap dataMap = context.getMergedJobDataMap();
    RequestScopedJobParams jobParams = (RequestScopedJobParams) dataMap.get("jobParams");
    UUID serverRequestUID = (UUID) dataMap.get("dataExtractionUniqueID");
    PromptModel promptModel = (PromptModel) dataMap.get("promptModel");

    AIExtractionRequest request = jobParams.getRequest();

    try {
        // ---- THIS is where IngestionStrategy is used ----
        IngestionStrategy ingestionStrategy = ingestionStrategyResolver.resolve(
                SourceType.fromValue(request.getData().getDocumentSource().getStorageType())
        );

        List<StorageService.StoredFileInfo> supportedDocumentsToExtract =
                ingestionStrategy.ingest(request, serverRequestUID, promptModel);

        // Update request_state with doc count now that ingestion is done
        transactionStMgmtService.updateRequestStateForDocCountAndStatus(
                serverRequestUID, AppConstants.EXTRACTION_PENDING, supportedDocumentsToExtract.size()
        );

        // Now schedule the extraction job(s)
        jobSchedulerService.scheduleDataExtractionJob(jobParams, serverRequestUID, supportedDocumentsToExtract);

    } catch (UnsupportedOperationException e) {
        log.error("No ingestion strategy found. requestID: {}, uniqueID: {}",
                request.getReqHeader().getRequestID(), serverRequestUID, e);
        throw new JobExecutionException(e, false);
    } catch (Exception e) {
        log.error("DocumentIngestionJob failed. requestID: {}, uniqueID: {}",
                request.getReqHeader().getRequestID(), serverRequestUID, e);
        throw new JobExecutionException(e, true);
    }
}
