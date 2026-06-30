
Map<String, Object> jobMap = jobParams.toMap();
jobMap.put("storedFile.path", storedFileInfo.getPath());
jobMap.put("storedFile.fileName", storedFileInfo.getFileName());
jobMap.put("storedFile.size", storedFileInfo.getSize());
jobMap.put("storedFile.mimeType", storedFileInfo.getMimeType());
