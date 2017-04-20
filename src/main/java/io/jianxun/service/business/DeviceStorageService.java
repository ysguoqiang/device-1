package io.jianxun.service.business;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import io.jianxun.service.BusinessException;

@Component
public class DeviceStorageService {

	private final Path rootLocation;

	private DeviceStorageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
	}

	public Path getRootLocation() {
		return rootLocation;
	}

	public void store(MultipartFile file) {
		try {
			if (file.isEmpty()) {
				throw new BusinessException("Failed to store empty file " + file.getOriginalFilename());
			}
			Files.copy(file.getInputStream(), this.rootLocation.resolve(file.getOriginalFilename()),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new BusinessException("Failed to store file " + file.getOriginalFilename(), e);
		}
	}

	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new BusinessException("Could not read file: " + filename);

			}
		} catch (MalformedURLException e) {
			throw new BusinessException("Could not read file: " + filename, e);
		}
	}

}
