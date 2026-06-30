
package com.hanover.entp.dataext.job;

import com.hanover.entp.dataext.model.data.prompt.PromptModel;
import com.hanover.entp.dataext.model.job.ExtractionJobMap;
import com.hanover.entp.dataext.model.job.RequestScopedJobParams;
import com.hanover.entp.dataext.service.PromptService;
import com.hanover.entp.dataext.service.async.JobSchedulerService;
import com.hanover.entp.dataext.service.dataextraction.IngestionStrategy;
import com.hanover.entp.dataext.service.dataextraction.IngestionStrategyResolver;
import com.hanover.entp.dataext.service.dataextraction.SourceType;
import com.hanover.entp.dataext.service.storage.StorageService;
import com.hanover.entp.dataext.service.transaction.TransactionStMgmtService;
import com.hanover.entp.dataext.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quartz job that performs document ingestion for a given AI extraction request.
 * <p>
 * Runs asynchronously on the ingestion scheduler's thread pool, decoupled from the
 * original request thread. After ingestion completes, it updates request_state with
 * the document count and schedules the downstream data extraction job(s), one per
 * ingested document.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIngestionJob implements Job {

    private final IngestionStrategyResolver ingestionStrategyResolver;
    private final TransactionStMgmtService transactionStMgmtService;
    private final JobSchedulerService jobSchedulerService;
    private final PromptService promptService;

    /**
     * Job name prefix used for the downstream data extraction job, appended with a
     * unique id per document to identify each extraction job.
     */
    @Value("${app.quartz.extraction.jobNamePrefix}")
    private String extractionJobNamePrefix;

    /**
     * Executes the document ingestion job.
     * <p>
     * Reconstructs {@link RequestScopedJobParams} from the Quartz {@link JobDataMap},
     * resolves the appropriate {@link IngestionStrategy} based on source type, ingests
     * documents, updates request_state with the resulting document count, and schedules
     * the data extraction job(s) for the ingested documents.
     *
     * @param context the Quartz job execution context
     * @throws JobExecutionException if the job fails; non-retryable for unsupported
     *                               source types, retryable for all other exceptions
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap dataMap = context.getMergedJobDataMap();

        // Reconstruct flattened job params straight from the Quartz JobDataMap
        RequestScopedJobParams jobParams = new RequestScopedJobParams(dataMap);
        UUID serverRequestUID = jobParams.getServerRequestId();

        log.info("DocumentIngestionJob started for requestID: {}, uniqueID: {}",
                jobParams.getClientRequestId(),
                serverRequestUID);

        try {
            // Load the prompt model (not carried in JobDataMap, re-fetched here)
            PromptModel promptModel = promptService.getPromptByIdChecked(jobParams.getPromptId());

            // Resolve ingestion strategy based on source type.
            // NOTE: storageType isn't currently stored on RequestScopedJobParams
            // (it's commented out in the constructor) - add it there if needed,
            // or otherwise re-derive it for this job to call resolve() correctly.
            IngestionStrategy ingestionStrategy = ingestionStrategyResolver.resolve(
                    SourceType.fromValue(jobParams.getStorageType())
            );

            // Ingest documents — returns list of StoredFileInfo objects
            List<StorageService.StoredFileInfo> supportedDocumentsToExtract =
                    ingestionStrategy.ingest(jobParams, serverRequestUID, promptModel);

            // Update request_state table to indicate status of REQUEST_VALIDATED
            // and record the number of documents to extract
            transactionStMgmtService.updateRequestStateForDocCountAndStatus(
                    serverRequestUID,
                    AppConstants.EXTRACTION_PENDING,
                    supportedDocumentsToExtract.size()
            );

            // Schedule the downstream data extraction job(s) now that ingestion is complete
            List<ExtractionJobMap> extractionJobMaps =
                    buildExtractionJobMaps(jobParams, supportedDocumentsToExtract);

            jobSchedulerService.scheduleDataExtractionJobsAsync(
                    extractionJobNamePrefix,
                    extractionJobMaps,
                    Duration.ZERO
            );

            log.info("DocumentIngestionJob completed for requestID: {}, uniqueID: {}. " +
                            "Files ingested: {}",
                    jobParams.getClientRequestId(),
                    serverRequestUID,
                    supportedDocumentsToExtract.size());

        } catch (UnsupportedOperationException e) {
            log.error("No ingestion strategy found for sourceType. requestID: {}, uniqueID: {}",
                    jobParams.getClientRequestId(), serverRequestUID, e);
            // false = do NOT re-fire; unsupported source type won't fix itself on retry
            throw new JobExecutionException(e, false);
        } catch (Exception e) {
            log.error("DocumentIngestionJob failed for requestID: {}, uniqueID: {}",
                    jobParams.getClientRequestId(), serverRequestUID, e);
            // true = re-fire; transient failures (network, DB) may succeed on retry
            throw new JobExecutionException(e, true);
        }
    }

    /**
     * Builds the list of {@link ExtractionJobMap} objects required by
     * {@link JobSchedulerService#scheduleDataExtractionJobsAsync}, one per ingested document.
     * <p>
     * Each document gets its own generated UUID (StoredFileInfo has no inherent ID),
     * used to uniquely identify the extraction job/trigger for that document. The flattened
     * {@link RequestScopedJobParams} is re-serialized via {@code getJobDataMap()} so it
     * survives Quartz's JobDataMap requirements, consistent with how this job itself
     * was invoked.
     *
     * @param jobParams                    the request-scoped job params for this run
     * @param supportedDocumentsToExtract  the list of ingested documents
     * @return list of job maps, one per document, ready for batch scheduling
     */
    private List<ExtractionJobMap> buildExtractionJobMaps(
            RequestScopedJobParams jobParams,
            List<StorageService.StoredFileInfo> supportedDocumentsToExtract) {

        return supportedDocumentsToExtract.stream()
                .map(storedFileInfo -> {
                    UUID documentID = UUID.randomUUID();

                    Map<String, Object> jobMap = new HashMap<>(jobParams.getJobDataMap());
                    jobMap.put("storedFileInfo", storedFileInfo);

                    return new ExtractionJobMap(documentID, jobMap);
                })
                .toList();
    }
}
