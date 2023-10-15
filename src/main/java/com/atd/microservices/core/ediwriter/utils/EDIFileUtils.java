package com.atd.microservices.core.ediwriter.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EDIFileUtils {

	public static String getFileAsString(String name) {
		Resource modelResolverYaml = new ClassPathResource(name);
		String yamlString = null;
		try (Reader reader = new InputStreamReader(modelResolverYaml.getInputStream())) {
			yamlString = FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return yamlString;
	}

}
