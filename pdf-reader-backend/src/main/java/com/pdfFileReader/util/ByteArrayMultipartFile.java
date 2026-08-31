package com.pdfFileReader.util;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.NonNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP istegi bittikten sonra da (asenkron kuyrukta) kullanilabilmesi icin
 * dosya icerigini byte[] olarak tasiyan MultipartFile.
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String originalFilename;
    private final String contentType;

    public ByteArrayMultipartFile(byte[] content, String originalFilename, String contentType) {
        this.content = content.clone();
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    @NonNull
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    @NonNull
    public byte[] getBytes() {
        return content.clone();
    }

    @Override
    @NonNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(@NonNull File dest) throws IOException {
        java.nio.file.Files.write(dest.toPath(), content);
    }
}
