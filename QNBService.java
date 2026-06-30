
private List<ExtractionJobMap> buildExtractionJobMaps(
        RequestScopedJobParams jobParams,
        List<StorageService.StoredFileInfo> supportedDocumentsToExtract) {

    return supportedDocumentsToExtract.stream()
            .map(storedFileInfo -> {
                UUID documentID = UUID.randomUUID();

                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put(AppConstants.QUARTZ_REQUEST_ID, jobParams.getServerRequestId().toString());
                jobMap.put(AppConstants.QUARTZ_CLIENT_REQUEST_ID, jobParams.getClientRequestId().toString());
                jobMap.put(AppConstants.QUARTZ_CLIENT_ITEM_ID, jobParams.getClientItemId());
                jobMap.put(AppConstants.QUARTZ_CLIENT_TRACE_ID, jobParams.getClientTraceId().toString());
                jobMap.put(AppConstants.QUARTZ_CLIENT_APP_CODE, jobParams.getClientAppCode());
                jobMap.put(AppConstants.QUARTZ_STORAGE_URL, jobParams.getStorageURL());
                jobMap.put(AppConstants.QUARTZ_CALLBACK_URL, jobParams.getCallbackURL());
                jobMap.put(AppConstants.QUARTZ_PROMPT_ID, jobParams.getPromptId());
                jobMap.put("storedFileInfo", storedFileInfo);

                return new ExtractionJobMap(documentID, jobMap);
            })
            .toList();
}
