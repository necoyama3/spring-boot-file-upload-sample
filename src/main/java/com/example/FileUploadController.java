package com.example;

import static java.nio.file.StandardOpenOption.*;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class FileUploadController {

	private final static String SUCCESS_MESSAGE = "OK";
	private final static String FAIL_MESSAGE = "NG";
	private final static String EMPTY_MESSAGE = "You failed to upload because the file was empty.";
	private final static File countFile = new File("count.txt");
	private final static String COUNT_FILE_PERMISSION = "rwxr-xr-x";

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public @ResponseBody String provideUploadInfo() {
		return "You can upload a file by posting to this same URL.";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public @ResponseBody String handleFileUpload(
			@RequestParam("id") String id,
			@RequestParam("image") MultipartFile file,
			@RequestParam("resetCount") Optional<Boolean> resetCount) {
		
		Logger logger = Logger.getLogger(this.getClass());
		
		boolean isResetCount = resetCount.orElse(false);
		if (isResetCount && resetCount.get()) {
			isResetCount = true;
		}

		logger.info("id = " + id);
		logger.info("file name = " + file.getOriginalFilename());
		logger.info("file size = " + file.getSize());
		logger.info("resetCount = " + isResetCount);

		RequestInfo requestInfo = new RequestInfo();
		requestInfo.setId(getSeq(isResetCount));
		requestInfo.setResult(true);
		requestInfo.setMessage(SUCCESS_MESSAGE);

		if (!file.isEmpty()) {
			String userProfile = System.getProperty("user.home");
			try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(userProfile + "/" + file.getOriginalFilename()))) {
				byte[] bytes = file.getBytes();
				stream.write(bytes);
				return toJson(requestInfo);
			} catch (Exception e) {
				requestInfo.setResult(false);
				requestInfo.setMessage(FAIL_MESSAGE);
				return toJson(requestInfo);
			}
		} else {
			requestInfo.setResult(false);
			requestInfo.setMessage(EMPTY_MESSAGE);
			return toJson(requestInfo);
		}
	}

	private int getSeq(boolean isResetCount) {
		int count = 1;
		try {
			Path path = countFile.toPath();
			if (Files.exists(path) && !isResetCount) {
				count = Files.readAllLines(path, Charset.defaultCharset())
						.stream().mapToInt(Integer::parseInt)
						.findFirst()
						.getAsInt();
				count += 1;
			} else if (!Files.exists(path)) {
				Set<PosixFilePermission> filePermission = PosixFilePermissions.fromString(COUNT_FILE_PERMISSION);
				FileAttribute<Set<PosixFilePermission>> attribute = PosixFilePermissions.asFileAttribute(filePermission);
				Files.createFile(path, attribute);
			}
			writeCount(path, Integer.toString(count));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return count;
	}

	private void writeCount(Path path, String count) {
		try (BufferedWriter bw = Files.newBufferedWriter(path, CREATE, TRUNCATE_EXISTING);) {
			bw.write(count);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String toJson(RequestInfo requestInfo) {
		ObjectMapper mapper = new ObjectMapper();
		String json = "";
		try {
			json = mapper.writeValueAsString(requestInfo);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return json;
	}

}
