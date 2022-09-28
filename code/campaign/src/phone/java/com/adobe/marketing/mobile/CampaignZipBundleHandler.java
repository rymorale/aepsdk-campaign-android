/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import java.io.*;

class CampaignZipBundleHandler implements CampaignRulesRemoteDownloader.RulesBundleNetworkProtocolHandler {

	private static final String LOG_TAG = CampaignZipBundleHandler.class.getSimpleName();
	private static final String DEFAULT_CHARSET = "UTF-8";
	private static final String META_FILE_NAME = "meta.txt";
	private CompressedFileService compressedFileService;
	private static final int TOKENS_SIZE_WITH_ETAG = 3;

	static class ZipMetadata implements CampaignRulesRemoteDownloader.Metadata {
		private static final String SEPARATOR = "|";
		private static final String REGEX = "\\" + SEPARATOR;

		private long lastModifiedDate;
		private long size;
		private String etag;

		public String getETag() {
			return etag;
		}

		void setETag(final String etag) {
			this.etag = etag;
		}

		@Override
		public long getLastModifiedDate() {
			return lastModifiedDate;
		}

		void setLastModifiedDate(final long lastModifiedDate) {
			this.lastModifiedDate = lastModifiedDate;
		}

		@Override
		public long getSize() {
			return size;
		}

		void setSize(final long size) {
			this.size = size;
		}

		@Override
		public String toString() {
			return lastModifiedDate + SEPARATOR + size + (etag != null ?  SEPARATOR + etag : "") + SEPARATOR;
		}

		static ZipMetadata getMetadataFromString(final String string) {
			if (string == null) {
				Log.debug(LOG_TAG, "getMetadataFromString -  File path to rules json is null, returning null meta data.");
				return null;
			}

			ZipMetadata metadata = null;

			String [] tokens = string.split(REGEX);

			try {
				boolean didExtractTokens = false;

				if (tokens.length >= 2) {
					metadata = new ZipMetadata();
					metadata.setLastModifiedDate(Long.parseLong(tokens[0]));
					metadata.setSize(Long.parseLong(tokens[1]));
					didExtractTokens = true;
				}

				if (tokens.length >= TOKENS_SIZE_WITH_ETAG) { // Check if ETag is present in the filename.
					metadata.setETag(tokens[2]);
				}

				if (!didExtractTokens) {
					Log.trace(LOG_TAG, "getMetadataFromString -  Could not de-serialize metadata!");
				}
			} catch (NumberFormatException ne) {
				Log.error(LOG_TAG, "Could not read metadata for rules json (%s)", ne);
				metadata = null;
			}

			return metadata;
		}
	}

	CampaignZipBundleHandler(final CompressedFileService compressedFileService) throws MissingPlatformServicesException {
		if (compressedFileService == null) {
			throw new MissingPlatformServicesException("CompressedFileService is null, Rules Engine needs zip support for downloading rules!");
		}

		this.compressedFileService = compressedFileService;
	}

	@Override
	public boolean processDownloadedBundle(final File downloadedBundle, final String outputPath,
										   final long lastModifiedDateForBundle, final String etag) {

		if (downloadedBundle == null || outputPath == null) {
			Log.debug(LOG_TAG,
					  "processDownloadedBundle -  Cannot process downloaded bundle, either downloaded file ot output path is null.");
			return false;
		}

		boolean extracted =  compressedFileService.extract(downloadedBundle, CompressedFileService.FileType.ZIP, outputPath);

		if (extracted) {
			//To complete the "move"
			long sizeOfUnprocessedBundle = downloadedBundle.length();

			try {
				//Create metadata to be needed later
				createMetadata(outputPath, sizeOfUnprocessedBundle, lastModifiedDateForBundle, etag);
			} catch (IOException ex) {
				Log.trace(LOG_TAG, "Could not create metadata for the downloaded rules [%s]", ex);
			}
		}

		//Even if the extraction failed, we should delete the downloaded bundled file
		if (!downloadedBundle.delete()) {
			Log.debug(LOG_TAG, "Unable to delete the zip bundle : %s", downloadedBundle.getName());
		} else {
			Log.trace(LOG_TAG, "processDownloadedBundle -  Deleted downloaded bundle after processing.");
		}


		return extracted;
	}

	private void createMetadata(final String parentDirectory, final long bundleSize,
								final long lastModifiedDate, final String etag) throws IOException {
		File metadataFile = new File(parentDirectory, META_FILE_NAME);

		ZipMetadata metadata = new ZipMetadata();
		metadata.setSize(bundleSize);
		metadata.setLastModifiedDate(lastModifiedDate);
		metadata.setETag(etag);

		FileOutputStream fileOutputStream = null;

		try {
			fileOutputStream = new FileOutputStream(metadataFile);
			fileOutputStream.write(metadata.toString().getBytes(DEFAULT_CHARSET));
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (Exception e) {
					Log.trace(LOG_TAG, "createMetadata -  Failed to close the stream for %s", metadataFile);
				}
			}
		}
	}

	@Override
	public CampaignRulesRemoteDownloader.Metadata getMetadata(final File cachedBundlePath) {
		File metaFile = new File(cachedBundlePath, META_FILE_NAME);
		return ZipMetadata.getMetadataFromString(FileUtil.readStringFromFile(metaFile));

	}
}

