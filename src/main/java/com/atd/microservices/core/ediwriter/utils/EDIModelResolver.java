package com.atd.microservices.core.ediwriter.utils;

import com.atd.microservices.core.ediwriter.service.EDIWriterMetrics;
import com.berryworks.edimodel.serial.EdiModelDeserializer;
import com.berryworks.edireader.model.EdiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class EDIModelResolver {
	private Map<String, EdiModel> ediModelCache = new HashMap<>();
	
	@Autowired
	private EDIWriterMetrics ediWriterMetrics;

	public EdiModel getEDIModel(String docType, String version) {
		String key = docType + "_" + version;
		if (ediModelCache.get(key) != null) {
			return ediModelCache.get(key);
		} else {
			return createModel(key, docType, version);
		}
	}

	private EdiModel createModel(String key, String docType, String version) {
		String yamlString = null;
		EdiModel model = new EdiModel();
		try {
			yamlString = EDIFileUtils.getFileAsString(getFileName(docType, version.substring(2)));
		} catch(Exception e) {
			log.error("Error loading model from classpath : {}", e.getMessage());
		}
		ediWriterMetrics.increaseTotalTypeMsgCount(docType);
		if (null != yamlString) {
			try (StringReader yamlReader = new StringReader(yamlString)) {
				EdiModelDeserializer deserializer = new EdiModelDeserializer(yamlReader);
				model = deserializer.deserialize();
				ediModelCache.put(key, model);
			}
		}
		return model;
	}

	private String getFileName(String docType, String version) {
		return "modelresolvers/model-" + docType + "-" + version + "-e.yaml";
	}
}
